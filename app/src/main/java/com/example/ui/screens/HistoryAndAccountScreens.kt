package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.MonthlyBudget
import com.example.model.TransactionRecord
import com.example.model.UserProfile
import com.example.service.NotificationSettings
import com.example.ui.components.AsteraCard
import com.example.ui.components.AsteraLogoBadge
import com.example.ui.components.AsteraTextField
import com.example.ui.components.SimulasiBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AsteraAmber
import com.example.ui.theme.AsteraCard
import com.example.ui.theme.AsteraCardBorder
import com.example.ui.theme.AsteraFieldBg
import com.example.ui.theme.AsteraPrimary
import com.example.ui.theme.AsteraRed
import com.example.ui.theme.AsteraSkyBlue
import com.example.ui.theme.AsteraTextSecondary
import com.example.util.formatDateGroupLabel
import com.example.util.formatRupiah
import com.example.viewmodel.UiState

enum class HistoryFilter(val label: String) {
  SEMUA("Semua"),
  MASUK("Masuk"),
  KELUAR("Keluar"),
}

@Composable
fun HistoryScreen(
  currentUserId: String,
  transactionsState: UiState<List<TransactionRecord>>,
  onSelectTransaction: (TransactionRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedFilter by remember { mutableStateOf(HistoryFilter.SEMUA) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.widthIn(max = 600.dp).align(Alignment.TopCenter).fillMaxSize()) {
      Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
          text = "Riwayat Transaksi",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Pantau semua aktivitas saldo simulasi",
          style = MaterialTheme.typography.bodyMedium,
          color = AsteraTextSecondary,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          HistoryFilter.entries.forEach { filter ->
            val selected = selectedFilter == filter
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) AsteraPrimary else AsteraFieldBg,
              border =
                BorderStroke(
                  1.dp,
                  if (selected) AsteraAmber else AsteraCardBorder.copy(alpha = 0.5f),
                ),
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { selectedFilter = filter }
                  .minimumInteractiveComponentSize()
                  .testTag("history_filter_${filter.name.lowercase()}"),
            ) {
              Box(
                modifier = Modifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = filter.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) Color.White else AsteraTextSecondary,
                )
              }
            }
          }
        }
      }

      when (transactionsState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AsteraSkyBlue)
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            AsteraCard {
              Text(
                text = transactionsState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFCA5A5),
              )
            }
          }
        }
        is UiState.Success -> {
          val filtered =
            transactionsState.data.filter { tx ->
              val isIncoming = tx.type == "topup" || tx.receiverId == currentUserId
              when (selectedFilter) {
                HistoryFilter.SEMUA -> true
                HistoryFilter.MASUK -> isIncoming
                HistoryFilter.KELUAR -> !isIncoming
              }
            }

          if (filtered.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              AsteraCard {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = AsteraTextSecondary,
                    modifier = Modifier.size(44.dp),
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "Belum ada transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Aktivitas transfer dan top up simulasi akan tampil di sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AsteraTextSecondary,
                  )
                }
              }
            }
          } else {
            val grouped = filtered.groupBy { formatDateGroupLabel(it.createdAt) }
            LazyColumn(
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              grouped.forEach { (dateLabel, itemsInGroup) ->
                item(key = "header_$dateLabel") {
                  Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = AsteraAmber,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                  )
                }
                items(itemsInGroup, key = { it.id }) { tx ->
                  TransactionRowCard(
                    tx = tx,
                    currentUserId = currentUserId,
                    onClick = { onSelectTransaction(tx) },
                  )
                }
              }
              item { Spacer(modifier = Modifier.height(12.dp)) }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AccountScreen(
  profileState: UiState<UserProfile?>,
  budgetState: UiState<MonthlyBudget?>,
  notificationSettings: NotificationSettings,
  onUpdateName: (String, () -> Unit) -> Unit,
  onUpdatePhotoUri: (String) -> Unit,
  onUpdateMonthlyLimit: (Long, () -> Unit) -> Unit,
  onChangePin: (String, String, () -> Unit) -> Unit,
  onUpdateNotifications: (Boolean, Boolean, Boolean) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val profile = (profileState as? UiState.Success)?.data
  val currentLimit = (budgetState as? UiState.Success)?.data?.monthlyLimit ?: 1_000_000L

  var showProfileDialog by remember { mutableStateOf(false) }
  var showBudgetDialog by remember { mutableStateOf(false) }
  var showSecurityDialog by remember { mutableStateOf(false) }
  var showNotifDialog by remember { mutableStateOf(false) }
  var showHelpDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let { onUpdatePhotoUri(it.toString()) }
    }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 600.dp)
          .align(Alignment.TopCenter)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Akun",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
          )
          Text(
            text = "Kelola profil dan keamanan",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
          )
        }
        IconButton(
          onClick = { showNotifDialog = true },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("account_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Pengaturan",
            tint = Color.White,
          )
        }
      }

      // Profile Header Card (tap avatar to pick photo)
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(contentAlignment = Alignment.BottomEnd) {
          UserAvatar(
            name = profile?.name ?: "Nasabah",
            photoUrl = profile?.photoUrl,
            size = 86.dp,
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.testTag("account_avatar_picker"),
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = AsteraAmber,
            modifier = Modifier.size(26.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = "Ubah foto",
                tint = Color(0xFF101E40),
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = profile?.name ?: "Memuat...",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        Text(
          text = "Nasabah ASTERA",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraAmber,
        )
      }

      // Dompet ASTERA card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AsteraSkyBlue.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(0.dp)
              .clip(RoundedCornerShape(16.dp))
              .let { it },
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .let { mod ->
                  mod
                }
                .padding(16.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              AsteraLogoBadge(size = 44.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Dompet ASTERA",
                  style = MaterialTheme.typography.titleSmall,
                  color = Color.White,
                )
                Text(
                  text =
                    "${profile?.phone ?: "-"} • ${formatRupiah(profile?.balance ?: 0L)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = AsteraTextSecondary,
                )
              }
              SimulasiBadge()
            }
          }
        }
      }

      // Account Menus
      AccountMenuCard(
        icon = Icons.Outlined.Person,
        title = "Informasi Pribadi",
        subtitle = "Ubah nama dan lihat kontak",
        onClick = { showProfileDialog = true },
        testTag = "menu_personal_info",
      )
      AccountMenuCard(
        icon = Icons.Outlined.AccountBalanceWallet,
        title = "Anggaran Bulanan",
        subtitle = "Batas saat ini: ${formatRupiah(currentLimit)}",
        onClick = { showBudgetDialog = true },
        testTag = "menu_budget",
      )
      AccountMenuCard(
        icon = Icons.Outlined.Lock,
        title = "Keamanan",
        subtitle = "Ubah 6 digit PIN transaksi",
        onClick = { showSecurityDialog = true },
        testTag = "menu_security",
      )
      AccountMenuCard(
        icon = Icons.Outlined.Notifications,
        title = "Pengaturan Notifikasi",
        subtitle = "Transaksi, anggaran, dan keamanan",
        onClick = { showNotifDialog = true },
        testTag = "menu_notifications",
      )
      AccountMenuCard(
        icon = Icons.AutoMirrored.Rounded.HelpOutline,
        title = "Bantuan",
        subtitle = "Panduan simulasi perbankan",
        onClick = { showHelpDialog = true },
        testTag = "menu_help",
      )
      AccountMenuCard(
        icon = Icons.Outlined.Info,
        title = "Tentang Kami",
        subtitle = "ASTERA Digital Banking (Simulasi)",
        onClick = { showAboutDialog = true },
        testTag = "menu_about",
      )

      // Red Keluar button
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = AsteraRed.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, AsteraRed.copy(alpha = 0.5f)),
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSignOut)
            .minimumInteractiveComponentSize()
            .testTag("account_logout_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Logout,
            contentDescription = "Keluar",
            tint = AsteraRed,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Keluar",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = AsteraRed,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  if (showProfileDialog && profile != null) {
    var nameInput by remember { mutableStateOf(profile.name) }
    var nameError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
      onDismissRequest = { showProfileDialog = false },
      containerColor = AsteraCard,
      titleContentColor = Color.White,
      title = { Text("Informasi Pribadi") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          AsteraTextField(
            value = nameInput,
            onValueChange = {
              nameInput = it
              nameError = null
            },
            label = "Nama Lengkap",
            placeholder = "Masukkan nama",
            errorText = nameError,
          )
          Text(
            text = "Nomor HP: ${profile.phone}",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
          )
          Text(
            text = "Email: ${profile.email}",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (nameInput.trim().isEmpty()) {
              nameError = "Nama wajib diisi"
            } else {
              onUpdateName(nameInput) { showProfileDialog = false }
            }
          }
        ) {
          Text("Simpan", color = AsteraAmber, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showProfileDialog = false }) {
          Text("Batal", color = AsteraTextSecondary)
        }
      },
    )
  }

  if (showBudgetDialog) {
    EditBudgetDialog(
      initialLimit = currentLimit,
      onDismiss = { showBudgetDialog = false },
      onSave = { newLimit -> onUpdateMonthlyLimit(newLimit) { showBudgetDialog = false } },
    )
  }

  if (showSecurityDialog) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinErr by remember { mutableStateOf<String?>(null) }
    AlertDialog(
      onDismissRequest = { showSecurityDialog = false },
      containerColor = AsteraCard,
      titleContentColor = Color.White,
      title = { Text("Ubah PIN Transaksi") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          AsteraTextField(
            value = oldPin,
            onValueChange = {
              oldPin = it.filter { c -> c.isDigit() }.take(6)
              pinErr = null
            },
            label = "PIN Lama",
            placeholder = "6 digit PIN lama",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
          )
          AsteraTextField(
            value = newPin,
            onValueChange = {
              newPin = it.filter { c -> c.isDigit() }.take(6)
              pinErr = null
            },
            label = "PIN Baru",
            placeholder = "6 digit PIN baru",
            errorText = pinErr,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (!oldPin.matches(Regex("^[0-9]{6}$")) || !newPin.matches(Regex("^[0-9]{6}$"))) {
              pinErr = "PIN lama dan PIN baru harus 6 digit angka"
            } else {
              onChangePin(oldPin, newPin) { showSecurityDialog = false }
            }
          }
        ) {
          Text("Simpan PIN", color = AsteraAmber, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSecurityDialog = false }) {
          Text("Batal", color = AsteraTextSecondary)
        }
      },
    )
  }

  if (showNotifDialog) {
    var txAlert by remember(notificationSettings) { mutableStateOf(notificationSettings.transactionAlerts) }
    var budgetAlert by remember(notificationSettings) { mutableStateOf(notificationSettings.budgetWarnings) }
    var secAlert by remember(notificationSettings) { mutableStateOf(notificationSettings.securityAlerts) }

    AlertDialog(
      onDismissRequest = { showNotifDialog = false },
      containerColor = AsteraCard,
      titleContentColor = Color.White,
      title = { Text("Pengaturan Notifikasi") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          NotifToggleRow("Aktivitas Transaksi", txAlert) { txAlert = it }
          NotifToggleRow("Peringatan Anggaran (80%+)", budgetAlert) { budgetAlert = it }
          NotifToggleRow("Keamanan & PIN", secAlert) { secAlert = it }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            onUpdateNotifications(txAlert, budgetAlert, secAlert)
            showNotifDialog = false
          }
        ) {
          Text("Simpan", color = AsteraAmber, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNotifDialog = false }) {
          Text("Tutup", color = AsteraTextSecondary)
        }
      },
    )
  }

  if (showHelpDialog) {
    AlertDialog(
      onDismissRequest = { showHelpDialog = false },
      containerColor = AsteraCard,
      titleContentColor = Color.White,
      title = { Text("Bantuan ASTERA") },
      text = {
        Text(
          text =
            "• Semua saldo dan transaksi di aplikasi ini adalah data simulasi.\n" +
              "• Batas transfer maksimal Rp 5.000.000 per transaksi.\n" +
              "• Top Up Simulasi maksimal Rp 1.000.000 dengan pembagian otomatis dari Bank Alpha dan Bank Beta.\n" +
              "• Salah memasukkan PIN 3 kali berturut-turut akan mengunci transaksi selama 5 menit.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White,
        )
      },
      confirmButton = {
        TextButton(onClick = { showHelpDialog = false }) {
          Text("Mengerti", color = AsteraAmber, fontWeight = FontWeight.Bold)
        }
      },
    )
  }

  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      containerColor = AsteraCard,
      titleContentColor = Color.White,
      title = { Text("Tentang ASTERA") },
      text = {
        Text(
          text =
            "ASTERA Digital Banking adalah aplikasi perbankan digital simulasi untuk belajar manajemen transfer, top up multi-bank, QR transfer, dan kontrol anggaran bulanan.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White,
        )
      },
      confirmButton = {
        TextButton(onClick = { showAboutDialog = false }) {
          Text("Tutup", color = AsteraAmber, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}

@Composable
private fun AccountMenuCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
) {
  AsteraCard(
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    modifier = Modifier.testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = AsteraSkyBlue,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color.White)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = AsteraTextSecondary)
      }
      Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = AsteraTextSecondary,
      )
    }
  }
}

@Composable
private fun NotifToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = Color(0xFF101E40),
          checkedTrackColor = AsteraAmber,
        ),
    )
  }
}
