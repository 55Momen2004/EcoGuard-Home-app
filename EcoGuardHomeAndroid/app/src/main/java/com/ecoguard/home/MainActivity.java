package com.ecoguard.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String CHANNEL_ID = "ecoguard_alerts";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_PERMISSIONS = 71;

    private final int blue = Color.rgb(11, 61, 120);
    private final int green = Color.rgb(53, 168, 82);
    private final int red = Color.rgb(211, 47, 47);
    private final int orange = Color.rgb(244, 124, 32);
    private final int teal = Color.rgb(0, 137, 123);
    private final int dark = Color.rgb(25, 39, 60);
    private final int light = Color.rgb(245, 249, 253);
    private final int muted = Color.rgb(96, 112, 133);

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private Spinner deviceSpinner;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread readerThread;
    private volatile boolean connected = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView connectionText;
    private TextView fireStatusText;
    private TextView gasStatusText;
    private TextView alarmStatusText;
    private TextView smartStatusText;
    private TextView gasValueText;
    private TextView flameValueText;
    private TextView logText;
    private final Button[] relayButtons = new Button[8];
    private final boolean[] relayStates = new boolean[8];

    private final char[] relayOnCommands = {'L', 'R', 'Q', 'G', 'D', 'E', 'C', 'I'};
    private final char[] relayOffCommands = {'K', 'T', 'W', 'F', 'V', 'N', 'M', 'O'};
    private final String[] relayLabels = {
            "CH1 - Load 1", "CH2 - Vent / Servo", "CH3 - Load 3", "CH4 - Load 4",
            "CH5 - Load 5", "CH6 - Load 6", "CH7 - Load 7", "CH8 - Load 8"
    };

    private String lastAlertType = "";
    private long lastAlertTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createNotificationChannel();
        buildInterface();
        requestInitialPermissions();
        loadPairedDevices();
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void buildInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundColor(light);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(roundedGradient(blue, Color.rgb(5, 95, 161), dp(22)));
        root.addView(header, matchWrapBottom(dp(14)));

        TextView title = text("EcoGuard Home", 30, Color.WHITE, Typeface.BOLD);
        title.setLetterSpacing(0.02f);
        header.addView(title);

        TextView subtitle = text("Smart Protection & Energy Automation", 15, Color.WHITE, Typeface.NORMAL);
        subtitle.setAlpha(0.92f);
        subtitle.setPadding(0, dp(4), 0, dp(10));
        header.addView(subtitle);

        TextView tagline = text("Monitor  •  Protect  •  Automate", 14, Color.WHITE, Typeface.BOLD);
        tagline.setBackground(roundedSolid(Color.argb(45, 255, 255, 255), dp(30), Color.TRANSPARENT, 0));
        tagline.setPadding(dp(14), dp(8), dp(14), dp(8));
        header.addView(tagline, wrapWrap());

        LinearLayout connectionCard = card(root, "Bluetooth Connection", "Pair HC-05 from Android Bluetooth settings, then connect from here.");

        deviceSpinner = new Spinner(this);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>());
        deviceSpinner.setAdapter(spinnerAdapter);
        connectionCard.addView(deviceSpinner, matchWrapBottom(dp(10)));

        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        connectionCard.addView(buttonsRow, matchWrapBottom(dp(10)));

        Button refreshButton = actionButton("Refresh", blue);
        refreshButton.setOnClickListener(v -> loadPairedDevices());
        buttonsRow.addView(refreshButton, weightButton());

        Button connectButton = actionButton("Connect", green);
        connectButton.setOnClickListener(v -> connectSelectedDevice());
        buttonsRow.addView(connectButton, weightButton());

        Button disconnectButton = actionButton("Disconnect", red);
        disconnectButton.setOnClickListener(v -> disconnect());
        buttonsRow.addView(disconnectButton, weightButton());

        Button settingsButton = outlineButton("Open Bluetooth Settings", blue);
        settingsButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        connectionCard.addView(settingsButton, matchWrapBottom(dp(8)));

        connectionText = statusLine("Status: Not connected", muted);
        connectionCard.addView(connectionText);

        LinearLayout safetyCard = card(root, "Safety Monitor", "Live statuses are received from Arduino Mega through HC-05.");
        fireStatusText = safetyPill("Fire: Waiting", muted);
        gasStatusText = safetyPill("Gas: Waiting", muted);
        alarmStatusText = safetyPill("Alarm: Waiting", muted);
        smartStatusText = safetyPill("Smart Mode: OFF", muted);
        gasValueText = statusLine("MQ-135 raw value: --", muted);
        flameValueText = statusLine("Flame raw value: --", muted);
        safetyCard.addView(fireStatusText, matchWrapBottom(dp(8)));
        safetyCard.addView(gasStatusText, matchWrapBottom(dp(8)));
        safetyCard.addView(alarmStatusText, matchWrapBottom(dp(8)));
        safetyCard.addView(smartStatusText, matchWrapBottom(dp(12)));
        safetyCard.addView(gasValueText, matchWrapBottom(dp(4)));
        safetyCard.addView(flameValueText, matchWrapBottom(dp(4)));

        LinearLayout controlCard = card(root, "Appliance Control", "Manual Bluetooth control for the 8-channel relay module.");
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        controlCard.addView(grid, matchWrapBottom(dp(12)));
        for (int i = 0; i < 8; i++) {
            final int index = i;
            relayButtons[i] = actionButton(relayLabels[i] + "\nOFF", muted);
            relayButtons[i].setAllCaps(false);
            relayButtons[i].setTextSize(13);
            relayButtons[i].setMinHeight(dp(62));
            relayButtons[i].setOnClickListener(v -> toggleRelay(index));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(relayButtons[i], params);
        }

        LinearLayout smartRow = new LinearLayout(this);
        smartRow.setOrientation(LinearLayout.HORIZONTAL);
        controlCard.addView(smartRow, matchWrapBottom(dp(8)));
        Button smartOn = actionButton("Start Smart Mode", green);
        smartOn.setOnClickListener(v -> sendCommand("A"));
        smartRow.addView(smartOn, weightButton());
        Button smartOff = actionButton("Stop Smart Mode", orange);
        smartOff.setOnClickListener(v -> sendCommand("H"));
        smartRow.addView(smartOff, weightButton());

        LinearLayout utilityRow = new LinearLayout(this);
        utilityRow.setOrientation(LinearLayout.HORIZONTAL);
        controlCard.addView(utilityRow, matchWrapBottom(dp(2)));
        Button statusButton = outlineButton("Request Status", blue);
        statusButton.setOnClickListener(v -> sendCommand("S"));
        utilityRow.addView(statusButton, weightButton());
        Button silenceButton = outlineButton("Silence Alarm", red);
        silenceButton.setOnClickListener(v -> sendCommand("X"));
        utilityRow.addView(silenceButton, weightButton());

        LinearLayout logCard = card(root, "Event Log", "Connection messages, acknowledgements, and alerts.");
        logText = statusLine("Ready. Connect to HC-05 to start.\n", muted);
        logText.setTextIsSelectable(true);
        logCard.addView(logText, matchWrap());

        TextView footer = text("EcoGuard Home v1.0  •  Arduino Mega 2560  •  HC-05 Bluetooth", 12, muted, Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(12), 0, 0);
        root.addView(footer, matchWrap());

        setContentView(scrollView);
    }

    private LinearLayout card(LinearLayout parent, String titleValue, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundedSolid(Color.WHITE, dp(20), Color.rgb(224, 233, 243), dp(1)));
        parent.addView(card, matchWrapBottom(dp(14)));

        TextView title = text(titleValue, 20, blue, Typeface.BOLD);
        card.addView(title);
        TextView desc = text(description, 13, muted, Typeface.NORMAL);
        desc.setPadding(0, dp(4), 0, dp(12));
        card.addView(desc);
        return card;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setLineSpacing(dp(2), 1.0f);
        return tv;
    }

    private TextView statusLine(String value, int color) {
        TextView tv = text(value, 14, color, Typeface.NORMAL);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private TextView safetyPill(String value, int color) {
        TextView tv = text(value, 16, Color.WHITE, Typeface.BOLD);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setBackground(roundedSolid(color, dp(16), Color.TRANSPARENT, 0));
        return tv;
    }

    private Button actionButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setBackground(roundedSolid(color, dp(14), Color.TRANSPARENT, 0));
        return button;
    }

    private Button outlineButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(color);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setBackground(roundedSolid(Color.WHITE, dp(14), color, dp(1)));
        return button;
    }

    private GradientDrawable roundedSolid(int fill, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private GradientDrawable roundedGradient(int start, int end, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end, green});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapBottom(int bottom) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void requestInitialPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private boolean hasBluetoothPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadPairedDevices() {
        devices.clear();
        if (spinnerAdapter != null) spinnerAdapter.clear();

        if (bluetoothAdapter == null) {
            showToast("This phone does not support Bluetooth.");
            appendLog("Bluetooth hardware is not available on this device.");
            return;
        }

        if (!hasBluetoothPermission()) {
            requestInitialPermissions();
            appendLog("Bluetooth permission is required.");
            return;
        }

        try {
            Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
            if (bonded != null) {
                for (BluetoothDevice device : bonded) {
                    devices.add(device);
                    String name = safeDeviceName(device);
                    String mac = device.getAddress() == null ? "" : device.getAddress();
                    spinnerAdapter.add(name + "  " + mac);
                }
            }
            if (devices.isEmpty()) {
                spinnerAdapter.add("No paired devices found. Pair HC-05 first.");
                appendLog("No paired Bluetooth devices were found.");
            } else {
                int preferred = findPreferredDeviceIndex();
                deviceSpinner.setSelection(preferred);
                appendLog("Paired devices loaded: " + devices.size());
            }
            spinnerAdapter.notifyDataSetChanged();
        } catch (SecurityException ex) {
            appendLog("Bluetooth permission denied: " + ex.getMessage());
        }
    }

    private int findPreferredDeviceIndex() {
        for (int i = 0; i < devices.size(); i++) {
            String name = safeDeviceName(devices.get(i)).toUpperCase(Locale.US);
            if (name.contains("HC-05") || name.contains("HC05") || name.contains("ECOGUARD")) return i;
        }
        return 0;
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            if (TextUtils.isEmpty(name)) return "Unnamed device";
            return name;
        } catch (SecurityException ex) {
            return "Bluetooth device";
        }
    }

    private void connectSelectedDevice() {
        if (connected) {
            appendLog("Already connected.");
            return;
        }
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not available.");
            return;
        }
        if (!hasBluetoothPermission()) {
            requestInitialPermissions();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            try {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (SecurityException ex) {
                appendLog("Cannot request Bluetooth enable permission.");
            }
            return;
        }
        int position = deviceSpinner == null ? -1 : deviceSpinner.getSelectedItemPosition();
        if (position < 0 || position >= devices.size()) {
            showToast("Pair HC-05 first, then tap Refresh.");
            return;
        }

        BluetoothDevice selected = devices.get(position);
        connectionText.setText("Status: Connecting to " + safeDeviceName(selected) + " ...");
        connectionText.setTextColor(orange);
        appendLog("Connecting to " + safeDeviceName(selected) + " ...");

        new Thread(() -> {
            try {
                if (hasBluetoothPermission()) bluetoothAdapter.cancelDiscovery();
                BluetoothSocket tmpSocket = selected.createRfcommSocketToServiceRecord(SPP_UUID);
                tmpSocket.connect();
                socket = tmpSocket;
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                connected = true;
                runOnUiThread(() -> {
                    connectionText.setText("Status: Connected to " + safeDeviceName(selected));
                    connectionText.setTextColor(green);
                    showToast("Connected to " + safeDeviceName(selected));
                    appendLog("Connected successfully.");
                    sendCommand("S");
                });
                startReader();
            } catch (Exception ex) {
                connected = false;
                closeSocketOnly();
                runOnUiThread(() -> {
                    connectionText.setText("Status: Connection failed");
                    connectionText.setTextColor(red);
                    appendLog("Connection failed: " + ex.getMessage());
                    showError("Connection failed", "Make sure HC-05 is paired, powered, and not connected to another phone.\n\nDetails: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                while (connected && (line = reader.readLine()) != null) {
                    final String message = line.trim();
                    if (!message.isEmpty()) {
                        mainHandler.post(() -> handleIncomingMessage(message));
                    }
                }
            } catch (IOException ex) {
                if (connected) {
                    mainHandler.post(() -> appendLog("Bluetooth read stopped: " + ex.getMessage()));
                }
            } finally {
                if (connected) {
                    mainHandler.post(() -> {
                        appendLog("Disconnected by remote device.");
                        disconnect();
                    });
                }
            }
        });
        readerThread.start();
    }

    private void disconnect() {
        connected = false;
        closeSocketOnly();
        if (connectionText != null) {
            connectionText.setText("Status: Not connected");
            connectionText.setTextColor(muted);
        }
        appendLog("Disconnected.");
    }

    private void closeSocketOnly() {
        try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) { }
        try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) { }
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        inputStream = null;
        outputStream = null;
        socket = null;
    }

    private void toggleRelay(int index) {
        if (index < 0 || index >= 8) return;
        boolean newState = !relayStates[index];
        char command = newState ? relayOnCommands[index] : relayOffCommands[index];
        relayStates[index] = newState;
        updateRelayButton(index);
        sendCommand(String.valueOf(command));
    }

    private void updateRelayButton(int index) {
        if (relayButtons[index] == null) return;
        relayButtons[index].setText(relayLabels[index] + "\n" + (relayStates[index] ? "ON" : "OFF"));
        relayButtons[index].setBackground(roundedSolid(relayStates[index] ? green : muted, dp(14), Color.TRANSPARENT, 0));
    }

    private void sendCommand(String command) {
        if (!connected || outputStream == null) {
            appendLog("Cannot send '" + command + "': not connected.");
            showToast("Connect to HC-05 first.");
            return;
        }
        try {
            outputStream.write(command.getBytes(StandardCharsets.US_ASCII));
            outputStream.flush();
            appendLog("TX: " + command);
        } catch (IOException ex) {
            appendLog("Send failed: " + ex.getMessage());
            showError("Send failed", ex.getMessage());
        }
    }

    private void handleIncomingMessage(String message) {
        appendLog("RX: " + message);
        if (message.startsWith("STATUS")) {
            updateStatusFromMessage(message);
        } else if (message.startsWith("ALERT")) {
            updateStatusFromMessage(message);
            String type = getValue(message, "TYPE", "DANGER");
            showSafetyNotification(type, message);
        } else if (message.startsWith("ACK")) {
            String relays = getValue(message, "RELAYS", "");
            if (!relays.isEmpty()) applyRelayString(relays);
        }
    }

    private void updateStatusFromMessage(String message) {
        String gas = getValue(message, "GAS", "");
        String flame = getValue(message, "FLAME", "");
        String alarm = getValue(message, "ALARM", "");
        String smart = getValue(message, "SMART", "");
        String gasValue = getValue(message, "GAS_VALUE", "");
        String flameValue = getValue(message, "FLAME_VALUE", "");
        String relays = getValue(message, "RELAYS", "");

        if (!gas.isEmpty()) setGasStatus("1".equals(gas));
        if (!flame.isEmpty()) setFireStatus("1".equals(flame));
        if (!alarm.isEmpty()) setAlarmStatus("1".equals(alarm));
        if (!smart.isEmpty()) setSmartStatus("1".equals(smart));
        if (!gasValue.isEmpty()) gasValueText.setText("MQ-135 raw value: " + gasValue);
        if (!flameValue.isEmpty()) flameValueText.setText("Flame raw value: " + flameValue);
        if (!relays.isEmpty()) applyRelayString(relays);
    }

    private void setFireStatus(boolean danger) {
        fireStatusText.setText(danger ? "Fire: DANGER DETECTED" : "Fire: Safe");
        fireStatusText.setBackground(roundedSolid(danger ? red : green, dp(16), Color.TRANSPARENT, 0));
    }

    private void setGasStatus(boolean danger) {
        gasStatusText.setText(danger ? "Gas: DANGER DETECTED" : "Gas: Safe");
        gasStatusText.setBackground(roundedSolid(danger ? orange : green, dp(16), Color.TRANSPARENT, 0));
    }

    private void setAlarmStatus(boolean active) {
        alarmStatusText.setText(active ? "Alarm: Active" : "Alarm: Standby");
        alarmStatusText.setBackground(roundedSolid(active ? red : teal, dp(16), Color.TRANSPARENT, 0));
    }

    private void setSmartStatus(boolean active) {
        smartStatusText.setText(active ? "Smart Mode: ON" : "Smart Mode: OFF");
        smartStatusText.setBackground(roundedSolid(active ? blue : muted, dp(16), Color.TRANSPARENT, 0));
    }

    private void applyRelayString(String relays) {
        for (int i = 0; i < relays.length() && i < 8; i++) {
            char c = relays.charAt(i);
            relayStates[i] = c == '1';
            updateRelayButton(i);
        }
    }

    private String getValue(String message, String key, String fallback) {
        String[] parts = message.split(";");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String k = part.substring(0, idx).trim();
                String v = part.substring(idx + 1).trim();
                if (key.equalsIgnoreCase(k)) return v;
            }
        }
        return fallback;
    }

    private void showSafetyNotification(String type, String rawMessage) {
        long now = System.currentTimeMillis();
        if (type.equals(lastAlertType) && now - lastAlertTime < 5000L) return;
        lastAlertType = type;
        lastAlertTime = now;

        String title;
        String body;
        if ("FIRE".equalsIgnoreCase(type)) {
            title = "EcoGuard Fire Alert";
            body = "Flame sensor detected a possible fire condition.";
        } else if ("GAS".equalsIgnoreCase(type)) {
            title = "EcoGuard Gas Alert";
            body = "MQ-135 detected an abnormal gas/air-quality condition.";
        } else if ("BOTH".equalsIgnoreCase(type)) {
            title = "EcoGuard Critical Alert";
            body = "Fire and gas hazards were detected. Loads were switched off.";
        } else {
            title = "EcoGuard Safety Alert";
            body = "Abnormal safety condition detected.";
        }

        vibratePhone();
        if (!hasNotificationPermission()) {
            requestInitialPermissions();
            showError(title, body + "\n\nNotification permission is required for lock-screen alerts.");
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body + "\n" + rawMessage))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 300, 150, 300, 150, 600});

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify((int) (now % 100000), builder.build());
        showToast(title);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "EcoGuard Safety Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Fire and gas warnings from EcoGuard Home.");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 150, 300, 150, 600});
            channel.setSound(sound, attributes);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 250, 120, 250, 120, 500}, -1));
        } else {
            vibrator.vibrate(new long[]{0, 250, 120, 250, 120, 500}, -1);
        }
    }

    private void appendLog(String value) {
        if (logText == null) return;
        String current = logText.getText().toString();
        String newLine = String.format(Locale.US, "%tT  %s\n", System.currentTimeMillis(), value);
        if (current.length() > 5000) current = current.substring(current.length() - 3500);
        logText.setText(current + newLine);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
