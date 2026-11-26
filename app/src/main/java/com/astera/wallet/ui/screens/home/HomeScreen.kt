package com.astera.wallet.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.MonthlyBudget
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.model.UserProfile
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraLogoBadge
import com.astera.wallet.ui.components.EditBudgetDialog
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.components.TransactionRowCard
import com.astera.wallet.ui.components.WalletCard
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraBrightBlue
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraRed
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.viewmodel.UiState

@Composable
fun HomeScreen(
  currentUserId: String,
  profileState: UiState<UserProfile?>,
  budgetState: UiState<MonthlyBudget?>,
  transactionsState: UiState<List<TransactionRecord>>,
  unreadNotificationCount: Int,
  onOpenNotifications: () -> Unit,
  onOpenTransfer: () -> Unit,
  onOpenQris: () -> Unit,
  onOpenTopUp: () -> Unit,
  onOpenHistory: () -> Unit,
  onSelectTransaction: (TransactionRecord) -> Unit,
  onUpdateMonthlyLimit: (Long, () -> Unit) -> Unit,
  modifier: Modifier = Modifier,
) {
  var hideBalance by rememberSaveable { mutableStateOf(false) }
  var showBudgetDialog by remember { mutableStateOf(false) }

  val profile = (profileState as? UiState.Success)?.data
  val displayName = profile?.name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Nasabah"

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).align(Alignment.TopCenter).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Greeting Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Halo, $displayName 👋",
              style = MaterialTheme.typography.headlineMedium,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Saatnya wujudkan impianmu!",
              style = MaterialTheme.typography.bodyMedium,
              color = AsteraTextSecondary,
            )
          }
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("home_notifications_button"),
          ) {
            BadgedBox(
              badge = {
                if (unreadNotificationCount > 0) {
                  Badge(containerColor = AsteraRed) {
                    Text(if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString())
                  }
                }
              },
            ) {
              Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription =
                  if (unreadNotificationCount > 0) "Notifikasi, $unreadNotificationCount belum dibaca" else "Notifikasi",
                tint = AsteraAmber,
                modifier = Modifier.size(30.dp),
              )
            }
          }
        }
      }

      // Balance Card ("Total Saldo")
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, AsteraSkyBlue.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(
                  Brush.linearGradient(listOf(Color(0xFF2356B8), Color(0xFF193C8A)))
                )
                .padding(20.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Total Saldo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  IconButton(
                    onClick = { hideBalance = !hideBalance },
                    modifier = Modifier.size(32.dp).testTag("toggle_balance_visibility"),
                  ) {
                    Icon(
                      imageVector =
                        if (hideBalance) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                      contentDescription =
                        if (hideBalance) "Tampilkan saldo" else "Sembunyikan saldo",
                      tint = Color.White.copy(alpha = 0.9f),
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  SimulasiBadge(text = "Simulasi")
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (profileState) {
                  UiState.Loading -> {
                    CircularProgressIndicator(
                      modifier = Modifier.size(26.dp),
                      color = Color.White,
                      strokeWidth = 2.dp,
                    )
                  }
                  is UiState.Error -> {
                    Text(
                      text = "Gagal memuat saldo",
                      style = MaterialTheme.typography.titleMedium,
                      color = Color(0xFFFCA5A5),
                    )
                  }
                  is UiState.Success -> {
                    val balanceText =
                      if (hideBalance) "Rp ••••••••"
                      else formatRupiah(profile?.balance ?: 0L)
                    Text(
                      text = balanceText,
                      style =
                        MaterialTheme.typography.headlineMedium.copy(
                          fontWeight = FontWeight.Bold
                        ),
                      color = Color.White,
                    )
                  }
                }
              }

              AsteraLogoBadge(size = 64.dp)
            }
          }
        }
      }

      // Quick Actions Row (Transfer, Top Up, Scan QR, Riwayat)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          QuickActionItem(
            icon = Icons.AutoMirrored.Outlined.Send,
            label = "Transfer",
            onClick = onOpenTransfer,
            modifier = Modifier.weight(1f).testTag("quick_action_transfer"),
          )
          QuickActionItem(
            icon = Icons.Rounded.Add,
            label = "Top Up",
            onClick = onOpenTopUp,
            modifier = Modifier.weight(1f).testTag("quick_action_topup"),
          )
          QuickActionItem(
            icon = Icons.Outlined.QrCodeScanner,
            label = "Scan QR",
            onClick = onOpenQris,
            modifier = Modifier.weight(1f).testTag("quick_action_scan_qr"),
          )
          QuickActionItem(
            icon = Icons.Outlined.History,
            label = "Riwayat",
            onClick = onOpenHistory,
            modifier = Modifier.weight(1f).testTag("quick_action_history"),
          )
        }
      }

      // Rekening Kamu (Dompet ASTERA)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Rekening Kamu",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
          WalletCard(
            accountNumber = profile?.accountNumber,
            balance = profile?.balance,
            hideBalance = hideBalance,
          )
        }
      }

      // Monthly Budget Card
      item {
        BudgetProgressCard(
          budgetState = budgetState,
          onEditLimit = { showBudgetDialog = true },
        )
      }

      // Transaksi Terakhir Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Transaksi Terakhir",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenHistory)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("see_all_transactions"),
          ) {
            Text(
              text = "Lihat Semua",
              style = MaterialTheme.typography.labelLarge,
              color = AsteraAmber,
            )
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = null,
              tint = AsteraAmber,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      // Latest 3 Transactions
      when (transactionsState) {
        UiState.Loading -> {
          item {
            AsteraCard {
              Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator(color = AsteraSkyBlue, modifier = Modifier.size(28.dp))
              }
            }
          }
        }
        is UiState.Error -> {
          item {
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
          val latestThree = transactionsState.data.take(3)
          if (latestThree.isEmpty()) {
            item {
              AsteraCard {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = AsteraTextSecondary,
                    modifier = Modifier.size(36.dp),
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Belum ada transaksi",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Mulai simulasi transfer atau top up untuk melihat aktivitas saldo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AsteraTextSecondary,
                  )
                }
              }
            }
          } else {
            items(latestThree, key = { it.id }) { tx ->
              TransactionRowCard(
                tx = tx,
                currentUserId = currentUserId,
                onClick = { onSelectTransaction(tx) },
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }

  if (showBudgetDialog) {
    val currentLimit = (budgetState as? UiState.Success)?.data?.monthlyLimit ?: 1_000_000L
    EditBudgetDialog(
      initialLimit = currentLimit,
      onDismiss = { showBudgetDialog = false },
      onSave = { newLimit ->
        onUpdateMonthlyLimit(newLimit) { showBudgetDialog = false }
      },
    )
  }
}

@Composable
private fun QuickActionItem(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .padding(vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      shape = CircleShape,
      color = AsteraCard,
      border = BorderStroke(1.5.dp, AsteraBrightBlue.copy(alpha = 0.8f)),
      modifier = Modifier.size(54.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = AsteraSkyBlue,
          modifier = Modifier.size(24.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = Color.White,
    )
  }
}

@Composable
private fun BudgetProgressCard(
  budgetState: UiState<MonthlyBudget?>,
  onEditLimit: () -> Unit,
) {
  AsteraCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "Anggaran Bulan Ini",
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
        )
        Text(
          text = "Pantau batas pengeluaran simulasi",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
        )
      }
      Surface(
        shape = CircleShape,
        color = AsteraPrimary.copy(alpha = 0.5f),
        modifier =
          Modifier
            .clip(CircleShape)
            .clickable(onClick = onEditLimit)
            .minimumInteractiveComponentSize()
            .testTag("edit_budget_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Ubah batas anggaran",
            tint = AsteraAmber,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Atur",
            style = MaterialTheme.typography.labelMedium,
            color = AsteraAmber,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    when (budgetState) {
      UiState.Loading -> {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
          color = AsteraSkyBlue,
          trackColor = Color(0xFF162856),
        )
      }
      is UiState.Error -> {
        Text(
          text = budgetState.message,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFFCA5A5),
        )
      }
      is UiState.Success -> {
        val budget = budgetState.data
        val limit = budget?.monthlyLimit ?: 1_000_000L
        val spent = budget?.spentThisMonth ?: 0L
        val ratio = if (limit > 0L) (spent.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
        val isWarning = limit > 0L && (spent.toDouble() / limit.toDouble()) >= 0.8
        val barColor = if (isWarning) AsteraAmber else AsteraSkyBlue

        Text(
          text = "${formatRupiah(spent)} dari ${formatRupiah(limit)}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = if (isWarning) AsteraAmber else Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { ratio },
          modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
          color = barColor,
          trackColor = Color(0xFF162856),
        )
        if (isWarning) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              if (spent > limit) "Pengeluaran sudah melewati batas anggaran bulan ini"
              else "Penggunaan anggaran mencapai 80% atau lebih",
            style = MaterialTheme.typography.bodySmall,
            color = AsteraAmber,
          )
        }
      }
    }
  }
}
