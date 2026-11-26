package com.astera.wallet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.astera.wallet.R
import com.astera.wallet.service.AuthService
import com.astera.wallet.service.BudgetService
import com.astera.wallet.service.LoginPreferences
import com.astera.wallet.service.OtpService
import com.astera.wallet.service.WalletService
import com.astera.wallet.ui.screens.auth.OtpVerificationScreen
import com.astera.wallet.ui.screens.auth.SplashScreen
import com.astera.wallet.viewmodel.AuthGateState
import com.astera.wallet.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AsteraApp() {
  val authViewModel: AuthViewModel =
    viewModel(
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            AuthViewModel(
              authService = AuthService(db),
              walletService = WalletService(db),
              budgetService = BudgetService(db),
              otpService = OtpService(app.applicationContext),
              loginPreferences = LoginPreferences(app.applicationContext),
            )
          }
        }
    )

  val gateState by authViewModel.gateState.collectAsStateWithLifecycle()
  val isBusy by authViewModel.isBusy.collectAsStateWithLifecycle()
  val authError by authViewModel.authError.collectAsStateWithLifecycle()
  val otpChallenge by authViewModel.otpChallenge.collectAsStateWithLifecycle()
  val otpError by authViewModel.otpError.collectAsStateWithLifecycle()
  var splashDone by remember { mutableStateOf(false) }

  if (!splashDone) {
    SplashScreen(
      gateState = gateState,
      onNavigateNext = { splashDone = true },
    )
    return
  }

  when (val state = gateState) {
    is AuthGateState.Authenticated -> {
      val uid = state.uid
      androidx.compose.runtime.key(uid) {
        AuthenticatedAppFlow(
          currentUserId = uid,
          skipAppUnlock = authViewModel.isFreshSignIn,
          onSignOut = { authViewModel.signOut() },
        )
      }
    }
    is AuthGateState.NeedsOtp -> {
      OtpVerificationScreen(
        email = state.email,
        challenge = otpChallenge,
        errorMessage = otpError,
        onSubmit = { code -> authViewModel.verifyOtp(code) },
        onResend = { authViewModel.resendOtp() },
        onCancel = { authViewModel.signOut() },
      )
    }
    else -> {
      UnauthenticatedAppFlow(
        gateState = state,
        isBusy = isBusy,
        authError = authError,
        onClearError = { authViewModel.clearError() },
        rememberedEmail = authViewModel.rememberedEmail,
        onEmailSignIn = { email, password, rememberMe ->
          authViewModel.signInWithEmailPassword(email, password, rememberMe)
        },
        onForgotPassword = { email -> authViewModel.sendPasswordReset(email) },
        onRegister = { email, password, name, phone, pin, onDone ->
          authViewModel.registerWithEmailPassword(email, password, name, phone, pin, onDone)
        },
      )
    }
  }
}
