package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
@Entity(tableName = "blood_requests")
data class BloodRequest(
    @PrimaryKey val id: String = "",
    val patientName: String = "",
    val bloodGroup: String = "",
    val hemoglobin: Double = 0.0,
    val hospital: String = "",
    val contact: String = "",
    val location: String = "",
    val urgency: String = "normal", // normal, urgent, emergency
    val status: String = "pending", // pending, completed
    val createdBy: String = "anonymous",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@IgnoreExtraProperties
@Entity(tableName = "donors")
data class Donor(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val division: String = "",
    val district: String = "",
    val thana: String = "",
    val donations: Int = 0,
    val points: Int = 0,
    val addedBy: String = "",
    val addedByEmail: String = "",
    val lastDonationDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val photoURL: String? = null
) : Serializable

@IgnoreExtraProperties
@Entity(tableName = "banners")
data class Banner(
    @PrimaryKey val id: String = "",
    val imageUrl: String = ""
) : Serializable

@IgnoreExtraProperties
@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val number: String = "",
    val location: String = "",
    val description: String = ""
) : Serializable

@IgnoreExtraProperties
@Entity(tableName = "donation_records")
data class DonationRecord(
    @PrimaryKey val id: String = "",
    val donorId: String = "",
    val date: Long = System.currentTimeMillis(),
    val bags: Int = 1,
    val note: String = ""
) : Serializable

@IgnoreExtraProperties
@Entity(tableName = "users")
data class AppUser(
    @PrimaryKey val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val division: String = "",
    val district: String = "",
    val thana: String = "",
    val donations: Int = 0,
    val points: Int = 0,
    val lastDonationDate: Long? = null,
    @get:PropertyName("isDonor")
    @set:PropertyName("isDonor")
    var isDonor: Boolean = true,
    val donorsAdded: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
