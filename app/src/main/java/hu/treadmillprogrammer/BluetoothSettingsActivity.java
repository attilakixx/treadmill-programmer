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
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BluetoothSettingsActivity extends Activity {
    private static final int BG = 0xff0b0d0b, SURFACE = 0xff181b17, PANEL = 0xff111410;
    private static final int YELLOW = 0xfff7d436, TEXT = 0xfff5f5ef, MUTED = 0xffa0a69a, BORDER = 0xff343a30;
    private static final int SCAN_PERMISSIONS = 1, CONNECT_PERMISSIONS = 2;
    private static final ParcelUuid FTMS_UUID = new ParcelUuid(UUID.fromString("00001826-0000-1000-8000-00805f9b34fb"));
    private final Map<String, Button> devices = new LinkedHashMap<>();
    private final Map<String, String> deviceNames = new HashMap<>();
    private final Set<String> ftmsDevices = new HashSet<>();
    private TreadmillService service;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private LinearLayout deviceList;
    private TextView savedName, savedAddress, autoInfo, stateText, statusDot;
    private Button reconnectButton;
    private final TreadmillService.Listener listener = (state, ready, running, step, remaining) -> runOnUiThread(() -> {
        stateText.setText(state);
        stateText.setTextColor(ready ? TEXT : MUTED);
        statusDot.setTextColor(ready ? YELLOW : MUTED);
        reconnectButton.setEnabled(service != null && service.hasRememberedDevice() && !ready && !service.isConnecting() && !running);
        reconnectButton.setAlpha(reconnectButton.isEnabled() ? 1f : .45f);
        if (ready) refreshRemembered();
    });
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((TreadmillService.LocalBinder) binder).service();
            service.addListener(listener);
            refreshRemembered();
            if (service.hasRememberedDevice() && !service.isConnecting() && !service.isReady() && hasBluetoothPermissions()) service.connectRemembered();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            service = null;
            stateText.setText("A kapcsolat szolgáltatása megszakadt.");
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        bindService(new Intent(this, TreadmillService.class), connection, BIND_AUTO_CREATE);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String content, int size, int color) {
        TextView view = new TextView(this);
        view.setText(content); view.setTextSize(size); view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(fill); result.setCornerRadius(dp(radius));
        if (stroke != 0) result.setStroke(dp(1), stroke);
        return result;
    }
    private Button button(String title, Runnable action, boolean primary) {
        Button view = new Button(this);
        view.setText(title); view.setAllCaps(false); view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(primary ? BG : YELLOW);
        view.setMinHeight(dp(52)); view.setPadding(dp(16), dp(10), dp(16), dp(10));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55ffffff), shape(primary ? YELLOW : PANEL, primary ? 0 : BORDER, 14), null));
        view.setOnClickListener(v -> action.run());
        return view;
    }
    private void buildUi() {
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setNavigationBarContrastEnforced(false);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(content);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header);
        Button back = button("‹", this::finish, false);
        back.setTextSize(30); back.setContentDescription("Vissza");
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(14), 0, 0, 0);
        header.addView(titles);
        TextView title = text("Bluetooth", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titles.addView(title);
        TextView subtitle = text("ESZKÖZBEÁLLÍTÁSOK", 11, MUTED);
        subtitle.setLetterSpacing(.13f); titles.addView(subtitle);

        TextView savedTitle = text("MEGJEGYZETT FUTÓPAD", 12, YELLOW);
        savedTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        savedTitle.setLetterSpacing(.14f);
        savedTitle.setPadding(0, dp(30), 0, dp(10)); content.addView(savedTitle);
        LinearLayout savedCard = new LinearLayout(this);
        savedCard.setOrientation(LinearLayout.VERTICAL);
        savedCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        savedCard.setBackground(shape(SURFACE, BORDER, 20));
        content.addView(savedCard);
        savedName = text("Nincs megjegyzett eszköz", 19, TEXT);
        savedName.setTypeface(Typeface.DEFAULT, Typeface.BOLD); savedCard.addView(savedName);
        savedAddress = text("Az első sikeres FTMS csatlakozás után itt jelenik meg.", 13, MUTED);
        savedAddress.setPadding(0, dp(5), 0, dp(12)); savedCard.addView(savedAddress);
        autoInfo = text("Automatikus csatlakozás az app megnyitásakor", 13, YELLOW);
        savedCard.addView(autoInfo);
        reconnectButton = button("Újracsatlakozás", this::reconnect, false);
        reconnectButton.setEnabled(false); reconnectButton.setAlpha(.45f);
        LinearLayout.LayoutParams reconnectMargins = new LinearLayout.LayoutParams(-1, -2);
        reconnectMargins.topMargin = dp(15); savedCard.addView(reconnectButton, reconnectMargins);

        TextView statusTitle = text("JELENLEGI KAPCSOLAT", 12, YELLOW);
        statusTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusTitle.setLetterSpacing(.14f);
        statusTitle.setPadding(0, dp(28), 0, dp(10)); content.addView(statusTitle);
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setPadding(dp(16), dp(18), dp(16), dp(18));
        statusCard.setBackground(shape(SURFACE, BORDER, 20));
        content.addView(statusCard);
        statusDot = text("●", 18, MUTED);
        statusCard.addView(statusDot, new LinearLayout.LayoutParams(dp(28), -2));
        stateText = text("Nincs kapcsolat", 15, MUTED);
        statusCard.addView(stateText, new LinearLayout.LayoutParams(0, -2, 1));

        TextView scanTitle = text("MÁSIK ESZKÖZ", 12, YELLOW);
        scanTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scanTitle.setLetterSpacing(.14f);
        scanTitle.setPadding(0, dp(28), 0, dp(10)); content.addView(scanTitle);
        Button search = button("Bluetooth eszközök keresése", this::scan, true);
        content.addView(search, new LinearLayout.LayoutParams(-1, -2));
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL); content.addView(deviceList);
        TextView hint = text("A lista a hirdetett nevet, az FTMS jelzést és a jelerősséget mutatja. Új eszközt csak sikeres FTMS kapcsolat után jegyez meg az app.", 13, MUTED);
        hint.setPadding(0, dp(18), 0, 0); content.addView(hint);

        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            content.setPadding(dp(20) + bars.left, dp(18) + bars.top, dp(20) + bars.right, dp(28) + bars.bottom);
            return insets;
        });
        setContentView(scroll);
    }
    private void refreshRemembered() {
        if (service == null) return;
        if (service.hasRememberedDevice()) {
            savedName.setText(service.rememberedDeviceName());
            savedAddress.setText(service.rememberedDeviceAddress());
            autoInfo.setText("●  Automatikus csatlakozás bekapcsolva");
            autoInfo.setTextColor(YELLOW);
            reconnectButton.setVisibility(View.VISIBLE);
        } else {
            savedName.setText("Nincs megjegyzett eszköz");
            savedAddress.setText("Az első sikeres FTMS csatlakozás után itt jelenik meg.");
            autoInfo.setText("Az első csatlakozás után automatikusan visszacsatlakozik.");
            autoInfo.setTextColor(MUTED);
            reconnectButton.setVisibility(View.GONE);
        }
    }
    private boolean hasBluetoothPermissions() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
    private void reconnect() {
        if (service == null || service.isRunning()) return;
        if (service.isConnecting() || service.isReady()) {
            Toast.makeText(this, "A kapcsolat már aktív vagy folyamatban van.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasBluetoothPermissions()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, CONNECT_PERMISSIONS);
            return;
        }
        service.connectRemembered();
    }
    private void scan() {
        if (service != null && service.isRunning()) { showError("Futó edzés közben nem váltható Bluetooth-eszköz."); return; }
        if (!hasBluetoothPermissions()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, SCAN_PERMISSIONS);
            return;
        }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) { showError("Kapcsold be a Bluetooth-t."); return; }
        stopScan(); devices.clear(); deviceNames.clear(); ftmsDevices.clear(); deviceList.removeAllViews();
        TextView searching = text("Eszközök keresése (10 mp)…", 14, MUTED);
        searching.setPadding(0, dp(14), 0, 0); deviceList.addView(searching);
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) { showError("A BLE keresés nem érhető el."); return; }
        scanCallback = new ScanCallback() {
            @Override public void onScanResult(int type, ScanResult result) { runOnUiThread(() -> showScanResult(result)); }
            @Override public void onScanFailed(int code) { runOnUiThread(() -> showError("Bluetooth keresési hiba: " + code)); }
        };
        scanner.startScan(scanCallback);
        deviceList.postDelayed(() -> {
            if (scanCallback == null) return;
            stopScan();
            if (devices.isEmpty()) {
                TextView empty = text("Nem található BLE eszköz.", 14, MUTED);
                empty.setPadding(0, dp(14), 0, 0); deviceList.addView(empty);
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
                if (service == null) { showError("A Bluetooth szolgáltatás még nem indult el."); return; }
                if (service.isRunning()) { showError("Futó edzés közben nem váltható Bluetooth-eszköz."); return; }
                stopScan(); service.connect(device, deviceNames.get(address));
            }, false);
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); row.setTextSize(13);
            if (devices.isEmpty()) deviceList.removeAllViews();
            devices.put(address, row);
            LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2);
            margins.topMargin = dp(8);
            if (ftmsDevices.contains(address)) deviceList.addView(row, 0, margins);
            else deviceList.addView(row, margins);
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
        if (request == SCAN_PERMISSIONS && hasBluetoothPermissions()) scan();
        else if (request == SCAN_PERMISSIONS) showError("A Bluetooth kereséshez és csatlakozáshoz engedély szükséges.");
        else if (request == CONNECT_PERMISSIONS && hasBluetoothPermissions()) reconnect();
        else if (request == CONNECT_PERMISSIONS) showError("Az újracsatlakozáshoz Bluetooth-engedély szükséges.");
    }
    private void showError(String message) { new AlertDialog.Builder(this).setTitle("Bluetooth").setMessage(message).setPositiveButton("OK", null).show(); }
    @Override protected void onPause() { stopScan(); super.onPause(); }
    @Override protected void onDestroy() {
        if (service != null) service.removeListener(listener);
        unbindService(connection);
        super.onDestroy();
    }
}
