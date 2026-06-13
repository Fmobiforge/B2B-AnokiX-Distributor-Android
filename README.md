# anokiX Distributor Android App

Native Android app for the anokiX Distributor Portal, built in Java with Material Design. UI follows the mobile design mockup with purple branding (`#7c3aed`).

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

## Open in Android Studio

1. Open Android Studio
2. **File → Open** and select the `anokix_distributor_android` folder
3. Wait for Gradle sync to finish
4. Run on an emulator or device (API 24+)

## Build from command line

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## App structure

| Screen | Entry point |
|--------|-------------|
| Login | Launcher activity |
| Dashboard | Bottom nav → Home |
| Orders | Bottom nav → Orders |
| Inventory | Bottom nav → Inventory |
| Stores, Traders, Products, Promotions, Finances, Reports, Deliveries, Returns, Notifications, Settings | Bottom nav → More |

## Tech stack

- Java 17
- AndroidX AppCompat, Material Components, RecyclerView
- Mock data (no backend API yet)

## Package

`com.anokix.distributor`
