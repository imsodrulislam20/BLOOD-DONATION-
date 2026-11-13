package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    val context = LocalContext.current
    val recentRequests = state.requests.take(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banners slider
        CustomBannerSlider(banners = state.banners)

        // Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.FindDonors) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ডোনার খুঁজুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.BloodRequestForm) },
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("রক্তের আবেদন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatsCard(
                    number = state.donors.size.toString(),
                    label = "রক্তদাতা",
                    icon = Icons.Default.People,
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = CrimsonPrimary,
                    onClick = { viewModel.navigateTo(AppScreen.FindDonors) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatsCard(
                    number = state.requests.size.toString(),
                    label = "মোট আবেদন",
                    icon = Icons.Default.ListAlt,
                    containerColor = Color(0xFFFFF7ED),
                    contentColor = Color(0xFFD97706),
                    onClick = { viewModel.navigateTo(AppScreen.AllRequests) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatsCard(
                    number = state.requests.count { it.status == "pending" }.toString(),
                    label = "অপেক্ষমাণ",
                    icon = Icons.Default.HourglassEmpty,
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = CrimsonPrimary,
                    onClick = { viewModel.navigateTo(AppScreen.AllRequests) }
                )
            }
        }

        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "সাম্প্রতিক আবেদন",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark
            )
            Text(
                text = "সব দেখুন",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CrimsonPrimary,
                modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.AllRequests) }
            )
        }

        if (recentRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো আবেদন নেই",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            recentRequests.forEach { req ->
                BloodRequestCard(
                    request = req,
                    onCallClick = { phone ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun AllRequestsScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    val context = LocalContext.current
    val pendingCount = state.requests.count { it.status == "pending" }
    val completedCount = state.requests.count { it.status == "completed" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⏰ অপেক্ষমাণ: $pendingCount",
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFFD1FAE5), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✓ সম্পন্ন: $completedCount",
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (state.requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো রক্তের আবেদন পাওয়া যায়নি", color = TextMuted, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.requests) { req ->
                    BloodRequestCard(
                        request = req,
                        onCallClick = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FindDonorsScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    val context = LocalContext.current
    var division by remember { mutableStateOf(state.searchDivision) }
    var district by remember { mutableStateOf(state.searchDistrict) }
    var thana by remember { mutableStateOf(state.searchThana) }
    var bloodGroup by remember { mutableStateOf(state.searchBloodGroup) }

    // Dropdown filters state
    var bloodExpanded by remember { mutableStateOf(false) }

    val filteredDonors = state.donors.filter { donor ->
        val matchDiv = division.isEmpty() || donor.division.equals(division, ignoreCase = true)
        val matchDist = district.isEmpty() || donor.district.equals(district, ignoreCase = true)
        val matchThana = thana.isEmpty() || donor.thana.equals(thana, ignoreCase = true)
        val matchBlood = bloodGroup.isEmpty() || donor.bloodGroup.equals(bloodGroup, ignoreCase = true)
        matchDiv && matchDist && matchThana && matchBlood
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "রক্তদাতা খুঁজুন",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )

                // Blood group selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (bloodGroup.isEmpty()) "সকল গ্রুপ" else bloodGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("রক্তের গ্রুপ") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (bloodExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { bloodExpanded = !bloodExpanded }
                    )
                    DropdownMenu(
                        expanded = bloodExpanded,
                        onDismissRequest = { bloodExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("সকল রক্তের গ্রুপ") },
                            onClick = {
                                bloodGroup = ""
                                bloodExpanded = false
                            }
                        )
                        listOf("A+", "B+", "O+", "AB+", "A-", "B-", "O-", "AB-").forEach { bg ->
                            DropdownMenuItem(
                                text = { Text(bg) },
                                onClick = {
                                    bloodGroup = bg
                                    bloodExpanded = false
                                }
                            )
                        }
                    }
                }

                // Bangladesh cascades
                GeoDropdowns(
                    selectedDivision = division,
                    onDivisionSelected = {
                        division = it
                        district = ""
                        thana = ""
                    },
                    selectedDistrict = district,
                    onDistrictSelected = {
                        district = it
                        thana = ""
                    },
                    selectedThana = thana,
                    onThanaSelected = { thana = it }
                )

                Button(
                    onClick = {
                        viewModel.updateSearchFilters(division, district, thana, bloodGroup)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সার্চ করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // Count
        Text(
            text = "${filteredDonors.size} জন ডোনার পাওয়া গেছে",
            fontWeight = FontWeight.Bold,
            color = TextDark,
            fontSize = 15.sp
        )

        if (filteredDonors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো ডোনার পাওয়া যায়নি",
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDonors) { donor ->
                    DonorCard(
                        donor = donor,
                        onCallClick = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        onWhatsAppClick = { phone, bGroup ->
                            val msg = "আসসালামু আলাইকুম, আমি $bGroup রক্তের ডোনার খুঁজছি। আপনার সাথে যোগাযোগ করতে পারি?"
                            val formattedPhone = if (phone.startsWith("0")) "88$phone" else phone
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode(msg)}"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BloodRequestFormScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    var patientName by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var hemoglobin by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("normal") }

    var urgencyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "রক্তের আবেদন ফর্ম",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )

                Text(
                    text = "ℹ একাউন্ট ছাড়াই আবেদন করতে পারবেন",
                    color = CrimsonPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("রোগীর নাম *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                BloodGroupSelector(
                    selectedGroup = bloodGroup,
                    onSelected = { bloodGroup = it }
                )

                OutlinedTextField(
                    value = hemoglobin,
                    onValueChange = { hemoglobin = it },
                    label = { Text("হিমোগ্লোবিন (g/dL) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("যেমন: 7.5") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("হাসপাতাল *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("মোবাইল নম্বর *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("ঠিকানা *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Urgency selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when (urgency) {
                            "normal" -> "স্বাভাবিক"
                            "urgent" -> "জরুরি"
                            else -> "অতি জরুরি"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("জরুরি অবস্থা") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (urgencyExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { urgencyExpanded = !urgencyExpanded }
                    )
                    DropdownMenu(
                        expanded = urgencyExpanded,
                        onDismissRequest = { urgencyExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("স্বাভাবিক") },
                            onClick = {
                                urgency = "normal"
                                urgencyExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("জরুরি") },
                            onClick = {
                                urgency = "urgent"
                                urgencyExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("অতি জরুরি") },
                            onClick = {
                                urgency = "emergency"
                                urgencyExpanded = false
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        val hbDouble = hemoglobin.toDoubleOrNull() ?: 0.0
                        viewModel.submitBloodRequest(
                            patientName, bloodGroup, hbDouble, hospital, contact, location, urgency
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("আবেদন সাবমিট", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactsScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "জরুরী যোগাযোগ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "⏰ অ্যাডমিন আপডেটেড",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.emergencyContacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো যোগাযোগ পাওয়া যায়নি", color = TextMuted, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.emergencyContacts) { contact ->
                    EmergencyContactCard(
                        contact = contact,
                        onCallClick = { num ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                            context.startActivity(intent)
                        },
                        onWhatsAppClick = { num ->
                            val msg = "আসসালামু আলাইকুম, জরুরি প্রয়োজনে যোগাযোগ করছি।"
                            val formattedPhone = if (num.startsWith("0")) "88$num" else num
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode(msg)}"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    val user = state.currentUser

    if (user == null) {
        AuthTabs(viewModel, state)
        return
    }

    var recordDialogExpanded by remember { mutableStateOf(false) }
    var editDialogExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFEF2F2), CircleShape)
                        .border(3.dp, CrimsonPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = user.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )

                Box(
                    modifier = Modifier
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(CrimsonPrimary, CrimsonDark)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = user.bloodGroup.ifEmpty { "N/A" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Points Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${user.points} পয়েন্ট",
                            color = Color(0xFFD97706),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(user.donations.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                            Text("রক্তদান", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { recordDialogExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("রক্তদান রেকর্ড", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Contact Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "যোগাযোগ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    IconButton(onClick = { editDialogExpanded = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = CrimsonPrimary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("মোবাইল: ", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 13.sp)
                    Text(user.phone.ifEmpty { "যোগ করা হয়নি" }, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ইমেইল: ", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 13.sp)
                    Text(user.email, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val address = listOfNotNull(user.division, user.district, user.thana).filter { it.isNotEmpty() }.joinToString(", ")
                    Text("ঠিকানা: ", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 13.sp)
                    Text(address.ifEmpty { "যোগ করা হয়নি" }, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 13.sp)
                }
            }
        }

        // Record manually a donor add page redirect
        Button(
            onClick = { viewModel.navigateTo(AppScreen.AdminPanel) },
            colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Security, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("অ্যাডমিন প্যানেল", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { viewModel.logout() },
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("লগআউট", fontWeight = FontWeight.Bold)
        }
    }

    // Dialogs
    if (recordDialogExpanded) {
        var recordBags by remember { mutableStateOf("1") }
        var recordNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { recordDialogExpanded = false },
            title = { Text("রক্তদান রেকর্ড করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = recordBags,
                        onValueChange = { recordBags = it },
                        label = { Text("ব্যাগ সংখ্যা") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recordNote,
                        onValueChange = { recordNote = it },
                        label = { Text("মন্তব্য (যেমন: হাসপাতালের নাম)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bags = recordBags.toIntOrNull() ?: 1
                        viewModel.addDonationRecord(bags, recordNote)
                        recordDialogExpanded = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("সংরক্ষণ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordDialogExpanded = false }) {
                    Text("বাতিল", color = CrimsonPrimary)
                }
            }
        )
    }

    if (editDialogExpanded) {
        var upName by remember { mutableStateOf(user.name) }
        var upPhone by remember { mutableStateOf(user.phone) }
        var upBlood by remember { mutableStateOf(user.bloodGroup) }
        var upDiv by remember { mutableStateOf(user.division) }
        var upDist by remember { mutableStateOf(user.district) }
        var upThana by remember { mutableStateOf(user.thana) }

        AlertDialog(
            onDismissRequest = { editDialogExpanded = false },
            title = { Text("প্রোফাইল আপডেট করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = upName, onValueChange = { upName = it }, label = { Text("নাম") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upPhone, onValueChange = { upPhone = it }, label = { Text("মোবাইল") }, modifier = Modifier.fillMaxWidth())
                    BloodGroupSelector(selectedGroup = upBlood, onSelected = { upBlood = it })
                    GeoDropdowns(
                        selectedDivision = upDiv,
                        onDivisionSelected = { upDiv = it; upDist = ""; upThana = "" },
                        selectedDistrict = upDist,
                        onDistrictSelected = { upDist = it; upThana = "" },
                        selectedThana = upThana,
                        onThanaSelected = { upThana = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(upName, upBlood, upPhone, upDiv, upDist, upThana)
                        editDialogExpanded = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("আপডেট", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editDialogExpanded = false }) {
                    Text("বাতিল", color = CrimsonPrimary)
                }
            }
        )
    }
}

@Composable
fun AuthTabs(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    var tabIndex by remember { mutableStateOf(0) } // 0: Login, 1: Register

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabRow(
            selectedTabIndex = tabIndex,
            contentColor = CrimsonPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = CrimsonPrimary
                )
            }
        ) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
                Text("লগইন", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
                Text("রেজিস্টার", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
            }
        }

        if (tabIndex == 0) {
            LoginForm(viewModel, state)
        } else {
            RegisterForm(viewModel, state)
        }
    }
}

@Composable
fun LoginForm(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("লগইন করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("ইমেইল") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("পাসওয়ার্ড") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            state.loginError?.let { err ->
                Text(err, color = CrimsonPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.login(email, password) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoggingIn) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("লগইন", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = { viewModel.forgotPassword(email) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("পাসওয়ার্ড ভুলে গেছেন?", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RegisterForm(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("A+") }
    var division by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var thana by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("রেজিস্টার (ডোনার হিসেবে)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("ইমেইল *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("পাসওয়ার্ড (৬+ অক্ষর) *") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            BloodGroupSelector(selectedGroup = bloodGroup, onSelected = { bloodGroup = it })

            GeoDropdowns(
                selectedDivision = division,
                onDivisionSelected = { division = it; district = ""; thana = "" },
                selectedDistrict = district,
                onDistrictSelected = { district = it; thana = "" },
                selectedThana = thana,
                onThanaSelected = { thana = it }
            )

            state.registerError?.let { err ->
                Text(err, color = CrimsonPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.register(name, email, password, bloodGroup, division, district, thana)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isRegistering) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("রেজিস্টার", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    viewModel: BloodViewModel,
    state: AppUiState
) {
    val activeTab = state.adminTab

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = "অ্যাডমিন ড্যাশবোর্ড",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Horizontal tabs for admin controls
        ScrollableTabRow(
            selectedTabIndex = when (activeTab) {
                "dashboard" -> 0
                "requests" -> 1
                "donors" -> 2
                "banners" -> 3
                else -> 4
            },
            contentColor = CrimsonPrimary,
            edgePadding = 0.dp
        ) {
            Tab(selected = activeTab == "dashboard", onClick = { viewModel.switchAdminTab("dashboard") }) {
                Text("সারসংক্ষেপ", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == "requests", onClick = { viewModel.switchAdminTab("requests") }) {
                Text("আবেদনসমূহ", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == "donors", onClick = { viewModel.switchAdminTab("donors") }) {
                Text("ডোনারস", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == "banners", onClick = { viewModel.switchAdminTab("banners") }) {
                Text("ব্যানারস", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == "emergency", onClick = { viewModel.switchAdminTab("emergency") }) {
                Text("জরুরি", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "dashboard" -> AdminDashboardTab(state)
                "requests" -> AdminRequestsTab(viewModel, state)
                "donors" -> AdminDonorsTab(viewModel, state)
                "banners" -> AdminBannersTab(viewModel, state)
                "emergency" -> AdminEmergencyTab(viewModel, state)
            }
        }
    }
}

@Composable
fun AdminDashboardTab(state: AppUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("পরিসংখ্যান", fontWeight = FontWeight.Bold, color = TextDark)
                Text("মোট নিবন্ধিত রক্তদাতা: ${state.donors.size} জন", fontWeight = FontWeight.SemiBold, color = TextMuted)
                Text("মোট রক্তের আবেদন: ${state.requests.size} বার", fontWeight = FontWeight.SemiBold, color = TextMuted)
                Text("সক্রিয় অপেক্ষমাণ আবেদন: ${state.requests.count { it.status == "pending" }} বার", fontWeight = FontWeight.SemiBold, color = TextMuted)
                Text("সফল সম্পন্ন রক্তদান: ${state.requests.count { it.status == "completed" }} বার", fontWeight = FontWeight.SemiBold, color = TextMuted)
            }
        }
    }
}

@Composable
fun AdminRequestsTab(viewModel: BloodViewModel, state: AppUiState) {
    if (state.requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো আবেদন পাওয়া যায়নি")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.requests) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("রোগী: ${req.patientName} (${req.bloodGroup})", fontWeight = FontWeight.Bold, color = TextDark)
                        Text("হাসপাতাল: ${req.hospital}", fontSize = 13.sp)
                        Text("স্ট্যাটাস: ${if (req.status == "completed") "✓ সম্পন্ন" else "⏰ অপেক্ষমাণ"}", fontSize = 13.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (req.status != "completed") {
                                Button(
                                    onClick = { viewModel.completeRequest(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("সম্পন্ন করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { viewModel.deleteRequest(req.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("মুছুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDonorsTab(viewModel: BloodViewModel, state: AppUiState) {
    var dialogOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { dialogOpen = true },
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("নতুন ডোনার যোগ করুন", fontWeight = FontWeight.Bold)
        }

        if (state.donors.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো ডোনার পাওয়া যায়নি")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(state.donors) { donor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(donor.name, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("রক্তের গ্রুপ: ${donor.bloodGroup}", fontSize = 13.sp, color = TextMuted)
                                Text("ফোন: ${donor.phone}", fontSize = 13.sp, color = TextMuted)
                            }
                            IconButton(onClick = { viewModel.deleteDonor(donor.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        var dName by remember { mutableStateOf("") }
        var dBlood by remember { mutableStateOf("A+") }
        var dPhone by remember { mutableStateOf("") }
        var dDiv by remember { mutableStateOf("") }
        var dDist by remember { mutableStateOf("") }
        var dThana by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("নতুন ডোনার যোগ করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = dName, onValueChange = { dName = it }, label = { Text("নাম *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dPhone, onValueChange = { dPhone = it }, label = { Text("মোবাইল") }, modifier = Modifier.fillMaxWidth())
                    BloodGroupSelector(selectedGroup = dBlood, onSelected = { dBlood = it })
                    GeoDropdowns(
                        selectedDivision = dDiv,
                        onDivisionSelected = { dDiv = it; dDist = ""; dThana = "" },
                        selectedDistrict = dDist,
                        onDistrictSelected = { dDist = it; dThana = "" },
                        selectedThana = dThana,
                        onThanaSelected = { dThana = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addDonorManually(dName, dBlood, dPhone, dDiv, dDist, dThana)
                        dialogOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("সংরক্ষণ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("বাতিল", color = CrimsonPrimary)
                }
            }
        )
    }
}

@Composable
fun AdminBannersTab(viewModel: BloodViewModel, state: AppUiState) {
    var bannerUrl by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = bannerUrl,
                onValueChange = { bannerUrl = it },
                label = { Text("ব্যানার ইমেজ URL") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    viewModel.addBanner(bannerUrl)
                    bannerUrl = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("যোগ", fontWeight = FontWeight.Bold)
            }
        }

        if (state.banners.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো ব্যানার ইমেজ পাওয়া যায়নি")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.banners) { b ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = b.imageUrl,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Banner", tint = CrimsonPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminEmergencyTab(viewModel: BloodViewModel, state: AppUiState) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("নতুন জরুরি যোগাযোগ যোগ করুন", fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("নম্বর *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("অবস্থান") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("বিবরণ") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        viewModel.addEmergencyContact(name, number, location, desc)
                        name = ""; number = ""; location = ""; desc = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("সংরক্ষণ", fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.emergencyContacts) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(c.name, fontWeight = FontWeight.Bold)
                            Text("📞 ${c.number}", fontSize = 13.sp)
                        }
                        IconButton(onClick = { viewModel.deleteEmergencyContact(c.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonPrimary)
                        }
                    }
                }
            }
        }
    }
}
