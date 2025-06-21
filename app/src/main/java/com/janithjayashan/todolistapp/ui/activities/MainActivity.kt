package com.janithjayashan.todolistapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.auth.AuthViewModel
import com.janithjayashan.todolistapp.ui.auth.LoginScreen
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import com.janithjayashan.todolistapp.utils.NetworkConnectivityObserver
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var firebaseBackupManager: FirebaseBackupManager
    private lateinit var networkObserver: NetworkConnectivityObserver
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModels and managers
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        firebaseBackupManager = FirebaseBackupManager(this)
        networkObserver = NetworkConnectivityObserver(this, firebaseBackupManager)

        // Set up the Compose UI
        setContent {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    // Navigate to ListsActivity when authenticated
                    startActivity(Intent(this, ListsActivity::class.java))
                    if (!isFinishing) {
                        finish() //finish MainActivity - don't want users to come back to login
                    }
                }
            )
        }

        // Start observing network changes
        lifecycleScope.launch {
            networkObserver.observe().collect { isConnected ->
                if (isConnected) {
                    // Network is available, pending backups will be processed automatically
                    Toast.makeText(this@MainActivity, "Network connected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only show backup/restore options if user is logged in
        if (firebaseBackupManager.isUserLoggedIn()) {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                if (firebaseBackupManager.isUserLoggedIn()) {
                    lifecycleScope.launch {
                        firebaseBackupManager.backupToFirebase()
                    }
                } else {
                    Toast.makeText(this, "Please log in to backup data", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_restore -> {
                if (firebaseBackupManager.isUserLoggedIn()) {
                    lifecycleScope.launch {
                        firebaseBackupManager.restoreUserData()
                    }
                } else {
                    Toast.makeText(this, "Please log in to restore data", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseBackupManager.onDestroy()
    }
}