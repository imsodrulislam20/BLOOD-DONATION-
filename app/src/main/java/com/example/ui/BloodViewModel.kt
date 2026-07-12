package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MyFirebaseMessagingService
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BloodViewModel(
    application: Application,
    private val repository: BloodDonationRepository
) : AndroidViewModel(application) {

    private val TAG = "BloodViewModel"

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()

    init {
        // Collect flows from Repository
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

        // Initialize user status from FirebaseAuth if available
        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            val auth = FirebaseManager.auth!!
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    viewModelScope.launch {
                        // Load full user details from local DB or fallback
                        val appDatabase = AppDatabase.getDatabase(getApplication())
                        val localUser = appDatabase.bloodDao().getUserById(user.uid)
                        if (localUser != null) {
                            _uiState.update { it.copy(currentUser = localUser) }
                        } else {
                            val tempUser = AppUser(
                                uid = user.uid,
                                name = user.displayName ?: "ইউজার",
                                email = user.email ?: ""
                            )
                            _uiState.update { it.copy(currentUser = tempUser) }
                        }
                        
                        // Register Messaging Token
                        registerFcmToken()
                    }
                } else {
                    _uiState.update { it.copy(currentUser = null) }
                }
            }
        }
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
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(loginError = "সব তথ্য দিন") }
            return
        }

        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }

        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            FirebaseManager.auth!!.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        viewModelScope.launch {
                            val appDatabase = AppDatabase.getDatabase(getApplication())
                            val localUser = appDatabase.bloodDao().getUserById(user.uid)
                            _uiState.update {
                                it.copy(
                                    currentUser = localUser ?: AppUser(uid = user.uid, name = user.displayName ?: "ইউজার", email = email),
                                    isLoggingIn = false
                                )
                            }
                            showToast(success = "লগইন সফল হয়েছে!")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Login failed: ${e.message}")
                    _uiState.update { it.copy(isLoggingIn = false, loginError = "ভুল ইমেইল অথবা পাসওয়ার্ড") }
                }
        } else {
            // Offline local-only login mock fallback for frictionless development/previewing
            _uiState.update { it.copy(isLoggingIn = false) }
            viewModelScope.launch {
                val appDatabase = AppDatabase.getDatabase(getApplication())
                val localUser = appDatabase.bloodDao().getUserById("offline_demo_user")
                if (localUser != null && localUser.email == email) {
                    _uiState.update { it.copy(currentUser = localUser) }
                    showToast(success = "লগইন সফল হয়েছে (অফলাইন মোড)!")
                } else {
                    // Create direct offline demo user on first attempt
                    val newUser = AppUser(
                        uid = "offline_demo_user",
                        name = "সদস্য",
                        email = email,
                        isDonor = true
                    )
                    appDatabase.bloodDao().insertUser(newUser)
                    _uiState.update { it.copy(currentUser = newUser) }
                    showToast(success = "রেজিস্ট্রেশন এবং লগইন সফল হয়েছে (অফলাইন মোড)!")
                }
            }
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
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(registerError = "নাম, ইমেইল ও পাসওয়ার্ড আবশ্যক") }
            return
        }
        if (pass.length < 6) {
            _uiState.update { it.copy(registerError = "পাসওয়ার্ড ৬+ অক্ষর হতে হবে") }
            return
        }

        _uiState.update { it.copy(isRegistering = true, registerError = null) }

        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            FirebaseManager.auth!!.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        viewModelScope.launch {
                            val appUser = AppUser(
                                uid = user.uid,
                                name = name,
                                email = email,
                                bloodGroup = bloodGroup,
                                division = division,
                                district = district,
                                thana = thana,
                                isDonor = true
                            )
                            val appDatabase = AppDatabase.getDatabase(getApplication())
                            appDatabase.bloodDao().insertUser(appUser)
                            
                            // Insert into Donor list as well
                            val donor = Donor(
                                id = user.uid,
                                name = name,
                                bloodGroup = bloodGroup,
                                division = division,
                                district = district,
                                thana = thana,
                                addedByEmail = email
                            )
                            repository.insertDonor(donor)

                            _uiState.update { it.copy(currentUser = appUser, isRegistering = false) }
                            showToast(success = "রেজিস্ট্রেশন সফল হয়েছে!")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Registration failed: ${e.message}")
                    _uiState.update { it.copy(isRegistering = false, registerError = "ইমেইল ইতিমধ্যেই ব্যবহৃত") }
                }
        } else {
            // Local fallback
            viewModelScope.launch {
                val appUser = AppUser(
                    uid = "offline_demo_user",
                    name = name,
                    email = email,
                    bloodGroup = bloodGroup,
                    division = division,
                    district = district,
                    thana = thana,
                    isDonor = true
                )
                val appDatabase = AppDatabase.getDatabase(getApplication())
                appDatabase.bloodDao().insertUser(appUser)
                
                val donor = Donor(
                    id = "offline_demo_user",
                    name = name,
                    bloodGroup = bloodGroup,
                    division = division,
                    district = district,
                    thana = thana,
                    addedByEmail = email
                )
                repository.insertDonor(donor)

                _uiState.update { it.copy(currentUser = appUser, isRegistering = false) }
                showToast(success = "রেজিস্ট্রেশন সফল হয়েছে (অফলাইন মোড)!")
            }
        }
    }

    fun logout() {
        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            FirebaseManager.auth!!.signOut()
        }
        _uiState.update { it.copy(currentUser = null) }
        showToast(success = "লগআউট সম্পন্ন হয়েছে")
        navigateTo(AppScreen.Home)
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            showToast(error = "ইমেইল দিন")
            return
        }
        if (FirebaseManager.isInitialized && FirebaseManager.auth != null) {
            FirebaseManager.auth!!.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    showToast(success = "পাসওয়ার্ড রিসেট লিঙ্ক আপনার ইমেইলে পাঠানো হয়েছে!")
                }
                .addOnFailureListener { e ->
                    showToast(error = "রিসেট ব্যর্থ: ${e.message}")
                }
        } else {
            showToast(success = "রিসেট লিঙ্ক পাঠানো হয়েছে (ডেমো মোড)!")
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
            name = name,
            bloodGroup = bloodGroup,
            phone = phone,
            division = division,
            district = district,
            thana = thana
        )

        viewModelScope.launch {
            val appDatabase = AppDatabase.getDatabase(getApplication())
            appDatabase.bloodDao().insertUser(updatedUser)
            _uiState.update { it.copy(currentUser = updatedUser) }

            // If user is also a donor, sync/update the donor entry
            if (user.isDonor) {
                val donor = Donor(
                    id = user.uid,
                    name = name,
                    bloodGroup = bloodGroup,
                    phone = phone,
                    division = division,
                    district = district,
                    thana = thana,
                    donations = user.donations,
                    points = user.points,
                    lastDonationDate = user.lastDonationDate
                )
                repository.insertDonor(donor)
            }
            showToast(success = "প্রোফাইল সফলভাবে আপডেট হয়েছে!")
            navigateTo(AppScreen.Profile)
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
            val req = BloodRequest(
                patientName = patientName,
                bloodGroup = bloodGroup,
                hemoglobin = hemoglobin,
                hospital = hospital,
                contact = contact,
                location = location,
                urgency = urgency,
                createdBy = _uiState.value.currentUser?.uid ?: "anonymous",
                createdAt = System.currentTimeMillis()
            )
            repository.insertBloodRequest(req)
            showToast(success = "আবেদনটি সফলভাবে করা হয়েছে!")
            navigateTo(AppScreen.Home)
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
            val donor = Donor(
                name = name,
                bloodGroup = bloodGroup,
                phone = phone,
                division = division,
                district = district,
                thana = thana,
                addedBy = _uiState.value.currentUser?.uid ?: "anonymous",
                addedByEmail = _uiState.value.currentUser?.email ?: "anonymous@blood.com"
            )
            repository.insertDonor(donor)

            // Reward points to current logged-in user
            _uiState.value.currentUser?.let { user ->
                val updatedUser = user.copy(
                    points = user.points + 5,
                    donorsAdded = user.donorsAdded + 1
                )
                val appDatabase = AppDatabase.getDatabase(getApplication())
                appDatabase.bloodDao().insertUser(updatedUser)
                _uiState.update { it.copy(currentUser = updatedUser) }
            }

            showToast(success = "ডোনার সফলভাবে যোগ হয়েছে! (+৫ পয়েন্ট)")
            navigateTo(AppScreen.Home)
        }
    }

    // Donation Records
    fun addDonationRecord(bags: Int, note: String) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val record = DonationRecord(
                donorId = user.uid,
                date = System.currentTimeMillis(),
                bags = bags,
                note = note
            )
            repository.addDonationRecord(record)

            val updatedUser = user.copy(
                donations = user.donations + bags,
                points = user.points + (bags * 10),
                lastDonationDate = System.currentTimeMillis()
            )
            val appDatabase = AppDatabase.getDatabase(getApplication())
            appDatabase.bloodDao().insertUser(updatedUser)
            _uiState.update { it.copy(currentUser = updatedUser) }

            // Sync donor info
            if (user.isDonor) {
                val donor = Donor(
                    id = user.uid,
                    name = user.name,
                    bloodGroup = user.bloodGroup,
                    phone = user.phone,
                    division = user.division,
                    district = user.district,
                    thana = user.thana,
                    donations = updatedUser.donations,
                    points = updatedUser.points,
                    lastDonationDate = updatedUser.lastDonationDate
                )
                repository.insertDonor(donor)
            }

            showToast(success = "রক্তদান রেকর্ড সফলভাবে সংরক্ষণ করা হয়েছে! (+${bags * 10} পয়েন্ট)")
            navigateTo(AppScreen.Profile)
        }
    }

    // Admin Interactions
    fun switchAdminTab(tab: String) {
        _uiState.update { it.copy(adminTab = tab) }
    }

    fun completeRequest(requestId: String) {
        viewModelScope.launch {
            repository.updateBloodRequestStatus(requestId, "completed")
            showToast(success = "আবেদনটি সম্পন্ন করা হয়েছে!")
        }
    }

    fun deleteRequest(requestId: String) {
        viewModelScope.launch {
            repository.deleteBloodRequest(requestId)
            showToast(success = "আবেদনটি মুছে ফেলা হয়েছে!")
        }
    }

    fun deleteDonor(donorId: String) {
        viewModelScope.launch {
            repository.deleteDonor(donorId)
            showToast(success = "ডোনার মুছে ফেলা হয়েছে!")
        }
    }

    fun addBanner(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            repository.insertBanner(Banner(imageUrl = url))
            showToast(success = "নতুন ব্যানার যোগ করা হয়েছে!")
        }
    }

    fun deleteBanner(bannerId: String) {
        viewModelScope.launch {
            repository.deleteBanner(bannerId)
            showToast(success = "ব্যানার মুছে ফেলা হয়েছে!")
        }
    }

    fun addEmergencyContact(name: String, number: String, location: String, description: String) {
        if (name.isBlank() || number.isBlank()) return
        viewModelScope.launch {
            val contact = EmergencyContact(name = name, number = number, location = location, description = description)
            repository.insertEmergencyContact(contact)
            showToast(success = "নতুন জরুরি যোগাযোগ যোগ করা হয়েছে!")
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(contactId)
            showToast(success = "যোগাযোগটি মুছে ফেলা হয়েছে!")
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
