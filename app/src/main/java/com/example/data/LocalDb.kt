package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodDao {
    @Query("SELECT * FROM blood_requests ORDER BY createdAt DESC")
    fun getBloodRequests(): Flow<List<BloodRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodRequest(request: BloodRequest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodRequests(requests: List<BloodRequest>)

    @Query("UPDATE blood_requests SET status = :status WHERE id = :id")
    suspend fun updateBloodRequestStatus(id: String, status: String)

    @Query("DELETE FROM blood_requests WHERE id = :id")
    suspend fun deleteBloodRequest(id: String)

    @Query("DELETE FROM blood_requests")
    suspend fun clearBloodRequests()

    @Transaction
    suspend fun replaceBloodRequests(requests: List<BloodRequest>) {
        clearBloodRequests()
        if (requests.isNotEmpty()) {
            insertBloodRequests(requests)
        }
    }

    // Donors
    @Query("SELECT * FROM donors ORDER BY createdAt DESC")
    fun getDonors(): Flow<List<Donor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonor(donor: Donor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonors(donors: List<Donor>)

    @Query("DELETE FROM donors WHERE id = :id")
    suspend fun deleteDonor(id: String)

    @Query("DELETE FROM donors")
    suspend fun clearDonors()

    @Transaction
    suspend fun replaceDonors(donors: List<Donor>) {
        clearDonors()
        if (donors.isNotEmpty()) {
            insertDonors(donors)
        }
    }

    // Banners
    @Query("SELECT * FROM banners")
    fun getBanners(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<Banner>)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBanner(id: String)

    @Query("DELETE FROM banners")
    suspend fun clearBanners()

    @Transaction
    suspend fun replaceBanners(banners: List<Banner>) {
        clearBanners()
        if (banners.isNotEmpty()) {
            insertBanners(banners)
        }
    }

    // Emergency Contacts
    @Query("SELECT * FROM emergency_contacts")
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getEmergencyContactsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(contact: EmergencyContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContacts(contacts: List<EmergencyContact>)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteEmergencyContact(id: String)

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearEmergencyContacts()

    @Transaction
    suspend fun replaceEmergencyContacts(contacts: List<EmergencyContact>) {
        clearEmergencyContacts()
        if (contacts.isNotEmpty()) {
            insertEmergencyContacts(contacts)
        }
    }

    // Users
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): AppUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUser)

    // Donation Records
    @Query("SELECT * FROM donation_records WHERE donorId = :donorId ORDER BY date DESC")
    fun getDonationRecords(donorId: String): Flow<List<DonationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonationRecord(record: DonationRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonationRecords(records: List<DonationRecord>)

    @Query("DELETE FROM donation_records WHERE donorId = :donorId")
    suspend fun clearDonationRecordsForDonor(donorId: String)

    @Transaction
    suspend fun replaceDonationRecordsForDonor(donorId: String, records: List<DonationRecord>) {
        clearDonationRecordsForDonor(donorId)
        if (records.isNotEmpty()) {
            insertDonationRecords(records)
        }
    }
}

@Database(
    entities = [
        BloodRequest::class,
        Donor::class,
        Banner::class,
        EmergencyContact::class,
        DonationRecord::class,
        AppUser::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bloodDao(): BloodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blood_donation_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
