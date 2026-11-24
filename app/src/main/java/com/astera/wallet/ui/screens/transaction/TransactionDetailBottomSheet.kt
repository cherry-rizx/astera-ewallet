package com.astera.wallet.ui.screens.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.DetailRow
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraDivider
import com.astera.wallet.util.formatFullTimestamp
import com.astera.wallet.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
  transaction: TransactionRecord,
  currentUserId: String,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = AsteraCard,
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Detail Transaksi",
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        SimulasiBadge()
      }

      HorizontalDivider(color = AsteraDivider)

      DetailRow(label = "ID Transaksi", value = transaction.id)
      DetailRow(
        label = "Jenis",
        value = if (transaction.type == "topup") "Top Up Simulasi" else "Transfer Saldo",
      )
      DetailRow(label = "Pengirim", value = transaction.senderName)
      DetailRow(label = "Penerima", value = transaction.receiverName)
      DetailRow(label = "Nominal", value = formatRupiah(transaction.amount))
      DetailRow(label = "Biaya", value = "Rp 0")
      DetailRow(label = "Waktu", value = formatFullTimestamp(transaction.createdAt))
      if (transaction.note.isNotBlank()) {
        DetailRow(label = "Catatan", value = transaction.note)
      }
      if (transaction.outOfBudget) {
        DetailRow(label = "Status Anggaran", value = "Di luar anggaran")
      }
      if (transaction.splitDetail.isNotEmpty()) {
        HorizontalDivider(color = AsteraDivider)
        Text(
          text = "Rincian Sumber Bank (Multi-Bank Split)",
          style = MaterialTheme.typography.labelLarge,
          color = AsteraAmber,
        )
        transaction.splitDetail.forEach { split ->
          DetailRow(label = split.bankName, value = formatRupiah(split.amount))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      AsteraPrimaryButton(text = "Tutup", onClick = onDismiss)
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
