package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.TransactionRecord
import com.example.service.AuthService
import com.example.service.BankService
import com.example.service.BudgetService
import com.example.service.ProfileService
import com.example.service.TransactionService
import com.example.service.WalletService
import com.example.ui.components.AsteraBackground
import com.example.ui.components.AsteraBottomBar
import com.example.ui.components.MainTab
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AccountBlockedScreen
import com.example.ui.screens.AppUnlockScreen
import com.example.ui.screens.ConfirmOperationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PinVerificationScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SuccessScreen
import com.example.ui.screens.TopUpScreen
import com.example.ui.screens.TransactionDetailBottomSheet
import com.example.ui.screens.TransferScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.AsteraTheme
import com.example.viewmodel.AuthGateState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiState
import com.astera.wallet.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object AsteraRoutes {
  const val SPLASH = "splash"
  const val WELCOME = "welcome"
  const val LOGIN = "login"
  const val REGISTER = "register"
  const val BERANDA = "beranda"
  const val TRANSAKSI = "transaksi"
  const val AKUN = "akun"
  const val TRANSFER_MANUAL = "transfer_manual"
  const val TRANSFER_QR = "transfer_qr"
  const val TOP_UP = "top_up"
  const val CONFIRM = "confirm"
  const val PIN = "pin"
  const val SUCCESS = "success"
}

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      AsteraTheme {
        AsteraApp()
      }
    }
  }
}

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
            )
          }
        }
    )

  val gateState by authViewModel.gateState.collectAsStateWithLifecycle()
  val isBusy by authViewModel.isBusy.collectAsStateWithLifecycle()
  val authError by authViewModel.authError.collectAsStateWithLifecycle()
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
          onSignOut = { authViewModel.signOut() },
        )
      }
    }
    else -> {
      UnauthenticatedAppFlow(
        gateState = state,
        isBusy = isBusy,
        authError = authError,
        onClearError = { authViewModel.clearError() },
        onEmailSignIn = { email, password -> authViewModel.signInWithEmailPassword(email, password) },
        onForgotPassword = { email -> authViewModel.sendPasswordReset(email) },
        onRegister = { email, password, name, phone, pin, onDone ->
          authViewModel.registerWithEmailPassword(email, password, name, phone, pin, onDone)
        },
      )
    }
  }
}

@Composable
private fun UnauthenticatedAppFlow(
  gateState: AuthGateState,
  isBusy: Boolean,
  authError: String?,
  onClearError: () -> Unit,
  onEmailSignIn: (String, String) -> Unit,
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

@Composable
private fun AuthenticatedAppFlow(
  currentUserId: String,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

  LaunchedEffect(currentUserId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        "astera_transactions",
        "Transaksi ASTERA",
        NotificationManager.IMPORTANCE_DEFAULT,
      )
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    if (Build.VERSION.SDK_INT >= 33 &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    runCatching {
      val token = FirebaseMessaging.getInstance().token.await()
      FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        .collection("fcm_tokens").document(currentUserId)
        .set(mapOf("uid" to currentUserId, "token" to token)).await()
    }
  }

  val viewModel: MainViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            MainViewModel(
              currentUserId = currentUserId,
              walletService = WalletService(db),
              transactionService = TransactionService(db),
              budgetService = BudgetService(db),
              bankService = BankService(db),
              profileService = ProfileService(db, app.applicationContext),
            )
          }
        },
    )

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val budgetState by viewModel.budgetState.collectAsStateWithLifecycle()
  val bankAccountsState by viewModel.bankAccountsState.collectAsStateWithLifecycle()
  val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
  val selectedRecipient by viewModel.selectedRecipient.collectAsStateWithLifecycle()
  val pendingOperation by viewModel.pendingOperation.collectAsStateWithLifecycle()
  val completedTransaction by viewModel.completedTransaction.collectAsStateWithLifecycle()
  val pinState by viewModel.pinState.collectAsStateWithLifecycle()
  val notificationSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val userSecret by viewModel.userSecret.collectAsStateWithLifecycle()

  var appUnlocked by remember(currentUserId) { mutableStateOf(false) }
  var unlockError by remember(currentUserId) { mutableStateOf<String?>(null) }
  val hostActivity = context as? FragmentActivity
  val biometricManager = remember(context) { BiometricManager.from(context) }
  val biometricPrompt =
    remember(hostActivity) {
      hostActivity?.let { activity ->
        BiometricPrompt(
          activity,
          activity.mainExecutor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
              appUnlocked = true
              unlockError = null
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) unlockError = errString.toString()
            }
          },
        )
      }
    }
  val biometricPromptInfo =
    remember {
      BiometricPrompt.PromptInfo.Builder()
        .setTitle("Buka ASTERA")
        .setSubtitle("Verifikasi biometrik atau lanjutkan dengan PIN transaksi")
        .setNegativeButtonText("Gunakan PIN")
        .build()
    }

  LaunchedEffect(currentUserId, userSecret?.uid, biometricPrompt) {
    if (userSecret != null && userSecret?.blocked != true && !appUnlocked && biometricPrompt != null &&
      biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    ) {
      biometricPrompt.authenticate(biometricPromptInfo)
    }
  }

  if (userSecret?.blocked == true) {
    AccountBlockedScreen(onSignOut = onSignOut)
    return
  }

  if (!appUnlocked) {
    AppUnlockScreen(
      errorMessage = unlockError,
      onBiometric = {
        if (biometricPrompt != null) biometricPrompt.authenticate(biometricPromptInfo)
        else unlockError = "Biometrik perangkat tidak tersedia. Gunakan PIN transaksi."
      },
      onVerifyPin = { pin ->
        viewModel.verifyAppUnlockPin(pin) { error ->
          unlockError = error
          if (error == null) appUnlocked = true
        }
      },
    )
    return
  }

  val snackbarHostState = remember { SnackbarHostState() }
  var inspectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

  LaunchedEffect(actionMessage) {
    val msg = actionMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearActionMessage()
    }
  }

  val showBottomBar =
    currentRoute == AsteraRoutes.BERANDA ||
      currentRoute == AsteraRoutes.TRANSAKSI ||
      currentRoute == AsteraRoutes.AKUN

  val profile = (profileState as? UiState.Success)?.data

  AsteraBackground {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (showBottomBar) {
          AsteraBottomBar(
            currentRoute = currentRoute,
            onSelectTab = { tab ->
              navController.navigate(tab.route) {
                popUpTo(AsteraRoutes.BERANDA) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
          )
        }
      },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        NavHost(
          navController = navController,
          startDestination = AsteraRoutes.BERANDA,
        ) {
          composable(AsteraRoutes.BERANDA) {
            HomeScreen(
              currentUserId = currentUserId,
              profileState = profileState,
              budgetState = budgetState,
              transactionsState = transactionsState,
              onOpenAccount = {
                navController.navigate(MainTab.AKUN.route) {
                  popUpTo(AsteraRoutes.BERANDA) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              onOpenTransfer = { startWithQr ->
                navController.navigate(
                  if (startWithQr) AsteraRoutes.TRANSFER_QR else AsteraRoutes.TRANSFER_MANUAL
                )
              },
              onOpenTopUp = { navController.navigate(AsteraRoutes.TOP_UP) },
              onOpenHistory = {
                navController.navigate(MainTab.TRANSAKSI.route) {
                  popUpTo(AsteraRoutes.BERANDA) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              onSelectTransaction = { inspectedTransaction = it },
              onUpdateMonthlyLimit = { limit, onDone ->
                viewModel.updateMonthlyLimit(limit, onDone)
              },
            )
          }

          composable(AsteraRoutes.TRANSAKSI) {
            HistoryScreen(
              currentUserId = currentUserId,
              transactionsState = transactionsState,
              onSelectTransaction = { inspectedTransaction = it },
            )
          }

          composable(AsteraRoutes.AKUN) {
            AccountScreen(
              profileState = profileState,
              budgetState = budgetState,
              notificationSettings = notificationSettings,
              onUpdateName = { name, onDone -> viewModel.updateName(name, onDone) },
              onUpdatePhotoUri = { uri -> viewModel.updatePhotoUri(uri) },
              onUpdateMonthlyLimit = { limit, onDone ->
                viewModel.updateMonthlyLimit(limit, onDone)
              },
              onChangePin = { oldPin, newPin, onDone ->
                viewModel.changePin(oldPin, newPin, onDone)
              },
              onUpdateNotifications = { tx, budget, sec ->
                viewModel.updateNotificationSettings(tx, budget, sec)
              },
              onSignOut = onSignOut,
            )
          }

          composable(AsteraRoutes.TRANSFER_MANUAL) {
            TransferScreen(
              currentUserId = currentUserId,
              currentUserName = profile?.name ?: "Nasabah ASTERA",
              currentUserPhone = profile?.phone ?: "-",
              currentBalance = profile?.balance ?: 0L,
              initialStartWithQr = false,
              searchResults = searchResults,
              selectedRecipient = selectedRecipient,
              onSearchQueryChanged = { viewModel.searchRecipients(it) },
              onSelectRecipient = { viewModel.selectRecipient(it) },
              onQrUidScanned = { uid, ok, err -> viewModel.selectRecipientByQrUid(uid, ok, err) },
              onContinueTransfer = { recipient, amount, note ->
                viewModel.prepareTransfer(recipient, amount, note)
                navController.navigate(AsteraRoutes.CONFIRM)
              },
              onBack = { navController.popBackStack() },
            )
          }

          composable(AsteraRoutes.TRANSFER_QR) {
            TransferScreen(
              currentUserId = currentUserId,
              currentUserName = profile?.name ?: "Nasabah ASTERA",
              currentUserPhone = profile?.phone ?: "-",
              currentBalance = profile?.balance ?: 0L,
              initialStartWithQr = true,
              searchResults = searchResults,
              selectedRecipient = selectedRecipient,
              onSearchQueryChanged = { viewModel.searchRecipients(it) },
              onSelectRecipient = { viewModel.selectRecipient(it) },
              onQrUidScanned = { uid, ok, err -> viewModel.selectRecipientByQrUid(uid, ok, err) },
              onContinueTransfer = { recipient, amount, note ->
                viewModel.prepareTransfer(recipient, amount, note)
                navController.navigate(AsteraRoutes.CONFIRM)
              },
              onBack = { navController.popBackStack() },
            )
          }

          composable(AsteraRoutes.TOP_UP) {
            TopUpScreen(
              bankAccountsState = bankAccountsState,
              onContinueTopUp = { amount, bankIds, splitPreview ->
                viewModel.prepareTopUp(amount, bankIds, splitPreview)
                navController.navigate(AsteraRoutes.CONFIRM)
              },
              onBack = { navController.popBackStack() },
            )
          }

          composable(AsteraRoutes.CONFIRM) {
            ConfirmOperationScreen(
              pendingOperation = pendingOperation,
              onProceedToPin = { navController.navigate(AsteraRoutes.PIN) },
              onBack = { navController.popBackStack() },
            )
          }

          composable(AsteraRoutes.PIN) {
            PinVerificationScreen(
              pendingOperation = pendingOperation,
              pinState = pinState,
              onResetPinState = { viewModel.resetPinState() },
              onSubmitPin = { pin ->
                viewModel.submitPin(pin) {
                  navController.navigate(AsteraRoutes.SUCCESS) {
                    popUpTo(AsteraRoutes.BERANDA) { inclusive = false }
                  }
                }
              },
              onBack = { navController.popBackStack() },
            )
          }

          composable(AsteraRoutes.SUCCESS) {
            SuccessScreen(
              transaction = completedTransaction,
              onBackHome = {
                navController.popBackStack(AsteraRoutes.BERANDA, inclusive = false)
              },
            )
          }
        }
      }
    }
  }

  val txToShow = inspectedTransaction
  if (txToShow != null) {
    TransactionDetailBottomSheet(
      transaction = txToShow,
      currentUserId = currentUserId,
      onDismiss = { inspectedTransaction = null },
    )
  }
}
