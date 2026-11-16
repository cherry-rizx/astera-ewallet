# ASTERA Digital Banking (Simulasi)

Aplikasi dompet digital simulasi untuk Android, dibuat dengan Kotlin, Jetpack
Compose, Firebase Authentication, Cloud Firestore, Firebase Storage, dan FCM.
Saldo dan transaksi di aplikasi ini hanya untuk simulasi.

## Menjalankan aplikasi

1. Buka folder ini di Android Studio.
2. Daftarkan aplikasi Android dengan package `com.astera.wallet` di Firebase
   project `astera-3d3b0`.
3. Unduh `google-services.json` dari Firebase Console dan simpan di
   `app/google-services.json`.
4. Aktifkan provider Email/Password, Firestore `(default)`, dan Firebase
   Storage di Firebase Console.
5. Sync Gradle, lalu jalankan konfigurasi `app` di emulator atau perangkat.

Petunjuk Firebase CLI, Rules, Storage, dan Cloud Functions ada di
[FIREBASE_SETUP.md](FIREBASE_SETUP.md).

## Fitur

- Registrasi dan login email/kata sandi serta reset kata sandi.
- Dompet simulasi, transfer pengguna, top up multi-bank, dan riwayat.
- Konfirmasi transaksi dengan PIN, blokir setelah tiga PIN salah, dan
  pembukaan aplikasi dengan biometrik atau PIN.
- Batas transfer harian Rp5.000.000.
- Foto profil Firebase Storage dan notifikasi transfer melalui FCM.
