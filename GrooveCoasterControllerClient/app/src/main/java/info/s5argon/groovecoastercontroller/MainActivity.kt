package info.s5argon.groovecoastercontroller

import android.bluetooth.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Point
import android.support.v7.app.AlertDialog
import android.widget.FrameLayout
import java.util.*

/*
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
*/

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter : BluetoothAdapter? = null
    private var firstBonded : BluetoothDevice? = null
    private var socket : BluetoothSocket? = null
    private var pointTracker : GroovePointTracker? = GroovePointTracker(this)

    override fun onCreate(savedInstanceState: Bundle?)
    {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideClutters()
        val touchReceiver = findViewById<FrameLayout>(R.id.touchReceiver)


        val builder = AlertDialog.Builder(this)
        val welcomeMessage =
                "For more information see 詳しくは : http://5argon.info/gccon \n" +
                "\n" +
                "Before pressing the OK button below you must : \n" +
                "\n" +
                "1. Enable Bluetooth on your PC and your Android device. \n" +
                "2. Run the server .exe which you can get at http://5argon.info/gccon. It will crash if you did not turn on Bluetooth on the PC. \n" +
                "3. Pair your phone to the PC manually via Bluetooth. Make sure your phone only pair to one Bluetooth device that is the PC, otherwise it might crash \n"
                "\n" +
                "Contributions welcomed at https://github.com/5argon/GrooveCoasterController !"

        builder.setMessage(welcomeMessage).setTitle("Groove Coaster Controller")
        builder.setNeutralButton("OK")
        { d : DialogInterface, i : Int ->
            touchReceiver.setOnTouchListener { v: View ,m : MotionEvent->
                val size = Point()
                windowManager.defaultDisplay.getSize(size)
                pointTracker?.processMotionEvent(m, socket, size.x)
                true
            }
            getBluetooth()
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()

        /*
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorManager?.registerListener(mSensorListener, mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        mAccel = 0.00f
        mAccelCurrent = SensorManager.GRAVITY_EARTH
        mAccelLast = SensorManager.GRAVITY_EARTH
        */
    }


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
        println("Connected! Probably!")
    }

    private fun hideClutters(){
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    // a joke shake function lol I added it so that it is stupid enough for Stupid Hackathon Thailand 2

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
            if(mAccel > 28) //Adjust sensitivity here!
            {
                socket?.outputStream?.write(1 shl 7)
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
