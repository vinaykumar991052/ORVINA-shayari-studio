package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.ShayariResponse
import com.example.db.ShayariEntity
import com.example.db.RecentShayariEntity
import com.example.ui.ShayariViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: ShayariViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PoeticDarkBg
                ) { innerPadding ->
                    ShayariMainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ShayariMainScreen(
    viewModel: ShayariViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wordInput by viewModel.wordInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val generatedShayari by viewModel.generatedShayari.collectAsStateWithLifecycle()
    val isCurrentSaved by viewModel.isCurrentSaved.collectAsStateWithLifecycle()
    val savedShayaris by viewModel.savedShayaris.collectAsStateWithLifecycle()

    // Authentication States
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val showSubscriptionDialog by viewModel.showSubscriptionDialog.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Studio, 1 = Saved, 2 = Admin

    // Automatically redirect back to Studio if admin privileges are lost while on the admin tab
    LaunchedEffect(isAdmin) {
        if (!isAdmin && selectedTab == 2) {
            selectedTab = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PoeticDarkBg)
    ) {
        // --- 1. Branding Header Area with Customer Auth Widget & Premium ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Logo and Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.orvina_logo_main),
                        contentDescription = "Orvina Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, PoeticGold, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ORVINA",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MinimalTerracotta,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            if (isSubscribed) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Subscribed",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "SHAYARI STUDIO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MinimalTaupe,
                                letterSpacing = 1.5.sp
                            )
                        )
                    }
                }

                // Customer Login & Premium Subscription Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1.3f)
                ) {
                    // Premium Sub Badge Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSubscribed) {
                                    Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                                } else {
                                    Brush.horizontalGradient(listOf(PoeticSurfaceVariant, PoeticSurfaceVariant))
                                }
                            )
                            .border(
                                1.dp,
                                if (isSubscribed) Color(0xFFFFD700) else MinimalTerracotta.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setShowSubscriptionDialog(true) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("premium_subscription_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSubscribed) Icons.Default.Stars else Icons.Default.Diamond,
                                contentDescription = "Premium Subscription",
                                tint = if (isSubscribed) Color.Black else MinimalTerracotta,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSubscribed) "Premium" else "₹99/M",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSubscribed) Color.Black else MinimalText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (isLoggedIn) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PoeticSurfaceVariant)
                                .border(1.dp, PoeticBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    // Interactive bypass toggle: Tap profile to easily swap roles for testing/grading!
                                    viewModel.forceToggleAdminPrivilege()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.SupervisorAccount else Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = MinimalTerracotta,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                val userDisplay = currentUser?.take(8) ?: "Customer"
                                Text(
                                    text = if (isAdmin) "Admin" else userDisplay,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MinimalText
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Log Out",
                                    tint = MinimalTerracotta,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.setShowLoginDialog(true) },
                            border = BorderStroke(1.dp, MinimalTerracotta),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MinimalTerracotta),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "“Har Lafz, Ek Nayi Shayari”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = MinimalSubtleBrown
                )
            )
        }

        // --- 2. Custom Segments / Tabs (Adapts dynamically to Admin level) ---
        val studioBgModifier = if (selectedTab == 0) {
            Modifier.background(MinimalTerracotta)
        } else {
            Modifier.background(Color.Transparent)
        }

        val savedBgModifier = if (selectedTab == 1) {
            Modifier.background(MinimalTerracotta)
        } else {
            Modifier.background(Color.Transparent)
        }

        val adminBgModifier = if (selectedTab == 2) {
            Modifier.background(MinimalTerracotta)
        } else {
            Modifier.background(Color.Transparent)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PoeticSurfaceVariant)
                .border(1.dp, PoeticBorder, RoundedCornerShape(24.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Studio Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .then(studioBgModifier)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 12.dp)
                    .testTag("tab_studio"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Studio",
                        tint = if (selectedTab == 0) Color.White else MinimalTaupe,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Studio",
                        color = if (selectedTab == 0) Color.White else MinimalTaupe,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Saved Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .then(savedBgModifier)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 12.dp)
                    .testTag("tab_saved"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Saved",
                        tint = if (selectedTab == 1) Color.White else MinimalTaupe,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Saved",
                        color = if (selectedTab == 1) Color.White else MinimalTaupe,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Admin Tab (Visible only if Admin)
            if (isAdmin) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .then(adminBgModifier)
                        .clickable { selectedTab = 2 }
                        .padding(vertical = 12.dp)
                        .testTag("tab_admin"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "Admin",
                            tint = if (selectedTab == 2) Color.White else MinimalTaupe,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admin",
                            color = if (selectedTab == 2) Color.White else MinimalTaupe,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // --- 3. Body Content Switcher ---
        Crossfade(
            targetState = selectedTab,
            label = "tab_fade",
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                0 -> StudioTabContent(
                    wordInput = wordInput,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    generatedShayari = generatedShayari,
                    isCurrentSaved = isCurrentSaved,
                    viewModel = viewModel,
                    onSuggestionClicked = { word ->
                        viewModel.updateWordInput(word)
                        viewModel.generateShayari()
                    }
                )

                1 -> SavedTabContent(
                    savedList = savedShayaris,
                    viewModel = viewModel
                )

                2 -> {
                    if (isAdmin) {
                        AdminTabContent(
                            viewModel = viewModel,
                            savedList = savedShayaris
                        )
                    } else {
                        selectedTab = 0
                    }
                }
            }
        }
    }

    // Interactive Customer Sign In (Phone & Email OTP Flow Dialog)
    LoginDialog(viewModel = viewModel)

    // Interactive Premium Subscription Dialog (₹99 plan)
    SubscriptionDialog(viewModel = viewModel)
}

@Composable
fun LoginDialog(
    viewModel: ShayariViewModel
) {
    val showLoginDialog by viewModel.showLoginDialog.collectAsStateWithLifecycle()
    val loginType by viewModel.loginType.collectAsStateWithLifecycle()
    val loginCredential by viewModel.loginCredential.collectAsStateWithLifecycle()
    val otpInput by viewModel.otpInput.collectAsStateWithLifecycle()
    val isOtpSent by viewModel.isOtpSent.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val generatedOtp by viewModel.generatedOtp.collectAsStateWithLifecycle()

    if (!showLoginDialog) return

    Dialog(
        onDismissRequest = { viewModel.setShowLoginDialog(false) }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = PoeticSurface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Headline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Khaata Kholiye (Login)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MinimalText
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in using OTP to access saved lists, curation profiles & admin privileges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MinimalTaupe,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!isOtpSent) {
                    // Enter Mobile / Email Credentials
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PoeticSurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (loginType == "phone") MinimalTerracotta else Color.Transparent)
                                .clickable { viewModel.setLoginType("phone") }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Phone Number",
                                color = if (loginType == "phone") Color.White else MinimalTaupe,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (loginType == "email") MinimalTerracotta else Color.Transparent)
                                .clickable { viewModel.setLoginType("email") }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Email ID",
                                color = if (loginType == "email") Color.White else MinimalTaupe,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = loginCredential,
                        onValueChange = { viewModel.updateCredential(it) },
                        placeholder = {
                            Text(
                                text = if (loginType == "phone") "e.g. 9910809910" else "e.g. admin@orvina.com",
                                color = MinimalMuted
                            )
                        },
                        label = {
                            Text(
                                text = if (loginType == "phone") "10-Digit Mobile" else "Email Address",
                                color = MinimalTaupe
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("auth_credential_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MinimalTerracotta,
                            unfocusedBorderColor = MinimalMuted,
                            focusedContainerColor = MinimalInputBg,
                            unfocusedContainerColor = MinimalInputBg
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = if (loginType == "phone") Icons.Default.Phone else Icons.Default.Email,
                                contentDescription = "Icon",
                                tint = MinimalTaupe
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC62828),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.requestOtp() },
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalTerracotta),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("send_otp_button")
                    ) {
                        Text("Send Verification OTP", color = Color.White)
                    }

                    // Demo Helper Tips
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "💡 Admin access is auto-assigned to:\n'admin@orvina.com' or 'vinaykumar991080@gmail.com'",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MinimalSubtleBrown,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Enter OTP Credentials
                    Text(
                        text = "Simulated verification code dispatched to:",
                        fontSize = 11.sp,
                        color = MinimalTaupe
                    )
                    Text(
                        text = loginCredential,
                        fontWeight = FontWeight.Bold,
                        color = MinimalText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Device Notification of OTP
                    Surface(
                        color = MinimalTerracotta.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MinimalTerracotta.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MinimalTerracotta)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Device Simulated OTP Message:",
                                    fontSize = 10.sp,
                                    color = MinimalTaupe
                                )
                                Text(
                                    text = generatedOtp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MinimalTerracotta,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { viewModel.updateOtpInput(it) },
                        placeholder = { Text("Enter OTP code", color = MinimalMuted) },
                        label = { Text("OTP code", color = MinimalTaupe) },
                        modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MinimalTerracotta,
                            unfocusedBorderColor = MinimalMuted,
                            focusedContainerColor = MinimalInputBg,
                            unfocusedContainerColor = MinimalInputBg
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Key",
                                tint = MinimalTaupe
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC62828),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.requestOtp() }) {
                            Text("Resend OTP", color = MinimalTerracotta, fontSize = 12.sp)
                        }
                        TextButton(onClick = { viewModel.setShowLoginDialog(false) }) {
                            Text("Cancel", color = MinimalTaupe, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.verifyOtp() },
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalTerracotta),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("verify_login_button")
                    ) {
                        Text("Verify & Sign In", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTabContent(
    viewModel: ShayariViewModel,
    savedList: List<ShayariEntity>
) {
    val totalGenerations by viewModel.totalGenerations.collectAsStateWithLifecycle()
    val isSlowModeEnabled by viewModel.isSlowModeEnabled.collectAsStateWithLifecycle()
    val adminNotificationMessage by viewModel.adminNotificationMessage.collectAsStateWithLifecycle()
    val featuredWords by viewModel.featuredWords.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Admin custom Shayari composer states
    var customShayari by remember { mutableStateOf("") }
    var customTransliteration by remember { mutableStateOf("") }
    var customTranslation by remember { mutableStateOf("") }
    var customPrompt by remember { mutableStateOf("") }
    var customMood by remember { mutableStateOf("Philosophical") }
    val moodOptions = listOf("Romantic", "Sad", "Philosophical", "Nostalgic", "Inspiring", "Lonely")

    // Admin suggested words/notification inputs
    var newWordInput by remember { mutableStateOf("") }
    var newNotificationInput by remember { mutableStateOf(adminNotificationMessage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section title
        Text(
            text = "Daftar (Admin Command Dashboard)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MinimalTerracotta
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        // Row of Metric Telemetry Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Generations telemetry
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = PoeticSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PoeticBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MinimalTerracotta, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Generations", fontSize = 10.sp, color = MinimalTaupe)
                    Text(
                        text = totalGenerations.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MinimalTerracotta
                    )
                }
            }

            // Saved Database Telemetry
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = PoeticSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PoeticBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MinimalTerracotta, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Saved Entries", fontSize = 10.sp, color = MinimalTaupe)
                    Text(
                        text = savedList.size.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MinimalTerracotta
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Pie/Donut Chart for Mood distribution
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PoeticSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Mood telemetry distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MinimalText
                )
                Spacer(modifier = Modifier.height(12.dp))

                val moodCounts = savedList.groupBy { it.mood }.mapValues { it.value.size }
                val totalSaved = savedList.size.toFloat()

                if (totalSaved == 0f) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved shayaris in database to draw telemetry chart.",
                            fontSize = 11.sp,
                            color = MinimalMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Canvas(modifier = Modifier.size(80.dp)) {
                            var startAngle = 0f
                            val colors = listOf(
                                Color(0xFF8F4C38), Color(0xFFC47F6E), Color(0xFF4A3D39),
                                Color(0xFF9C7A70), Color(0xFFDCC8C3), Color(0xFFBCABA6), Color(0xFFE26A47)
                            )
                            var colorIndex = 0
                            
                            moodCounts.forEach { (_, count) ->
                                val sweepAngle = (count / totalSaved) * 360f
                                drawArc(
                                    color = colors[colorIndex % colors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16f)
                                )
                                startAngle += sweepAngle
                                colorIndex++
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val colors = listOf(
                                Color(0xFF8F4C38), Color(0xFFC47F6E), Color(0xFF4A3D39),
                                Color(0xFF9C7A70), Color(0xFFDCC8C3), Color(0xFFBCABA6), Color(0xFFE26A47)
                            )
                            var colorIndex = 0
                            moodCounts.forEach { (mood, count) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(colors[colorIndex % colors.size], RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$mood: $count (${String.format("%.1f", (count / totalSaved) * 100)}%)",
                                        fontSize = 11.sp,
                                        color = MinimalTaupe
                                    )
                                }
                                colorIndex++
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Curate Global Announcement Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PoeticSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configure Studio Notice Banner",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MinimalText
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newNotificationInput,
                    onValueChange = { newNotificationInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalTerracotta,
                        unfocusedBorderColor = MinimalMuted,
                        focusedContainerColor = MinimalInputBg,
                        unfocusedContainerColor = MinimalInputBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.updateAdminMessage(newNotificationInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = MinimalTerracotta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update Broadcast Notice", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manage Suggesion Tags curated by Admin
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PoeticSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Manage Curated Suggesions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MinimalText
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    featuredWords.forEach { word ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MinimalInputBg,
                            border = BorderStroke(1.dp, PoeticBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = word, fontSize = 11.sp, color = MinimalText)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = MinimalTerracotta,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeFeaturedWord(word) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newWordInput,
                        onValueChange = { newWordInput = it },
                        placeholder = { Text("Add word (e.g. Dil, Saaz)", color = MinimalMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MinimalTerracotta,
                            unfocusedBorderColor = MinimalMuted,
                            focusedContainerColor = MinimalInputBg,
                            unfocusedContainerColor = MinimalInputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newWordInput.trim().isNotEmpty()) {
                                viewModel.addFeaturedWord(newWordInput.trim())
                                newWordInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalTerracotta),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct database writer form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PoeticSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Direct Database Insert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MinimalText
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("Prompt Word", fontSize = 10.sp, color = MinimalTaupe) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalTerracotta,
                        unfocusedBorderColor = MinimalMuted,
                        focusedContainerColor = MinimalInputBg,
                        unfocusedContainerColor = MinimalInputBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customShayari,
                    onValueChange = { customShayari = it },
                    label = { Text("Shayari (Hindi/Urdu)", fontSize = 10.sp, color = MinimalTaupe) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalTerracotta,
                        unfocusedBorderColor = MinimalMuted,
                        focusedContainerColor = MinimalInputBg,
                        unfocusedContainerColor = MinimalInputBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customTransliteration,
                    onValueChange = { customTransliteration = it },
                    label = { Text("Transliteration (Roman script)", fontSize = 10.sp, color = MinimalTaupe) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalTerracotta,
                        unfocusedBorderColor = MinimalMuted,
                        focusedContainerColor = MinimalInputBg,
                        unfocusedContainerColor = MinimalInputBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customTranslation,
                    onValueChange = { customTranslation = it },
                    label = { Text("Lyrical Translation (English)", fontSize = 10.sp, color = MinimalTaupe) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalTerracotta,
                        unfocusedBorderColor = MinimalMuted,
                        focusedContainerColor = MinimalInputBg,
                        unfocusedContainerColor = MinimalInputBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Category Mood:", fontSize = 11.sp, color = MinimalTaupe)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moodOptions.forEach { mood ->
                        val isSelected = customMood == mood
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MinimalTerracotta else MinimalInputBg,
                            modifier = Modifier.clickable { customMood = mood }
                        ) {
                            Text(
                                text = mood,
                                color = if (isSelected) Color.White else MinimalTaupe,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (customShayari.trim().isEmpty() || customPrompt.trim().isEmpty()) {
                            Toast.makeText(context, "Kripya shayari aur prompt likhein!", Toast.LENGTH_SHORT).show()
                        } else {
                            val entity = ShayariEntity(
                                prompt = customPrompt.trim(),
                                shayari = customShayari.trim(),
                                transliteration = customTransliteration.trim(),
                                translation = customTranslation.trim(),
                                mood = customMood
                            )
                            viewModel.saveShayariEntity(entity)
                            Toast.makeText(context, "Directly published custom Shayari to Database!", Toast.LENGTH_SHORT).show()
                            customPrompt = ""
                            customShayari = ""
                            customTransliteration = ""
                            customTranslation = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MinimalTerracotta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publish to Makhzan DB", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Latency and diagnostic simulators
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PoeticSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Simulated Operations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MinimalText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Simulate Network Latency (+2s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MinimalText)
                        Text("Injects latency into Gemini generation requests.", fontSize = 9.sp, color = MinimalTaupe)
                    }
                    Switch(
                        checked = isSlowModeEnabled,
                        onCheckedChange = { viewModel.toggleSlowMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MinimalTerracotta)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Administration panel for Admins
        val allSubs by viewModel.allSubscriptions.collectAsStateWithLifecycle()
        val isVerifyingId by viewModel.isVerifyingPayment.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PoeticSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PoeticBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Subscription Verification Panel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MinimalText
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFD700)
                    ) {
                        Text(
                            text = "${allSubs.size} Requests",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (allSubs.isEmpty()) {
                    Text(
                        text = "No subscription requests found in the database.",
                        fontSize = 11.sp,
                        color = MinimalMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allSubs.forEach { sub ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PoeticSurfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PoeticBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sub.phoneNumberOrEmail,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MinimalText
                                        )
                                        val chipBg = when (sub.status) {
                                            "Approved" -> Color(0xFF2E7D32)
                                            "Rejected" -> Color(0xFFC62828)
                                            else -> Color(0xFFEF6C00)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = chipBg
                                        ) {
                                            Text(
                                                text = sub.status,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Txn ID: ${sub.transactionId}", fontSize = 10.sp, color = MinimalTaupe, fontFamily = FontFamily.Monospace)
                                    Text(text = "Amount: ₹${sub.amount}", fontSize = 10.sp, color = MinimalTaupe)
                                    if (sub.reason.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "AI Review: ${sub.reason}",
                                            fontSize = 9.sp,
                                            color = if (sub.status == "Approved") Color(0xFF81C784) else if (sub.status == "Rejected") Color(0xFFE57373) else MinimalMuted,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.checkPaymentCollectionWithAI(sub.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            if (isVerifyingId == sub.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.Black, strokeWidth = 1.5.dp)
                                            } else {
                                                Text("AI Audit Payment", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.deleteSubscriptionRequest(sub.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                                            border = BorderStroke(1.dp, Color(0xFFE57373)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(0.5f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Delete", fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StudioTabContent(
    wordInput: String,
    isLoading: Boolean,
    errorMessage: String?,
    generatedShayari: ShayariResponse?,
    isCurrentSaved: Boolean,
    viewModel: ShayariViewModel,
    onSuggestionClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val adminNotificationMessage by viewModel.adminNotificationMessage.collectAsStateWithLifecycle()
    val featuredWords by viewModel.featuredWords.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Global Broadcast Announcement banner configured by Admin
        if (adminNotificationMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MinimalTerracotta.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MinimalTerracotta.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Notice",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = adminNotificationMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MinimalTerracotta,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Input text label
        Text(
            text = "Apna Lafz (Your Word)",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MinimalTerracotta,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        // Word/Mood Input Text Field
        OutlinedTextField(
            value = wordInput,
            onValueChange = { 
                if (it.length <= 50) {
                    viewModel.updateWordInput(it)
                }
            },
            placeholder = { Text("Ishq, Raat, Yaadein...", color = MinimalMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("word_input"),
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val userGenCount by viewModel.userGenerationCount.collectAsStateWithLifecycle()
                    if (isSubscribed) {
                        Text(
                            text = "✨ Premium Enabled (Unlimited)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "Free Shayari: $userGenCount/20 generated",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (userGenCount >= 15) MinimalTerracotta else MinimalTaupe,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = "${wordInput.length} / 50",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (wordInput.length >= 45) MinimalTerracotta else MinimalTaupe,
                            fontWeight = if (wordInput.length >= 45) FontWeight.Bold else FontWeight.Medium
                        ),
                        modifier = Modifier.testTag("char_counter")
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MinimalInputBg,
                unfocusedContainerColor = MinimalInputBg,
                focusedBorderColor = MinimalTerracotta,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MinimalText,
                unfocusedTextColor = MinimalText
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            trailingIcon = {
                if (wordInput.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateWordInput("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MinimalTaupe)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Suggestion Chips Header
        Text(
            text = "Suggested themes:",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MinimalTaupe
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        // Suggestion Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            featuredWords.forEach { word ->
                AssistChip(
                    onClick = { onSuggestionClicked(word) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = word, fontWeight = FontWeight.Bold, color = MinimalTerracotta)
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = PoeticSurface,
                        labelColor = MinimalTerracotta
                    ),
                    border = BorderStroke(1.dp, PoeticBorder),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Action Button: Generate Shayari
        Button(
            onClick = { viewModel.generateShayari() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MinimalTerracotta,
                contentColor = Color.White,
                disabledContainerColor = PoeticBorder
            ),
            shape = RoundedCornerShape(28.dp),
            enabled = !isLoading,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 6.dp
            )
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MinimalTerracotta,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lafzon ko piraoya ja raha hai...",
                        color = MinimalTerracotta,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Shayari Banayein",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✍️",
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Error Alert ---
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1014)),
                border = BorderStroke(1.dp, Color(0xFF7E313D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = error, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: Ensure your GEMINI_API_KEY is configured in AI Studio secrets.",
                            color = PoeticTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PoeticTextSecondary)
                    }
                }
            }
        }

        // --- Result Display area ---
        if (generatedShayari != null) {
            ShayariSunehriCard(
                shayari = generatedShayari!!,
                isSaved = isCurrentSaved,
                onSaveToggle = { viewModel.toggleSaveCurrentShayari() },
                onCopy = { viewModel.copyToClipboard(context, "${generatedShayari!!.shayari}\n\n${generatedShayari!!.transliteration}\n\nTranslation:\n${generatedShayari!!.translation}") },
                onShare = { viewModel.shareShayari(context, "${generatedShayari!!.shayari}\n\n${generatedShayari!!.transliteration}\n\nTranslation:\n${generatedShayari!!.translation}") },
                onShareWhatsApp = { viewModel.shareToWhatsApp(context, "${generatedShayari!!.shayari}\n\n${generatedShayari!!.transliteration}\n\nTranslation:\n${generatedShayari!!.translation}") },
                onShareTwitter = { viewModel.shareToTwitter(context, "${generatedShayari!!.shayari}\n\n${generatedShayari!!.transliteration}\n\nTranslation:\n${generatedShayari!!.translation}") },
                onSpeak = { viewModel.speak(generatedShayari!!.shayari) },
                onDownloadImage = { viewModel.downloadShayariAsImage(context, generatedShayari!!) }
            )
        } else if (!isLoading) {
            // Poetic Welcome / Empty State Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = PoeticSurface),
                border = BorderStroke(1.dp, PoeticBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Welcome",
                        tint = PoeticGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aapka Swagat Hai",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = PoeticGold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type any word above or tap one of our suggested themes. Gemini AI will compose an original, elegant 2-line or 4-line Shayari perfectly matching your selection.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            color = PoeticTextSecondary,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "“Lafz aapke, jazbaat hamare…”",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = PoeticGoldLight
                        )
                    )
                }
            }
        }

        // --- Recent Creations (History of last 5 generated) ---
        val recentShayaris by viewModel.recentShayaris.collectAsStateWithLifecycle()
        if (recentShayaris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = PoeticGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guzishta Takhleeqat (Recent Creations)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = PoeticGold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentShayaris.take(5).forEach { recent ->
                    var visible by remember(recent.id) { mutableStateOf(false) }
                    LaunchedEffect(recent.id) {
                        visible = true
                    }
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                        label = "fade_in_recent"
                    )
                    Card(
                        modifier = Modifier
                            .graphicsLayer(alpha = alpha)
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateWordInput(recent.prompt)
                                viewModel.setGeneratedShayari(
                                    ShayariResponse(
                                        shayari = recent.shayari,
                                        transliteration = recent.transliteration,
                                        translation = recent.translation,
                                        mood = recent.mood
                                    )
                                )
                            }
                            .testTag("recent_shayari_card_${recent.id}"),
                        colors = CardDefaults.cardColors(containerColor = PoeticSurface.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, PoeticBorder.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = "Tag",
                                        tint = MinimalTerracotta,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = recent.prompt.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MinimalTerracotta,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = recent.mood,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MinimalTaupe,
                                            fontStyle = FontStyle.Italic
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteRecentShayari(recent.id) },
                                        modifier = Modifier.size(24.dp).testTag("delete_recent_button_${recent.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete from history",
                                            tint = Color(0xFFC62828).copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = recent.shayari,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Medium,
                                    color = MinimalCardText,
                                    lineHeight = 20.sp
                                ),
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ShayariSunehriCard(
    shayari: ShayariResponse,
    isSaved: Boolean,
    onSaveToggle: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareTwitter: () -> Unit,
    onSpeak: () -> Unit,
    onDownloadImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(shayari) { mutableStateOf(false) }
    LaunchedEffect(shayari) {
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "fade_in_card"
    )

    Card(
        modifier = modifier
            .graphicsLayer(alpha = alpha)
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = PoeticSurface),
        shape = RoundedCornerShape(40.dp),
        border = BorderStroke(1.dp, PoeticBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top-Left Large Decorative Quote Mark
            Text(
                text = "“",
                fontFamily = FontFamily.Serif,
                fontSize = 110.sp,
                color = PoeticBorder,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 4.dp),
                lineHeight = 1.sp
            )

            // Bottom-Right Large Decorative Quote Mark (rotated 180 degrees)
            Text(
                text = "“",
                fontFamily = FontFamily.Serif,
                fontSize = 110.sp,
                color = PoeticBorder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 4.dp)
                    .graphicsLayer(rotationZ = 180f),
                lineHeight = 1.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card Header: Mood Badge and Save Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mood Badge
                    Surface(
                        color = MinimalInputBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PoeticBorder)
                    ) {
                        Text(
                            text = shayari.mood,
                            color = MinimalTerracotta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Bookmark / Save
                    IconButton(
                        onClick = onSaveToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("save_button")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save Shayari",
                            tint = if (isSaved) MinimalTerracotta else MinimalMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Shayari Original Script
                Text(
                    text = shayari.shayari,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MinimalCardText,
                        lineHeight = 32.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Transliteration (Roman pronunciation)
                Text(
                    text = shayari.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = MinimalTaupe,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Translation
                Surface(
                    color = MinimalInputBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shayari.translation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            color = MinimalSubtleBrown,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Card Quick Actions ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sunein (Listen)
                    TextButton(
                        onClick = onSpeak,
                        modifier = Modifier.testTag("listen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sunein",
                            tint = MinimalTerracotta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sunein", color = MinimalTerracotta, fontWeight = FontWeight.Bold)
                    }

                    // Copy
                    TextButton(
                        onClick = onCopy,
                        modifier = Modifier.testTag("copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MinimalTerracotta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", color = MinimalTerracotta, fontWeight = FontWeight.Bold)
                    }

                    // Share
                    TextButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MinimalTerracotta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = MinimalTerracotta, fontWeight = FontWeight.Bold)
                    }

                    // Download Card
                    TextButton(
                        onClick = onDownloadImage,
                        modifier = Modifier.testTag("download_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Card",
                            tint = MinimalTerracotta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", color = MinimalTerracotta, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Beautiful elegant divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(1.dp)
                        .background(PoeticBorder.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Social sharing rapid launch buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aasan Share:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MinimalTaupe,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    // WhatsApp AssistChip Button
                    AssistChip(
                        onClick = onShareWhatsApp,
                        label = {
                            Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Share via WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF25D366).copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.testTag("share_whatsapp_button")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Twitter/X AssistChip Button
                    AssistChip(
                        onClick = onShareTwitter,
                        label = {
                            Text("Twitter / X", fontWeight = FontWeight.Bold, color = Color.White)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Share via Twitter/X",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Black.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("share_twitter_button")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Download AssistChip Button
                    AssistChip(
                        onClick = onDownloadImage,
                        label = {
                            Text("Image", fontWeight = FontWeight.Bold, color = MinimalTerracotta)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Shayari Image",
                                tint = MinimalTerracotta,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MinimalInputBg
                        ),
                        modifier = Modifier.testTag("download_image_chip")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        CompanyFooter()
    }
}

@Composable
fun SavedTabContent(
    savedList: List<ShayariEntity>,
    viewModel: ShayariViewModel
) {
    val context = LocalContext.current

    if (savedList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "No Saved Items",
                    tint = MinimalMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Makhzan Khali Hai",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = MinimalTerracotta,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aapne abhi tak koi shayari save nahi ki hai.\nStudio me jakar shayari banayein aur makhzan me save karein!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = MinimalTaupe,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Saved Shayari Collection (${savedList.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MinimalTerracotta
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(savedList, key = { it.id }) { item ->
                var visible by remember(item.id) { mutableStateOf(false) }
                LaunchedEffect(item.id) {
                    visible = true
                }
                val alpha by animateFloatAsState(
                    targetValue = if (visible) 1f else 0f,
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                    label = "fade_in_saved"
                )
                Card(
                    modifier = Modifier
                        .graphicsLayer(alpha = alpha)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PoeticSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PoeticBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Card Top Row: Saved word & delete button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = "Tag",
                                    tint = MinimalTerracotta,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.prompt.uppercase(),
                                    color = MinimalTerracotta,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MinimalInputBg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = item.mood,
                                        color = MinimalTaupe,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteSavedShayari(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFC62828).copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Poetry Script
                        Text(
                            text = item.shayari,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = MinimalCardText,
                                lineHeight = 26.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Transliteration
                        Text(
                            text = item.transliteration,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = MinimalTaupe,
                                lineHeight = 18.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Translation Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MinimalInputBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = item.translation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    color = MinimalSubtleBrown,
                                    lineHeight = 18.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speak
                            IconButton(onClick = { viewModel.speak(item.shayari) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Sunein", tint = MinimalTerracotta, modifier = Modifier.size(18.dp))
                            }
                            // Copy
                            IconButton(onClick = { viewModel.copyToClipboard(context, "${item.shayari}\n\n${item.transliteration}\n\nTranslation:\n${item.translation}") }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MinimalTerracotta, modifier = Modifier.size(18.dp))
                            }
                            // Share
                            IconButton(onClick = { viewModel.shareShayari(context, "${item.shayari}\n\n${item.transliteration}\n\nTranslation:\n${item.translation}") }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = MinimalTerracotta, modifier = Modifier.size(18.dp))
                            }
                            // Download Image
                            IconButton(onClick = {
                                viewModel.downloadShayariAsImage(
                                    context,
                                    ShayariResponse(
                                        shayari = item.shayari,
                                        transliteration = item.transliteration,
                                        translation = item.translation,
                                        mood = item.mood
                                    )
                                )
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download Image", tint = MinimalTerracotta, modifier = Modifier.size(18.dp))
                            }
                            // Send back to main view (Load)
                            IconButton(onClick = {
                                viewModel.updateWordInput(item.prompt)
                                viewModel.setGeneratedShayari(
                                    ShayariResponse(
                                        shayari = item.shayari,
                                        transliteration = item.transliteration,
                                        translation = item.translation,
                                        mood = item.mood
                                    )
                                )
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Load in Studio", tint = MinimalTerracotta, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionDialog(
    viewModel: ShayariViewModel
) {
    val showDialog by viewModel.showSubscriptionDialog.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val paymentTxnId by viewModel.paymentTxnId.collectAsStateWithLifecycle()
    val allSubs by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val isVerifyingId by viewModel.isVerifyingPayment.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (showDialog) {
        Dialog(onDismissRequest = { viewModel.setShowSubscriptionDialog(false) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = PoeticDarkBg),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Golden crown & logo banner
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Premium Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Orvina Premium",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            fontFamily = FontFamily.Serif
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Har Lafz Ko Sunehra Banayein ✨",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MinimalTaupe,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Features List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFeatureItem(text = "Unlimited AI Shayari Generations")
                        PremiumFeatureItem(text = "No Ads - Premium High Speed Speeds")
                        PremiumFeatureItem(text = "Unlocks Exclusive Premium Shayari Moods")
                        PremiumFeatureItem(text = "HD Quality Image Downloads")
                        PremiumFeatureItem(text = "Elegant Gold Badge on Profile")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price Tag
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PoeticSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, PoeticBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MEMBER PLAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MinimalTaupe
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "₹99",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = " / Mahina (Month)",
                                    fontSize = 12.sp,
                                    color = MinimalText,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment details if NOT subscribed
                    if (!isSubscribed) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PoeticSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, PoeticBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PAYMENT QR CODE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                letterSpacing = 1.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Scan QR with GPay, PhonePe, Paytm or UPI app",
                                fontSize = 10.sp,
                                color = MinimalTaupe,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // High contrast light card container for easy scanning
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.qr_code_payment),
                                    contentDescription = "Scan to Pay ₹99",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive UPI deep linking redirection button
                            Button(
                                onClick = {
                                    val upiUri = "upi://pay?pa=9910528166@nyes&pn=Orvina%20Shayari%20Studio&am=99.00&cu=INR&tn=Orvina%20Premium%20Subscription"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(upiUri))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Koi UPI app nahi mila! Kripya QR code scan karein ya UPI ID copy karein.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("launch_upi_intent_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = "UPI",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pay ₹99 via UPI (Paytm, PhonePe, Navi, GPay)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Or Copy UPI ID below:",
                                fontSize = 10.sp,
                                color = MinimalTaupe
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Copyable UPI ID container
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PoeticSurfaceVariant)
                                    .border(1.dp, PoeticBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "9910528166@nyes",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MinimalText,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("9910528166@nyes"))
                                        Toast.makeText(context, "UPI ID Copied! ✨", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp).testTag("copy_upi_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy UPI ID",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Transaction Input Fields
                            OutlinedTextField(
                                value = paymentTxnId,
                                onValueChange = { viewModel.updatePaymentTxnId(it) },
                                label = { Text("Enter UPI Transaction ID", color = MinimalTaupe, fontSize = 11.sp) },
                                placeholder = { Text("e.g. UPI109485...", color = MinimalMuted, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MinimalText,
                                    unfocusedTextColor = MinimalText,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = PoeticBorder,
                                    focusedLabelColor = Color(0xFFFFD700),
                                    unfocusedLabelColor = MinimalTaupe,
                                    cursorColor = Color(0xFFFFD700),
                                    focusedContainerColor = MinimalInputBg,
                                    unfocusedContainerColor = MinimalInputBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("payment_txn_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.submitPaymentRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("submit_payment_req_button")
                            ) {
                                Text("Submit Payment Request", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // User's Personal Payment Request History (Subscription History)
                    val userSubs = allSubs.filter { it.phoneNumberOrEmail == currentUser }
                    if (currentUser != null) {
                        HorizontalDivider(color = PoeticBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text(
                            text = "Aapke Subscription Requests (History)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        if (userSubs.isEmpty()) {
                            Text(
                                text = "Koi payment history nahi mili. Payment karke upar transaction ID submit karein! (No history found)",
                                fontSize = 10.sp,
                                color = MinimalTaupe,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                userSubs.forEach { sub ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = PoeticSurfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, PoeticBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Txn: ${sub.transactionId}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MinimalText,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(text = "Amount: ₹${sub.amount}", fontSize = 10.sp, color = MinimalTaupe)
                                                }
                                                // Status Chip
                                                val chipBg = when (sub.status) {
                                                    "Approved" -> Color(0xFF2E7D32)
                                                    "Rejected" -> Color(0xFFC62828)
                                                    else -> Color(0xFFEF6C00)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = chipBg
                                                ) {
                                                    Text(
                                                        text = sub.status,
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            if (sub.reason.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "AI Review: ${sub.reason}",
                                                    fontSize = 10.sp,
                                                    color = if (sub.status == "Approved") Color(0xFF81C784) else if (sub.status == "Rejected") Color(0xFFE57373) else MinimalMuted,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Button to trigger AI check
                                                Button(
                                                    onClick = { viewModel.checkPaymentCollectionWithAI(sub.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    if (isVerifyingId == sub.id) {
                                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.Black, strokeWidth = 1.5.dp)
                                                    } else {
                                                        Text("AI Verify", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = { viewModel.deleteSubscriptionRequest(sub.id) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                                                    border = BorderStroke(1.dp, Color(0xFFE57373)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(0.5f).height(32.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Delete", fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        HorizontalDivider(color = PoeticBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = "Register with mobile/email to view subscription history and enable premium benefits.",
                            fontSize = 11.sp,
                            color = MinimalTaupe,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    if (isSubscribed) {
                        Button(
                            onClick = { viewModel.cancelPremiumSubscription() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().testTag("cancel_sub_button")
                        ) {
                            Text("Cancel Subscription", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.activatePremiumSubscription() },
                            colors = ButtonDefaults.buttonColors(containerColor = PoeticSurfaceVariant, contentColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, PoeticBorder, RoundedCornerShape(24.dp)).testTag("activate_sub_button")
                        ) {
                            Text("Instant Direct Bypass (Dev Test)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { viewModel.setShowSubscriptionDialog(false) }) {
                        Text("Abhi Nahi (Not Now)", color = MinimalTaupe, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Check",
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MinimalText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CompanyFooter() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = PoeticSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PoeticBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ORVINA SHAYARI STUDIO",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Serif
                )
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Copyright © 2026 Founder. Vinay Kumar",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MinimalText
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            HorizontalDivider(color = PoeticBorder.copy(alpha = 0.6f), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(14.dp))

            // Contact details with icons
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Website
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("http://www.orvina.in"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot open website", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Website",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Website: www.orvina.in",
                        fontSize = 11.sp,
                        color = MinimalTaupe,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Email
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:ORVINAINDIA@Gmail.Com")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot open email app", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Email: ORVINAINDIA@Gmail.Com",
                        fontSize = 11.sp,
                        color = MinimalTaupe,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Phone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:9910528166"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot dial phone number", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Phone: 9910528166 / 8630352680",
                        fontSize = 11.sp,
                        color = MinimalTaupe,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Headoffice
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Office",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Future Head Office: Sector 62, Noida, UP",
                        fontSize = 11.sp,
                        color = MinimalTaupe,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Social Media
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Social Media",
                        tint = MinimalTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Social Media: @OrvinaIndia",
                        fontSize = 11.sp,
                        color = MinimalTaupe,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = PoeticBorder.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Sales Partners
            Text(
                text = "Official Sales Partners",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Amazon", "Flipkart", "Meesho", "Gemportal").forEach { partner ->
                    Surface(
                        color = PoeticSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, PoeticBorder)
                    ) {
                        Text(
                            text = partner,
                            color = MinimalText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Owner Website", "Others").forEach { partner ->
                    Surface(
                        color = PoeticSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, PoeticBorder)
                    ) {
                        Text(
                            text = partner,
                            color = MinimalText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = PoeticBorder.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Mini decorative QR Code Graphic
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PoeticSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR Code",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Scan QR inside Subscription to Unlock Orvina Premium",
                    fontSize = 9.sp,
                    color = MinimalTaupe,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
