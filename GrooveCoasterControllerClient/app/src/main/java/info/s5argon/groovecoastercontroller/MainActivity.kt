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

/*
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
*/

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideClutters()
        getBluetooth()

        /*
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager?.registerListener(mSensorListener, mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        mAccel = 0.00f
        mAccelCurrent = SensorManager.GRAVITY_EARTH
        mAccelLast = SensorManager.GRAVITY_EARTH
        */
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

    private fun hideClutters(){
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    // Removed a joke shake function lol I added it so that it is stupid enough for Stupid Hackathon Thailand 2

    /*
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
    */
}
