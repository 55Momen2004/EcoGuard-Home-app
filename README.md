EcoGuard Home Android App

This folder contains a complete Android application for controlling the EcoGuard Home system via HC-05 Bluetooth, along with an updated Arduino firmware that enables fire and gas alerts to be sent to the mobile app without requiring any hardware changes.

Can the required functionality be implemented?

Yes. The existing hardware can support the required features, provided that the updated firmware is uploaded to the Arduino Mega 2560.

The reason is that the mobile app cannot independently detect fire or gas conditions. The Arduino must detect the hazard and send an alert message to the app through the HC-05 Bluetooth module.

Key Files
app/ – Complete Android Studio project.
firmware/EcoGuard_Home_Bluetooth_Notification_Firmware.ino – Updated Arduino firmware.
App Name

EcoGuard Home

Main App Features
Classic Bluetooth connection using HC-05.
Select and connect to a paired HC-05 device.
Control 8 relay channels.
Enable or disable Smart Mode.
Turn off all connected loads using All Loads OFF.
Mute the buzzer directly from the app.
Real-time display of Fire, Gas, Alarm, and Smart Mode status.
Display Gas ADC and Flame ADC readings.
Receive phone notifications whenever an ALERT message is sent by the Arduino.
Vibrate the phone when a fire or gas warning is detected.
Building the APK
Open the EcoGuardHome_Android_Project folder in Android Studio.
Wait for Gradle Sync to complete.
Go to Build > Build APK(s).
Once the build finishes, the APK will be available at:

app/build/outputs/apk/debug/app-debug.apk

Transfer the APK to the Android phone and install it.
Using the App
Power on the Arduino Mega and the EcoGuard Home system.
Open the phone's Bluetooth settings and pair it with HC-05.
If a PIN is requested, try 1234 or 0000.
Launch the EcoGuard Home app.
Grant the required Bluetooth and notification permissions.
Tap Refresh Devices.
Select HC-05 and press Connect.
You can now control the system, and alerts will be displayed automatically when the Arduino sends them.
HC-05 and Arduino Mega Connection

The firmware is configured by default to use:

#define BT_SERIAL Serial1

This means the HC-05 should be connected to Serial1 on the Arduino Mega:

Mega TX1 (Pin 18) → HC-05 RX
Mega RX1 (Pin 19) → HC-05 TX
GND → Common ground

If the HC-05 is currently connected through Serial0, change the line to:

#define BT_SERIAL Serial

No hardware modification is needed as long as the current wiring matches the firmware configuration.

Communication Protocol
Commands Sent from the App to the Arduino
Command	Function
A	Enable Smart Mode
H	Disable Smart Mode
L / P	Relay 1 ON / OFF
R / T	Relay 2 ON / OFF + Servo action
Q / W	Relay 3 ON / OFF
G / F	Relay 4 ON / OFF
D / V	Relay 5 ON / OFF
E / N	Relay 6 ON / OFF
C / M	Relay 7 ON / OFF
I / O	Relay 8 ON / OFF
Y	Turn all loads OFF
X	Mute Alarm
S	Request system status
Messages Sent from the Arduino to the App
Message	Example	Purpose
READY	READY,ECOGUARD_HOME	Confirms that the controller is ready
STATUS	STATUS,FIRE=0,GAS=0,GAS_ADC=120,FLAME_ADC=800,SMART=0,ALARM=0,R1=0...	Updates the app with the current system status
ALERT	ALERT,FIRE=1,GAS=0,...	Sends a fire or gas warning
SAFE	SAFE,FIRE=0,GAS=0	Indicates that the system has returned to a safe state
Important Notes
The application is designed for Android, since the HC-05 communicates using Bluetooth Classic (SPP).
Without uploading the updated firmware, the app can still control the system only if the existing Arduino code already supports the same commands.
However, fire and gas notifications will not work with the old firmware, because it does not send the detected hazard status to the mobile application.
