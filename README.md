# Blood Donation App

A mobile application to connect blood donors and recipients in the community.

## Features
- Find blood donors nearby
- Post blood donation requests
- User profiles and verification
- Real-time notifications
- Blood bank integration

## Tech Stack
- **Language**: Kotlin
- **Framework**: Android SDK 34
- **Minimum SDK**: Android 7.0 (API 24)
- **Build Tool**: Gradle
- **Networking**: Retrofit + OkHttp
- **Database**: Room

## Setup Instructions

### Prerequisites
- Android Studio (Latest version)
- JDK 11 or higher
- Android SDK 34+

### Building the APK

1. **Clone the repository**
   ```bash
   git clone https://github.com/imsodrulislam20/BLOOD-DONATION-.git
   cd BLOOD-DONATION-
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the project folder

3. **Build the APK**
   - Go to `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Or use the terminal:
     ```bash
     ./gradlew assembleRelease
     ```
   - The APK will be generated in: `app/build/outputs/apk/release/`

4. **Debug APK**
   - For testing/debugging:
     ```bash
     ./gradlew assembleDebug
     ```
   - Debug APK location: `app/build/outputs/apk/debug/`

### Install on Device/Emulator

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Project Structure

```
BLOOD-DONATION-/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/blooddonation/app/
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       ├── values/
│   │   │       └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Development

To add new features:
1. Create a new branch from `android-app-setup`
2. Implement your feature
3. Test on emulator or device
4. Create a pull request

## License

MIT License - Feel free to use this project for your purposes.

## Support

For issues or questions, please open a GitHub issue.
