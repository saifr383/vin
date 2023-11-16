package com.example.myapplication

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
class PrintDemoApp : Application() {
    companion object {
        lateinit var instance: PrintDemoApp
            private set
    }

    var currentSelectedPrinter: IPrinterInfo? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
