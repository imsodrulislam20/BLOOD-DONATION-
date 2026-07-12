package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CrimsonToast(
    message: String,
    isError: Boolean = false,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        delay(3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = if (isError) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                val stripeColor = if (isError) Color(0xFFEF4444) else Color(0xFF10B981)
                drawRect(
                    color = stripeColor,
                    size = this.size.copy(width = 6.dp.toPx())
                )
            }
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Cancel else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) Color(0xFFEF4444) else Color(0xFF10B981)
            )
            Text(
                text = message,
                color = if (isError) Color(0xFF991B1B) else Color(0xFF065F46),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CustomBannerSlider(
    banners: List<Banner>,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CrimsonPrimary, CrimsonDark)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "রক্তদান করুন, জীবন বাঁচান",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "আপনার এক ব্যাগ রক্ত বাঁচাতে পারে একটি পরিবারের সব স্বপ্ন",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(banners) {
        while (true) {
            delay(5000)
            currentIndex = (currentIndex + 1) % banners.size
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = banners[currentIndex].imageUrl,
                contentDescription = "Banner Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            banners.forEachIndexed { index, _ ->
                val active = index == currentIndex
                val width by animateDpAsState(targetValue = if (active) 24.dp else 8.dp)
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (active) CrimsonPrimary else Color.LightGray)
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    number: String,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = number,
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BloodGroupSelector(
    selectedGroup: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = listOf("A+", "B+", "O+", "AB+", "A-", "B-", "O-", "AB-")
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedGroup,
            onValueChange = {},
            readOnly = true,
            label = { Text("রক্তের গ্রুপ *") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onSelected(group)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GeoDropdowns(
    selectedDivision: String,
    onDivisionSelected: (String) -> Unit,
    selectedDistrict: String,
    onDistrictSelected: (String) -> Unit,
    selectedThana: String,
    onThanaSelected: (String) -> Unit
) {
    var divExpanded by remember { mutableStateOf(false) }
    var distExpanded by remember { mutableStateOf(false) }
    var thanaExpanded by remember { mutableStateOf(false) }

    val divisions = BangladeshData.data.keys.toList()
    val districts = BangladeshData.data[selectedDivision]?.keys?.toList() ?: emptyList()
    val thanas = BangladeshData.data[selectedDivision]?.get(selectedDistrict) ?: emptyList()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Division Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (selectedDivision.isNotEmpty()) "$selectedDivision বিভাগ" else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("বিভাগ") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.clickable { divExpanded = !divExpanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { divExpanded = !divExpanded },
                shape = RoundedCornerShape(12.dp)
            )
            DropdownMenu(
                expanded = divExpanded,
                onDismissRequest = { divExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("বিভাগ নির্বাচন করুন", color = Color.Gray) },
                    onClick = {
                        onDivisionSelected("")
                        divExpanded = false
                    }
                )
                divisions.forEach { div ->
                    DropdownMenuItem(
                        text = { Text("$div বিভাগ") },
                        onClick = {
                            onDivisionSelected(div)
                            divExpanded = false
                        }
                    )
                }
            }
        }

        // District Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedDistrict,
                onValueChange = {},
                readOnly = true,
                enabled = selectedDivision.isNotEmpty(),
                label = { Text("জেলা") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.clickable { if (selectedDivision.isNotEmpty()) distExpanded = !distExpanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (selectedDivision.isNotEmpty()) distExpanded = !distExpanded },
                shape = RoundedCornerShape(12.dp)
            )
            DropdownMenu(
                expanded = distExpanded && selectedDivision.isNotEmpty(),
                onDismissRequest = { distExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("জেলা নির্বাচন করুন", color = Color.Gray) },
                    onClick = {
                        onDistrictSelected("")
                        distExpanded = false
                    }
                )
                districts.forEach { dist ->
                    DropdownMenuItem(
                        text = { Text(dist) },
                        onClick = {
                            onDistrictSelected(dist)
                            distExpanded = false
                        }
                    )
                }
            }
        }

        // Thana Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedThana,
                onValueChange = {},
                readOnly = true,
                enabled = selectedDistrict.isNotEmpty(),
                label = { Text("থানা") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationCity,
                        contentDescription = null,
                        modifier = Modifier.clickable { if (selectedDistrict.isNotEmpty()) thanaExpanded = !thanaExpanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (selectedDistrict.isNotEmpty()) thanaExpanded = !thanaExpanded },
                shape = RoundedCornerShape(12.dp)
            )
            DropdownMenu(
                expanded = thanaExpanded && selectedDistrict.isNotEmpty(),
                onDismissRequest = { thanaExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("থানা নির্বাচন করুন", color = Color.Gray) },
                    onClick = {
                        onThanaSelected("")
                        thanaExpanded = false
                    }
                )
                thanas.forEach { thana ->
                    DropdownMenuItem(
                        text = { Text(thana) },
                        onClick = {
                            onThanaSelected(thana)
                            thanaExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BloodRequestCard(
    request: BloodRequest,
    onCallClick: (String) -> Unit
) {
    val isUrgent = request.urgency == "urgent" || request.urgency == "emergency"
    val cardBg = if (request.status == "completed") Color(0xFFF0FDF4) else if (isUrgent) Color(0xFFFFF5F5) else Color.White
    val borderStroke = if (request.status == "completed") Success else if (request.urgency == "emergency") CrimsonDark else if (isUrgent) CrimsonPrimary else Color(0xFFF1F5F9)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = borderStroke,
                    size = this.size.copy(width = 4.dp.toPx())
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blood badge
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(listOf(CrimsonPrimary, CrimsonDark)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = request.bloodGroup,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (request.status == "completed") Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (request.status == "completed") "✓ সম্পন্ন" else "⏰ অপেক্ষমাণ",
                        color = if (request.status == "completed") Color(0xFF065F46) else Color(0xFF92400E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "রোগী: ${request.patientName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocalHospital, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = request.hospital,
                    fontSize = 14.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = "হিমোগ্লোবিন: ${request.hemoglobin} g/dL",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = request.location,
                    fontSize = 13.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { onCallClick(request.contact) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("call_button"),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("কল করুন (${request.contact})", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DonorCard(
    donor: Donor,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String, String) -> Unit
) {
    val address = listOfNotNull(donor.division, donor.district, donor.thana)
        .filter { it.isNotEmpty() }
        .joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFECACA), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (donor.name.take(1).ifEmpty { "অ" }),
                        color = CrimsonDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = donor.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = address.ifEmpty { "অবস্থান নেই" },
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Blood Badge
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(listOf(CrimsonPrimary, CrimsonDark)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = donor.bloodGroup,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (donor.donations > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Opacity, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${donor.donations} বার রক্তদান করেছেন",
                        fontSize = 13.sp,
                        color = TextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            donor.lastDonationDate?.let { date ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD1FAE5), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "সর্বশেষ রক্তদান: ${getTimeAgo(date)}",
                        color = Color(0xFF065F46),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Button
                OutlinedButton(
                    onClick = { onCallClick(donor.phone) },
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("কল", fontWeight = FontWeight.Bold)
                }

                // WhatsApp Button
                Button(
                    onClick = { onWhatsAppClick(donor.phone, donor.bloodGroup) },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsApp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalPhone, contentDescription = null, tint = CrimsonPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                if (contact.location.isNotEmpty()) {
                    Text(
                        text = "📍 ${contact.location}",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (contact.description.isNotEmpty()) {
                    Text(
                        text = contact.description,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onCallClick(contact.number) },
                    modifier = Modifier.background(Color(0xFFFEF2F2), CircleShape)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onWhatsAppClick(contact.number) },
                    modifier = Modifier.background(Color(0xFFE8FDF0), CircleShape)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, tint = WhatsApp, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun getTimeAgo(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
    return when {
        diffDays <= 0 -> {
            val diffHours = (diffMs / (1000 * 60 * 60)).toInt()
            if (diffHours <= 0) {
                val diffMins = (diffMs / (1000 * 60)).toInt()
                if (diffMins <= 0) "এইমাত্র" else "$diffMins মিনিট আগে"
            } else {
                "$diffHours ঘণ্টা আগে"
            }
        }
        diffDays == 1 -> "গতকাল"
        diffDays < 7 -> "$diffDays দিন আগে"
        diffDays < 30 -> "${diffDays / 7} সপ্তাহ আগে"
        diffDays < 365 -> "${diffDays / 30} মাস আগে"
        else -> "${diffDays / 365} বছর আগে"
    }
}
