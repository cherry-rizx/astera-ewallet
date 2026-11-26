package com.astera.wallet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.astera.wallet.ui.screens.auth.LoginScreen
import com.astera.wallet.ui.screens.auth.RegisterScreen
import com.astera.wallet.ui.screens.auth.WelcomeScreen
import com.astera.wallet.viewmodel.AuthGateState

@Composable
internal fun UnauthenticatedAppFlow(
  gateState: AuthGateState,
  isBusy: Boolean,
  authError: String?,
  onClearError: () -> Unit,
  rememberedEmail: String?,
  onEmailSignIn: (email: String, password: String, rememberMe: Boolean) -> Unit,
  onForgotPassword: (String) -> Unit,
  onRegister: (String, String, String, String, String, () -> Unit) -> Unit,
) {
  val navController = rememberNavController()
  val needsReg = gateState as? AuthGateState.NeedsRegistration

  LaunchedEffect(needsReg) {
    if (needsReg != null) {
      navController.navigate(AsteraRoutes.REGISTER) {
        launchSingleTop = true
      }
    }
  }

  NavHost(
    navController = navController,
    startDestination = AsteraRoutes.WELCOME,
  ) {
    composable(AsteraRoutes.WELCOME) {
      WelcomeScreen(
        onNavigateLogin = {
          onClearError()
          navController.navigate(AsteraRoutes.LOGIN)
        },
        onNavigateRegister = {
          onClearError()
          navController.navigate(AsteraRoutes.REGISTER)
        },
      )
    }
    composable(AsteraRoutes.LOGIN) {
      LoginScreen(
        isBusy = isBusy,
        errorMessage = authError,
        rememberedEmail = rememberedEmail,
        onEmailSignIn = onEmailSignIn,
        onForgotPassword = onForgotPassword,
        onNavigateRegister = {
          onClearError()
          navController.navigate(AsteraRoutes.REGISTER)
        },
        onBack = { navController.popBackStack() },
      )
    }
    composable(AsteraRoutes.REGISTER) {
      RegisterScreen(
        needsProfileCompletion = needsReg,
        isBusy = isBusy,
        serverError = authError,
        onSubmitRegistration = { email, password, name, phone, pin ->
          onRegister(email, password, name, phone, pin) {}
        },
        onBack = { navController.popBackStack() },
      )
    }
  }
}
