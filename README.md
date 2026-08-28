# NeonRoutine ⚡
> Modern, High-Performance 144Hz Habit & Routine Tracker for Android

<p align="center">
  <a href="https://github.com/ramaneon/routine/releases/latest">
    <img src="https://img.shields.io/badge/Download-Release%20APK-brightgreen?style=for-the-badge&logo=android" alt="Download Release APK" />
  </a>
  <img src="https://img.shields.io/badge/Kotlin-100%25-blue?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-144Hz%20Smooth-blueviolet?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-MIT-orange?style=for-the-badge" alt="MIT License" />
</p>

---

## 📥 Download & Install APK

Directly install the latest production-ready build onto any Android device (Android 8.0+):

👉 **[Download Latest NeonRoutine Release APK](https://github.com/ramaneon/routine/releases/latest/download/NeonRoutine-release.apk)**  
*(Also available under the [Releases](https://github.com/ramaneon/routine/releases) tab)*

---

## 📱 Screenshots & UI Showcase

<p align="center">
  <img src="images/home_panel.jpg" width="31%" alt="Home Panel" />
  <img src="images/month_panel.jpg" width="31%" alt="Month Calendar & Heatmap" />
  <img src="images/stats_panel.jpg" width="31%" alt="Analytics & Streaks" />
</p>

<p align="center">
  <img src="images/settings_themes_backup.jpg" width="48%" alt="Settings, Themes & Full Backup" />
  <img src="images/face_timelapse.jpg" width="48%" alt="Face Stencil Camera Memory" />
</p>

---

## ✨ Key Features

- ⚡ **Zero-Latency 144Hz Navigation**: Instantaneous multi-tab transitions with touch isolation and zero UI-thread stutter.
- 🎯 **Advanced Habit & Task Tracking**: Daily, Weekly, Monthly, and custom interval habits with streak tracking, point scoring, and quick cycle completion.
- 📅 **Interactive Month Heatmap**: Deep historical logging, full month calendar grid, daily completion badges, and past record editing.
- 📊 **Deep Analytics & KPIs**: Real-time completion rates, streak metrics, weekly performance charts, and discipline analytics.
- 📸 **Face Stencil Photographic Memory**: Embedded camera with facial guideline overlay to log consistent daily progress snapshots.
- 🎨 **Adaptive Glassmorphic & Neon Themes**: Switch between **Aero Glass**, **Crystal Aurora**, **Frosted Midnight**, **Neon Glow**, and **Soft Pastel** with auto-adjusting text contrast.
- 📦 **Complete Full-Archive Backup & Restore**: Export all habits, history, notes, and photos into a compressed `.neonbak` archive for seamless device-to-device migration.
- 📴 **100% Offline & Private**: Local-first Room SQLite persistence with zero telemetry or forced cloud dependencies.

---

## 🛠️ Tech Stack & Architecture

- **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3
- **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite)
- **Architecture**: MVVM with Kotlin Coroutines & Reactive `StateFlow` pipelines
- **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) Launcher Widgets
- **Serialization**: Kotlinx Serialization
- **Image Pipeline**: Coil Image Loader + Local Encrypted App Storage

---

## 🚀 Build from Source

1. Clone repository:
   ```bash
   git clone https://github.com/ramaneon/routine.git
   ```
2. Open in **Android Studio (Ladybug or newer)**.
3. Build & assemble debug/release APK:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 📄 License
MIT License. Created for the modern high-performance Android ecosystem.
