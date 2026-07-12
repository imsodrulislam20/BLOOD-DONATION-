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

    @Query("DELETE FROM blood_requests WHERE id = :id")
    suspend fun deleteBloodRequest(id: String)

    @Query("DELETE FROM blood_requests")
    suspend fun clearBloodRequests()

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

    // Banners
    @Query("SELECT * FROM banners")
    fun getBanners(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<Banner>)

    @Query("DELETE FROM banners")
    suspend fun clearBanners()

    // Emergency Contacts
    @Query("SELECT * FROM emergency_contacts")
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContacts(contacts: List<EmergencyContact>)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteEmergencyContact(id: String)

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearEmergencyContacts()

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
