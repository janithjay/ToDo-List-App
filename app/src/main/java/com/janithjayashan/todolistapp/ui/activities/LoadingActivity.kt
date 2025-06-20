package com.janithjayashan.todolistapp.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieDrawable
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import com.ncorti.slidetoact.SlideToActView
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


        // Setup swipe button
        val swipeButton = findViewById<SlideToActView>(R.id.swipeButton)
        swipeButton.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                navigateToNextScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
        CoroutineScope(Dispatchers.Main).launch {
            if (backupManager.isUserLoggedIn()) {
                // Attempt to restore user data and navigate to lists
                backupManager.restoreUserData()
                startActivity(Intent(this@LoadingActivity, ListsActivity::class.java))
            } else {
                // If no user is logged in, go to MainActivity (login screen)
                startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
            }
        }
    }
}
