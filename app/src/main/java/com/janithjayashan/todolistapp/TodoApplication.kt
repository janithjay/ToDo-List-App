package com.janithjayashan.todolistapp

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase persistence before any database operation
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
