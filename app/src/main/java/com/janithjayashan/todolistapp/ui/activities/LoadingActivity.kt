package com.janithjayashan.todolistapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieDrawable
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

        // Setup Lottie animation
        val animationView = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.animationView)
        animationView.setAnimation(R.raw.loading_animation)
        animationView.repeatCount = LottieDrawable.INFINITE
        animationView.playAnimation()

        // Start data loading
        loadUserData()
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            if (backupManager.isUserLoggedIn()) {
                // Attempt to restore user data
                backupManager.restoreUserData()
                // Add a slight delay for smoother transition
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@LoadingActivity, ListsActivity::class.java))
                    finish()
                }, 2000) // 2 seconds delay
            } else {
                // If no user is logged in, go to MainActivity (login screen)
                startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
