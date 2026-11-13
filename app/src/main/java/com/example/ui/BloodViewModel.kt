package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MyFirebaseMessagingService
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BloodViewModel(
    application: Application,
    private val repository: BloodDonationRepository
) : AndroidViewModel(application) {

    private val TAG = "BloodViewModel"

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()
    private var userObserverJob: Job? = null

    init {
        viewModelScope.launch {
            repository.bloodRequests.collect { reqs ->
                _uiState.update { it.copy(requests = reqs) }
            }
        }
        viewModelScope.launch {
            repository.donors.collect { dns ->
                _uiState.update { it.copy(donors = dns) }
            }
        }
        viewModelScope.launch {
            repository.banners.collect { bn ->
                _uiState.update { it.copy(banners = bn) }
            }
        }
        viewModelScope.launch {
            repository.emergencyContacts.collect { contacts ->
                _uiState.update { it.copy(emergencyContacts = contacts) }
            }
        }
        viewModelScope.launch {
            repository.isRealtimeActive.collect { active ->
                _uiState.update { it.copy(isRealtimeActive = active) }
            }
        }

        // Observe FirebaseAuth state and attach real-time Firestore user profile listener
        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            val auth = FirebaseManager.auth!!
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                userObserverJob?.cancel()
                if (user != null) {
                    viewModelScope.launch {
                        val loadedUser = repository.getOrFetchUser(
                            uid = user.uid,
                            fallbackName = user.displayName ?: "ইউজার",
                            fallbackEmail = user.email ?: ""
                        )
                        _uiState.update { it.copy(currentUser = loadedUser) }
                        registerFcmToken()
                    }
                    userObserverJob = viewModelScope.launch {
                        repository.observeUserRealtime(user.uid).collect { realtimeUser ->
                            if (realtimeUser != null) {
                                _uiState.update { it.copy(currentUser = realtimeUser) }
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(currentUser = null) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userObserverJob?.cancel()
        repository.cleanup()
    }

    private fun registerFcmToken() {
        if (FirebaseManager.isInitialized && FirebaseManager.messaging != null) {
            FirebaseManager.messaging!!.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token registered: $token")
                    MyFirebaseMessagingService.saveTokenToDatabase(token)
                    _uiState.update { it.copy(isFcmActive = true) }
                }
            }
        }
    }

    // Navigation
    fun navigateTo(screen: AppScreen) {
        if (_uiState.value.currentScreen != screen) {
            backStack.add(_uiState.value.currentScreen)
            _uiState.update { it.copy(currentScreen = screen) }
        }
    }

    fun handleBackPress(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _uiState.update { it.copy(currentScreen = prev) }
            return true
        }
        return false
    }

    // Feedback helpers
    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun showToast(success: String? = null, error: String? = null) {
        _uiState.update { it.copy(successMessage = success, errorMessage = error) }
    }

    // Auth flows
    fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(loginError = "সব তথ্য দিন") }
            return
        }

        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }

        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            viewModelScope.launch {
                try {
                    val result = FirebaseManager.auth!!.signInWithEmailAndPassword(trimmedEmail, pass).await()
                    val user = result.user
                    if (user != null) {
                        val loadedUser = repository.getOrFetchUser(
                            uid = user.uid,
                            fallbackName = user.displayName ?: "ইউজার",
                            fallbackEmail = user.email ?: trimmedEmail
                        )
                        _uiState.update { it.copy(currentUser = loadedUser, isLoggingIn = false) }
                        showToast(success = "লগইন সফল হয়েছে!")
                    } else {
                        _uiState.update { it.copy(isLoggingIn = false, loginError = "লগইন ব্যর্থ হয়েছে") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Login failed: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            loginError = "ভুল ইমেইল অথবা পাসওয়ার্ড"
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoggingIn = false, loginError = "ফায়ারবেজ সংযোগ পাওয়া যায়নি") }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        bloodGroup: String,
        division: String,
        district: String,
        thana: String
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(registerError = "নাম, ইমেইল ও পাসওয়ার্ড আবশ্যক") }
            return
        }
        if (pass.length < 6) {
            _uiState.update { it.copy(registerError = "পাসওয়ার্ড ৬+ অক্ষর হতে হবে") }
            return
        }

        _uiState.update { it.copy(isRegistering = true, registerError = null) }

        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            viewModelScope.launch {
                try {
                    val result = FirebaseManager.auth!!.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                    val user = result.user
                    if (user != null) {
                        val appUser = AppUser(
                            uid = user.uid,
                            name = trimmedName,
                            email = trimmedEmail,
                            bloodGroup = bloodGroup,
                            division = division,
                            district = district,
                            thana = thana,
                            isDonor = true,
                            createdAt = System.currentTimeMillis()
                        )
                        repository.upsertUser(appUser)
                        _uiState.update { it.copy(currentUser = appUser, isRegistering = false) }
                        showToast(success = "রেজিস্ট্রেশন সফল হয়েছে!")
                    } else {
                        _uiState.update { it.copy(isRegistering = false, registerError = "রেজিস্ট্রেশন ব্যর্থ হয়েছে") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Registration failed: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            registerError = e.localizedMessage ?: "ইমেইল ইতিমধ্যেই ব্যবহৃত বা ত্রুটি ঘটেছে"
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isRegistering = false, registerError = "ফায়ারবেজ সংযোগ পাওয়া যায়নি") }
        }
    }

    fun logout() {
        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            FirebaseManager.auth!!.signOut()
        }
        userObserverJob?.cancel()
        _uiState.update { it.copy(currentUser = null) }
        showToast(success = "লগআউট সম্পন্ন হয়েছে")
        navigateTo(AppScreen.Home)
    }

    fun forgotPassword(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            showToast(error = "ইমেইল দিন")
            return
        }
        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            FirebaseManager.auth!!.sendPasswordResetEmail(trimmedEmail)
                .addOnSuccessListener {
                    showToast(success = "পাসওয়ার্ড রিসেট লিঙ্ক আপনার ইমেইলে পাঠানো হয়েছে!")
                }
                .addOnFailureListener { e ->
                    showToast(error = "রিসেট ব্যর্থ: ${e.message}")
                }
        } else {
            showToast(error = "ফায়ারবেজ সংযোগ পাওয়া যায়নি")
        }
    }

    fun updateProfile(
        name: String,
        bloodGroup: String,
        phone: String,
        division: String,
        district: String,
        thana: String
    ) {
        val user = _uiState.value.currentUser ?: return
        val updatedUser = user.copy(
            name = name.trim(),
            bloodGroup = bloodGroup,
            phone = phone.trim(),
            division = division,
            district = district,
            thana = thana
        )

        viewModelScope.launch {
            try {
                repository.upsertUser(updatedUser)
                _uiState.update { it.copy(currentUser = updatedUser) }
                showToast(success = "প্রোফাইল সফলভাবে আপডেট হয়েছে!")
                navigateTo(AppScreen.Profile)
            } catch (e: Exception) {
                Log.e(TAG, "Profile update failed: ${e.message}")
                showToast(error = "প্রোফাইল আপডেট ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    // Blood Request submissions
    fun submitBloodRequest(
        patientName: String,
        bloodGroup: String,
        hemoglobin: Double,
        hospital: String,
        contact: String,
        location: String,
        urgency: String
    ) {
        if (patientName.isBlank() || bloodGroup.isBlank() || hospital.isBlank() || contact.isBlank() || location.isBlank()) {
            showToast(error = "সব তথ্য সঠিক মত দিন")
            return
        }

        viewModelScope.launch {
            try {
                val req = BloodRequest(
                    patientName = patientName.trim(),
                    bloodGroup = bloodGroup,
                    hemoglobin = hemoglobin,
                    hospital = hospital.trim(),
                    contact = contact.trim(),
                    location = location.trim(),
                    urgency = urgency,
                    createdBy = _uiState.value.currentUser?.uid ?: "anonymous",
                    createdAt = System.currentTimeMillis()
                )
                repository.insertBloodRequest(req)
                showToast(success = "আবেদনটি সফলভাবে করা হয়েছে!")
                navigateTo(AppScreen.Home)
            } catch (e: Exception) {
                Log.e(TAG, "Submit request failed: ${e.message}")
                showToast(error = "আবেদন সাবমিট ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    // Add Donor manually
    fun addDonorManually(
        name: String,
        bloodGroup: String,
        phone: String,
        division: String,
        district: String,
        thana: String
    ) {
        if (name.isBlank() || bloodGroup.isBlank()) {
            showToast(error = "নাম ও রক্তের গ্রুপ আবশ্যক")
            return
        }

        viewModelScope.launch {
            try {
                val donor = Donor(
                    name = name.trim(),
                    bloodGroup = bloodGroup,
                    phone = phone.trim(),
                    division = division,
                    district = district,
                    thana = thana,
                    addedBy = _uiState.value.currentUser?.uid ?: "anonymous",
                    addedByEmail = _uiState.value.currentUser?.email ?: "anonymous@blood.com"
                )
                repository.insertDonor(donor)

                // Reward points to current logged-in user and sync to Firestore
                _uiState.value.currentUser?.let { user ->
                    val updatedUser = user.copy(
                        points = user.points + 5,
                        donorsAdded = user.donorsAdded + 1
                    )
                    repository.upsertUser(updatedUser)
                    _uiState.update { it.copy(currentUser = updatedUser) }
                }

                showToast(success = "ডোনার সফলভাবে যোগ হয়েছে! (+৫ পয়েন্ট)")
                navigateTo(AppScreen.Home)
            } catch (e: Exception) {
                Log.e(TAG, "Add donor failed: ${e.message}")
                showToast(error = "ডোনার যোগ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    // Donation Records
    fun addDonationRecord(bags: Int, note: String) {
        val user = _uiState.value.currentUser ?: return
        val safeBags = bags.coerceAtLeast(1)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val record = DonationRecord(
                    donorId = user.uid,
                    date = now,
                    bags = safeBags,
                    note = note.trim()
                )
                repository.addDonationRecord(record)

                val updatedUser = user.copy(
                    donations = user.donations + safeBags,
                    points = user.points + (safeBags * 10),
                    lastDonationDate = now
                )
                repository.upsertUser(updatedUser)
                _uiState.update { it.copy(currentUser = updatedUser) }

                showToast(success = "রক্তদান রেকর্ড সফলভাবে সংরক্ষণ করা হয়েছে! (+${safeBags * 10} পয়েন্ট)")
                navigateTo(AppScreen.Profile)
            } catch (e: Exception) {
                Log.e(TAG, "Add donation record failed: ${e.message}")
                showToast(error = "রেকর্ড সংরক্ষণ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    // Admin Interactions
    fun switchAdminTab(tab: String) {
        _uiState.update { it.copy(adminTab = tab) }
    }

    fun completeRequest(requestId: String) {
        viewModelScope.launch {
            try {
                repository.updateBloodRequestStatus(requestId, "completed")
                showToast(success = "আবেদনটি সম্পন্ন করা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "আপডেট ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun deleteRequest(requestId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBloodRequest(requestId)
                showToast(success = "আবেদনটি মুছে ফেলা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "মুছে ফেলা ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun deleteDonor(donorId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDonor(donorId)
                showToast(success = "ডোনার মুছে ফেলা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "মুছে ফেলা ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun addBanner(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            try {
                repository.insertBanner(Banner(imageUrl = url.trim()))
                showToast(success = "নতুন ব্যানার যোগ করা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "ব্যানার যোগ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun deleteBanner(bannerId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBanner(bannerId)
                showToast(success = "ব্যানার মুছে ফেলা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "মুছে ফেলা ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun addEmergencyContact(name: String, number: String, location: String, description: String) {
        if (name.isBlank() || number.isBlank()) return
        viewModelScope.launch {
            try {
                val contact = EmergencyContact(
                    name = name.trim(),
                    number = number.trim(),
                    location = location.trim(),
                    description = description.trim()
                )
                repository.insertEmergencyContact(contact)
                showToast(success = "নতুন জরুরি যোগাযোগ যোগ করা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "যোগাযোগ যোগ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEmergencyContact(contactId)
                showToast(success = "যোগাযোগটি মুছে ফেলা হয়েছে!")
            } catch (e: Exception) {
                showToast(error = "মুছে ফেলা ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    // Search Query filters
    fun updateSearchFilters(
        division: String,
        district: String,
        thana: String,
        bloodGroup: String
    ) {
        _uiState.update {
            it.copy(
                searchDivision = division,
                searchDistrict = district,
                searchThana = thana,
                searchBloodGroup = bloodGroup
            )
        }
    }

    fun updateNotificationPermission(granted: Boolean) {
        _uiState.update { it.copy(isNotificationPermissionGranted = granted) }
        if (granted) {
            registerFcmToken()
        }
    }
}

class BloodViewModelFactory(
    private val application: Application,
    private val repository: BloodDonationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BloodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BloodViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
