package info.s5argon.groovecoastercontroller

import android.drm.DrmStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        //window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideClutters()
        /*
        window.decorView.setOnSystemUiVisibilityChangeListener {
            println("Visibility change $it")
            hideClutters()
        }
        */
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //println("Touched! ${event}")
        if(event?.action == MotionEvent.ACTION_DOWN) {
            println("Firing Bluetooth event")
        }
        return super.onTouchEvent(event)
    }

    private fun hideClutters(){
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
    /*

    override fun onResume() {
        super.onResume()
        println("Resume")
        hideClutters()
    }
    */
}
