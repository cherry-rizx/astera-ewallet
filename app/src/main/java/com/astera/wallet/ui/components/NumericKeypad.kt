package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraBrightBlue
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextSecondary

/** Enam kotak kode (OTP) yang terisi sesuai jumlah digit yang sudah diketik. */
@Composable
fun CodeBoxes(
  code: String,
  length: Int = 6,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    repeat(length) { index ->
      val digit = code.getOrNull(index)
      val isCurrent = index == code.length
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = AsteraFieldBg,
        border =
          BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent || digit != null) AsteraSkyBlue else AsteraCardBorder.copy(alpha = 0.6f),
          ),
        modifier = Modifier.size(width = 44.dp, height = 52.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = digit?.toString() ?: "",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
          )
        }
      }
    }
  }
}

/** Papan angka 0-9 dengan tombol hapus, sama gayanya dengan papan PIN. */
@Composable
fun NumericKeypad(
  onDigit: (Char) -> Unit,
  onDelete: () -> Unit,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val rows = listOf("123", "456", "789")
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    rows.forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        row.forEach { digit -> KeypadButton(digit.toString(), enabled) { onDigit(digit) } }
      }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(28.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Spacer(modifier = Modifier.size(64.dp))
      KeypadButton("0", enabled) { onDigit('0') }
      Box(
        modifier =
          Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onDelete)
            .minimumInteractiveComponentSize()
            .testTag("keypad_delete"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Backspace,
          contentDescription = "Hapus",
          tint = AsteraSkyBlue,
        )
      }
    }
  }
}

@Composable
private fun KeypadButton(label: String, enabled: Boolean, onClick: () -> Unit) {
  Surface(
    shape = CircleShape,
    color = AsteraCard.copy(alpha = if (enabled) 0.9f else 0.4f),
    border = BorderStroke(1.dp, AsteraBrightBlue.copy(alpha = 0.35f)),
    modifier =
      Modifier
        .size(64.dp)
        .clip(CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("keypad_$label"),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall,
        color = if (enabled) Color.White else AsteraTextSecondary,
      )
    }
  }
}
