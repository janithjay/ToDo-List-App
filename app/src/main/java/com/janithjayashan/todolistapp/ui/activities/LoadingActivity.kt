package com.janithjayashan.todolistapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {
    private lateinit var backupManager: FirebaseBackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        backupManager = FirebaseBackupManager(this)

        // Hide action bar for full-screen effect
        supportActionBar?.hide()

        // Setup go to lists button
        findViewById<Button>(R.id.btnGoToLists).setOnClickListener {
            navigateToNextScreen()
        }

        // Update button text based on login state
        updateButtonText()
    }

    private fun updateButtonText() {
        val button = findViewById<Button>(R.id.btnGoToLists)
        button.text = if (backupManager.isUserLoggedIn()) "Go to Lists" else "Login"
    }

    private fun navigateToNextScreen() {
        CoroutineScope(Dispatchers.Main).launch {
            if (backupManager.isUserLoggedIn()) {
                // Attempt to restore user data and navigate to lists
                backupManager.restoreUserData()
                startActivity(Intent(this@LoadingActivity, ListsActivity::class.java))
            } else {
                // If no user is logged in, go directly to login screen
                startActivity(Intent(this@LoadingActivity, LoginActivity::class.java))
            }
            finish() // Close the loading activity
        }
    }
}
