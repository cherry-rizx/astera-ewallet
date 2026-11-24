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

## Struktur proyek

```
astera/
├── app/                         Modul Android
│   └── src/
│       ├── main/java/com/astera/wallet/
│       │   ├── MainActivity.kt          Titik masuk Activity
│       │   ├── navigation/              Rute dan alur navigasi
│       │   │   ├── AsteraRoutes.kt          Daftar nama rute
│       │   │   ├── AsteraApp.kt             Gerbang login (splash -> auth / main)
│       │   │   ├── UnauthenticatedAppFlow.kt  Welcome, login, registrasi
│       │   │   └── AuthenticatedAppFlow.kt    Kunci aplikasi + semua layar utama
│       │   ├── model/Models.kt          Data class Firestore
│       │   ├── service/                 Akses Firebase (Auth, Wallet, Transaction,
│       │   │                            Bank, Budget, Profile)
│       │   ├── notification/            FCM dan notifikasi lokal (OTP simulasi)
│       │   ├── nfc/                     Tap HP via NFC (Host Card Emulation + reader mode)
│       │   ├── viewmodel/               AuthViewModel, MainViewModel
│       │   ├── util/                    Format rupiah/tanggal, split multi-bank,
│       │   │                            hash PIN, QR, daftar notifikasi, ringkasan
│       │   │                            bulanan, pesan error Firestore
│       │   └── ui/
│       │       ├── theme/               Warna, tipografi, tema
│       │       ├── components/          Komponen bersama (tombol, kartu, field,
│       │       │                        bottom bar + tombol QRIS, kartu dompet,
│       │       │                        keypad, scanner kamera QR, dialog budget)
│       │       └── screens/             Satu folder per fitur, satu file per layar
│       │           ├── auth/                Splash, Welcome, Login, Register, OTP,
│       │           │                        AppUnlock, AccountBlocked
│       │           ├── home/                Beranda
│       │           ├── qris/                QR Astera (scan / tunjukkan QR, NFC)
│       │           ├── notifications/       Daftar notifikasi
│       │           ├── summary/             Ringkasan pengeluaran bulanan
│       │           ├── transfer/            Transfer + scan QR
│       │           ├── topup/               Top up multi-bank
│       │           ├── transaction/         Konfirmasi, PIN, berhasil, detail transaksi
│       │           ├── history/             Riwayat transaksi
│       │           └── account/             Akun, profil, keamanan, notifikasi
│       ├── main/res/                    Resource Android
│       └── test/java/com/astera/wallet/ Unit test (JUnit) + test Firestore emulator
├── functions/                   Cloud Functions (notifikasi FCM)
├── firestore.rules              Security Rules Firestore
├── firestore.test.js            Test rules untuk emulator
├── storage.rules                Security Rules Storage
├── firebase.json, .firebaserc   Konfigurasi Firebase CLI
└── docs/
    ├── screenshots/             Desain awal dari AI Studio (bukan hasil build)
    └── firebase-blueprint.json  Blueprint data dari AI Studio
```

Nama package Kotlin sama dengan `applicationId`, yaitu `com.astera.wallet`.

## Fitur

- Registrasi dan login email/kata sandi, opsi "Ingat saya", dan reset kata sandi.
- Verifikasi dua langkah dengan OTP simulasi (kode 6 digit, berlaku 5 menit,
  maksimal 3 percobaan, kirim ulang setelah 30 detik). Tidak ada SMS sungguhan:
  kode muncul sebagai notifikasi lokal dan kartu "SMS Simulasi".
- Dompet simulasi, transfer pengguna, top up multi-bank, dan riwayat.
- QR Astera (tombol QRIS di tengah bottom nav): scan QR, ambil QR dari galeri,
  senter, dan tunjukkan QR milik sendiri.
- Tap HP via NFC untuk mengisi penerima: HP penerima membuka QRIS > Tunjukkan QR
  dengan "Terima via NFC" aktif, HP pengirim ditempelkan dari layar Transfer atau
  QRIS > Scan QR. Butuh NFC + HCE di kedua HP.
- Layar Notifikasi (Semua / Transaksi / Promo / Info) dengan penanda belum dibaca
  di ikon lonceng Beranda.
- Ringkasan pengeluaran: grafik 6 bulan, transfer masuk dan top up, tabel per bulan,
  dan penerima terbanyak.
- Konfirmasi transaksi dengan PIN, blokir setelah tiga PIN salah, dan
  pembukaan aplikasi dengan biometrik atau PIN.
- Batas transfer harian Rp5.000.000.
- Foto profil Firebase Storage dan notifikasi transfer melalui FCM.

## Menjalankan test

```sh
gradle :app:testDebugUnitTest --tests 'com.astera.wallet.util.*' --tests 'com.astera.wallet.nfc.*'
```

Test `AsteraRepositoryRuleTest` membutuhkan Firestore emulator dan Java 21
(Robolectric untuk SDK 36).
