package com.janithjayashan.todolistapp.ui.activities


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.janithjayashan.todolistapp.R
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import com.janithjayashan.todolistapp.utils.NetworkConnectivityObserver
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var firebaseBackupManager: FirebaseBackupManager
    private lateinit var networkObserver: NetworkConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val goToListsButton = findViewById<Button>(R.id.btnGoToLists)
        goToListsButton.setOnClickListener {
            startActivity(Intent(this, ListsActivity::class.java))
        }

        // Initialize backup system
        firebaseBackupManager = FirebaseBackupManager(this)
        networkObserver = NetworkConnectivityObserver(this, firebaseBackupManager)

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

    // Add backup and restore menu options
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                lifecycleScope.launch {
                    firebaseBackupManager.backupToFirebase()
                }
                true
            }
            R.id.action_restore -> {
                lifecycleScope.launch {
                    firebaseBackupManager.restoreFromFirebase()
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