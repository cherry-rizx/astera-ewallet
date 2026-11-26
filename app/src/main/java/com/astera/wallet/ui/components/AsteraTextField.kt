package com.astera.wallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.astera.wallet.ui.theme.AsteraAmber
import com.astera.wallet.ui.theme.AsteraCardBorder
import com.astera.wallet.ui.theme.AsteraFieldBg
import com.astera.wallet.ui.theme.AsteraSkyBlue
import com.astera.wallet.ui.theme.AsteraTextPrimary
import com.astera.wallet.ui.theme.AsteraTextSecondary

@Composable
fun AsteraTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  modifier: Modifier = Modifier,
  errorText: String? = null,
  helperText: String? = null,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  prefix: (@Composable () -> Unit)? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  singleLine: Boolean = true,
  readOnly: Boolean = false,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = AsteraTextPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(text = placeholder, color = AsteraTextSecondary.copy(alpha = 0.75f))
      },
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      prefix = prefix,
      isError = errorText != null,
      singleLine = singleLine,
      readOnly = readOnly,
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      shape = RoundedCornerShape(14.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = AsteraFieldBg,
          unfocusedContainerColor = AsteraFieldBg,
          disabledContainerColor = AsteraFieldBg,
          errorContainerColor = AsteraFieldBg,
          focusedBorderColor = AsteraSkyBlue,
          unfocusedBorderColor = AsteraCardBorder.copy(alpha = 0.6f),
          errorBorderColor = MaterialTheme.colorScheme.error,
          focusedTextColor = AsteraTextPrimary,
          unfocusedTextColor = AsteraTextPrimary,
          cursorColor = AsteraAmber,
        ),
      modifier = Modifier.fillMaxWidth(),
    )
    if (errorText != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    } else if (helperText != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = helperText,
        style = MaterialTheme.typography.bodySmall,
        color = AsteraTextSecondary,
      )
    }
  }
}
