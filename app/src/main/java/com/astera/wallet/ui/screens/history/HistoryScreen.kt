package com.astera.wallet.ui.screens.history

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.TransactionRowCard
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatDateGroupLabel
import com.astera.wallet.viewmodel.UiState

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
