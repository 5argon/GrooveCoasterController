package info.s5argon.groovecoastercontroller

import android.bluetooth.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.content.Context
import android.content.Intent
import android.graphics.Point
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideClutters()
        getBluetooth()
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
        pointTracker?.AddMotionEvent(event,socket, size.y)
        return super.onTouchEvent(event)
    }

    private class GroovePointTracker()
    {
        object GrooveKey {
            const val LDown = 0
            const val LUp = 1
            const val LSlideLeft = 2
            const val LSlideDown= 3
            const val LSlideUp = 4
            const val LSlideRight= 5

            const val RDown = 6
            const val RUp = 7
            const val RSlideLeft = 8
            const val RSlideDown= 9
            const val RSlideUp = 10
            const val RSlideRight= 11
        }

        fun AddMotionEvent(event : MotionEvent?, socket: BluetoothSocket?, screenWidth : Int)
        {
            val pointerCount = event?.pointerCount ?: 0
            for(e in 0..(pointerCount-1))
            {
                val pointerId = event?.getPointerId(e)
                val x = event?.getX(e)
                val y = event?.getY(e)
                println("Each pointer $x $y $pointerId")
            }
            println("$screenWidth Action ${event?.action} ${event?.actionMasked} Pointer Count ${event?.pointerCount}")
            socket?.outputStream?.write(3)
        }
    }

    private fun hideClutters(){
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
