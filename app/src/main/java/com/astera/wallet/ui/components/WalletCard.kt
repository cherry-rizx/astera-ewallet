package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.util.formatRupiah
import com.astera.wallet.util.maskedWalletNumber
import com.astera.wallet.util.formatAccountNumber

/**
 * Kartu "Dompet ASTERA" sesuai desain: logo, nomor dompet tersamar, dan saldo.
 *
 * @param compact true untuk versi satu baris ("•••• 4821 · Rp 10.000.000") seperti di layar Akun.
 */
@Composable
fun WalletCard(
  accountNumber: String?,
  balance: Long?,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
  hideBalance: Boolean = false,
) {
  val balanceText = when {
    balance == null -> "Memuat..."
    hideBalance -> "Rp ••••••••"
    else -> formatRupiah(balance)
  }
  Surface(
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, AsteraSkyBlue.copy(alpha = 0.45f)),
    color = Color.Transparent,
    modifier = modifier.fillMaxWidth().testTag("wallet_card"),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            if (compact) Brush.linearGradient(listOf(Color(0xFF2356B8), Color(0xFF193C8A)))
            else Brush.linearGradient(listOf(Color(0xFF22386A), Color(0xFF1B2F5E)))
          )
          .padding(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AsteraLogoBadge(size = 48.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Dompet ASTERA",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(4.dp))
          if (compact) {
            Text(
              text = "${formatAccountNumber(accountNumber.orEmpty()).ifBlank { "Membuat nomor..." }} · $balanceText",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.85f),
            )
          } else {
            Text(
              text = maskedWalletNumber(accountNumber),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = balanceText,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
        }
        SimulasiBadge()
      }
    }
  }
}
