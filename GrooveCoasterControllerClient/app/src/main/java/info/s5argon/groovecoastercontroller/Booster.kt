package info.s5argon.groovecoastercontroller

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Booster(private val rightBooster: Boolean) {

    private var boosterPosition: BoosterPosition = BoosterPosition()
    private var pressed: Boolean = false

    companion object Constants {
        const val edgeDistance: Float = 370f
        const val slideActivationDistance: Float = 140f
        private const val straightAngleDegree: Float = 15f
        const val halfStraightAngle: Float = straightAngleDegree / 2f
    }

    class BoosterPosition {
        private var x: Float = 0f
        private var y: Float = 0f
        val magnitude get() = sqrt((x * x) + (y * y))
        fun moveAndClampToMagnitude(moveX : Float, moveY: Float, newMagnitude: Float) {
            x += moveX
            y += moveY
            val currentMagnitude = magnitude
            if (currentMagnitude > newMagnitude) {
                x = (x / currentMagnitude) * newMagnitude
                y = (y / currentMagnitude) * newMagnitude
            }
        }

        fun reset()
        {
            x = 0f
            y = 0f
        }

        override fun toString(): String {
            val dir = activeDirections
           return "$x $y $magnitude DIR : ${dir.left} ${dir.down} ${dir.up} ${dir.right}"
        }

        data class ActiveDirections(val left: Boolean, val down: Boolean, val up: Boolean, val right: Boolean)
        private val Float.radian: Float get() = this * PI.toFloat() / 180f

        /** There is an angle covering each axis direction that results in purely that direction.
         * Otherwise, it is a diagonal direction.
               (0, 1)

        (-1,0)   +   (1, 0)

              (0, -1)

        (cos, sin)
         */
        val activeDirections: ActiveDirections
            get() {
                val mag = magnitude
                if (mag < slideActivationDistance) {
                    return ActiveDirections(false, false, false, false)
                }

                val xUnit = x/mag
                val yUnit = y/mag

                val right = yUnit > sin(halfStraightAngle.radian)
                val left = yUnit < sin((-halfStraightAngle).radian)

                val up = xUnit > cos((90 - halfStraightAngle).radian)
                val down = xUnit < cos((90 + halfStraightAngle).radian)

                println("Active Direction ($xUnit, $yUnit)")

                return ActiveDirections(left, down, up, right)
            }

    }

    /**
     * Auto bounce back to zero on up like the real controller
     */
    fun up(){
        boosterPosition.reset()
        pressed = false
    }

    fun down() {
        boosterPosition.reset()
        pressed = true
    }

    fun move(moveByX: Float, moveByY: Float) {
        boosterPosition.moveAndClampToMagnitude(moveByX, moveByY, edgeDistance)
    }

    override fun toString(): String = "Right? $rightBooster Pressed $pressed Pos $boosterPosition"

    private val Boolean.asBit: Int
        get() {
            return if (!this) 0 else 1
        }

    /**
     * Format of the byte : [N/A][N/A][Left Status][Down Status] [Up Status][Right Status][Button Status][Booster Index (0/1)]
     * All the status : 0 = currently down, 1 = currently up
     * At server side the program will remember the previous byte, so that it can do action if there is a different on each bit
     * e.g. Previous frame 1 -> this frame 0 = we call "up" for that button.
     * Booster index : 0 = Left, 1 = Right
     */
    fun generateByteMessage(): Byte {
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