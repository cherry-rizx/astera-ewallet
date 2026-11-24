package com.astera.wallet.service

import android.content.Context

/** Menyimpan email untuk opsi "Ingat saya" di layar login. Kata sandi tidak pernah disimpan. */
class LoginPreferences(appContext: Context) {
  private val prefs = appContext.getSharedPreferences("astera_login", Context.MODE_PRIVATE)

  fun rememberedEmail(): String? = prefs.getString(KEY_EMAIL, null)

  fun save(email: String, remember: Boolean) {
    if (remember) prefs.edit().putString(KEY_EMAIL, email.trim()).apply()
    else prefs.edit().remove(KEY_EMAIL).apply()
  }

  private companion object {
    const val KEY_EMAIL = "remembered_email"
  }
}
