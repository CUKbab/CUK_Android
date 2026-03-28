# CUK밥 (CUK-Android)

[![Android CI](https://github.com/CUKbab/CUK_Android/actions/workflows/android-release.yml/badge.svg)](https://github.com/CUKbab/CUK_Android/actions/workflows/android-release.yml)
[![Version](https://img.shields.io/badge/version-2.1.0-blue.svg)](https://github.com/CUKbab/CUK_Android/releases)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-purple.svg)](https://kotlinlang.org/)

An unofficial Android client for the Catholic University of Korea (CUK) cafeteria menus. This application provides up-to-date menus for Buon Pranzo and Cafe Bona.

---

## Key Features

- **Cafeteria Menus:** Access daily menus for Buon Pranzo and Cafe Bona.
- **Smart Calendar:** Navigate through the menu archive using a date-based selector.
- **Home Screen Widgets:** View current menus via Android Glance-based widgets.
- **Meal Reminders:** Configurable notifications to alert users before meal service begins.
- **Material You:** Full integration with Dynamic Colors, custom accent colors, and adjustable typography.
- **Multilingual Support:** Localized for Korean, English, Japanese, and Simplified Chinese.
- **Dark Mode:** Native support for system-wide light and dark themes.
- **Feedback System:** Integrated reporting for menu errors and feature suggestions via Firebase.

---

## Tech Stack

- **Language:** Kotlin 2.3.10
- **UI Framework:** Jetpack Compose (BOM 2026.02.01)
- **Widgets:** Android Glance
- **Networking:** Retrofit 3.0.0 + Gson
- **Image Loading:** Coil 3.0.4
- **Backend:** Firebase (Authentication, Cloud Firestore)
- **Design:** Material 3 (Material You)
- **Architecture:** MVVM with Repository Pattern

---

## Requirements

- **Minimum SDK:** 30 (Android 11)
- **Target SDK:** 36 (Android 15+)
- **Build System:** Gradle 9.1+

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer.
- JDK 17.

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/CUKbab/CUK_Android.git
   ```
2. Open the project in Android Studio.
3. Place a valid `google-services.json` file in the `app/` directory (required for Firebase services).
4. Build and deploy the application to a compatible device or emulator.

---

## Project Structure

```text
app/src/main/java/com/cukbab/
├── data/           # Data layer: Repositories, Preferences, and Data Managers
├── ui/             # Presentation layer: Screens, Components, and Themes
│   ├── components/ # Reusable UI components
│   ├── screens/    # Screen-level composables
│   └── theme/      # Material 3 Theme and Styling
└── widget/         # Glance-based Home Screen Widget implementation
```

---

## License

This project is an unofficial client and is not affiliated with, authorized by, or endorsed by the Catholic University of Korea.

Distributed under the MIT License. See the LICENSE file for more information.

---

**CUK밥** - Simplifies access to campus dining information.
