#include <Servo.h>

#define BT_SERIAL Serial1

const int relayPins[8] = {2, 3, 4, 5, 6, 7, 8, 9};
const int mqAnalogPin = A0;
const int mqDigitalPin = 12;
const int flameAnalogPin = A1;
const int flameDigitalPin = 10;
const int buzzerPin = 11;
const int servoPin = 22;

const int gasThreshold = 350;
const int flameThreshold = 300;

Servo windowServo;

bool smartSystem = false;
bool manualState[8] = {false, false, false, false, false, false, false, false};
bool alarmActive = false;
bool dangerAlreadyDetected = false;

unsigned long startTime = 0;
unsigned long alarmStartTime = 0;
unsigned long lastSensorRead = 0;
unsigned long lastStatusSend = 0;

int lastGasValue = 0;
int lastFlameValue = 0;
bool lastGasDetected = false;
bool lastFlameDetected = false;

void setup() {
  Serial.begin(9600);
  BT_SERIAL.begin(9600);

  for (int i = 0; i < 8; i++) {
    pinMode(relayPins[i], OUTPUT);
    digitalWrite(relayPins[i], HIGH);
  }

  pinMode(mqDigitalPin, INPUT_PULLUP);
  pinMode(flameDigitalPin, INPUT_PULLUP);
  pinMode(buzzerPin, OUTPUT);
  noTone(buzzerPin);

  windowServo.attach(servoPin);
  windowServo.write(0);

  sendStatus("BOOT");
}

void loop() {
  readBluetoothCommand();

  if (millis() - lastSensorRead >= 500) {
    lastSensorRead = millis();
    checkGasAndFlame();
  }

  if (smartSystem) {
    runSmartSystem();
  } else {
    for (int i = 0; i < 8; i++) {
      applyManualState(i);
    }
  }

  runAlarm();

  if (millis() - lastStatusSend >= 1000) {
    lastStatusSend = millis();
    sendStatus("STATUS");
  }
}

void readBluetoothCommand() {
  while (BT_SERIAL.available()) {
    char data = BT_SERIAL.read();
    if (data == '\n' || data == '\r') continue;

    switch (data) {
      case 'A':
        smartSystem = true;
        startTime = millis();
        break;

      case 'H':
        smartSystem = false;
        manualState[4] = false;
        manualState[5] = false;
        manualState[6] = false;
        digitalWrite(relayPins[4], HIGH);
        digitalWrite(relayPins[5], HIGH);
        digitalWrite(relayPins[6], HIGH);
        break;

      case 'L': setManual(0, true); break;
      case 'K': setManual(0, false); break;

      case 'R':
        setManual(1, true);
        windowServo.write(80);
        break;

      case 'T':
        setManual(1, false);
        windowServo.write(180);
        break;

      case 'Q': setManual(2, true); break;
      case 'W': setManual(2, false); break;
      case 'G': setManual(3, true); break;
      case 'F': setManual(3, false); break;
      case 'D': setManual(4, true); break;
      case 'V': setManual(4, false); break;
      case 'E': setManual(5, true); break;
      case 'N': setManual(5, false); break;
      case 'C': setManual(6, true); break;
      case 'M': setManual(6, false); break;
      case 'I': setManual(7, true); break;
      case 'O': setManual(7, false); break;

      case 'X':
        alarmActive = false;
        noTone(buzzerPin);
        break;

      case 'S':
        sendStatus("STATUS");
        break;
    }

    sendAck(data);
  }
}

void setManual(int relayIndex, bool state) {
  if (relayIndex < 0 || relayIndex > 7) return;
  manualState[relayIndex] = state;
  if (!smartSystem) {
    applyManualState(relayIndex);
  }
}

void applyManualState(int relayIndex) {
  digitalWrite(relayPins[relayIndex], manualState[relayIndex] ? LOW : HIGH);
}

void turnOffAllLoads() {
  smartSystem = false;
  for (int i = 0; i < 8; i++) {
    manualState[i] = false;
    digitalWrite(relayPins[i], HIGH);
  }
}

void checkGasAndFlame() {
  lastGasValue = analogRead(mqAnalogPin);
  int gasDigital = digitalRead(mqDigitalPin);
  lastFlameValue = analogRead(flameAnalogPin);
  int flameDigital = digitalRead(flameDigitalPin);

  lastGasDetected = (gasDigital == LOW) || (lastGasValue >= gasThreshold);
  lastFlameDetected = (flameDigital == LOW) || (lastFlameValue <= flameThreshold);
  bool dangerDetected = lastGasDetected || lastFlameDetected;

  if (dangerDetected && !dangerAlreadyDetected) {
    dangerAlreadyDetected = true;
    turnOffAllLoads();
    windowServo.write(90);
    alarmActive = true;
    alarmStartTime = millis();
    sendAlert();
  }

  if (!dangerDetected) {
    dangerAlreadyDetected = false;
  }
}

void runAlarm() {
  if (alarmActive) {
    if (millis() - alarmStartTime < 20000) {
      tone(buzzerPin, 3000);
    } else {
      noTone(buzzerPin);
      alarmActive = false;
    }
  }
}

void runSmartSystem() {
  unsigned long elapsedTime = millis() - startTime;

  for (int i = 0; i < 8; i++) {
    applyManualState(i);
  }

  if (elapsedTime < 20000) {
    digitalWrite(relayPins[6], HIGH);
    digitalWrite(relayPins[4], HIGH);
    digitalWrite(relayPins[5], HIGH);
  } else if (elapsedTime < 40000) {
    digitalWrite(relayPins[6], LOW);
    digitalWrite(relayPins[4], HIGH);
    digitalWrite(relayPins[5], HIGH);
  } else if (elapsedTime < 60000) {
    digitalWrite(relayPins[6], LOW);
    digitalWrite(relayPins[4], LOW);
    digitalWrite(relayPins[5], HIGH);
  } else if (elapsedTime < 80000) {
    digitalWrite(relayPins[6], HIGH);
    digitalWrite(relayPins[4], HIGH);
    digitalWrite(relayPins[5], LOW);
  } else {
    smartSystem = false;
    manualState[4] = false;
    manualState[5] = false;
    manualState[6] = false;
    digitalWrite(relayPins[4], HIGH);
    digitalWrite(relayPins[5], HIGH);
    digitalWrite(relayPins[6], HIGH);
  }
}

void sendAck(char command) {
  BT_SERIAL.print("ACK;CMD=");
  BT_SERIAL.print(command);
  BT_SERIAL.print(";RELAYS=");
  BT_SERIAL.println(relayStateString());
}

void sendAlert() {
  BT_SERIAL.print("ALERT;TYPE=");
  if (lastGasDetected && lastFlameDetected) BT_SERIAL.print("BOTH");
  else if (lastFlameDetected) BT_SERIAL.print("FIRE");
  else BT_SERIAL.print("GAS");
  BT_SERIAL.print(";GAS=");
  BT_SERIAL.print(lastGasDetected ? 1 : 0);
  BT_SERIAL.print(";FLAME=");
  BT_SERIAL.print(lastFlameDetected ? 1 : 0);
  BT_SERIAL.print(";ALARM=");
  BT_SERIAL.print(alarmActive ? 1 : 0);
  BT_SERIAL.print(";GAS_VALUE=");
  BT_SERIAL.print(lastGasValue);
  BT_SERIAL.print(";FLAME_VALUE=");
  BT_SERIAL.print(lastFlameValue);
  BT_SERIAL.print(";RELAYS=");
  BT_SERIAL.println(relayStateString());
}

void sendStatus(const char *prefix) {
  BT_SERIAL.print(prefix);
  BT_SERIAL.print(";GAS=");
  BT_SERIAL.print(lastGasDetected ? 1 : 0);
  BT_SERIAL.print(";FLAME=");
  BT_SERIAL.print(lastFlameDetected ? 1 : 0);
  BT_SERIAL.print(";ALARM=");
  BT_SERIAL.print(alarmActive ? 1 : 0);
  BT_SERIAL.print(";SMART=");
  BT_SERIAL.print(smartSystem ? 1 : 0);
  BT_SERIAL.print(";GAS_VALUE=");
  BT_SERIAL.print(lastGasValue);
  BT_SERIAL.print(";FLAME_VALUE=");
  BT_SERIAL.print(lastFlameValue);
  BT_SERIAL.print(";RELAYS=");
  BT_SERIAL.println(relayStateString());
}

String relayStateString() {
  String output = "";
  for (int i = 0; i < 8; i++) {
    output += (digitalRead(relayPins[i]) == LOW) ? "1" : "0";
  }
  return output;
}
