package com.janithjayashan.todolistapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.auth.AuthViewModel
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import com.janithjayashan.todolistapp.utils.NetworkConnectivityObserver
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var firebaseBackupManager: FirebaseBackupManager
    private lateinit var networkObserver: NetworkConnectivityObserver
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Initialize ViewModels and managers
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        firebaseBackupManager = FirebaseBackupManager(this)
        networkObserver = NetworkConnectivityObserver(this, firebaseBackupManager)

        // Set up button click listener
        findViewById<Button>(R.id.btnGoToLists).setOnClickListener {
            if (firebaseBackupManager.isUserLoggedIn()) {
                startActivity(Intent(this, ListsActivity::class.java))
            } else {
                // User needs to login first
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        // Start observing network changes
        lifecycleScope.launch {
            networkObserver.observe().collect { isConnected ->
                if (isConnected) {
                    // Network is available, pending backups will be processed automatically
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is logged in
        if (firebaseBackupManager.isUserLoggedIn()) {
            findViewById<Button>(R.id.btnGoToLists).text = "Go to Lists"
        } else {
            findViewById<Button>(R.id.btnGoToLists).text = "Login"
        }
    }
}