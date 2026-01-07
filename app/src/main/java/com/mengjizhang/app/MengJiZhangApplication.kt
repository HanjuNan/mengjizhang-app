package com.mengjizhang.app

import android.app.Application
import com.mengjizhang.app.data.local.AppDatabase

class MengJiZhangApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MengJiZhangApplication
            private set
    }
}
