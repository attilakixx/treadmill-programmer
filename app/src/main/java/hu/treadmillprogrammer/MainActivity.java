package hu.treadmillprogrammer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int BG = 0xff0b0d0b, SURFACE = 0xff181b17, PANEL = 0xff111410;
    private static final int YELLOW = 0xfff7d436, TEXT = 0xfff5f5ef, MUTED = 0xffa0a69a, BORDER = 0xff343a30;
    private static final int PICK_FILE = 1, PERMISSIONS = 2, AUTO_PERMISSIONS = 3;
    private final List<Workout> programs = new ArrayList<>();
    private static final ParcelUuid FTMS_UUID = new ParcelUuid(UUID.fromString("00001826-0000-1000-8000-00805f9b34fb"));
    private final Map<String, Button> devices = new LinkedHashMap<>();
    private final Map<String, String> deviceNames = new HashMap<>();
    private final Set<String> ftmsDevices = new HashSet<>();
    private Workout selected;
    private TreadmillService service;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private TextView stateText, statusDot, selectedSummary, programCountText;
    private LinearLayout deviceList, programList;
    private Button startButton, stopButton;
    private final TreadmillService.Listener listener = (state, ready, running, step, remaining) -> runOnUiThread(() -> {
        stateText.setText(state + (running && remaining > 0 ? "\nHátralévő szakaszidő: " + (remaining / 1000) + " mp" : ""));
        statusDot.setTextColor(ready ? YELLOW : MUTED);
        stateText.setTextColor(ready ? TEXT : MUTED);
        startButton.setEnabled(ready && !running && selected != null);
        startButton.setAlpha(startButton.isEnabled() ? 1f : .45f);
        stopButton.setEnabled(running);
        stopButton.setVisibility(running ? View.VISIBLE : View.GONE);
        if (running) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    });
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((TreadmillService.LocalBinder) binder).service(); service.setListener(listener);
            if (service.isRunning()) {
                for (Workout program : programs) if (program.samePlan(service.currentWorkout())) { selected = program; break; }
                refreshPrograms();
            }
            autoConnectRemembered();
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; stateText.setText("Futópad: szolgáltatás megszakadt"); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        try { programs.addAll(new WorkoutStore(this).load()); if (!programs.isEmpty()) selected = programs.get(0); refreshPrograms(); }
        catch (Exception error) { showError("Mentett programok olvasási hibája: " + error.getMessage()); }
        bindService(new Intent(this, TreadmillService.class), connection, BIND_AUTO_CREATE);
    }
    @Override protected void onResume() {
        super.onResume();
        autoConnectRemembered();
    }
    private void autoConnectRemembered() {
        if (service == null || !service.hasRememberedDevice() || service.isReady() || service.isConnecting() || service.isRunning()) return;
        if (!hasBluetoothPermissions()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, AUTO_PERMISSIONS);
            return;
        }
        service.connectRemembered();
    }
    private TextView label(String content, int size) {
        TextView view = new TextView(this);
        view.setText(content); view.setTextSize(size); view.setTextColor(TEXT);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    private Button button(String text, Runnable action) {
        return styledButton(text, action, false);
    }
    private Button styledButton(String text, Runnable action, boolean primary) {
        Button view = new Button(this);
        view.setText(text); view.setTextSize(15); view.setAllCaps(false);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(primary ? BG : YELLOW);
        view.setMinHeight(dp(52)); view.setPadding(dp(16), dp(10), dp(16), dp(10));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55ffffff), shape(primary ? YELLOW : PANEL, primary ? 0 : BORDER, 14), null));
        view.setOnClickListener(v -> action.run());
        return view;
    }
    private GradientDrawable shape(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill); drawable.setCornerRadius(dp(radiusDp));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void buildUi() {
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setNavigationBarContrastEnforced(false);
        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL); frame.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(content);
        frame.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_treadmill_mark);
        mark.setBackground(shape(SURFACE, BORDER, 16));
        mark.setPadding(dp(4), dp(4), dp(4), dp(4));
        header.addView(mark, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL); brand.setPadding(dp(14), 0, 0, 0);
        TextView title = label("PACE LAB", 26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setLetterSpacing(.09f);
        brand.addView(title);
        TextView subtitle = label("FUTÓPAD PROGRAMOZÓ", 11);
        subtitle.setTextColor(MUTED); subtitle.setLetterSpacing(.13f);
        brand.addView(subtitle);
        header.addView(brand); content.addView(header);
        TextView intro = label("A saját tempód. A saját programod.", 15);
        intro.setTextColor(MUTED); intro.setPadding(0, dp(15), 0, dp(26));
        content.addView(intro);

        TextView connectionTitle = label("KAPCSOLAT", 12);
        connectionTitle.setTextColor(YELLOW); connectionTitle.setLetterSpacing(.14f);
        connectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(connectionTitle);
        LinearLayout connectionCard = new LinearLayout(this);
        connectionCard.setOrientation(LinearLayout.VERTICAL);
        connectionCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        connectionCard.setBackground(shape(SURFACE, BORDER, 20));
        LinearLayout.LayoutParams connectionMargins = new LinearLayout.LayoutParams(-1, -2);
        connectionMargins.topMargin = dp(10); content.addView(connectionCard, connectionMargins);
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.TOP);
        statusDot = label("●", 18); statusDot.setTextColor(MUTED);
        statusRow.addView(statusDot, new LinearLayout.LayoutParams(dp(28), -2));
        stateText = label("Nincs kapcsolat", 15);
        stateText.setTextColor(MUTED); statusRow.addView(stateText, new LinearLayout.LayoutParams(0, -2, 1));
        connectionCard.addView(statusRow);
        Button scanButton = button("Bluetooth eszközök keresése", this::scan);
        LinearLayout.LayoutParams scanMargins = new LinearLayout.LayoutParams(-1, -2);
        scanMargins.topMargin = dp(15); connectionCard.addView(scanButton, scanMargins);
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL); connectionCard.addView(deviceList);

        LinearLayout libraryHeader = new LinearLayout(this);
        libraryHeader.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams headerMargins = new LinearLayout.LayoutParams(-1, -2);
        headerMargins.topMargin = dp(32); content.addView(libraryHeader, headerMargins);
        TextView libraryTitle = label("Programjaid", 25);
        libraryTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        libraryHeader.addView(libraryTitle, new LinearLayout.LayoutParams(0, -2, 1));
        programCountText = label("0 program", 12); programCountText.setTextColor(MUTED);
        libraryHeader.addView(programCountText);
        Button importButton = styledButton("＋  CSV importálása", this::pickFile, true);
        LinearLayout.LayoutParams importMargins = new LinearLayout.LayoutParams(-1, -2);
        importMargins.topMargin = dp(16); content.addView(importButton, importMargins);
        programList = new LinearLayout(this);
        programList.setOrientation(LinearLayout.VERTICAL); content.addView(programList);
        TextView safety = label("A program csak a sebességet állítja. A szalagot mindig a futópad konzolján vagy a biztonsági kulccsal állítsd le.", 13);
        safety.setTextColor(MUTED); safety.setPadding(0, dp(24), 0, 0);
        content.addView(safety);

        LinearLayout actionBar = new LinearLayout(this);
        actionBar.setOrientation(LinearLayout.VERTICAL);
        actionBar.setPadding(dp(20), dp(14), dp(20), dp(18));
        actionBar.setBackground(shape(PANEL, BORDER, 0));
        frame.addView(actionBar, new LinearLayout.LayoutParams(-1, -2));
        selectedSummary = label("Nincs kiválasztott program", 15);
        selectedSummary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selectedSummary.setPadding(0, 0, 0, dp(10)); actionBar.addView(selectedSummary);
        startButton = styledButton("Edzés indítása", this::startWorkout, true);
        startButton.setEnabled(false); startButton.setAlpha(.45f);
        actionBar.addView(startButton, new LinearLayout.LayoutParams(-1, -2));
        stopButton = button("Automatikus program leállítása", () -> { if (service != null) service.stopWorkout("Program leállítva. A futópadot a konzolon állítsd le."); });
        stopButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams stopMargins = new LinearLayout.LayoutParams(-1, -2);
        stopMargins.topMargin = dp(8); actionBar.addView(stopButton, stopMargins);
        TextView actionHint = label("Előbb indítsd el a futópadot a konzolon.", 12);
        actionHint.setTextColor(MUTED); actionHint.setPadding(0, dp(9), 0, 0); actionBar.addView(actionHint);
        frame.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            content.setPadding(dp(20) + bars.left, dp(18) + bars.top, dp(20) + bars.right, dp(28));
            actionBar.setPadding(dp(20) + bars.left, dp(14), dp(20) + bars.right, dp(18) + bars.bottom);
            return insets;
        });
        setContentView(frame);
    }
    private void refreshPrograms() {
        programList.removeAllViews();
        programCountText.setText(programs.size() + " program");
        if (programs.isEmpty()) {
            TextView empty = label("Még nincs mentett edzésed. Importálj egy CSV fájlt, és itt megjelenik a sebességprofilja.", 15);
            empty.setTextColor(MUTED); empty.setPadding(dp(16), dp(20), dp(16), dp(20));
            empty.setBackground(shape(SURFACE, BORDER, 18));
            LinearLayout.LayoutParams emptyMargins = new LinearLayout.LayoutParams(-1, -2);
            emptyMargins.topMargin = dp(14); programList.addView(empty, emptyMargins);
        }
        for (int index = 0; index < programs.size(); index++) {
            Workout workout = programs.get(index);
            boolean chosen = workout == selected;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            GradientDrawable background = shape(SURFACE, chosen ? YELLOW : BORDER, 20);
            if (chosen) background.setStroke(dp(2), YELLOW);
            card.setBackground(background);
            card.setOnClickListener(v -> {
                if (service != null && service.isRunning()) {
                    Toast.makeText(this, "Edzés közben nem váltható program.", Toast.LENGTH_SHORT).show();
                    return;
                }
                selected = workout; refreshPrograms();
            });
            LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2);
            margins.topMargin = dp(14);
            programList.addView(card, margins);
            TextView eyebrow = label(String.format(java.util.Locale.ROOT, "%02d / EDZÉSPROGRAM", index + 1), 11);
            eyebrow.setTextColor(chosen ? YELLOW : MUTED); eyebrow.setLetterSpacing(.12f);
            card.addView(eyebrow);
            TextView name = label(workout.name, 21);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            name.setPadding(0, dp(6), 0, dp(15)); card.addView(name);
            LinearLayout stats = new LinearLayout(this);
            stats.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(stats);
            LinearLayout duration = stat("IDŐTARTAM", workout.durationLabel());
            stats.addView(duration, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout maximum = stat("MAX. SEBESSÉG", workout.maxSpeedLabel() + " km/h");
            stats.addView(maximum, new LinearLayout.LayoutParams(0, -2, 1));
            SpeedChart chart = new SpeedChart(this);
            chart.setWorkout(workout);
            chart.setBackground(shape(PANEL, 0, 14));
            chart.setPadding(dp(6), dp(6), dp(6), dp(6));
            LinearLayout.LayoutParams chartMargins = new LinearLayout.LayoutParams(-1, dp(154));
            chartMargins.topMargin = dp(15); card.addView(chart, chartMargins);
            TextView choice = label(chosen ? "●  KIVÁLASZTVA" : "○  Kiválasztás", 13);
            choice.setTextColor(chosen ? YELLOW : MUTED);
            choice.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            choice.setPadding(0, dp(14), 0, 0); card.addView(choice);
        }
        selectedSummary.setText(selected == null ? "Nincs kiválasztott program" : "Kiválasztva: " + selected.name + "  •  " + selected.durationLabel());
        startButton.setEnabled(selected != null && service != null && service.isReady() && !service.isRunning());
        startButton.setAlpha(startButton.isEnabled() ? 1f : .45f);
    }
    private LinearLayout stat(String title, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView caption = label(title, 10);
        caption.setTextColor(MUTED); caption.setLetterSpacing(.10f);
        box.addView(caption);
        TextView number = label(value, 18);
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setPadding(0, dp(4), 0, 0); box.addView(number);
        return box;
    }
    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        startActivityForResult(intent, PICK_FILE);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_FILE || result != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream stream = getContentResolver().openInputStream(data.getData())) {
            if (stream == null) throw new IllegalArgumentException("A fájl nem olvasható.");
            byte[] bytes = stream.readNBytes(256 * 1024 + 1);
            if (bytes.length > 256 * 1024) throw new IllegalArgumentException("A fájl túl nagy (maximum 256 KB).");
            String csv = new String(bytes, StandardCharsets.UTF_8);
            EditText nameInput = new EditText(this); nameInput.setSingleLine(true); nameInput.setHint("Program neve");
            new AlertDialog.Builder(this).setTitle("Program importálása").setView(nameInput)
                .setPositiveButton("Mentés", (dialog, which) -> {
                    try {
                        Workout workout = Workout.parse(nameInput.getText().toString(), csv);
                        new WorkoutStore(this).save(workout, csv); programs.add(workout);
                        if (service == null || !service.isRunning()) selected = workout;
                        refreshPrograms();
                        Toast.makeText(this, "Program elmentve", Toast.LENGTH_SHORT).show();
                    } catch (Exception error) { showError(error.getMessage()); }
                }).setNegativeButton("Mégse", null).show();
        } catch (Exception error) { showError(error.getMessage()); }
    }
    private boolean hasBluetoothPermissions() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
    private void scan() {
        if (!hasBluetoothPermissions()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS}, PERMISSIONS);
            return;
        }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) { showError("Kapcsold be a Bluetooth-t."); return; }
        stopScan(); devices.clear(); deviceNames.clear(); ftmsDevices.clear(); deviceList.removeAllViews();
        TextView searching = label("Eszközök keresése (10 mp)…", 14);
        searching.setTextColor(MUTED); searching.setPadding(0, dp(14), 0, 0);
        deviceList.addView(searching);
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) { showError("A BLE keresés nem érhető el."); return; }
        scanCallback = new ScanCallback() {
            @Override public void onScanResult(int type, ScanResult result) {
                runOnUiThread(() -> showScanResult(result));
            }
            @Override public void onScanFailed(int code) { runOnUiThread(() -> showError("Bluetooth keresési hiba: " + code)); }
        };
        scanner.startScan(scanCallback);
        deviceList.postDelayed(() -> {
            if (scanCallback != null) {
                stopScan(); if (devices.isEmpty()) {
                    TextView empty = label("Nem található BLE eszköz.", 14);
                    empty.setTextColor(MUTED); empty.setPadding(0, dp(14), 0, 0);
                    deviceList.addView(empty);
                }
            }
        }, 10000);
    }
    private void showScanResult(ScanResult result) {
        if (scanCallback == null) return;
        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        ScanRecord record = result.getScanRecord();
        String name = record == null ? null : record.getDeviceName();
        if (name == null || name.isBlank()) name = device.getName();
        if (name == null || name.isBlank()) name = "Név nélküli eszköz";
        if (!name.equals("Név nélküli eszköz") || !deviceNames.containsKey(address)) deviceNames.put(address, name);
        boolean ftmsAdvertised = record != null && record.getServiceUuids() != null && record.getServiceUuids().contains(FTMS_UUID);
        if (ftmsAdvertised) ftmsDevices.add(address);
        String title = (ftmsDevices.contains(address) ? "FTMS • " : "") + deviceNames.get(address) + "\n" + address + " • " + result.getRssi() + " dBm";
        Button row = devices.get(address);
        if (row == null) {
            row = button(title, () -> {
                stopScan(); if (service != null) service.connect(device, deviceNames.get(address));
                else showError("A Bluetooth szolgáltatás még nem indult el.");
            });
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setTextSize(13);
            if (devices.isEmpty()) deviceList.removeAllViews();
            devices.put(address, row);
            LinearLayout.LayoutParams rowMargins = new LinearLayout.LayoutParams(-1, -2);
            rowMargins.topMargin = dp(8);
            if (ftmsDevices.contains(address)) deviceList.addView(row, 0, rowMargins); else deviceList.addView(row, rowMargins);
        } else {
            row.setText(title);
            if (ftmsAdvertised) { deviceList.removeView(row); deviceList.addView(row, 0); }
        }
    }
    private void stopScan() {
        if (scanner != null && scanCallback != null && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) scanner.stopScan(scanCallback);
        scanCallback = null;
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == PERMISSIONS && hasBluetoothPermissions()) scan();
        else if (request == PERMISSIONS) showError("A Bluetooth kereséshez és csatlakozáshoz engedély szükséges.");
        else if (request == AUTO_PERMISSIONS && hasBluetoothPermissions()) autoConnectRemembered();
    }
    private void startWorkout() {
        if (service == null || selected == null || !service.isReady()) return;
        Intent intent = new Intent(this, TreadmillService.class);
        startForegroundService(intent);
        service.startWorkout(selected);
    }
    private void showError(String message) { new AlertDialog.Builder(this).setTitle("Hiba").setMessage(message).setPositiveButton("OK", null).show(); }
    @Override protected void onDestroy() {
        stopScan(); if (service != null) service.clearListener(listener); unbindService(connection); super.onDestroy();
    }
}
