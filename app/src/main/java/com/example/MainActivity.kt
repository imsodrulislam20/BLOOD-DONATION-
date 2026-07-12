package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely Initialize Firebase Client
        FirebaseManager.initialize(applicationContext)

        // Instantiate Local Room Database, Repository & ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BloodDonationRepository(database.bloodDao(), lifecycleScope)
        val viewModel = ViewModelProvider(
            this,
            BloodViewModelFactory(application, repository)
        )[BloodViewModel::class.java]

        // Handle System Back Presses with Custom State Back stack
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.handleBackPress()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Request Push Notification Permission on API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                viewModel.updateNotificationPermission(isGranted)
            }
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.updateNotificationPermission(true)
        }

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: BloodViewModel) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe feedbacks/toasts
    state.successMessage?.let { success ->
        CrimsonToast(message = success, isError = false) {
            viewModel.clearFeedback()
        }
    }
    state.errorMessage?.let { error ->
        CrimsonToast(message = error, isError = true) {
            viewModel.clearFeedback()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color.White
            ) {
                // Drawer Header matching HTML Sidebar Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrimsonPrimary)
                        .padding(vertical = 32.dp, horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "স্বেচ্ছায় রক্তদান",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "জীবন বাঁচানোর অঙ্গীকার",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Drawer items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null, tint = CrimsonPrimary) },
                    label = { Text("হোম", fontWeight = FontWeight.Bold, color = TextDark) },
                    selected = state.currentScreen == AppScreen.Home,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.Home)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary) },
                    label = { Text("ডোনার খুঁজুন", fontWeight = FontWeight.Bold, color = TextDark) },
                    selected = state.currentScreen == AppScreen.FindDonors,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.FindDonors)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = CrimsonPrimary) },
                    label = { Text("রক্তের আবেদন", fontWeight = FontWeight.Bold, color = TextDark) },
                    selected = state.currentScreen == AppScreen.BloodRequestForm,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.BloodRequestForm)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Call, contentDescription = null, tint = CrimsonPrimary) },
                    label = { Text("জরুরী যোগাযোগ", fontWeight = FontWeight.Bold, color = TextDark) },
                    selected = state.currentScreen == AppScreen.EmergencyContacts,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.EmergencyContacts)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null, tint = CrimsonPrimary) },
                    label = { Text("প্রোফাইল", fontWeight = FontWeight.Bold, color = TextDark) },
                    selected = state.currentScreen == AppScreen.Profile,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.Profile)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Security, contentDescription = null, tint = CrimsonPrimary) },
                    label = { Text("অ্যাডমিন প্যানেল", fontWeight = FontWeight.Bold, color = TextDark) },
                    selected = state.currentScreen == AppScreen.AdminPanel,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.AdminPanel)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Social CTA links at the bottom of the drawer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/profile.php?id=100083044966529"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Facebook Page", tint = CrimsonPrimary)
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801718142072"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp Contact", tint = WhatsApp)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "স্বেচ্ছায় রক্তদান",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = CrimsonPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = CrimsonPrimary)
                        }
                    },
                    actions = {
                        // Safe notification state indicator
                        if (state.isFcmActive) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Push Notifications Connected",
                                tint = Success,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Push Notifications Disabled",
                                tint = TextMuted,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        viewModel.showToast(success = "পুশ নোটিফিকেশন অন করা হয়েছে!")
                                    }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("হোম", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = state.currentScreen == AppScreen.Home,
                        onClick = { viewModel.navigateTo(AppScreen.Home) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            selectedTextColor = CrimsonPrimary,
                            indicatorColor = CrimsonLight
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("ডোনার", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = state.currentScreen == AppScreen.FindDonors,
                        onClick = { viewModel.navigateTo(AppScreen.FindDonors) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            selectedTextColor = CrimsonPrimary,
                            indicatorColor = CrimsonLight
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("আবেদন", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = state.currentScreen == AppScreen.BloodRequestForm,
                        onClick = { viewModel.navigateTo(AppScreen.BloodRequestForm) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            selectedTextColor = CrimsonPrimary,
                            indicatorColor = CrimsonLight
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        label = { Text("জরুরী", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = state.currentScreen == AppScreen.EmergencyContacts,
                        onClick = { viewModel.navigateTo(AppScreen.EmergencyContacts) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            selectedTextColor = CrimsonPrimary,
                            indicatorColor = CrimsonLight
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("প্রোফাইল", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = state.currentScreen == AppScreen.Profile,
                        onClick = { viewModel.navigateTo(AppScreen.Profile) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrimsonPrimary,
                            selectedTextColor = CrimsonPrimary,
                            indicatorColor = CrimsonLight
                        )
                    )
                }
            },
            floatingActionButton = {
                if (state.currentScreen == AppScreen.Home || state.currentScreen == AppScreen.AllRequests) {
                    FloatingActionButton(
                        onClick = { viewModel.navigateTo(AppScreen.BloodRequestForm) },
                        containerColor = CrimsonPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Request")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BgLight)
            ) {
                when (state.currentScreen) {
                    AppScreen.Home -> HomeScreen(viewModel, state)
                    AppScreen.AllRequests -> AllRequestsScreen(viewModel, state)
                    AppScreen.FindDonors -> FindDonorsScreen(viewModel, state)
                    AppScreen.BloodRequestForm -> BloodRequestFormScreen(viewModel, state)
                    AppScreen.EmergencyContacts -> EmergencyContactsScreen(viewModel, state)
                    AppScreen.Profile -> ProfileScreen(viewModel, state)
                    AppScreen.AdminPanel -> AdminPanelScreen(viewModel, state)
                }
            }
        }
    }
}
