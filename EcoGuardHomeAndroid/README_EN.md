# EcoGuard Home Android App

A complete native Android project for the EcoGuard Home graduation project. The app controls appliances through HC-05 Bluetooth and receives live fire/gas alert messages from Arduino Mega 2560.

## Hardware change required?

No hardware change is required. The Arduino firmware must be updated so that the existing HC-05 link becomes bidirectional: commands are sent from the phone to Arduino, and safety alerts are sent from Arduino to the phone.

## Main files

- `app/src/main/java/com/ecoguard/home/MainActivity.java`: Android app source code.
- `firmware/EcoGuard_Home_App_Ready_Firmware.ino`: Arduino Mega firmware compatible with the app.
- `docs/COMMAND_PROTOCOL.md`: Bluetooth commands and alert/status packets.

## Build and install

1. Open `EcoGuardHomeAndroid` in Android Studio.
2. Let Gradle sync.
3. Choose `Build > Build APK(s)`.
4. Install the generated APK on an Android phone.
5. Pair the phone with HC-05 from Android Bluetooth settings. Default PIN is commonly `1234` or `0000`.
6. Open the app, tap Refresh, select HC-05, and tap Connect.

## Notification behavior

The app shows high-priority Android notifications when Arduino sends `ALERT;TYPE=FIRE`, `ALERT;TYPE=GAS`, or `ALERT;TYPE=BOTH`. On Android 13+, notification permission must be granted.
