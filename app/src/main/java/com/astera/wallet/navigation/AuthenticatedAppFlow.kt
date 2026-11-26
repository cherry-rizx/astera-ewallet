package com.astera.wallet.navigation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astera.wallet.R
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.service.BankService
import com.astera.wallet.service.BudgetService
import com.astera.wallet.service.ProfileService
import com.astera.wallet.service.TransactionService
import com.astera.wallet.service.WalletService
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraBottomBar
import com.astera.wallet.ui.components.MainTab
import com.astera.wallet.ui.screens.account.AccountScreen
import com.astera.wallet.ui.screens.auth.AccountBlockedScreen
import com.astera.wallet.ui.screens.auth.AppUnlockScreen
import com.astera.wallet.ui.screens.history.HistoryScreen
import com.astera.wallet.ui.screens.home.HomeScreen
import com.astera.wallet.ui.screens.notifications.NotificationsScreen
import com.astera.wallet.ui.screens.qris.QrisScreen
import com.astera.wallet.ui.screens.summary.SpendingSummaryScreen
import com.astera.wallet.ui.screens.topup.TopUpScreen
import com.astera.wallet.ui.screens.transaction.ConfirmOperationScreen
import com.astera.wallet.ui.screens.transaction.PinVerificationScreen
import com.astera.wallet.ui.screens.transaction.SuccessScreen
import com.astera.wallet.ui.screens.transaction.TransactionDetailBottomSheet
import com.astera.wallet.ui.screens.transfer.TransferScreen
import com.astera.wallet.viewmodel.MainViewModel
import com.astera.wallet.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

@Composable
internal fun AuthenticatedAppFlow(
  currentUserId: String,
  skipAppUnlock: Boolean,
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
  val notificationFeed by viewModel.notificationFeed.collectAsStateWithLifecycle()
  val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()

  // Baru login/daftar -> langsung masuk. Sesi lama yang dipulihkan -> minta PIN/biometrik.
  var appUnlocked by remember(currentUserId) { mutableStateOf(skipAppUnlock) }
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
              // Dibatalkan / pilih "Gunakan PIN" bukan error: keypad PIN sudah tampil di belakangnya.
              val dismissed =
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                  errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                  errorCode == BiometricPrompt.ERROR_CANCELED
              if (!dismissed) unlockError = errString.toString()
            }
          },
        )
      }
    }
  val biometricPromptInfo =
    remember {
      BiometricPrompt.PromptInfo.Builder()
        .setTitle("Buka ASTERA")
        .setSubtitle("Tempelkan sidik jari untuk masuk")
        .setNegativeButtonText("Gunakan PIN")
        .build()
    }

  val biometricAvailable =
    biometricPrompt != null &&
      biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

  LaunchedEffect(currentUserId, userSecret?.uid, biometricPrompt) {
    if (userSecret != null && userSecret?.blocked != true && !appUnlocked && biometricAvailable) {
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
      biometricAvailable = biometricAvailable,
      onBiometric = {
        unlockError = null
        biometricPrompt?.authenticate(biometricPromptInfo)
      },
      onSignOut = onSignOut,
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

  val showBottomBar = MainTab.entries.any { it.route == currentRoute }

  // Pindah antar-tab bottom nav tanpa menumpuk back stack.
  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(AsteraRoutes.BERANDA) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  // Dibaca di dalam tiap layar (bukan di sini) supaya perubahan saldo tidak me-recompose seluruh flow.
  fun currentProfile() = (profileState as? UiState.Success)?.data

  AsteraBackground {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (showBottomBar) {
          AsteraBottomBar(
            currentRoute = currentRoute,
            onSelectTab = { tab -> navigateToTab(tab.route) },
          )
        }
      },
    ) { innerPadding ->
      // Ruang ekstra di atas bottom nav supaya konten tidak tertutup tombol QRIS yang melayang.
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
            // Inset (termasuk keyboard) sudah dipakai Scaffold; jangan dihitung lagi oleh layar anak.
            .consumeWindowInsets(innerPadding)
            .padding(bottom = if (showBottomBar) 24.dp else 0.dp)
      ) {
        NavHost(
          navController = navController,
          startDestination = AsteraRoutes.BERANDA,
          // Bawaan Navigation adalah crossfade 700 ms yang terasa lambat; dipersingkat.
          enterTransition = { fadeIn(tween(180)) },
          exitTransition = { fadeOut(tween(120)) },
          popEnterTransition = { fadeIn(tween(180)) },
          popExitTransition = { fadeOut(tween(120)) },
        ) {
          composable(AsteraRoutes.BERANDA) {
            HomeScreen(
              currentUserId = currentUserId,
              profileState = profileState,
              budgetState = budgetState,
              transactionsState = transactionsState,
              unreadNotificationCount = unreadNotificationCount,
              onOpenNotifications = { navController.navigate(AsteraRoutes.NOTIFIKASI) },
              onOpenTransfer = { navController.navigate(AsteraRoutes.TRANSFER_MANUAL) },
              onOpenQris = { navigateToTab(AsteraRoutes.QRIS) },
              onOpenTopUp = { navController.navigate(AsteraRoutes.TOP_UP) },
              onOpenHistory = { navigateToTab(AsteraRoutes.TRANSAKSI) },
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
              currentUserName = currentProfile()?.name ?: "Nasabah ASTERA",
              currentUserAccountNumber = currentProfile()?.accountNumber.orEmpty(),
              currentBalance = currentProfile()?.balance ?: 0L,
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
              onContinueExternalTransfer = { type, provider, account, name, amount, note ->
                viewModel.prepareExternalTransfer(type, provider, account, name, amount, note)
                navController.navigate(AsteraRoutes.CONFIRM)
              },
              onBack = { navController.popBackStack() },
            )
          }

          composable(AsteraRoutes.QRIS) {
            QrisScreen(
              currentUserId = currentUserId,
              profile = currentProfile(),
              onQrUidScanned = { uid, ok, err -> viewModel.selectRecipientByQrUid(uid, ok, err) },
              // launchSingleTop: scan/NFC bisa terpicu dua kali cepat, jangan tumpuk dua layar Transfer.
              onRecipientReady = { navController.navigate(AsteraRoutes.TRANSFER_MANUAL) { launchSingleTop = true } },
            )
          }

          composable(AsteraRoutes.RINGKASAN) {
            SpendingSummaryScreen(
              currentUserId = currentUserId,
              transactionsState = transactionsState,
            )
          }

          composable(AsteraRoutes.NOTIFIKASI) {
            // Tandai dibaca saat dibuka dan saat ditutup (notifikasi yang masuk selama layar terbuka ikut terbaca).
            DisposableEffect(Unit) {
              viewModel.markNotificationsSeen()
              onDispose { viewModel.markNotificationsSeen() }
            }
            NotificationsScreen(
              notifications = notificationFeed,
              onOpenTransaction = { id ->
                inspectedTransaction = (transactionsState as? UiState.Success)?.data?.firstOrNull { it.id == id }
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
