package com.example.data

import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BloodDonationRepository(
    private val dao: BloodDao,
    private val scope: CoroutineScope
) {
    private val TAG = "BloodDonationRepo"

    val bloodRequests: Flow<List<BloodRequest>> = dao.getBloodRequests()
    val donors: Flow<List<Donor>> = dao.getDonors()
    val banners: Flow<List<Banner>> = dao.getBanners()
    val emergencyContacts: Flow<List<EmergencyContact>> = dao.getEmergencyContacts()

    private val _isRealtimeActive = MutableStateFlow(false)
    val isRealtimeActive: StateFlow<Boolean> = _isRealtimeActive.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        // Seed default Bangladesh emergency contacts once if local cache is empty
        scope.launch(Dispatchers.IO) {
            if (dao.getEmergencyContactsCount() == 0) {
                val defaults = listOf(
                    EmergencyContact("em_1", "জাতীয় জরুরি সেবা", "৯৯৯", "সারাদেশ", "সব ধরনের জরুরি প্রয়োজনে"),
                    EmergencyContact("em_2", "ফায়ার সার্ভিস", "১০২", "সারাদেশ", "অগ্নিকাণ্ড ও দুর্যোগ"),
                    EmergencyContact("em_3", "অ্যাম্বুলেন্স", "১৯৯", "সারাদেশ", "জরুরি চিকিৎসা সেবা"),
                    EmergencyContact("em_4", "পুলিশ", "১০০", "সারাদেশ", "জরুরি পুলিশ সহায়তা")
                )
                dao.insertEmergencyContacts(defaults)
            }
        }

        // Start Cloud Firestore real-time snapshot listeners if Firebase is initialized
        if (FirebaseManager.isInitialized && FirebaseManager.firestore != null) {
            setupFirestoreRealtimeSync()
        }
    }

    private fun setupFirestoreRealtimeSync() {
        val firestore = FirebaseManager.firestore ?: return

        // 1. Real-time Blood Requests listener
        val requestsReg = firestore.collection("requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore requests snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                _isRealtimeActive.value = true

                scope.launch(Dispatchers.IO) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(BloodRequest::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing request ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    dao.replaceBloodRequests(list)
                }
            }
        listeners.add(requestsReg)

        // 2. Real-time Donors / Users listener
        val usersReg = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore users snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                _isRealtimeActive.value = true

                scope.launch(Dispatchers.IO) {
                    val donorList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val userObj = doc.toObject(AppUser::class.java)
                            val isDonorFlag = doc.getBoolean("isDonor") ?: userObj?.isDonor ?: true
                            if (userObj != null && isDonorFlag && userObj.bloodGroup.isNotBlank()) {
                                Donor(
                                    id = doc.id,
                                    name = userObj.name,
                                    bloodGroup = userObj.bloodGroup,
                                    phone = userObj.phone,
                                    division = userObj.division,
                                    district = userObj.district,
                                    thana = userObj.thana,
                                    donations = userObj.donations,
                                    points = userObj.points,
                                    addedByEmail = userObj.email,
                                    lastDonationDate = userObj.lastDonationDate,
                                    createdAt = userObj.createdAt
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            try {
                                doc.toObject(Donor::class.java)?.copy(id = doc.id)
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error parsing donor ${doc.id}: ${ex.message}")
                                null
                            }
                        }
                    }.sortedByDescending { it.createdAt }

                    dao.replaceDonors(donorList)
                }
            }
        listeners.add(usersReg)

        // 3. Real-time Banners listener
        val bannersReg = firestore.collection("banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore banners snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val url = doc.getString("imageUrl") ?: ""
                        if (url.isNotEmpty()) Banner(id = doc.id, imageUrl = url) else null
                    }
                    dao.replaceBanners(list)
                }
            }
        listeners.add(bannersReg)

        // 4. Real-time Emergency Contacts listener
        val contactsReg = firestore.collection("emergencyContacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore emergencyContacts snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(EmergencyContact::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing emergency contact ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        dao.replaceEmergencyContacts(list)
                    }
                }
            }
        listeners.add(contactsReg)
    }

    fun observeUserRealtime(uid: String): Flow<AppUser?> = callbackFlow {
        val firestore = FirebaseManager.firestore
        if (!FirebaseManager.isInitialized || firestore == null) {
            trySend(dao.getUserById(uid))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "User profile snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(AppUser::class.java)?.copy(uid = snapshot.id)
                    if (user != null) {
                        scope.launch(Dispatchers.IO) { dao.insertUser(user) }
                        trySend(user)
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getOrFetchUser(uid: String, fallbackName: String, fallbackEmail: String): AppUser {
        return withContext(Dispatchers.IO) {
            val firestore = FirebaseManager.firestore
            if (FirebaseManager.isInitialized && firestore != null) {
                try {
                    val doc = firestore.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val remoteUser = doc.toObject(AppUser::class.java)?.copy(uid = uid)
                        if (remoteUser != null) {
                            dao.insertUser(remoteUser)
                            return@withContext remoteUser
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch user from Firestore: ${e.message}")
                }
            }
            dao.getUserById(uid) ?: AppUser(
                uid = uid,
                name = fallbackName.ifBlank { "ইউজার" },
                email = fallbackEmail
            )
        }
    }

    suspend fun upsertUser(user: AppUser) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            firestore.collection("users").document(user.uid)
                .set(user, SetOptions.merge())
                .await()
        }
    }

    suspend fun insertBloodRequest(request: BloodRequest) = withContext(Dispatchers.IO) {
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            val docRef = firestore.collection("requests").document()
            val newRequest = request.copy(id = docRef.id)
            dao.insertBloodRequest(newRequest)
            docRef.set(newRequest).await()
        } else {
            val localId = "req_${System.currentTimeMillis()}"
            dao.insertBloodRequest(request.copy(id = localId))
        }
    }

    suspend fun updateBloodRequestStatus(requestId: String, status: String) = withContext(Dispatchers.IO) {
        dao.updateBloodRequestStatus(requestId, status)
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            firestore.collection("requests").document(requestId)
                .update("status", status)
                .await()
        }
    }

    suspend fun deleteBloodRequest(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBloodRequest(id)
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            firestore.collection("requests").document(id).delete().await()
        }
    }

    suspend fun insertDonor(donor: Donor) = withContext(Dispatchers.IO) {
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            val docRef = if (donor.id.isNotEmpty()) {
                firestore.collection("users").document(donor.id)
            } else {
                firestore.collection("users").document()
            }
            val newDonor = donor.copy(id = docRef.id)
            val userRep = AppUser(
                uid = newDonor.id,
                name = newDonor.name,
                email = newDonor.addedByEmail,
                bloodGroup = newDonor.bloodGroup,
                phone = newDonor.phone,
                division = newDonor.division,
                district = newDonor.district,
                thana = newDonor.thana,
                donations = newDonor.donations,
                points = newDonor.points,
                lastDonationDate = newDonor.lastDonationDate,
                isDonor = true,
                createdAt = newDonor.createdAt
            )
            dao.insertDonor(newDonor)
            docRef.set(userRep, SetOptions.merge()).await()
        } else {
            val localId = if (donor.id.isNotEmpty()) donor.id else "donor_${System.currentTimeMillis()}"
            dao.insertDonor(donor.copy(id = localId))
        }
    }

    suspend fun deleteDonor(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDonor(id)
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            firestore.collection("users").document(id).delete().await()
        }
    }

    suspend fun insertBanner(banner: Banner) = withContext(Dispatchers.IO) {
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            val docRef = firestore.collection("banners").document()
            val newBanner = banner.copy(id = docRef.id)
            dao.insertBanner(newBanner)
            docRef.set(newBanner).await()
        } else {
            val localId = "banner_${System.currentTimeMillis()}"
            dao.insertBanner(banner.copy(id = localId))
        }
    }

    suspend fun deleteBanner(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBanner(id)
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            firestore.collection("banners").document(id).delete().await()
        }
    }

    suspend fun insertEmergencyContact(contact: EmergencyContact) = withContext(Dispatchers.IO) {
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            val docRef = firestore.collection("emergencyContacts").document()
            val newContact = contact.copy(id = docRef.id)
            dao.insertEmergencyContact(newContact)
            docRef.set(newContact).await()
        } else {
            val localId = "contact_${System.currentTimeMillis()}"
            dao.insertEmergencyContact(contact.copy(id = localId))
        }
    }

    suspend fun deleteEmergencyContact(id: String) = withContext(Dispatchers.IO) {
        dao.deleteEmergencyContact(id)
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            firestore.collection("emergencyContacts").document(id).delete().await()
        }
    }

    fun getDonationHistory(donorId: String): Flow<List<DonationRecord>> = dao.getDonationRecords(donorId)

    suspend fun addDonationRecord(record: DonationRecord) = withContext(Dispatchers.IO) {
        val firestore = FirebaseManager.firestore
        if (FirebaseManager.isInitialized && firestore != null) {
            val docRef = firestore.collection("donationRecords").document()
            val finalRecord = record.copy(id = docRef.id)
            dao.insertDonationRecord(finalRecord)
            docRef.set(finalRecord).await()
        } else {
            val id = "record_${System.currentTimeMillis()}"
            dao.insertDonationRecord(record.copy(id = id))
        }
    }

    fun cleanup() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
