package com.zw.template.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.zw.template.R

class SplashActivity : BaseActivity() {
    private var handler = Handler(Looper.getMainLooper())
    private var runnable = Runnable {
        startActivity(Intent(mActivity, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 1500)
    }
}