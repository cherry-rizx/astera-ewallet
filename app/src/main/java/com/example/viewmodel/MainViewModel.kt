package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BankAccount
import com.example.model.MonthlyBudget
import com.example.model.ServiceResult
import com.example.model.SplitDetailItem
import com.example.model.TransactionRecord
import com.example.model.UserDirectoryEntry
import com.example.model.UserProfile
import com.example.model.UserSecret
import com.example.service.BankService
import com.example.service.BudgetService
import com.example.service.NotificationSettings
import com.example.service.PinVerifyOutcome
import com.example.service.ProfileService
import com.example.service.TransactionService
import com.example.service.WalletService
import com.example.util.friendlyFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

sealed interface PendingOperation {
  val amount: Long
  val outOfBudget: Boolean

  data class Transfer(
    val receiverId: String,
    val receiverName: String,
    val receiverPhone: String,
    override val amount: Long,
    val note: String,
    override val outOfBudget: Boolean,
  ) : PendingOperation

  data class TopUp(
    override val amount: Long,
    val selectedBankIds: List<String>,
    val splitPreview: List<SplitDetailItem>,
    override val outOfBudget: Boolean,
  ) : PendingOperation
}

sealed interface PinSubmissionState {
  data object Idle : PinSubmissionState

  data object Processing : PinSubmissionState

  data class WrongPin(val remainingAttempts: Int, val triggerId: Int) : PinSubmissionState

  data object Blocked : PinSubmissionState

  data class Error(val message: String) : PinSubmissionState
}

class MainViewModel(
  private val currentUserId: String,
  private val walletService: WalletService,
  private val transactionService: TransactionService,
  private val budgetService: BudgetService,
  private val bankService: BankService,
  private val profileService: ProfileService,
) : ViewModel() {

  val profileState: StateFlow<UiState<UserProfile?>> =
    walletService
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = unwrapFirestore(error)
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing user profile", firestoreError ?: error)
        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error, "Gagal memuat profil.")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val transactionsState: StateFlow<UiState<List<TransactionRecord>>> =
    transactionService
      .observeTransactions(currentUserId)
      .map<List<TransactionRecord>, UiState<List<TransactionRecord>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = unwrapFirestore(error)
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing transactions", firestoreError ?: error)
        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error, "Gagal memuat riwayat.")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val budgetState: StateFlow<UiState<MonthlyBudget?>> =
    budgetService
      .observeBudget(currentUserId)
      .map<MonthlyBudget?, UiState<MonthlyBudget?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = unwrapFirestore(error)
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing budget", firestoreError ?: error)
        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error, "Gagal memuat anggaran.")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val bankAccountsState: StateFlow<UiState<List<BankAccount>>> =
    bankService
      .observeBankAccounts(currentUserId)
      .map<List<BankAccount>, UiState<List<BankAccount>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = unwrapFirestore(error)
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing bank accounts", firestoreError ?: error)
        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error, "Gagal memuat rekening bank.")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val userSecret: StateFlow<UserSecret?> =
    profileService
      .observeUserSecret(currentUserId)
      .catch { emit(null) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val notificationSettings: StateFlow<NotificationSettings> =
    profileService
      .observeNotificationSettings()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), NotificationSettings())

  private val _searchResults = MutableStateFlow<UiState<List<UserDirectoryEntry>>>(UiState.Success(emptyList()))
  val searchResults: StateFlow<UiState<List<UserDirectoryEntry>>> = _searchResults.asStateFlow()

  private val _selectedRecipient = MutableStateFlow<UserDirectoryEntry?>(null)
  val selectedRecipient: StateFlow<UserDirectoryEntry?> = _selectedRecipient.asStateFlow()

  private val _pendingOperation = MutableStateFlow<PendingOperation?>(null)
  val pendingOperation: StateFlow<PendingOperation?> = _pendingOperation.asStateFlow()

  private val _completedTransaction = MutableStateFlow<TransactionRecord?>(null)
  val completedTransaction: StateFlow<TransactionRecord?> = _completedTransaction.asStateFlow()

  private val _pinState = MutableStateFlow<PinSubmissionState>(PinSubmissionState.Idle)
  val pinState: StateFlow<PinSubmissionState> = _pinState.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private var wrongPinCounter = 0

  init {
    viewModelScope.launch {
      budgetService.ensureCurrentMonthReset(currentUserId)
      searchRecipients("")
    }
  }

  fun searchRecipients(query: String) {
    _searchResults.value = UiState.Loading
    viewModelScope.launch {
      when (val res = walletService.searchRecipients(query, currentUserId)) {
        is ServiceResult.Success -> _searchResults.value = UiState.Success(res.data)
        is ServiceResult.Error -> _searchResults.value = UiState.Error(res.message)
      }
    }
  }

  fun selectRecipient(entry: UserDirectoryEntry?) {
    _selectedRecipient.value = entry
  }

  fun selectRecipientByQrUid(
    uid: String,
    onSuccess: (UserDirectoryEntry) -> Unit,
    onError: (String) -> Unit,
  ) {
    if (uid == currentUserId) {
      onError("Tidak dapat mentransfer ke QR akun sendiri.")
      return
    }
    viewModelScope.launch {
      when (val res = walletService.getRecipientByUid(uid)) {
        is ServiceResult.Success -> {
          val entry = res.data
          if (entry != null) {
            _selectedRecipient.value = entry
            onSuccess(entry)
          } else {
            onError("Pengguna QR tidak ditemukan di ASTERA.")
          }
        }
        is ServiceResult.Error -> onError(res.message)
      }
    }
  }

  fun prepareTransfer(
    recipient: UserDirectoryEntry,
    amount: Long,
    note: String,
  ) {
    val budget = (budgetState.value as? UiState.Success)?.data
    val overBudget =
      budget != null && budget.monthlyLimit > 0L && (budget.spentThisMonth + amount > budget.monthlyLimit)
    _pendingOperation.value =
      PendingOperation.Transfer(
        receiverId = recipient.uid,
        receiverName = recipient.name,
        receiverPhone = recipient.phone,
        amount = amount,
        note = note.trim(),
        outOfBudget = overBudget,
      )
    _pinState.value = PinSubmissionState.Idle
  }

  fun prepareTopUp(
    amount: Long,
    selectedBankIds: List<String>,
    splitPreview: List<SplitDetailItem>,
  ) {
    val budget = (budgetState.value as? UiState.Success)?.data
    val overBudget =
      budget != null && budget.monthlyLimit > 0L && (budget.spentThisMonth + amount > budget.monthlyLimit)
    _pendingOperation.value =
      PendingOperation.TopUp(
        amount = amount,
        selectedBankIds = selectedBankIds,
        splitPreview = splitPreview,
        outOfBudget = overBudget,
      )
    _pinState.value = PinSubmissionState.Idle
  }

  fun resetPinState() {
    _pinState.value =
      if (userSecret.value?.blocked == true) PinSubmissionState.Blocked else PinSubmissionState.Idle
  }

  fun verifyAppUnlockPin(pin: String, onResult: (String?) -> Unit) {
    viewModelScope.launch {
      when (val res = profileService.verifyPin(currentUserId, pin)) {
        is ServiceResult.Error -> onResult(res.message)
        is ServiceResult.Success -> when (val outcome = res.data) {
          PinVerifyOutcome.Verified -> onResult(null)
          is PinVerifyOutcome.WrongPin -> onResult("PIN salah. Sisa percobaan: ${outcome.remainingAttempts}.")
          PinVerifyOutcome.Blocked -> onResult("Akun diblokir setelah 3 kali salah PIN. Hubungi petugas ASTERA.")
        }
      }
    }
  }

  fun submitPin(pin: String, onSuccess: () -> Unit) {
    val operation = _pendingOperation.value
    if (operation == null) {
      _pinState.value = PinSubmissionState.Error("Data transaksi tidak ditemukan.")
      return
    }
    if (_pinState.value is PinSubmissionState.Processing) return

    _pinState.value = PinSubmissionState.Processing
    viewModelScope.launch {
      when (val pinRes = profileService.verifyPin(currentUserId, pin)) {
        is ServiceResult.Error -> {
          _pinState.value = PinSubmissionState.Error(pinRes.message)
        }
        is ServiceResult.Success -> {
          when (val outcome = pinRes.data) {
            PinVerifyOutcome.Blocked -> _pinState.value = PinSubmissionState.Blocked
            is PinVerifyOutcome.WrongPin -> {
              wrongPinCounter++
              _pinState.value = PinSubmissionState.WrongPin(outcome.remainingAttempts, wrongPinCounter)
            }
            PinVerifyOutcome.Verified -> {
              val senderName = (profileState.value as? UiState.Success)?.data?.name ?: "Nasabah ASTERA"
              val txRes =
                when (operation) {
                  is PendingOperation.Transfer ->
                    transactionService.executeTransfer(
                      senderId = currentUserId,
                      senderName = senderName,
                      receiverId = operation.receiverId,
                      receiverName = operation.receiverName,
                      amount = operation.amount,
                      note = operation.note,
                      outOfBudget = operation.outOfBudget,
                    )
                  is PendingOperation.TopUp ->
                    transactionService.executeTopUp(
                      uid = currentUserId,
                      userName = senderName,
                      amount = operation.amount,
                      selectedBankIds = operation.selectedBankIds,
                      outOfBudget = operation.outOfBudget,
                    )
                }
              when (txRes) {
                is ServiceResult.Success -> {
                  _completedTransaction.value = txRes.data
                  _pendingOperation.value = null
                  _pinState.value = PinSubmissionState.Idle
                  onSuccess()
                }
                is ServiceResult.Error -> {
                  _pinState.value = PinSubmissionState.Error(txRes.message)
                }
              }
            }
          }
        }
      }
    }
  }

  fun updateMonthlyLimit(limit: Long, onDone: () -> Unit) {
    viewModelScope.launch {
      when (val res = budgetService.updateMonthlyLimit(currentUserId, limit)) {
        is ServiceResult.Success -> {
          _actionMessage.value = "Anggaran bulanan berhasil disimpan."
          onDone()
        }
        is ServiceResult.Error -> _actionMessage.value = res.message
      }
    }
  }

  fun updateName(newName: String, onDone: () -> Unit) {
    viewModelScope.launch {
      when (val res = profileService.updateName(currentUserId, newName)) {
        is ServiceResult.Success -> {
          _actionMessage.value = "Nama berhasil diperbarui."
          onDone()
        }
        is ServiceResult.Error -> _actionMessage.value = res.message
      }
    }
  }

  fun updatePhotoUri(uriString: String) {
    viewModelScope.launch {
      when (val res = profileService.updatePhotoUri(currentUserId, uriString)) {
        is ServiceResult.Success -> _actionMessage.value = "Foto profil berhasil diperbarui."
        is ServiceResult.Error -> _actionMessage.value = res.message
      }
    }
  }

  fun changePin(oldPin: String, newPin: String, onDone: () -> Unit) {
    viewModelScope.launch {
      when (val res = profileService.changePin(currentUserId, oldPin, newPin)) {
        is ServiceResult.Success -> {
          _actionMessage.value = "PIN transaksi berhasil diubah."
          onDone()
        }
        is ServiceResult.Error -> _actionMessage.value = res.message
      }
    }
  }

  fun updateNotificationSettings(tx: Boolean, budget: Boolean, sec: Boolean) {
    viewModelScope.launch {
      when (val res = profileService.updateNotificationSetting(currentUserId, tx, budget, sec)) {
        is ServiceResult.Success -> _actionMessage.value = "Pengaturan notifikasi disimpan."
        is ServiceResult.Error -> _actionMessage.value = res.message
      }
    }
  }

  fun formatPinError(state: PinSubmissionState, nowMs: Long): String? =
    when (state) {
      is PinSubmissionState.WrongPin -> "PIN salah. Sisa percobaan: ${state.remainingAttempts}"
      PinSubmissionState.Blocked -> "Akun diblokir. Hubungi petugas ASTERA untuk membuka blokir."
      is PinSubmissionState.Error -> state.message
      else -> null
    }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? =
    generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()

  private companion object {
    const val TAG = "MainViewModel"
  }
}
