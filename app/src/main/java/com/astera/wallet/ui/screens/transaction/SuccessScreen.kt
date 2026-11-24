package com.astera.wallet.ui.screens.transaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.model.TransactionRecord
import com.astera.wallet.ui.components.AsteraBackground
import com.astera.wallet.ui.components.AsteraCard
import com.astera.wallet.ui.components.AsteraOutlinedButton
import com.astera.wallet.ui.components.AsteraPrimaryButton
import com.astera.wallet.ui.components.SimulasiBadge
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraBrightBlue
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraPrimary
import com.astera.wallet.ui.theme.AsteraTextSecondary
import com.astera.wallet.util.formatFullTimestamp
import com.astera.wallet.util.formatRupiah

@Composable
fun SuccessScreen(
  transaction: TransactionRecord?,
  onBackHome: () -> Unit,
) {
  var showDetailSheet by remember { mutableStateOf(false) }
  val scale = remember { Animatable(0.3f) }

  LaunchedEffect(Unit) {
    scale.animateTo(
      targetValue = 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
  }

  AsteraBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .widthIn(max = 600.dp)
          .align(Alignment.Center)
          .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        // Animated checkmark in amber & royal blue
        Box(
          modifier =
            Modifier
              .size(96.dp)
              .scale(scale.value)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(AsteraAmber, AsteraPrimary)))
              .border(3.dp, AsteraAmber.copy(alpha = 0.75f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Berhasil",
            tint = Color.White,
            modifier = Modifier.size(52.dp),
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
          text = if (transaction?.type == "topup") "Top Up Berhasil!" else "Transfer Berhasil!",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = formatRupiah(transaction?.amount ?: 0L),
          style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
          color = AsteraAmber,
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (transaction != null) {
          AsteraCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (transaction.type == "topup") "Tujuan" else "Penerima",
                style = MaterialTheme.typography.bodySmall,
                color = AsteraTextSecondary,
              )
              SimulasiBadge()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = transaction.receiverName,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "ID Transaksi: ${transaction.id}",
              style = MaterialTheme.typography.bodySmall,
              color = AsteraTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = formatFullTimestamp(transaction.createdAt),
              style = MaterialTheme.typography.bodySmall,
              color = AsteraTextSecondary,
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        AsteraOutlinedButton(
          text = "Lihat Detail",
          onClick = { showDetailSheet = true },
          borderColor = AsteraBrightBlue,
          modifier = Modifier.testTag("success_detail_button"),
        )
        AsteraPrimaryButton(
          text = "Kembali ke Beranda",
          onClick = onBackHome,
          modifier = Modifier.testTag("success_home_button"),
        )
      }
    }
  }

  if (showDetailSheet && transaction != null) {
    TransactionDetailBottomSheet(
      transaction = transaction,
      currentUserId = transaction.senderId,
      onDismiss = { showDetailSheet = false },
    )
  }
}
