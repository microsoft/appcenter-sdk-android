package com.microsoft.appcenter.sasquatch_kotlin

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log.VERBOSE
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.microsoft.appcenter.kotlin.Analytics
import com.microsoft.appcenter.kotlin.AppCenter
import com.microsoft.appcenter.kotlin.Auth
import com.microsoft.appcenter.kotlin.Crashes
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nav_view.setupWithNavController(nav_host_fragment.findNavController())

        if (!AppCenter.isConfigured) {
            configureAppCenter()
        }
    }

    private fun configureAppCenter() {
        AppCenter.logLevel = VERBOSE
        AppCenter.start(application, "45d1d9f6-2492-4e68-bd44-7190351eb5f3", Analytics, Auth, Crashes)
    }
}
