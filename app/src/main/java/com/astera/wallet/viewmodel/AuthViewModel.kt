package com.astera.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astera.wallet.model.ServiceResult
import com.astera.wallet.service.AuthService
import com.astera.wallet.service.BudgetService
import com.astera.wallet.service.LoginPreferences
import com.astera.wallet.service.OtpService
import com.astera.wallet.service.OtpVerifyResult
import com.astera.wallet.service.WalletService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AuthGateState {
  data object Loading : AuthGateState

  data object Unauthenticated : AuthGateState

  data class NeedsRegistration(
    val uid: String,
    val email: String,
    val suggestedName: String,
  ) : AuthGateState

  /** Login email/kata sandi berhasil, menunggu verifikasi dua langkah (OTP simulasi). */
  data class NeedsOtp(val uid: String, val email: String) : AuthGateState

  data class Authenticated(val uid: String) : AuthGateState
}

/** Kode OTP simulasi yang sedang aktif, ditampilkan di kartu "SMS Simulasi". */
data class OtpChallenge(val issuedAtMs: Long, val notificationShown: Boolean)

class AuthViewModel(
  private val authService: AuthService,
  private val walletService: WalletService,
  private val budgetService: BudgetService,
  private val otpService: OtpService,
  private val loginPreferences: LoginPreferences,
) : ViewModel() {

  private val _gateState = MutableStateFlow<AuthGateState>(AuthGateState.Loading)
  val gateState: StateFlow<AuthGateState> = _gateState.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _otpChallenge = MutableStateFlow<OtpChallenge?>(null)
  val otpChallenge: StateFlow<OtpChallenge?> = _otpChallenge.asStateFlow()

  private val _otpError = MutableStateFlow<String?>(null)
  val otpError: StateFlow<String?> = _otpError.asStateFlow()

  /**
   * true jika sesi ini baru saja dibuat lewat login (+OTP) atau registrasi, sehingga layar kunci
   * PIN/biometrik dilewati. false jika sesi lama dipulihkan saat aplikasi dibuka ulang.
   */
  var isFreshSignIn: Boolean = false
    private set

  /** Email tersimpan dari opsi "Ingat saya", atau null. */
  val rememberedEmail: String? = loginPreferences.rememberedEmail()

  init {
    viewModelScope.launch {
      authService.authStateFlow().collectLatest { firebaseUser ->
        resolveUserSession(firebaseUser)
      }
    }
  }

  private suspend fun resolveUserSession(user: FirebaseUser?) {
    if (user == null) {
      _gateState.value = AuthGateState.Unauthenticated
      return
    }
    _gateState.value = AuthGateState.Loading
    when (val res = walletService.getUserProfile(user.uid)) {
      is ServiceResult.Success -> {
        val profile = res.data
        if (profile != null) {
          budgetService.ensureCurrentMonthReset(user.uid)
          if (otpService.isLoginPending()) {
            _gateState.value = AuthGateState.NeedsOtp(user.uid, user.email ?: profile.email)
            if (_otpChallenge.value == null || !otpService.hasActiveCode()) issueOtp()
          } else {
            _gateState.value = AuthGateState.Authenticated(user.uid)
          }
        } else {
          _gateState.value =
            AuthGateState.NeedsRegistration(
              uid = user.uid,
              email = user.email ?: "",
              suggestedName = user.displayName ?: "",
            )
        }
      }
      is ServiceResult.Error -> {
        _authError.value = res.message
        _gateState.value = AuthGateState.Unauthenticated
      }
    }
  }

  fun signInWithEmailPassword(email: String, password: String, rememberMe: Boolean) {
    if (_isBusy.value) return
    _isBusy.value = true
    _authError.value = null
    viewModelScope.launch {
      try {
        // Ditandai sebelum login agar listener auth langsung mengarahkan ke layar OTP.
        otpService.markLoginPending()
        when (val res = authService.signInWithEmailPassword(email, password)) {
          is ServiceResult.Success -> {
            loginPreferences.save(email, rememberMe)
            resolveUserSession(res.data)
          }
          is ServiceResult.Error -> {
            otpService.clear()
            _authError.value = res.message
          }
        }
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun registerWithEmailPassword(
    email: String,
    password: String,
    name: String,
    phone: String,
    pin: String,
    onSuccess: () -> Unit,
  ) {
    if (_isBusy.value) return
    _isBusy.value = true
    _authError.value = null
    viewModelScope.launch {
      try {
        val pendingUid = (gateState.value as? AuthGateState.NeedsRegistration)?.uid
        val res =
          if (pendingUid != null) {
            authService.completeProfileRegistration(pendingUid, email, name, phone, pin)
          } else {
            authService.registerWithEmailPassword(email, password, name, phone, pin)
          }
        when (res) {
          is ServiceResult.Success -> {
            otpService.clear()
            isFreshSignIn = true
            _gateState.value = AuthGateState.Authenticated(res.data.uid)
            onSuccess()
          }
          is ServiceResult.Error -> _authError.value = res.message
        }
      } finally {
        _isBusy.value = false
      }
    }
  }

  fun sendPasswordReset(email: String) {
    viewModelScope.launch {
      when (val res = authService.sendPasswordReset(email)) {
        is ServiceResult.Success -> _authError.value = "Tautan reset kata sandi telah dikirim ke email Anda."
        is ServiceResult.Error -> _authError.value = res.message
      }
    }
  }

  fun verifyOtp(code: String) {
    val pending = _gateState.value as? AuthGateState.NeedsOtp ?: return
    when (val result = otpService.verify(code)) {
      OtpVerifyResult.Valid -> {
        otpService.clear()
        _otpChallenge.value = null
        _otpError.value = null
        isFreshSignIn = true
        _gateState.value = AuthGateState.Authenticated(pending.uid)
      }
      is OtpVerifyResult.Wrong ->
        _otpError.value = "Kode OTP salah. Sisa percobaan: ${result.remainingAttempts}."
      OtpVerifyResult.Expired ->
        _otpError.value = "Kode OTP sudah kedaluwarsa. Silakan kirim ulang kode."
      OtpVerifyResult.TooManyAttempts ->
        _otpError.value = "Terlalu banyak percobaan salah. Silakan kirim ulang kode."
    }
  }

  /** @param force lewati jeda kirim ulang, misalnya tepat setelah izin notifikasi diberikan. */
  fun resendOtp(force: Boolean = false) {
    if (_gateState.value !is AuthGateState.NeedsOtp) return
    val elapsed = System.currentTimeMillis() - otpService.lastIssuedAtMs()
    if (!force && elapsed < OtpService.RESEND_COOLDOWN_MS) return
    _otpError.value = null
    issueOtp()
  }

  private fun issueOtp() {
    val now = System.currentTimeMillis()
    _otpChallenge.value = OtpChallenge(issuedAtMs = now, notificationShown = otpService.issueCode(now))
  }

  fun signOut() {
    isFreshSignIn = false
    otpService.clear()
    _otpChallenge.value = null
    _otpError.value = null
    authService.signOut()
    _gateState.value = AuthGateState.Unauthenticated
  }

  fun clearError() {
    _authError.value = null
  }
}
