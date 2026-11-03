package hu.treadmillprogrammer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TreadmillService extends Service {
    interface Listener { void onState(String state, boolean ready, boolean running, int step, long remainingMs); }
    private static final UUID FTMS = uuid(0x1826), FEATURES = uuid(0x2ACC), CONTROL = uuid(0x2AD9), RANGE = uuid(0x2AD4), CCCD = uuid(0x2902);
    private static UUID uuid(int shortUuid) { return UUID.fromString(String.format("0000%04x-0000-1000-8000-00805f9b34fb", shortUuid)); }
    private static final String CHANNEL = "workout", STOP = "hu.treadmillprogrammer.STOP";
    private static final String DEVICE_PREFS = "remembered_treadmill", ADDRESS_KEY = "address", NAME_KEY = "name";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IBinder binder = new LocalBinder();
    private BluetoothGatt gatt;
    private String deviceName = "futópad";
    private String deviceAddress;
    private boolean automaticConnection;
    private BluetoothGattCharacteristic control;
    private final Set<Listener> listeners = new HashSet<>();
    private String state = "Nincs kapcsolat";
    private boolean ready, running, controlGranted, featureSupported, rangeAvailable;
    private int minSpeed, maxSpeed, speedStep, pendingOpcode = -1, stepIndex;
    private Workout workout;
    private long stepEndsAt;
    private Runnable timeout;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running || pendingOpcode != -1 || workout == null) return;
            long remain = stepEndsAt - SystemClock.elapsedRealtime();
            if (remain <= 0) advance();
            else { publish(); handler.postDelayed(this, Math.min(remain, 500)); }
        }
    };

    final class LocalBinder extends Binder { TreadmillService service() { return TreadmillService.this; } }
    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { stopWorkout("Program leállítva. A futópadot a konzolon állítsd le."); stopSelf(); }
        return START_NOT_STICKY;
    }
    void addListener(Listener listener) { listeners.add(listener); publish(); }
    void removeListener(Listener listener) { listeners.remove(listener); }
    boolean isReady() { return ready; }
    boolean isRunning() { return running; }
    Workout currentWorkout() { return workout; }
    int minSpeed() { return minSpeed; }
    int maxSpeed() { return maxSpeed; }
    int speedStep() { return speedStep; }
    boolean hasRange() { return rangeAvailable; }
    boolean isConnecting() { return gatt != null; }
    boolean hasRememberedDevice() { return getSharedPreferences(DEVICE_PREFS, MODE_PRIVATE).contains(ADDRESS_KEY); }
    String rememberedDeviceName() { return getSharedPreferences(DEVICE_PREFS, MODE_PRIVATE).getString(NAME_KEY, "Mentett futópad"); }
    String rememberedDeviceAddress() { return getSharedPreferences(DEVICE_PREFS, MODE_PRIVATE).getString(ADDRESS_KEY, null); }

    void connect(BluetoothDevice device, String displayName) {
        connect(device, displayName, false);
    }
    void connectRemembered() {
        if (gatt != null || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
        String address = getSharedPreferences(DEVICE_PREFS, MODE_PRIVATE).getString(ADDRESS_KEY, null);
        if (address == null) return;
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) return;
        try {
            String name = getSharedPreferences(DEVICE_PREFS, MODE_PRIVATE).getString(NAME_KEY, "Mentett futópad");
            connect(adapter.getRemoteDevice(address), name, true);
        } catch (IllegalArgumentException error) { fail("A mentett Bluetooth-cím érvénytelen."); }
    }
    private void connect(BluetoothDevice device, String displayName, boolean automatic) {
        disconnect();
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { fail("Nincs Bluetooth csatlakozási engedély."); return; }
        deviceName = displayName;
        deviceAddress = device.getAddress();
        automaticConnection = automatic;
        state = (automatic ? "Automatikus csatlakozás: " : "Csatlakozás: ") + deviceName; publish();
        gatt = device.connectGatt(this, automatic, callback, BluetoothDevice.TRANSPORT_LE);
    }
    void disconnect() {
        stopWorkout("Kapcsolat bontva. A futópadot szükség esetén a konzolon állítsd le.");
        ready = false; controlGranted = false; featureSupported = false; rangeAvailable = false;
        control = null; pendingOpcode = -1; automaticConnection = false;
        if (gatt != null) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) gatt.disconnect();
            gatt.close(); gatt = null;
        }
        state = "Nincs kapcsolat"; publish();
    }
    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt source, int status, int newState) {
            handler.post(() -> {
                if (source != gatt) return;
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    state = "Szolgáltatások keresése"; publish();
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) source.discoverServices();
                } else {
                    stopWorkout("Bluetooth kapcsolat megszakadt. A futópadot a konzolon ellenőrizd.");
                    ready = false; controlGranted = false; pendingOpcode = -1; control = null;
                    if (automaticConnection && status == BluetoothGatt.GATT_SUCCESS) {
                        state = "Várakozás a mentett futópadra: " + deviceName; publish();
                    } else {
                        source.close(); gatt = null; state = "Kapcsolat megszakadt"; publish();
                        if (!automaticConnection && !listeners.isEmpty() && hasRememberedDevice()) handler.postDelayed(() -> {
                            if (gatt == null && !listeners.isEmpty()) connectRemembered();
                        }, 2000);
                    }
                }
            });
        }
        @Override public void onServicesDiscovered(BluetoothGatt source, int status) {
            handler.post(() -> {
                if (source != gatt) return;
                BluetoothGattService service = source.getService(FTMS);
                if (status != BluetoothGatt.GATT_SUCCESS || service == null) { fail("Az FTMS szolgáltatás nem található."); return; }
                control = service.getCharacteristic(CONTROL);
                BluetoothGattCharacteristic features = service.getCharacteristic(FEATURES);
                if (control == null || features == null) { fail("A gép nem kínál FTMS sebességvezérlést."); return; }
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || !source.readCharacteristic(features)) fail("Nem olvashatók a gép képességei.");
            });
        }
        @Override public void onCharacteristicRead(BluetoothGatt source, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
            handler.post(() -> {
                if (source != gatt) return;
                if (status != BluetoothGatt.GATT_SUCCESS) { fail("FTMS jellemző olvasási hiba."); return; }
                if (FEATURES.equals(characteristic.getUuid())) {
                    if (value.length < 8) { fail("Hiányos FTMS képességadat."); return; }
                    featureSupported = (value[4] & 1) != 0;
                    if (!featureSupported) { fail("A gép nem támogatja a célsebesség beállítását."); return; }
                    BluetoothGattCharacteristic range = source.getService(FTMS).getCharacteristic(RANGE);
                    if (range == null || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || !source.readCharacteristic(range)) fail("Nem olvasható a támogatott sebességtartomány.");
                } else if (RANGE.equals(characteristic.getUuid())) {
                    if (value.length < 6) { fail("Hiányos sebességtartomány adat."); return; }
                    minSpeed = u16(value, 0); maxSpeed = u16(value, 2); speedStep = u16(value, 4);
                    rangeAvailable = maxSpeed >= minSpeed && speedStep > 0;
                    if (!rangeAvailable) { fail("Érvénytelen sebességtartomány."); return; }
                    BluetoothGattDescriptor cccd = control.getDescriptor(CCCD);
                    if (cccd == null || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || !source.setCharacteristicNotification(control, true) || source.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) != BluetoothGatt.GATT_SUCCESS) fail("Nem kapcsolhatók be a vezérlési válaszok.");
                }
            });
        }
        @Override public void onDescriptorWrite(BluetoothGatt source, BluetoothGattDescriptor descriptor, int status) {
            handler.post(() -> {
                if (source != gatt) return;
                if (status != BluetoothGatt.GATT_SUCCESS) { fail("FTMS értesítés beállítási hiba."); return; }
                ready = true;
                getSharedPreferences(DEVICE_PREFS, MODE_PRIVATE).edit().putString(ADDRESS_KEY, deviceAddress).putString(NAME_KEY, deviceName).apply();
                state = "Kapcsolódva: " + deviceName + " (FTMS) • sebesség: " + minSpeed / 100.0 + "–" + maxSpeed / 100.0 + " km/h"; publish();
            });
        }
        @Override public void onCharacteristicWrite(BluetoothGatt source, BluetoothGattCharacteristic characteristic, int status) {
            handler.post(() -> { if (source == gatt && status != BluetoothGatt.GATT_SUCCESS) fail("FTMS parancs írási hiba: " + status); });
        }
        @Override public void onCharacteristicChanged(BluetoothGatt source, BluetoothGattCharacteristic characteristic, byte[] value) {
            handler.post(() -> {
                if (source != gatt || !CONTROL.equals(characteristic.getUuid()) || value.length < 3 || (value[0] & 255) != 0x80) return;
                int opcode = value[1] & 255, result = value[2] & 255;
                if (opcode != pendingOpcode) return;
                clearTimeout(); pendingOpcode = -1;
                if (result != 1) { fail("FTMS parancs elutasítva (" + opcode + ", kód " + result + ")."); return; }
                if (opcode == 0) { controlGranted = true; sendSpeed(); }
                else if (opcode == 2) {
                    stepEndsAt = SystemClock.elapsedRealtime() + workout.steps.get(stepIndex).durationMs;
                    state = "Fut: " + (stepIndex + 1) + "/" + workout.steps.size() + ". szakasz, " + workout.steps.get(stepIndex).speedLabel() + " km/h";
                    publish(); handler.post(tick);
                }
            });
        }
    };
    private static int u16(byte[] bytes, int offset) { return (bytes[offset] & 255) | ((bytes[offset + 1] & 255) << 8); }
    private void fail(String message) { stopWorkout(message); ready = false; state = message; publish(); }
    private void publish() {
        long remaining = running && pendingOpcode == -1 ? Math.max(0, stepEndsAt - SystemClock.elapsedRealtime()) : 0;
        for (Listener listener : listeners) listener.onState(state, ready, running, stepIndex, remaining);
    }
    private void clearTimeout() { if (timeout != null) handler.removeCallbacks(timeout); timeout = null; }
    private void send(byte[] bytes) {
        if (gatt == null || control == null || pendingOpcode != -1 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { fail("Nem küldhető FTMS parancs."); return; }
        pendingOpcode = bytes[0] & 255;
        if (gatt.writeCharacteristic(control, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) != BluetoothGatt.GATT_SUCCESS) { pendingOpcode = -1; fail("FTMS parancs küldési hiba."); return; }
        timeout = () -> { pendingOpcode = -1; fail("Nem érkezett válasz a futópadtól."); };
        handler.postDelayed(timeout, 5000);
    }
    void startWorkout(Workout selected) {
        if (!ready || running || selected == null || !featureSupported || !rangeAvailable) return;
        for (Workout.Step step : selected.steps) {
            if (step.speedHundredths < minSpeed || step.speedHundredths > maxSpeed || (step.speedHundredths - minSpeed) % speedStep != 0) {
                state = "Nem támogatott sebesség: " + step.speedLabel() + " km/h. Tartomány: " + minSpeed / 100.0 + "–" + maxSpeed / 100.0 + " km/h, lépés: " + speedStep / 100.0;
                publish(); return;
            }
        }
        workout = selected; stepIndex = 0; running = true;
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Futópad edzés", NotificationManager.IMPORTANCE_LOW));
        Intent stop = new Intent(this, TreadmillService.class).setAction(STOP);
        PendingIntent stopIntent = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Futópad program fut").setContentText("Sebességvezérlés aktív; a futópadot a konzolon állítsd le.").addAction(android.R.drawable.ic_media_pause, "Program leállítása", stopIntent).build();
        startForeground(1, notification);
        state = "Vezérlés kérése"; publish();
        if (controlGranted) sendSpeed(); else send(new byte[]{0});
    }
    private void sendSpeed() {
        if (!running || workout == null) return;
        int speed = workout.steps.get(stepIndex).speedHundredths;
        state = "Sebesség beállítása: " + workout.steps.get(stepIndex).speedLabel() + " km/h"; publish();
        send(new byte[]{2, (byte) speed, (byte) (speed >> 8)});
    }
    private void advance() {
        handler.removeCallbacks(tick);
        stepIndex++;
        if (workout == null || stepIndex >= workout.steps.size()) stopWorkout("Program vége. A futópad tovább járhat; állítsd le a konzolon.");
        else sendSpeed();
    }
    void stopWorkout(String message) {
        handler.removeCallbacks(tick); clearTimeout(); pendingOpcode = -1;
        if (running) { running = false; workout = null; stopForeground(STOP_FOREGROUND_REMOVE); }
        state = message; publish();
    }
    @Override public void onDestroy() { disconnect(); super.onDestroy(); }
}
