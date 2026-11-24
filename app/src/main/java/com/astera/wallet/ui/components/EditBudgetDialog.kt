package com.astera.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCard
import com.astera.wallet.ui.theme.AsteraTextSecondary

@Composable
fun EditBudgetDialog(
  initialLimit: Long,
  onDismiss: () -> Unit,
  onSave: (Long) -> Unit,
) {
  var limitText by remember { mutableStateOf(initialLimit.toString()) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = AsteraCard,
    titleContentColor = Color.White,
    textContentColor = AsteraTextSecondary,
    title = { Text("Atur Anggaran Bulanan", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column {
        Text(
          text = "Tentukan batas pengeluaran simulasi per bulan.",
          style = MaterialTheme.typography.bodySmall,
          color = AsteraTextSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AsteraTextField(
          value = limitText,
          onValueChange = { input ->
            limitText = input.filter { it.isDigit() }.take(10)
            errorText = null
          },
          label = "Batas Bulanan (Rp)",
          placeholder = "1000000",
          prefix = { Text("Rp ", color = AsteraAmber) },
          errorText = errorText,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val parsed = limitText.toLongOrNull()
          if (parsed == null || parsed <= 0L) {
            errorText = "Masukkan nominal lebih dari Rp 0"
          } else {
            onSave(parsed)
          }
        }
      ) {
        Text("Simpan", color = AsteraAmber, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal", color = AsteraTextSecondary)
      }
    },
  )
}
