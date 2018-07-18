package info.s5argon.groovecoastercontroller

import android.bluetooth.BluetoothSocket
import android.view.MotionEvent
import android.widget.ImageView

/**
 * Decide and gives motion to the correct booster based on captured touch screen movement.
 * Also push data to Bluetooth stream
 */
class GroovePointTracker(private val mainActivity: MainActivity)  {

    private val boosterL: Booster = Booster(false)
    private val boosterR: Booster = Booster(true)

    class TrackingPoint
    {
        var boundedTouchId : Int = -1
        var previousX : Float = -1f
        var previousY : Float = -1f

        val isBounded : Boolean get() = boundedTouchId != -1

        fun reset()
        {
            boundedTouchId = -1
            previousX = -1f
            previousY = -1f
        }

        fun bound(touchId : Int, x : Float, y : Float)
        {
            previousX = x
            previousY = y
            boundedTouchId = touchId
        }

        fun updatePosition(x: Float, y: Float) : Pair<Float,Float>
        {
            val diffX = x - previousX
            val diffY = y - previousY
            previousX = x
            previousY = y
            return Pair(diffX, diffY)
        }
    }

    val leftTracking : TrackingPoint = TrackingPoint()
    val rightTracking : TrackingPoint = TrackingPoint()

    fun processMotionEvent(event: MotionEvent?, socket: BluetoothSocket?, screenWidth: Int) {
        if(event == null) return

        println(event)

        val pointerCount = event.pointerCount
        val action = event.actionMasked

        if(action == MotionEvent.ACTION_CANCEL)
        {
            leftTracking.reset()
            rightTracking.reset()
            boosterL.up()
            boosterR.up()
            return
        }

        for (e in 0..(pointerCount - 1)) {
            val pointerId = event.getPointerId(e)
            val x = event.getX(e)
            val y = event.getY(e)
            val act = event.actionMasked

            //actionIndex is always 0 when the action is not the _POINTER_ one (when we have only 1 point)
            //That means it is not representing the real active pointer ID as it might be 1, 2, etc..
            val pointerIdOfAction = if(pointerCount > 1)  event.actionIndex else pointerId
            val leftSide = x < (screenWidth / 2f)

            val pointTrackerOfCurrentSide = if(leftSide) leftTracking else rightTracking
            val boosterOfCurrentSide = if(leftSide) boosterL else boosterR

            val downAction = act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN
            val upAction = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP
            val moveAction = act == MotionEvent.ACTION_MOVE

            val actionOccurOnThisPointer = if(pointerCount > 1) pointerIdOfAction == pointerId else true

            //Down action will be registered on the touched side, if not already touching
            if(downAction && actionOccurOnThisPointer && !pointTrackerOfCurrentSide.isBounded)
            {
                println("Bound L$leftSide to $pointerIdOfAction > ${leftTracking.boundedTouchId} ${leftTracking.isBounded} ${rightTracking.boundedTouchId} ${rightTracking.isBounded}")
                pointTrackerOfCurrentSide.bound(pointerIdOfAction, x,y)
                boosterOfCurrentSide.down()
            }
            else
            {
                println("Not bound! $downAction $actionOccurOnThisPointer ${pointTrackerOfCurrentSide.isBounded} | ${leftTracking.isBounded} ${rightTracking.isBounded} | ${leftSide}")
            }

            //Up action will search for registered side regardless of where you up the touch
            if(upAction && actionOccurOnThisPointer)
            {
                if(leftTracking.boundedTouchId == pointerIdOfAction)
                {
                    println("Upping LEFT side because ${leftTracking.boundedTouchId} ${rightTracking.boundedTouchId} $pointerIdOfAction")
                    leftTracking.reset()
                    boosterL.up()
                }
                else if(rightTracking.boundedTouchId == pointerIdOfAction)
                {
                    println("Upping RIGHT side because ${leftTracking.boundedTouchId} ${rightTracking.boundedTouchId} $pointerIdOfAction")
                    rightTracking.reset()
                    boosterR.up()
                }
            }

            //otherwise it might be down or up action on other pointer, but this pointer need to move
            if(leftTracking.boundedTouchId == pointerId)
            {
                val diff = leftTracking.updatePosition(x,y)
                boosterL.move(diff.first, diff.second)
            }
            else if(rightTracking.boundedTouchId == pointerId)
            {
                val diff = rightTracking.updatePosition(x,y)
                boosterR.move(diff.first, diff.second)
            }

            println("Pointer $pointerId : $x $y $actionOccurOnThisPointer $pointerIdOfAction | $downAction $upAction $moveAction")
        }
        val byteL = boosterL.generateByteMessage()
        val byteR = boosterR.generateByteMessage()
        debugBooster(byteL, byteR)
        println("BoosterL $boosterL ${byteL}")
        println("BoosterR $boosterR ${byteR}")
        if(socket == null) return
        socket.outputStream?.write(byteL)
        socket.outputStream?.write(byteR)
    }

    fun debugBooster(byteL : Int, byteR : Int)
    {
        val activateL = mainActivity.findViewById<ImageView>(R.id.leftBoosterActivate)
        val activateR = mainActivity.findViewById<ImageView>(R.id.rightBoosterActivate)
        activateL.alpha = if(byteL and (1 shl 1) != 0) 0.5f else 0f
        activateR.alpha = if(byteR and (1 shl 1) != 0) 0.5f else 0f

        val paddingConst = 40
        val topL = mainActivity.findViewById<ImageView>(R.id.leftBoosterTop)
        val padLLeft = if(byteL and (1 shl 5) != 0) paddingConst else 0
        val padLDown = if(byteL and (1 shl 4) != 0) paddingConst else 0
        val padLUp = if(byteL and (1 shl 3) != 0) paddingConst else 0
        val padLRight = if(byteL and (1 shl 2) != 0) paddingConst else 0
        //Set in reverse direction so it push the image to the other directon
        topL.setPadding(padLRight,padLDown, padLLeft, padLUp)

        val topR = mainActivity.findViewById<ImageView>(R.id.rightBoosterTop)
        val padRLeft = if(byteR and (1 shl 5) != 0) paddingConst else 0
        val padRDown = if(byteR and (1 shl 4) != 0) paddingConst else 0
        val padRUp = if(byteR and (1 shl 3) != 0) paddingConst else 0
        val padRRight = if(byteR and (1 shl 2) != 0) paddingConst else 0
        //Set in reverse direction so it push the image to the other directon
        topR.setPadding(padRRight,padRDown, padRLeft, padRUp)
    }
}
