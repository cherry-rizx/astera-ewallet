# Menyiapkan Firebase ASTERA

Konfigurasi lokal repo sudah diarahkan ke project `astera-3d3b0` dan database
Firestore `(default)`. Perubahan lokal ini belum membuat atau mengubah layanan
di Firebase Console.

## Yang perlu dilakukan di Firebase Console

1. Buka project `astera-3d3b0`.
2. Daftarkan aplikasi Android dengan package name `com.astera.wallet`, lalu
   unduh `google-services.json` dan ganti `app/google-services.json` yang ada.
3. Di Authentication, aktifkan provider **Email/Password**.
4. Buat database Firestore dengan ID `(default)` jika belum tersedia.
5. Aktifkan Firebase Storage dan pastikan bucket project tersedia.
6. Jika ingin memakai notifikasi server, aktifkan Cloud Functions dan
   gunakan paket **Blaze**. Trigger Functions membaca transaksi dari database
   `(default)`.

## Deploy rules dan Cloud Functions

Setelah Firebase Console siap dan Firebase CLI sudah login, jalankan dari
folder `aplikasi-android`:

```sh
firebase deploy --only firestore:rules,storage --project astera-3d3b0
firebase deploy --only functions --project astera-3d3b0
```

Functions membutuhkan Node.js 20 dan penagihan Blaze. Deploy Rules/Functions
belum dijalankan dari repo ini.
