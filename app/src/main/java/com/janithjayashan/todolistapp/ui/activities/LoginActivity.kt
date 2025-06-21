package com.janithjayashan.todolistapp.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.janithjayashan.todolistapp.auth.AuthViewModel
import com.janithjayashan.todolistapp.ui.auth.LoginScreen

class LoginActivity : AppCompatActivity() {
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Set up the Compose UI
        setContent {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    // Navigate to ListsActivity when authenticated
                    startActivity(Intent(this, ListsActivity::class.java))
                    finish() // Don't keep login activity in back stack
                }
            )
        }
    }
}
