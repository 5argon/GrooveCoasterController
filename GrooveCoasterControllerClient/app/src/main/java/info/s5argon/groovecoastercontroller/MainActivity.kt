package info.s5argon.groovecoastercontroller

import android.bluetooth.*
import android.drm.DrmStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MotionEvent
import android.view.View
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.view.Window
import android.view.WindowManager
import android.content.Intent
import android.bluetooth.le.ScanResult
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
    private var gatt : BluetoothGatt? = null
    private var firstBonded : BluetoothDevice? = null
    private var socket : BluetoothSocket? = null

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
        //gatt = firstBonded?.connectGatt(this,true,MyGattCallback())
        //Postmortem : Bluetooth LE scanning require location service?? Too many damn callbacks??
    }

    class MyCallback(contextReceive : Context ) : ScanCallback()
    {
        val context = contextReceive
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            println("Scan result")
            result?.device?.connectGatt(context,true, MyGattCallback())
            super.onScanResult(callbackType, result)
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            println("Batch Scan Result")
            super.onBatchScanResults(results)
        }
        override fun onScanFailed(errorCode: Int) {
            println("Scan failed!")
            super.onScanFailed(errorCode)
        }
    }

    class MyGattCallback : BluetoothGattCallback()
    {
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            println("Service discovered $status ${gatt?.device?.name}")
            super.onServicesDiscovered(gatt, status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            println("Connection state change from $status to $newState")
            super.onConnectionStateChange(gatt, status, newState)
        }
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //println("Touched! ${event}")
        if(event?.action == MotionEvent.ACTION_DOWN) {
            println("Firing Bluetooth event")
            socket?.outputStream?.write(3)
            //val service = gatt?.getService(UUID.fromString("0abfa6c2-384c-4844-8e9d-7fcc862b3a7d"))
            //println(service)
        }
        return super.onTouchEvent(event)
    }

    private fun hideClutters(){
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
