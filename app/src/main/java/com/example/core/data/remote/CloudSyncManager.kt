package com.example.core.data.remote

import android.content.Context
import android.util.Log
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudSyncManager(private val context: Context) {
    private val isFirebaseInitialized: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseInitialized) FirebaseFirestore.getInstance() else null
        
    private val auth: FirebaseAuth?
        get() = if (isFirebaseInitialized) FirebaseAuth.getInstance() else null

    suspend fun syncProfileToCloud(profile: CareerProfileEntity) {
        val currentAuth = auth ?: return
        val currentFirestore = firestore ?: return
        val user = currentAuth.currentUser ?: return
        
        try {
            currentFirestore.collection("users")
                .document(user.uid)
                .collection("profile")
                .document("career")
                .set(profile)
                .await()
            Log.d("CloudSyncManager", "Profile synced to cloud successfully.")
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to sync profile to cloud", e)
        }
    }

    suspend fun syncGameRecordToCloud(gameRecord: GameRecordEntity) {
        val currentAuth = auth ?: return
        val currentFirestore = firestore ?: return
        val user = currentAuth.currentUser ?: return
        
        try {
            currentFirestore.collection("users")
                .document(user.uid)
                .collection("games")
                .document(gameRecord.id.toString())
                .set(gameRecord)
                .await()
            Log.d("CloudSyncManager", "Game record synced to cloud successfully.")
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to sync game record", e)
        }
    }
}
