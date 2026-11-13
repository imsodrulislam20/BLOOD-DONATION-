package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    var isInitialized = false
        private set

    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
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
                auth = Firebase.auth
                val db = Firebase.firestore
                try {
                    db.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(persistentCacheSettings {})
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore settings already configured: ${e.message}")
                }
                firestore = db

                messaging = Firebase.messaging
                messaging?.subscribeToTopic("blood_requests")
                    ?.addOnFailureListener { e ->
                        Log.w(TAG, "Topic subscription warning: ${e.message}")
                    }

                isInitialized = true
                Log.d(TAG, "Firebase initialized successfully for project: ${app.options.projectId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}. Using offline local database fallback.")
            isInitialized = false
            auth = null
            firestore = null
            messaging = null
        }
    }
}
