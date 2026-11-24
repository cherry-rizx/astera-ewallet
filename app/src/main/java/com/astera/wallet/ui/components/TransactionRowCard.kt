package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraAmberSoft
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraGreen
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.util.formatTimeShort

@Composable
fun TransactionRowCard(
  tx: TransactionRecord,
  currentUserId: String,
  onClick: () -> Unit,
) {
  val isIncoming = tx.type == "topup" || tx.receiverId == currentUserId
  val title =
    when {
      tx.type == "topup" -> "Top Up Simulasi"
      isIncoming -> "Transfer dari ${tx.senderName}"
      else -> "Transfer ke ${tx.receiverName}"
    }
  val amountPrefix = if (isIncoming) "+" else "-"
  val amountColor = if (isIncoming) AsteraGreen else Color.White

  AsteraCard(
    onClick = onClick,
    contentPadding = PaddingValues(14.dp),
    modifier = Modifier.testTag("transaction_item_${tx.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color = AsteraPrimary.copy(alpha = 0.45f),
        modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector =
              if (isIncoming) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
            contentDescription = null,
            tint = if (isIncoming) AsteraGreen else AsteraSkyBlue,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formatTimeShort(tx.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = AsteraTextSecondary,
          )
          if (tx.outOfBudget) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = CircleShape,
              color = AsteraAmberSoft,
              border = BorderStroke(0.8.dp, AsteraAmber.copy(alpha = 0.6f)),
            ) {
              Text(
                text = "Di luar anggaran",
                style = MaterialTheme.typography.labelSmall,
                color = AsteraAmber,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "$amountPrefix ${formatRupiah(tx.amount)}",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = amountColor,
      )
    }
  }
}
