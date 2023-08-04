package com.zw.template.di

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.zw.template.di.modules.ContextModule

class ZwApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            component = DaggerAppComponent.builder().contextModule(ContextModule(this)).build()
            component.inject(this)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var component: AppComponent
    }
}