# MLBB 144Hz for Infinix (XOS)

Modul Xposed (LSPosed / LSPatch) yang membuka **144Hz di Mobile Legends: Bang Bang** pada HP Infinix ber-XOS yang layarnya 144Hz tapi dibatasi 120Hz oleh sistem.

## Cara kerja

1. **Spoof device** — `Build.BRAND/MANUFACTURER/MODEL/DEVICE/PRODUCT` di proses MLBB diganti ke iQOO 15R (`I2508`), sehingga MLBB menampilkan opsi frame rate tertinggi.
2. **Bypass cap 120Hz XOS** — XOS (`TranBaseRefreshRatePolicy`) membatasi game yang tidak ada di whitelist ke 120Hz. Saat activity MLBB `onResume`, modul mengirim event `game_scene` (`game_refresh_rate=144`) ke `IDisplayManager` (binder code 58, `updateRefreshRateForScene`), diulang tiap 5 detik. Saat `onPause`, dikirim `game_state=0` untuk kembali ke 120Hz.
3. **WiFi low-latency** — `WifiLock` `WIFI_MODE_FULL_LOW_LATENCY` aktif selama MLBB di depan, mematikan power-save radio WiFi (mengurangi lonjakan ping).

## Kompatibilitas

| Status | Device |
|---|---|
| ✅ Teruji | Infinix NOTE 50 Pro (X6855), Android 16, XOS, LSPatch (manager mode) |
| ❓ Belum teruji | Infinix/Tecno/itel lain dengan layar 144Hz — silakan lapor via Issues |

Binder code `58` spesifik untuk build XOS di atas. Di ROM lain transaksi gagal secara diam-diam (game tetap jalan, hanya tanpa 144Hz). Cek log untuk memastikan.

## Instalasi (tanpa root, LSPatch)

> ⚠️ Patch MLBB mengganti tanda tangan APK, jadi MLBB asli harus di-uninstall. **Ikat akun (Moonton/Google/Facebook) dulu**, data game (~11 GB) akan diunduh ulang.

### 1. Aktifkan Developer Options & Wireless debugging
1. **Setelan → Tentang ponsel** → ketuk **Nomor versi (Build number)** 7x.
2. **Setelan → Sistem → Opsi pengembang** → aktifkan **Wireless debugging** (HP harus terhubung WiFi).

### 2. Install & jalankan Shizuku
1. Install [Shizuku](https://github.com/RikkaApps/Shizuku/releases) (atau dari Play Store).
2. Buka Shizuku → **Pairing** → buka Wireless debugging → **Pasangkan dengan kode** → masukkan kode di notifikasi Shizuku.
3. Shizuku → **Start**. Status harus *Shizuku is running*.

### 3. Install LSPatch
1. Download APK manager dari [LSPatch Releases](https://github.com/LSPosed/LSPatch/releases) dan install.
2. Buka LSPatch, beri izin Shizuku saat diminta.

### 4. Install modul
1. Download `MLBB-144Hz-Infinix-vX.Y.Z.apk` dari [Releases](../../releases) dan install.

### 5. Patch MLBB
1. LSPatch → tab **Manage** → **+** → **Select apps installed** → pilih **Mobile Legends: Bang Bang**.
2. Mode patch: **Local**. Biarkan opsi lain default → **Start Patch**.
3. Setelah selesai → **Install**. Jika muncul konflik tanda tangan, setujui uninstall MLBB asli.

### 6. Aktifkan modul untuk MLBB
1. LSPatch → **Manage** → ketuk **Mobile Legends** → **Module scope**.
2. Centang **MLBB 144Hz Infinix** (matikan Game Unlocker/modul spoof lain jika ada).

### 7. Main
1. Buka MLBB, login, unduh resource.
2. **Settings → Basic → Frame Rate** → pilih tertinggi.

Selesai. Shizuku **tidak perlu** dijalankan tiap main; cukup **LSPatch tetap terinstall** (mode Local membaca modul dari LSPatch).

### Update MLBB
Update Play Store gagal karena tanda tangan berbeda. Download APK MLBB versi baru (mis. dari APKMirror, split APK didukung LSPatch), lalu ulangi langkah 5. Tidak perlu uninstall lagi selama tanda tangan patch sama.

### Root (LSPosed)
Install modul → LSPosed → aktifkan modul → scope `com.mobile.legends` → force stop MLBB. Tanpa patch/uninstall.
## Verifikasi

```sh
adb logcat -s LSPosed-Bridge | grep MLBB
# MLBB 144Hz scene sent ok=true state=1 hz=144
adb shell dumpsys SurfaceFlinger | grep -i "refresh"
```

Catatan: XOS turun ke 60Hz saat layar diam (idle); sentuh layar untuk kembali 144Hz. Ini perilaku normal sistem.

### Masih 120Hz?
Cleaner/"hapus cache sistem" XOS bisa mereset batas refresh sistem ke 120. Cek & kembalikan:
- **Setelan → Layar → Kecepatan refresh** → pilih **144Hz**/tertinggi, atau via ADB:
```sh
adb shell settings get system peak_refresh_rate   # harus 144.0
adb shell settings put system tran_refresh_mode 144
adb shell settings put system last_tran_refresh_mode_in_refresh_setting 144
adb shell settings put system peak_refresh_rate 144.0
```
`tran_refresh_mode` wajib 144 — XOS menulis ulang `peak_refresh_rate` dari nilai itu saat game dibuka.
- Jangan pilih mode **Otomatis** (`tran_refresh_mode=10000`), itu membatasi 120.
- MLBB crash "Failed to get manager binder" setelah cleaner? Buka app LSPatch dulu, lalu izinkan autostart/background LSPatch di Phone Master.

## Build dari source

Butuh JDK 17+ dan Android SDK (build-tools 34+, `platforms;android-34`).

```powershell
.\build.ps1
```

Default ditandatangani dengan debug keystore. Untuk rilis, set `KEYSTORE`, `KS_PASS`, `KEY_ALIAS`.

## Disclaimer

- Spoof device memodifikasi informasi yang dibaca game. **Risiko akun (ban) ditanggung sendiri.** Proyek ini tidak berafiliasi dengan Moonton maupun Infinix/Transsion.
- 144Hz meningkatkan panas dan konsumsi baterai. Throttling suhu (SKIN ≥ 47°C) bisa menurunkan FPS — hindari main sambil dicas.

## Kredit

- Ide spoof device: [rushiranpise/Game-Unlocker](https://github.com/rushiranpise/Game-Unlocker) (MIT).
- API Xposed: [rovo89/XposedBridge](https://github.com/rovo89/XposedBridge). Folder `stubs/` hanya stub compile-time, tidak ikut ke APK.

## Lisensi

MIT — lihat [LICENSE](LICENSE).
