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

1. Install [Shizuku](https://github.com/RikkaApps/Shizuku) dan [LSPatch](https://github.com/LSPosed/LSPatch) (jika belum).
2. Download `MLBB-144Hz-Infinix.apk` dari [Releases](../../releases) dan install.
3. LSPatch → **Manage** → patch `com.mobile.legends` dengan mode **Local/manager**, centang modul **MLBB 144Hz Infinix**.
4. Force stop MLBB, buka lagi. Di **Settings → Basic → Frame Rate** pilih tertinggi.

Dengan LSPosed (root): aktifkan modul, scope `com.mobile.legends`, force stop MLBB.

> Jika sebelumnya memakai Game Unlocker lain yang men-spoof MLBB, nonaktifkan untuk MLBB agar tidak bentrok.

## Verifikasi

```sh
adb logcat -s LSPosed-Bridge | grep MLBB
# MLBB 144Hz scene sent ok=true state=1 hz=144
adb shell dumpsys SurfaceFlinger | grep -i "refresh"
```

Catatan: XOS turun ke 60Hz saat layar diam (idle); sentuh layar untuk kembali 144Hz. Ini perilaku normal sistem.

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
