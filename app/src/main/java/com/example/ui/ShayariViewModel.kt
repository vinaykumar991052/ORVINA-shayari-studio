package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.api.ShayariResponse
import com.example.db.ShayariDatabase
import com.example.db.ShayariEntity
import com.example.db.RecentShayariEntity
import com.example.db.SubscriptionEntity
import com.example.db.ShayariRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.net.Uri
import java.net.URLEncoder

class ShayariViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: ShayariRepository
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    // --- Authentication & Sessions (Customer Login / Admin System) ---
    private val prefs = application.getSharedPreferences("orvina_shayari_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow(prefs.getString("current_user", null))
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(prefs.getBoolean("is_admin", false))
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // --- Premium Subscription States & Flows ---
    private val _isSubscribedOverride = MutableStateFlow<Boolean?>(null)
    
    val allSubscriptions: StateFlow<List<SubscriptionEntity>>

    val isSubscribed: StateFlow<Boolean>

    private val _showSubscriptionDialog = MutableStateFlow(false)
    val showSubscriptionDialog: StateFlow<Boolean> = _showSubscriptionDialog.asStateFlow()

    // Payment and verification states
    private val _paymentTxnId = MutableStateFlow("")
    val paymentTxnId: StateFlow<String> = _paymentTxnId.asStateFlow()

    private val _isVerifyingPayment = MutableStateFlow<Int?>(null)
    val isVerifyingPayment: StateFlow<Int?> = _isVerifyingPayment.asStateFlow()

    // Login Temporary States
    private val _loginType = MutableStateFlow("phone") // "phone" or "email"
    val loginType: StateFlow<String> = _loginType.asStateFlow()

    private val _loginCredential = MutableStateFlow("")
    val loginCredential: StateFlow<String> = _loginCredential.asStateFlow()

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput.asStateFlow()

    private val _generatedOtp = MutableStateFlow("")
    val generatedOtp: StateFlow<String> = _generatedOtp.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    // --- Admin Dashboard States & Live Statistics ---
    private val _totalGenerations = MutableStateFlow(prefs.getInt("total_generations", 12)) // Seed with some nice start statistics
    val totalGenerations: StateFlow<Int> = _totalGenerations.asStateFlow()

    private val _isSlowModeEnabled = MutableStateFlow(prefs.getBoolean("slow_mode", false))
    val isSlowModeEnabled: StateFlow<Boolean> = _isSlowModeEnabled.asStateFlow()

    private val _adminNotificationMessage = MutableStateFlow(prefs.getString("admin_message", null) ?: "Welcome to ORVINA Shayari Studio \u2728 Har Lafz, Ek Nayi Shayari")
    val adminNotificationMessage: StateFlow<String> = _adminNotificationMessage.asStateFlow()

    private val _featuredWords = MutableStateFlow<List<String>>(
        prefs.getStringSet("featured_words", setOf("Ishq", "Dosti", "Zindagi", "Khwab", "Tanhai"))?.toList() ?: listOf("Ishq", "Dosti", "Zindagi", "Khwab", "Tanhai")
    )
    val featuredWords: StateFlow<List<String>> = _featuredWords.asStateFlow()

    // UI States
    private val _wordInput = MutableStateFlow("")
    val wordInput: StateFlow<String> = _wordInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _generatedShayari = MutableStateFlow<ShayariResponse?>(null)
    val generatedShayari: StateFlow<ShayariResponse?> = _generatedShayari.asStateFlow()

    private val _isCurrentSaved = MutableStateFlow(false)
    val isCurrentSaved: StateFlow<Boolean> = _isCurrentSaved.asStateFlow()

    private val _userGenerationCount = MutableStateFlow(0)
    val userGenerationCount: StateFlow<Int> = _userGenerationCount.asStateFlow()

    fun getUserGenerationCount(): Int {
        val userKey = _currentUser.value ?: "guest_user"
        return prefs.getInt("user_generation_count_$userKey", 0)
    }

    private fun incrementUserGenerationCount() {
        val userKey = _currentUser.value ?: "guest_user"
        val currentCount = getUserGenerationCount()
        val newCount = currentCount + 1
        prefs.edit().putInt("user_generation_count_$userKey", newCount).apply()
        _userGenerationCount.value = newCount
    }

    // Saved shayaris from Room DB
    val savedShayaris: StateFlow<List<ShayariEntity>>

    // Recent shayaris from Room DB
    val recentShayaris: StateFlow<List<RecentShayariEntity>>

    init {
        val db = ShayariDatabase.getDatabase(application)
        repository = ShayariRepository(db.shayariDao())

        viewModelScope.launch {
            _currentUser.collect { user ->
                val userKey = user ?: "guest_user"
                _userGenerationCount.value = prefs.getInt("user_generation_count_$userKey", 0)
            }
        }
        
        savedShayaris = repository.savedShayaris.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        recentShayaris = repository.recentShayaris.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allSubscriptions = repository.allSubscriptions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        isSubscribed = kotlinx.coroutines.flow.combine(
            repository.allSubscriptions,
            _currentUser,
            _isSubscribedOverride
        ) { subs, user, overrideVal ->
            if (overrideVal != null) {
                overrideVal
            } else if (user == null) {
                false
            } else {
                subs.any { it.phoneNumberOrEmail == user && it.status == "Approved" }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

        // Initialize Text-to-Speech
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("ShayariViewModel", "Failed to construct TextToSpeech", e)
        }
    }

    // --- Authentication Operations ---

    fun setShowLoginDialog(show: Boolean) {
        _showLoginDialog.value = show
        if (!show) {
            // Reset fields on close
            _loginCredential.value = ""
            _otpInput.value = ""
            _generatedOtp.value = ""
            _isOtpSent.value = false
            _errorMessage.value = null
        }
    }

    fun setLoginType(type: String) {
        _loginType.value = type
        _loginCredential.value = ""
        _errorMessage.value = null
    }

    fun updateCredential(value: String) {
        _loginCredential.value = value
    }

    fun updateOtpInput(value: String) {
        _otpInput.value = value
    }

    fun requestOtp() {
        val credential = _loginCredential.value.trim()
        if (credential.isEmpty()) {
            _errorMessage.value = if (_loginType.value == "phone") {
                "Kripya apna mobile number likhein. (Please enter your mobile number.)"
            } else {
                "Kripya apna email ID likhein. (Please enter your email ID.)"
            }
            return
        }

        if (_loginType.value == "phone") {
            if (credential.length < 10 || !credential.all { it.isDigit() }) {
                _errorMessage.value = "Kripya ek sahi 10-digit mobile number likhein. (Please enter a valid 10-digit mobile number.)"
                return
            }
        } else {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(credential).matches()) {
                _errorMessage.value = "Kripya ek sahi email ID likhein. (Please enter a valid email address.)"
                return
            }
        }

        // Generate a random 4-digit OTP code for interactive validation
        val otp = (1000..9999).random().toString()
        _generatedOtp.value = otp
        _isOtpSent.value = true
        _errorMessage.value = null
        
        // Show OTP to the user for immediate entry in the emulator
        showToast("OTP sent to $credential! Code: $otp")
    }

    fun verifyOtp() {
        val input = _otpInput.value.trim()
        val actual = _generatedOtp.value
        
        if (input.isEmpty()) {
            _errorMessage.value = "Kripya OTP code likhein. (Please enter the OTP.)"
            return
        }

        // Allow actual generated OTP, or master "1234" / "8888" for ease of testing
        if (input == actual || input == "1234" || input == "8888") {
            val user = _loginCredential.value.trim()
            _isLoggedIn.value = true
            _currentUser.value = user
            
            // Auto-grant Admin access if:
            // 1) Logging in with specialized email/phones matching developer or standard admin roles
            // 2) OTP is the admin trigger "8888"
            val isAdminUser = user.lowercase() == "vinaykumar991080@gmail.com" || 
                              user.lowercase() == "admin@orvina.com" || 
                              user == "9910809910" ||
                              user == "9999999999" ||
                              input == "8888"
                              
            _isAdmin.value = isAdminUser

            prefs.edit().apply {
                putBoolean("is_logged_in", true)
                putString("current_user", user)
                putBoolean("is_admin", isAdminUser)
                apply()
            }

            _showLoginDialog.value = false
            showToast(if (isAdminUser) "Swagat hai, Admin! Logged in as administrator." else "Mubarak ho! Login successful.")
            _errorMessage.value = null
        } else {
            _errorMessage.value = "Galat OTP! Kripya sahi code daalein (Ya testing ke liye code '1234' ya '8888' use karein)."
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _isAdmin.value = false
        
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            putString("current_user", null)
            putBoolean("is_admin", false)
            apply()
        }
        
        showToast("Logged out successfully! Khuda Hafiz!")
    }

    // --- Premium Subscription Operations ---
    fun setShowSubscriptionDialog(show: Boolean) {
        _showSubscriptionDialog.value = show
    }

    fun updatePaymentTxnId(value: String) {
        _paymentTxnId.value = value
    }

    fun submitPaymentRequest() {
        val user = _currentUser.value
        if (user == null) {
            showToast("Kripya pehle register / login karein! (Please login first!)")
            _showLoginDialog.value = true
            return
        }
        val txnId = _paymentTxnId.value.trim()
        if (txnId.isEmpty()) {
            showToast("Kripya ek sahi UPI Transaction ID likhein! (Please enter a valid Transaction ID!)")
            return
        }

        viewModelScope.launch {
            val entity = SubscriptionEntity(
                phoneNumberOrEmail = user,
                transactionId = txnId,
                amount = 99,
                status = "Pending",
                reason = "Awaiting AI Collection Verification"
            )
            val subId = repository.saveSubscription(entity)
            _paymentTxnId.value = ""
            _showSubscriptionDialog.value = false
            showToast("Payment request submitted! AI verification starting... \u2728")
            
            // Auto-verify with AI immediately!
            checkPaymentCollectionWithAI(subId.toInt())
        }
    }

    fun checkPaymentCollectionWithAI(subId: Int) {
        viewModelScope.launch {
            val subList = allSubscriptions.value
            val sub = subList.find { it.id == subId }
            if (sub == null) {
                showToast("Request not found!")
                return@launch
            }

            _isVerifyingPayment.value = subId
            try {
                showToast("AI is verifying payment collection for Txn ID: ${sub.transactionId}... \u23f3")
                val response = withContext(Dispatchers.IO) {
                    GeminiApiClient.verifyPaymentAI(
                        phoneNumberOrEmail = sub.phoneNumberOrEmail,
                        transactionId = sub.transactionId,
                        amount = sub.amount
                    )
                }
                
                // Update DB with results
                repository.updateSubscriptionStatus(subId, response.status, response.reason)
                
                if (response.status == "Approved") {
                    showToast("Mubarak Ho! AI has APPROVED your payment! Premium active! \u2728")
                } else {
                    showToast("AI rejected payment: ${response.reason}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("AI Verification Error: ${e.message}")
            } finally {
                _isVerifyingPayment.value = null
            }
        }
    }

    fun deleteSubscriptionRequest(subId: Int) {
        viewModelScope.launch {
            repository.deleteSubscription(subId)
            showToast("Payment record deleted from history.")
        }
    }

    fun activatePremiumSubscription() {
        val user = _currentUser.value
        if (user == null) {
            showToast("Kripya pehle register / login karein! (Please register/login first!)")
            _showLoginDialog.value = true
            return
        }
        viewModelScope.launch {
            val entity = SubscriptionEntity(
                phoneNumberOrEmail = user,
                transactionId = "INSTANT_ACT_" + (100000..999999).random(),
                amount = 99,
                status = "Approved",
                reason = "Instant Activation (Bypassed by User/Admin) \u2728"
            )
            repository.saveSubscription(entity)
            _showSubscriptionDialog.value = false
            showToast("Shukriya! Orvina Premium Subscription activated successfully! \ud83c\udf1f\u2728")
        }
    }

    fun cancelPremiumSubscription() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                val approvedSubs = allSubscriptions.value.filter { it.phoneNumberOrEmail == user && it.status == "Approved" }
                approvedSubs.forEach {
                    repository.deleteSubscription(it.id)
                }
                _isSubscribedOverride.value = null
                showToast("Premium subscription has been cancelled.")
            }
        } else {
            showToast("Premium subscription has been cancelled.")
        }
    }

    // --- Admin Specific Operations ---

    fun updateAdminMessage(message: String) {
        _adminNotificationMessage.value = message
        prefs.edit().putString("admin_message", message).apply()
        showToast("System announcement updated!")
    }

    fun toggleSlowMode(enabled: Boolean) {
        _isSlowModeEnabled.value = enabled
        prefs.edit().putBoolean("slow_mode", enabled).apply()
        showToast(if (enabled) "Admin: Test slow mode enabled" else "Admin: Test slow mode disabled")
    }

    fun addFeaturedWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.isNotEmpty()) {
            val updated = _featuredWords.value.toMutableList()
            if (!updated.contains(trimmed)) {
                updated.add(trimmed)
                _featuredWords.value = updated
                prefs.edit().putStringSet("featured_words", updated.toSet()).apply()
                showToast("Added featured word: $trimmed")
            }
        }
    }

    fun removeFeaturedWord(word: String) {
        val updated = _featuredWords.value.filter { it != word }
        _featuredWords.value = updated
        prefs.edit().putStringSet("featured_words", updated.toSet()).apply()
        showToast("Removed featured word: $word")
    }

    fun forceToggleAdminPrivilege() {
        val newAdminState = !_isAdmin.value
        _isAdmin.value = newAdminState
        prefs.edit().putBoolean("is_admin", newAdminState).apply()
        showToast(if (newAdminState) "Admin Mode Activated!" else "Returned to Normal Mode")
    }

    // --- Core Operations ---

    fun updateWordInput(word: String) {
        if (word.length <= 50) {
            _wordInput.value = word
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setGeneratedShayari(shayari: ShayariResponse?) {
        _generatedShayari.value = shayari
        checkIfCurrentIsSaved()
    }

    /**
     * Generate Shayari using Gemini API.
     */
    fun generateShayari() {
        val word = _wordInput.value.trim()
        if (word.isEmpty()) {
            _errorMessage.value = "Aapne koi shabd nahi likha. Kripya ek shabd likhein! (Please enter a word!)"
            return
        }

        // Enforce 20 generations limit for non-subscribed users
        val isSub = isSubscribed.value
        val count = getUserGenerationCount()
        if (!isSub && count >= 20) {
            _errorMessage.value = "Aapne apne 20 free Shayari generations pure kar liye hain. Kripya premium subscription active karein! \u2728"
            _showSubscriptionDialog.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Support admin-enabled simulated latency to showcase progress metrics
            if (_isSlowModeEnabled.value) {
                kotlinx.coroutines.delay(2000)
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    GeminiApiClient.generateShayari(word)
                }
                _generatedShayari.value = result
                checkIfCurrentIsSaved()

                // Save to recent generations (up to 5)
                repository.saveRecentShayari(
                    RecentShayariEntity(
                        prompt = word,
                        shayari = result.shayari,
                        transliteration = result.transliteration,
                        translation = result.translation,
                        mood = result.mood
                    )
                )

                // Increment user generation count
                incrementUserGenerationCount()

                // Increment total generation metrics for Admin dashboard
                val currentCount = _totalGenerations.value + 1
                _totalGenerations.value = currentCount
                prefs.edit().putInt("total_generations", currentCount).apply()

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.message ?: "Kuch galat hua. Kripya fir se koshish karein. (Something went wrong. Please try again.)"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if the currently generated Shayari is already saved.
     */
    fun checkIfCurrentIsSaved() {
        val current = _generatedShayari.value ?: return
        viewModelScope.launch {
            _isCurrentSaved.value = repository.isSaved(current.shayari)
        }
    }

    /**
     * Toggle Save / Unsave for the current generated Shayari.
     */
    fun toggleSaveCurrentShayari() {
        val current = _generatedShayari.value ?: return
        val word = _wordInput.value.trim().ifEmpty { current.mood }

        viewModelScope.launch {
            val isSaved = repository.isSaved(current.shayari)
            if (isSaved) {
                repository.unsaveShayariByText(current.shayari)
                _isCurrentSaved.value = false
                showToast("Shayari removed from Saved!")
            } else {
                val entity = ShayariEntity(
                    prompt = word,
                    shayari = current.shayari,
                    transliteration = current.transliteration,
                    translation = current.translation,
                    mood = current.mood
                )
                repository.saveShayari(entity)
                _isCurrentSaved.value = true
                showToast("Shayari saved successfully!")
            }
        }
    }

    /**
     * Save a custom ShayariEntity directly.
     */
    fun saveShayariEntity(entity: ShayariEntity) {
        viewModelScope.launch {
            repository.saveShayari(entity)
            checkIfCurrentIsSaved()
        }
    }

    /**
     * Delete a saved Shayari by its Entity ID.
     */
    fun deleteSavedShayari(id: Int) {
        viewModelScope.launch {
            repository.deleteShayari(id)
            checkIfCurrentIsSaved()
        }
    }

    /**
     * Delete a recent Shayari from history by its Entity ID.
     */
    fun deleteRecentShayari(id: Int) {
        viewModelScope.launch {
            repository.deleteRecentShayari(id)
            showToast("Shayari deleted from History! ✨")
        }
    }

    /**
     * Delete a saved Shayari by its text.
     */
    fun deleteSavedShayariByText(text: String) {
        viewModelScope.launch {
            repository.unsaveShayariByText(text)
            checkIfCurrentIsSaved()
        }
    }

    // --- Utility Methods ---

    /**
     * Copy Shayari text to clipboard.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Shayari") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Shayari copied to clipboard! \u270D\uFE0F", Toast.LENGTH_SHORT).show()
    }

    /**
     * Share Shayari text with other apps.
     */
    fun shareShayari(context: Context, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ORVINA Shayari Studio")
            putExtra(Intent.EXTRA_TEXT, "$text\n\n- Generated via ORVINA Shayari Studio \u2728\n\"Har Lafz, Ek Nayi Shayari\"")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Shayari via"))
    }

    /**
     * Download the generated Shayari card as a beautifully stylized image file (PNG).
     * Saved to Gallery (API 29+) or external files Pictures folder with media scanner trigger.
     */
    fun downloadShayariAsImage(context: Context, shayari: ShayariResponse) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    // Create high resolution card bitmap (1080x1350, 4:5 portrait aspect ratio, ideal for social posts)
                    val width = 1080
                    val height = 1350
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)

                    // 1. Draw Cream Background (PoeticDarkBg)
                    val bgPaint = android.graphics.Paint().apply {
                        color = 0xFFFDF8F6.toInt()
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // 2. Draw Premium Outer Borders (Terracotta and Taupe/Muted accent lines)
                    val borderPaint = android.graphics.Paint().apply {
                        color = 0xFF8F4C38.toInt() // MinimalTerracotta
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 6f
                    }
                    canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

                    val borderPaint2 = android.graphics.Paint().apply {
                        color = 0xFFBCABA6.toInt() // MinimalMuted
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    canvas.drawRect(36f, 36f, width - 36f, height - 36f, borderPaint2)

                    // 3. Draw Inner White Card Container (PoeticSurface)
                    val cardLeft = 80f
                    val cardRight = width - 80f
                    val cardTop = 140f
                    val cardBottom = height - 160f
                    val cardPaint = android.graphics.Paint().apply {
                        color = 0xFFFFFFFF.toInt()
                        style = android.graphics.Paint.Style.FILL
                    }
                    val cardRect = android.graphics.RectF(cardLeft, cardTop, cardRight, cardBottom)
                    canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

                    val cardBorderPaint = android.graphics.Paint().apply {
                        color = 0xFFF4EDEB.toInt() // PoeticBorder
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                    canvas.drawRoundRect(cardRect, 48f, 48f, cardBorderPaint)

                    // 4. Draw Giant Stylized Quotes in background
                    val quotePaint = android.graphics.Paint().apply {
                        color = 0xFFFDF8F6.toInt() // Soft warm shade
                        textSize = 340f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                    }
                    canvas.drawText("“", cardLeft + 30f, cardTop + 240f, quotePaint)

                    canvas.save()
                    canvas.translate(cardRight - 30f, cardBottom - 30f)
                    canvas.rotate(180f)
                    canvas.drawText("“", 0f, 120f, quotePaint)
                    canvas.restore()

                    // 5. Draw Content Elements
                    val centerX = width / 2f
                    val contentWidth = (cardRight - cardLeft - 160f).toInt()

                    // 5.1 Mood Badge
                    val badgeTextPaint = android.text.TextPaint().apply {
                        color = 0xFF8F4C38.toInt() // MinimalTerracotta
                        textSize = 32f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val badgeText = shayari.mood.uppercase()
                    val textWidth = badgeTextPaint.measureText(badgeText)
                    val badgePaddingX = 24f
                    val badgePaddingY = 12f
                    val badgeLeft = centerX - (textWidth / 2f) - badgePaddingX
                    val badgeRight = centerX + (textWidth / 2f) + badgePaddingX
                    val badgeTop = cardTop + 60f
                    val badgeBottom = badgeTop + 40f + (badgePaddingY * 2)

                    val badgeBgPaint = android.graphics.Paint().apply {
                        color = 0xFFF4EDEB.toInt() // MinimalInputBg
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawRoundRect(android.graphics.RectF(badgeLeft, badgeTop, badgeRight, badgeBottom), 24f, 24f, badgeBgPaint)
                    canvas.drawText(badgeText, centerX - (textWidth / 2f), badgeTop + 30f + badgePaddingY, badgeTextPaint)

                    // Helper function to draw wrapped multiline text
                    fun drawWrappedText(
                        text: String,
                        paint: android.text.TextPaint,
                        w: Int,
                        x: Float,
                        y: Float
                    ): Int {
                        @Suppress("DEPRECATION")
                        val staticLayout = android.text.StaticLayout(
                            text,
                            paint,
                            w,
                            android.text.Layout.Alignment.ALIGN_CENTER,
                            1.15f,
                            0.0f,
                            false
                        )
                        canvas.save()
                        canvas.translate(x, y)
                        staticLayout.draw(canvas)
                        canvas.restore()
                        return staticLayout.height
                    }

                    // 5.2 Original Shayari Script
                    val shayariPaint = android.text.TextPaint().apply {
                        color = 0xFF4A3D39.toInt() // MinimalCardText
                        textSize = 48f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val shayariHeight = drawWrappedText(
                        shayari.shayari,
                        shayariPaint,
                        contentWidth,
                        cardLeft + 80f,
                        badgeBottom + 70f
                    )

                    // 5.3 Roman Transliteration
                    val translitPaint = android.text.TextPaint().apply {
                        color = 0xFF77564C.toInt() // MinimalTaupe
                        textSize = 36f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                        isAntiAlias = true
                    }
                    val translitHeight = drawWrappedText(
                        shayari.transliteration,
                        translitPaint,
                        contentWidth,
                        cardLeft + 80f,
                        badgeBottom + 70f + shayariHeight + 40f
                    )

                    // 5.4 English Translation Container Box
                    val translationBoxTop = badgeBottom + 70f + shayariHeight + 40f + translitHeight + 50f
                    val translationPaint = android.text.TextPaint().apply {
                        color = 0xFF9C7A70.toInt() // MinimalSubtleBrown
                        textSize = 32f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
                        isAntiAlias = true
                    }

                    val translationTextWidth = contentWidth - 80
                    @Suppress("DEPRECATION")
                    val tempLayout = android.text.StaticLayout(
                        shayari.translation,
                        translationPaint,
                        translationTextWidth,
                        android.text.Layout.Alignment.ALIGN_CENTER,
                        1.1f,
                        0.0f,
                        false
                    )
                    val translationHeight = tempLayout.height

                    val boxPadding = 32f
                    val boxLeft = cardLeft + 50f
                    val boxRight = cardRight - 50f
                    val boxBottom = translationBoxTop + translationHeight + (boxPadding * 2)

                    val boxPaint = android.graphics.Paint().apply {
                        color = 0xFFF4EDEB.toInt() // MinimalInputBg
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawRoundRect(android.graphics.RectF(boxLeft, translationBoxTop, boxRight, boxBottom), 32f, 32f, boxPaint)

                    drawWrappedText(
                        shayari.translation,
                        translationPaint,
                        translationTextWidth,
                        boxLeft + boxPadding,
                        translationBoxTop + boxPadding
                    )

                    // 5.5 Elegant Watermark Brand Signature at Card Footer
                    val footerPaint = android.text.TextPaint().apply {
                        color = 0xFF8F4C38.toInt() // PoeticGold
                        textSize = 30f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val footerText = "ORVINA SHAYARI STUDIO \u2728"
                    val footerWidth = footerPaint.measureText(footerText)
                    canvas.drawText(footerText, centerX - (footerWidth / 2f), cardBottom + 65f, footerPaint)

                    val subFooterPaint = android.text.TextPaint().apply {
                        color = 0xFFBCABA6.toInt() // MinimalMuted
                        textSize = 22f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                        isAntiAlias = true
                    }
                    val subFooterText = "Designed for Instagram Stories & Feed"
                    val subFooterWidth = subFooterPaint.measureText(subFooterText)
                    canvas.drawText(subFooterText, centerX - (subFooterWidth / 2f), cardBottom + 105f, subFooterPaint)

                    // 6. Save image to Device (Permissionless modern Flow)
                    val filename = "Shayari_${System.currentTimeMillis()}.png"
                    val resolver = context.contentResolver

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        // On Android 10+ (API 29+), save directly to public Gallery
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/OrvinaShayari")
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (imageUri != null) {
                            resolver.openOutputStream(imageUri).use { out ->
                                if (out != null) {
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                }
                            }
                            contentValues.clear()
                            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(imageUri, contentValues, null, null)
                            true
                        } else {
                            false
                        }
                    } else {
                        // On older APIs (Android 9 and below), save to App's External Pictures Dir (No storage permission required!)
                        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                        val file = java.io.File(dir, filename)
                        java.io.FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                        // Scan the saved file to register it with the System Media Indexer
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(file.absolutePath),
                            arrayOf("image/png"),
                            null
                        )
                        true
                    }
                } catch (e: Exception) {
                    Log.e("ShayariViewModel", "Error downloading/rendering Shayari image", e)
                    false
                }
            }

            if (success) {
                showToast("Shayari card downloaded to Gallery! \u2728")
            } else {
                showToast("Failed to download Shayari card. Please try again.")
            }
        }
    }

    /**
     * Share Shayari directly to WhatsApp.
     */
    fun shareToWhatsApp(context: Context, text: String) {
        val formattedText = "$text\n\n- Generated via ORVINA Shayari Studio \u2728"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, formattedText)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed, fallback to chooser
            Toast.makeText(context, "WhatsApp is not installed. Sharing via other options...", Toast.LENGTH_SHORT).show()
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formattedText)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share Shayari"))
        }
    }

    /**
     * Share Shayari directly to Twitter/X.
     */
    fun shareToTwitter(context: Context, text: String) {
        val formattedText = "$text\n\n- via @ORVINA Shayari Studio \u2728"
        try {
            val url = "https://twitter.com/intent/tweet?text=" + URLEncoder.encode(formattedText, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to package send
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = "com.twitter.android"
                putExtra(Intent.EXTRA_TEXT, formattedText)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Twitter/X is not installed. Sharing via other options...", Toast.LENGTH_SHORT).show()
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, formattedText)
                }
                context.startActivity(Intent.createChooser(chooserIntent, "Share Shayari"))
            }
        }
    }

    // --- Text-to-Speech (TTS) ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to Hindi / Urdu if available, default to English/Indian English
            val hindiLocale = Locale("hi", "IN")
            val result = tts?.setLanguage(hindiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            isTtsInitialized = true
        } else {
            Log.e("ShayariViewModel", "TTS Initialization failed!")
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            // Clean up text from special characters or emojis if any, but standard TTS handles them decently
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ShayariTTS")
        } else {
            showToast("Speech Engine is initializing, please try again.")
        }
    }

    fun stopSpeaking() {
        if (isTtsInitialized && tts != null) {
            tts?.stop()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
