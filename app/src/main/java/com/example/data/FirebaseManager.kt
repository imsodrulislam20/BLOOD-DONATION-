package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    var isInitialized = false
        private set

    var auth: FirebaseAuth? = null
        private set
    var db: FirebaseDatabase? = null
        private set
    var messaging: FirebaseMessaging? = null
        private set

    fun initialize(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                apps[0]
            }
            if (app != null) {
                auth = FirebaseAuth.getInstance()
                db = FirebaseDatabase.getInstance()
                try {
                    db?.setPersistenceEnabled(true)
                } catch (e: Exception) {
                    Log.w(TAG, "Persistence already enabled or failed: ${e.message}")
                }
                messaging = FirebaseMessaging.getInstance()
                isInitialized = true
                Log.d(TAG, "Firebase initialized successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}. Using offline local database fallback.")
            isInitialized = false
            auth = null
            db = null
            messaging = null
        }
    }
}
