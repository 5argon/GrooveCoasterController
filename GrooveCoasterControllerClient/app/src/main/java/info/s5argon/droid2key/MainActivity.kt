package info.s5argon.droid2key

import kotlin.math.*
import android.bluetooth.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.Sensor
import java.util.*
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideClutters()
        getBluetooth()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager?.registerListener(mSensorListener, mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        mAccel = 0.00f
        mAccelCurrent = SensorManager.GRAVITY_EARTH
        mAccelLast = SensorManager.GRAVITY_EARTH
    }

    private var bluetoothAdapter : BluetoothAdapter? = null
    private var firstBonded : BluetoothDevice? = null
    private var socket : BluetoothSocket? = null
    private var pointTracker : GroovePointTracker? = null

    private fun getBluetooth() {
        println("Getting Bluetooth")
        val bm =  getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter
        if (bluetoothAdapter?.enable() == false) {
            println("Bluetooth not enabled!!")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 555)
        }
        val bonded = bluetoothAdapter?.bondedDevices
        println(bonded?.size)
        firstBonded = bonded?.elementAt(0)
        println("Connected name ${firstBonded?.name}, getting RFCOMM")
        socket = firstBonded?.createRfcommSocketToServiceRecord(UUID.fromString("0abfa6c2-384c-4844-8e9d-7fcc862b3a7d"))
        socket?.connect()
        pointTracker = GroovePointTracker()
        println("Connected! Probably!")
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        pointTracker?.processMotionEvent(event,socket, size.y)
        return super.onTouchEvent(event)
    }

    private class GroovePointTracker
    {
        private class Booster(val rightBooster : Boolean)
        {
            companion object Constants {
                const val edgeDistance : Float = 100f
                const val slideActivationDistance : Float = 10f
                const val straightAngleDegree : Float = 15f
                const val halfStraightAngle : Float = straightAngleDegree / 2f
            }

            private class BoosterPosition
            {
                var x : Float = 0f
                var y : Float = 0f
                val magnitude = sqrt( (x*x) + (y*y))
                fun clampToMagnitude(newMagnitude : Float)
                {
                    val currentMagnitude = magnitude
                    if(currentMagnitude > newMagnitude) {
                        x = (x / currentMagnitude) * newMagnitude
                        y = (y / currentMagnitude) * newMagnitude
                    }
                }

                data class ActiveDirections(val left : Boolean, val down : Boolean, val up : Boolean, val right : Boolean)

                /** There is an angle covering each axis direction that results in purely that direction.
                 * Otherwise, it is a diagonal direction.
                       (0, 1)

                (-1,0)   +   (1, 0)

                     (0, -1)

                (cos, sin)
                 */
                private val Float.radian: Float get() = this * kotlin.math.PI.toFloat() / 180f

                val activeDirections : ActiveDirections
                get()
                {
                    val mag = magnitude
                    if(mag < slideActivationDistance)
                    {
                        return ActiveDirections(false,false,false,false)
                    }

                    val up = sin(y / mag) > sin(halfStraightAngle.radian)
                    val down = sin(y / mag) < sin((-halfStraightAngle).radian)

                    val left = cos(x / mag) > cos((90 - halfStraightAngle).radian)
                    val right = cos(x / mag) < cos((90 + halfStraightAngle).radian)

                    return ActiveDirections(left,down,up,right)
                }

            }

            var boosterPosition : BoosterPosition = BoosterPosition()
            var pressed : Boolean = false

            fun down() {
                pressed = true
            }

            fun up()
            {
                pressed = false
                move(0f,0f) //auto bounce back of GC controller
            }

            infix fun Float.clampPosNegTo(clampTo :Float) : Float = Math.max(Math.min(this, clampTo) , -clampTo)

            fun move(currentX: Float, currentY : Float)
            {
                boosterPosition.x = currentX
                boosterPosition.y = currentY
                boosterPosition.clampToMagnitude(edgeDistance)
            }

            private val Boolean.asBit : Int get() {
                return if(!this) 0 else 1
            }

            /**
             * Format of the byte : [N/A][N/A][Left Status][Down Status] [Up Status][Right Status][Button Status][Booster Index (0/1)]
             * All the status : 0 = currently down, 1 = currently up
             * At server side the program will remember the previous byte, so that it can do action if there is a different on each bit
             * e.g. Previous frame 1 -> this frame 0 = we call "up" for that button.
             * Booster index : 0 = Left, 1 = Right
             */
            fun generateByteMessage() : Byte
            {
                val direction = boosterPosition.activeDirections
                val byteMessage =
                (
                    0 shl 7 or
                    0 shl 6 or
                    direction.left.asBit shl 5 or
                    direction.down.asBit shl 4 or
                    direction.up.asBit shl 3 or
                    direction.right.asBit shl 2 or
                    pressed.asBit shl 1 or
                    rightBooster.asBit
                )
                return byteMessage.toByte()
            }
        }

        private val boosterL : Booster = Booster(false)
        private val boosterR : Booster = Booster(true)

        fun processMotionEvent(event : MotionEvent?, socket: BluetoothSocket?, screenWidth : Int)
        {
            val pointerCount = event?.pointerCount ?: 0
            for(e in 0..(pointerCount-1))
            {
                val pointerId = event?.getPointerId(e)
                val x = event?.getX(e)
                val y = event?.getY(e)
                println("Each pointer $x $y $pointerId")
            }
            println(event)
            println("$screenWidth Action ${event?.action} ${event?.actionMasked} Pointer Count ${event?.pointerCount}")
            if(event?.action == MotionEvent.ACTION_DOWN)
            {
                socket?.outputStream?.write(3)
            }
            else if(event?.action == MotionEvent.ACTION_UP)
            {
                socket?.outputStream?.write(4)
            }
        }
    }

    private fun hideClutters(){
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private var mSensorManager: SensorManager? = null
    private var mAccel: Float = 0.toFloat()
    private var mAccelCurrent: Float = 0.toFloat()
    private var mAccelLast: Float = 0.toFloat()

    private val mSensorListener = object : SensorEventListener {
        override fun onSensorChanged(se: SensorEvent) {
            val x = se.values[0]
            val y = se.values[1]
            val z = se.values[2]
            mAccelLast = mAccelCurrent
            mAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = mAccelCurrent - mAccelLast
            mAccel = (mAccel percentage 90) + delta // perform low-cut filter
            if(mAccel > 20)
            {
                socket?.outputStream?.write(5)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        infix fun Float.percentage (percentInteger : Int) : Float = this * (percentInteger / 100f)

    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(mSensorListener, mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        mSensorManager!!.unregisterListener(mSensorListener)
        super.onPause()
    }
}
