package com.example.ui

import com.example.data.*

sealed interface AppScreen {
    object Home : AppScreen
    object AllRequests : AppScreen
    object FindDonors : AppScreen
    object BloodRequestForm : AppScreen
    object EmergencyContacts : AppScreen
    object Profile : AppScreen
    object AdminPanel : AppScreen
}

data class AppUiState(
    val currentScreen: AppScreen = AppScreen.Home,
    val requests: List<BloodRequest> = emptyList(),
    val donors: List<Donor> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val isRealtimeActive: Boolean = false,

    // Authenticated User Info
    val currentUser: AppUser? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val isRegistering: Boolean = false,
    val registerError: String? = null,

    // Geo Selections for Search
    val searchDivision: String = "",
    val searchDistrict: String = "",
    val searchThana: String = "",
    val searchBloodGroup: String = "",

    // Admin Management Tabs
    val adminTab: String = "dashboard", // dashboard, donors, requests, banners, emergency

    // Status Feedbacks
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Push Notifications Status
    val isNotificationPermissionGranted: Boolean = false,
    val isFcmActive: Boolean = false
)
