package com.astera.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraPrimary

@Composable
fun AsteraPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
) {
  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    shape = CircleShape,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = AsteraPrimary,
        contentColor = Color.White,
        disabledContainerColor = AsteraPrimary.copy(alpha = 0.45f),
        disabledContentColor = Color.White.copy(alpha = 0.6f),
      ),
    modifier = modifier.fillMaxWidth().height(52.dp).minimumInteractiveComponentSize(),
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        color = Color.White,
        strokeWidth = 2.5.dp,
      )
    } else {
      Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      )
    }
  }
}

@Composable
fun AsteraOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  borderColor: Color = AsteraPrimary,
  contentColor: Color = Color.White,
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    border = BorderStroke(1.5.dp, borderColor),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    modifier = modifier.fillMaxWidth().height(52.dp).minimumInteractiveComponentSize(),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
  }
}
