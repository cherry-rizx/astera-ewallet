package com.astera.wallet.service

import android.content.Context
import com.astera.wallet.notification.LocalNotifier
import com.astera.wallet.util.PinHasher
import java.security.SecureRandom

sealed interface OtpVerifyResult {
  data object Valid : OtpVerifyResult

  data class Wrong(val remainingAttempts: Int) : OtpVerifyResult

  data object Expired : OtpVerifyResult

  data object TooManyAttempts : OtpVerifyResult
}

/**
 * Verifikasi dua langkah (2FA) dengan OTP simulasi.
 *
 * Tidak ada SMS sungguhan: kode 6 digit dibuat di perangkat, disimpan dalam bentuk hash, lalu
 * dikirim hanya lewat notifikasi lokal. Status "menunggu OTP"
 * disimpan di SharedPreferences supaya tetap diminta walau aplikasi ditutup sebelum verifikasi.
 */
class OtpService(private val appContext: Context) {
  private val prefs = appContext.getSharedPreferences("astera_otp", Context.MODE_PRIVATE)
  private val random = SecureRandom()

  /** Dipanggil tepat sebelum login email/kata sandi: login berikutnya wajib OTP. */
  fun markLoginPending() {
    prefs.edit().putBoolean(KEY_PENDING, true).apply()
  }

  fun isLoginPending(): Boolean = prefs.getBoolean(KEY_PENDING, false)

  fun hasActiveCode(nowMs: Long = System.currentTimeMillis()): Boolean =
    prefs.getString(KEY_HASH, null) != null && nowMs < prefs.getLong(KEY_EXPIRES_AT, 0L)

  fun lastIssuedAtMs(): Long = prefs.getLong(KEY_ISSUED_AT, 0L)

  /**
   * Membuat kode baru, menyimpan hash-nya, lalu mengirimnya lewat notifikasi lokal.
   *
   * @return true jika notifikasi berhasil tampil (izin notifikasi diberikan).
   */
  fun issueCode(nowMs: Long = System.currentTimeMillis()): Boolean {
    val code = (random.nextInt(900_000) + 100_000).toString()
    prefs
      .edit()
      .putString(KEY_HASH, PinHasher.hashPin(code))
      .putLong(KEY_ISSUED_AT, nowMs)
      .putLong(KEY_EXPIRES_AT, nowMs + CODE_VALIDITY_MS)
      .putInt(KEY_ATTEMPTS, 0)
      .apply()
    return LocalNotifier.showSecurityNotification(
      appContext,
      NOTIFICATION_ID,
      "ASTERA",
      "$code berikut kode OTP anda",
    )
  }

  fun verify(code: String, nowMs: Long = System.currentTimeMillis()): OtpVerifyResult {
    val hash = prefs.getString(KEY_HASH, null) ?: return OtpVerifyResult.Expired
    if (nowMs >= prefs.getLong(KEY_EXPIRES_AT, 0L)) return OtpVerifyResult.Expired
    val attempts = prefs.getInt(KEY_ATTEMPTS, 0)
    if (attempts >= MAX_ATTEMPTS) return OtpVerifyResult.TooManyAttempts
    if (PinHasher.verifyPin(code, hash)) return OtpVerifyResult.Valid
    val newAttempts = attempts + 1
    prefs.edit().putInt(KEY_ATTEMPTS, newAttempts).apply()
    return if (newAttempts >= MAX_ATTEMPTS) OtpVerifyResult.TooManyAttempts
    else OtpVerifyResult.Wrong(MAX_ATTEMPTS - newAttempts)
  }

  /** Menghapus status OTP (setelah berhasil, logout, atau login gagal). */
  fun clear() {
    prefs.edit().clear().apply()
  }

  companion object {
    const val CODE_VALIDITY_MS = 5 * 60 * 1000L
    const val RESEND_COOLDOWN_MS = 30 * 1000L
    const val MAX_ATTEMPTS = 3
    private const val NOTIFICATION_ID = 4821
    private const val KEY_PENDING = "login_pending"
    private const val KEY_HASH = "code_hash"
    private const val KEY_ISSUED_AT = "issued_at"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_ATTEMPTS = "attempts"
  }
}
