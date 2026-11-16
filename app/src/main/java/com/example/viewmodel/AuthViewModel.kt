package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ServiceResult
import com.example.service.AuthService
import com.example.service.BudgetService
import com.example.service.WalletService
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

  data class Authenticated(val uid: String) : AuthGateState
}

class AuthViewModel(
  private val authService: AuthService,
  private val walletService: WalletService,
  private val budgetService: BudgetService,
) : ViewModel() {

  private val _gateState = MutableStateFlow<AuthGateState>(AuthGateState.Loading)
  val gateState: StateFlow<AuthGateState> = _gateState.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

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
          _gateState.value = AuthGateState.Authenticated(user.uid)
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

  fun signInWithEmailPassword(email: String, password: String) {
    if (_isBusy.value) return
    _isBusy.value = true
    _authError.value = null
    viewModelScope.launch {
      try {
        when (val res = authService.signInWithEmailPassword(email, password)) {
        is ServiceResult.Success -> {
          resolveUserSession(res.data)
        }
        is ServiceResult.Error -> {
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

  fun signOut() {
    authService.signOut()
    _gateState.value = AuthGateState.Unauthenticated
  }

  fun clearError() {
    _authError.value = null
  }
}
