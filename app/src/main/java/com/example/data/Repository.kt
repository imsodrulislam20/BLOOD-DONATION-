package com.example.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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

    init {
        // Pre-populate defaults if Room is empty
        scope.launch(Dispatchers.IO) {
            dao.getEmergencyContacts().collect { list ->
                if (list.isEmpty()) {
                    val defaults = listOf(
                        EmergencyContact("em_1", "জাতীয় জরুরি সেবা", "৯৯৯", "সারাদেশ", "সব ধরনের জরুরি প্রয়োজনে"),
                        EmergencyContact("em_2", "ফায়ার সার্ভিস", "১০২", "সারাদেশ", "অগ্নিকাণ্ড ও দুর্যোগ"),
                        EmergencyContact("em_3", "অ্যাম্বুলেন্স", "১৯৯", "সারাদেশ", "জরুরি চিকিৎসা সেবা"),
                        EmergencyContact("em_4", "পুলিশ", "১০০", "সারাদেশ", "জরুরি পুলিশ সহায়তা")
                    )
                    dao.insertEmergencyContacts(defaults)
                }
            }
        }

        // Start Firebase real-time listeners if Firebase is initialized
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            setupFirebaseSync()
        }
    }

    private fun setupFirebaseSync() {
        val database = FirebaseManager.db ?: return

        // 1. Sync Requests
        database.getReference("requests").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val list = mutableListOf<BloodRequest>()
                    snapshot.children.forEach { child ->
                        try {
                            val req = child.getValue(BloodRequest::class.java)
                            if (req != null) {
                                list.add(req.copy(id = child.key ?: ""))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing request from firebase: ${e.message}")
                        }
                    }
                    dao.clearBloodRequests()
                    if (list.isNotEmpty()) {
                        dao.insertBloodRequests(list)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase requests read cancelled: ${error.message}")
            }
        })

        // 2. Sync Donors/Users
        database.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val donorList = mutableListOf<Donor>()
                    snapshot.children.forEach { child ->
                        try {
                            val userObj = child.getValue(AppUser::class.java)
                            if (userObj != null && userObj.isDonor) {
                                val donor = Donor(
                                    id = child.key ?: "",
                                    name = userObj.name,
                                    bloodGroup = userObj.bloodGroup,
                                    phone = userObj.phone,
                                    division = userObj.division,
                                    district = userObj.district,
                                    thana = userObj.thana,
                                    donations = userObj.donations,
                                    points = userObj.points,
                                    lastDonationDate = userObj.lastDonationDate,
                                    createdAt = userObj.createdAt
                                )
                                donorList.add(donor)
                            }
                        } catch (e: Exception) {
                            try {
                                val directDonor = child.getValue(Donor::class.java)
                                if (directDonor != null) {
                                    donorList.add(directDonor.copy(id = child.key ?: ""))
                                }
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error parsing donor: ${ex.message}")
                            }
                        }
                    }
                    dao.clearDonors()
                    if (donorList.isNotEmpty()) {
                        dao.insertDonors(donorList)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase users read cancelled: ${error.message}")
            }
        })

        // 3. Sync Banners
        database.getReference("banners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val list = mutableListOf<Banner>()
                    snapshot.children.forEach { child ->
                        val url = child.child("imageUrl").getValue(String::class.java) ?: ""
                        if (url.isNotEmpty()) {
                            list.add(Banner(id = child.key ?: "", imageUrl = url))
                        }
                    }
                    if (list.isNotEmpty()) {
                        dao.clearBanners()
                        dao.insertBanners(list)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // 4. Sync Emergency Contacts
        database.getReference("emergencyContacts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch(Dispatchers.IO) {
                    val list = mutableListOf<EmergencyContact>()
                    snapshot.children.forEach { child ->
                        try {
                            val contact = child.getValue(EmergencyContact::class.java)
                            if (contact != null) {
                                list.add(contact.copy(id = child.key ?: ""))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing contact: ${e.message}")
                        }
                    }
                    if (list.isNotEmpty()) {
                        dao.clearEmergencyContacts()
                        dao.insertEmergencyContacts(list)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun insertBloodRequest(request: BloodRequest) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            val ref = FirebaseManager.db!!.getReference("requests").push()
            val newRequest = request.copy(id = ref.key ?: "")
            ref.setValue(newRequest)
            dao.insertBloodRequest(newRequest)
        } else {
            val localId = "req_${System.currentTimeMillis()}"
            val localRequest = request.copy(id = localId)
            dao.insertBloodRequest(localRequest)
        }
    }

    suspend fun updateBloodRequestStatus(requestId: String, status: String) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            FirebaseManager.db!!.getReference("requests").child(requestId).child("status").setValue(status)
        } else {
            // Room update
            scope.launch(Dispatchers.IO) {
                // Find and update local request
                dao.getBloodRequests().collect { list ->
                    val found = list.find { it.id == requestId }
                    if (found != null) {
                        dao.insertBloodRequest(found.copy(status = status))
                    }
                }
            }
        }
    }

    suspend fun deleteBloodRequest(id: String) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            FirebaseManager.db!!.getReference("requests").child(id).removeValue()
        }
        dao.deleteBloodRequest(id)
    }

    suspend fun insertDonor(donor: Donor) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            val id = if (donor.id.isNotEmpty()) donor.id else FirebaseManager.db!!.getReference("users").push().key ?: "user_${System.currentTimeMillis()}"
            val newDonor = donor.copy(id = id)
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
            FirebaseManager.db!!.getReference("users").child(newDonor.id).setValue(userRep)
            dao.insertDonor(newDonor)
        } else {
            val localId = if (donor.id.isNotEmpty()) donor.id else "donor_${System.currentTimeMillis()}"
            val localDonor = donor.copy(id = localId)
            dao.insertDonor(localDonor)
        }
    }

    suspend fun deleteDonor(id: String) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            FirebaseManager.db!!.getReference("users").child(id).removeValue()
        }
        dao.deleteDonor(id)
    }

    suspend fun insertBanner(banner: Banner) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            val ref = FirebaseManager.db!!.getReference("banners").push()
            val newBanner = banner.copy(id = ref.key ?: "")
            ref.setValue(newBanner)
        } else {
            val localId = "banner_${System.currentTimeMillis()}"
            dao.insertBanners(listOf(banner.copy(id = localId)))
        }
    }

    suspend fun deleteBanner(id: String) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            FirebaseManager.db!!.getReference("banners").child(id).removeValue()
        }
    }

    suspend fun insertEmergencyContact(contact: EmergencyContact) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            val ref = FirebaseManager.db!!.getReference("emergencyContacts").push()
            val newContact = contact.copy(id = ref.key ?: "")
            ref.setValue(newContact)
        } else {
            val localId = "contact_${System.currentTimeMillis()}"
            dao.insertEmergencyContacts(listOf(contact.copy(id = localId)))
        }
    }

    suspend fun deleteEmergencyContact(id: String) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isInitialized && FirebaseManager.db != null) {
            FirebaseManager.db!!.getReference("emergencyContacts").child(id).removeValue()
        }
        dao.deleteEmergencyContact(id)
    }

    fun getDonationHistory(donorId: String): Flow<List<DonationRecord>> = dao.getDonationRecords(donorId)

    suspend fun addDonationRecord(record: DonationRecord) = withContext(Dispatchers.IO) {
        val id = "record_${System.currentTimeMillis()}"
        val finalRecord = record.copy(id = id)
        dao.insertDonationRecord(finalRecord)
    }
}
