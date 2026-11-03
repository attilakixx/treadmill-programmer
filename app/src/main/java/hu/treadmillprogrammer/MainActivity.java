package hu.treadmillprogrammer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BG = 0xff0b0d0b, SURFACE = 0xff181b17, PANEL = 0xff111410;
    private static final int YELLOW = 0xfff7d436, TEXT = 0xfff5f5ef, MUTED = 0xffa0a69a, BORDER = 0xff343a30;
    private static final int AUTO_PERMISSIONS = 3;
    private final List<ProgramEntry> programs = new ArrayList<>();
    private ProgramEntry selected;
    private ProgramCatalog catalog;
    private TreadmillService service;
    private TextView stateText, statusDot, selectedSummary, programCountText;
    private LinearLayout programList;
    private Button startButton, stopButton, liveButton;
    private final TreadmillService.Listener listener = (state, ready, running, step, remaining) -> runOnUiThread(() -> {
        stateText.setText(state + (running && remaining > 0 ? "\nHátralévő szakaszidő: " + (remaining / 1000) + " mp" : ""));
        statusDot.setTextColor(ready ? YELLOW : MUTED);
        stateText.setTextColor(ready ? TEXT : MUTED);
        startButton.setEnabled(ready && !running && selected != null);
        startButton.setAlpha(startButton.isEnabled() ? 1f : .45f);
        if (liveButton != null) {
            liveButton.setText(running && service != null && service.isLiveSession() ? "Élő edzés megnyitása" : "Élő vezérlés  ›");
            liveButton.setEnabled(ready && (!running || service != null && service.isLiveSession()));
            liveButton.setAlpha(liveButton.isEnabled() ? 1f : .45f);
        }
        stopButton.setEnabled(running);
        stopButton.setVisibility(running ? View.VISIBLE : View.GONE);
        if (running) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    });
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((TreadmillService.LocalBinder) binder).service(); service.addListener(listener);
            if (service.isRunning()) {
                for (ProgramEntry program : programs) if (program.workout.samePlan(service.currentWorkout())) { selected = program; break; }
                refreshPrograms();
            }
            autoConnectRemembered();
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; stateText.setText("Futópad: szolgáltatás megszakadt"); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        catalog = new ProgramCatalog(this);
        buildUi();
        loadPrograms();
        bindService(new Intent(this, TreadmillService.class), connection, BIND_AUTO_CREATE);
    }
    @Override protected void onResume() {
        super.onResume();
        if (catalog != null) loadPrograms();
        autoConnectRemembered();
    }
    private void loadPrograms() {
        try {
            programs.clear(); programs.addAll(catalog.load());
            String selectedId = catalog.selectedId(); selected = null;
            for (ProgramEntry entry : programs) if (entry.id.equals(selectedId)) { selected = entry; break; }
            if (selected == null && !programs.isEmpty()) { selected = programs.get(0); catalog.select(selected.id); }
            if (service != null && service.isRunning()) for (ProgramEntry entry : programs) {
                if (entry.workout.samePlan(service.currentWorkout())) { selected = entry; break; }
            }
            refreshPrograms();
        } catch (Exception error) { showError("Programok olvasási hibája: " + error.getMessage()); }
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
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button settingsIcon = button("⚙", this::openBluetoothSettings);
        settingsIcon.setTextSize(23); settingsIcon.setContentDescription("Bluetooth beállítások");
        header.addView(settingsIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        content.addView(header);
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
        Button scanButton = button("Bluetooth beállítások  ›", this::openBluetoothSettings);
        LinearLayout.LayoutParams scanMargins = new LinearLayout.LayoutParams(-1, -2);
        scanMargins.topMargin = dp(15); connectionCard.addView(scanButton, scanMargins);

        LinearLayout libraryHeader = new LinearLayout(this);
        libraryHeader.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams headerMargins = new LinearLayout.LayoutParams(-1, -2);
        headerMargins.topMargin = dp(32); content.addView(libraryHeader, headerMargins);
        TextView libraryTitle = label("Kedvencek", 25);
        libraryTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        libraryHeader.addView(libraryTitle, new LinearLayout.LayoutParams(0, -2, 1));
        programCountText = label("0 kedvenc", 12); programCountText.setTextColor(MUTED);
        libraryHeader.addView(programCountText);
        Button importButton = styledButton("Programkönyvtár  ›", this::openProgramLibrary, true);
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
        liveButton = button("Élő vezérlés  ›", () -> startActivity(new Intent(this, LiveControlActivity.class)));
        LinearLayout.LayoutParams liveMargins = new LinearLayout.LayoutParams(-1, -2);
        liveMargins.topMargin = dp(8); actionBar.addView(liveButton, liveMargins);
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
        int favoriteCount = 0;
        for (ProgramEntry entry : programs) if (entry.favorite) favoriteCount++;
        programCountText.setText(favoriteCount + " kedvenc");
        if (favoriteCount == 0) {
            TextView empty = label("Jelölj meg programokat a könyvtárban a ☆ gombbal. Itt gyorsan eléred a kedvenceidet.", 15);
            empty.setTextColor(MUTED); empty.setPadding(dp(16), dp(20), dp(16), dp(20));
            empty.setBackground(shape(SURFACE, BORDER, 18));
            LinearLayout.LayoutParams emptyMargins = new LinearLayout.LayoutParams(-1, -2);
            emptyMargins.topMargin = dp(14); programList.addView(empty, emptyMargins);
        }
        for (ProgramEntry entry : programs) {
            if (!entry.favorite) continue;
            LinearLayout card = ProgramCard.create(this, entry, selected != null && entry.id.equals(selected.id), () -> {
                if (service != null && service.isRunning()) { Toast.makeText(this, "Edzés közben nem váltható program.", Toast.LENGTH_SHORT).show(); return; }
                catalog.select(entry.id); loadPrograms();
            }, () -> { catalog.toggleFavorite(entry.id); loadPrograms(); });
            LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2);
            margins.topMargin = dp(14);
            programList.addView(card, margins);
        }
        selectedSummary.setText(selected == null ? "Nincs kiválasztott program" : "Kiválasztva: " + selected.workout.name + "  •  " + selected.workout.durationLabel());
        startButton.setEnabled(selected != null && service != null && service.isReady() && !service.isRunning());
        startButton.setAlpha(startButton.isEnabled() ? 1f : .45f);
        if (liveButton != null) {
            boolean live = service != null && service.isLiveSession();
            liveButton.setText(live ? "Élő edzés megnyitása" : "Élő vezérlés  ›");
            liveButton.setEnabled(service != null && service.isReady() && (!service.isRunning() || live));
            liveButton.setAlpha(liveButton.isEnabled() ? 1f : .45f);
        }
    }
    private boolean hasBluetoothPermissions() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
    private void openBluetoothSettings() { startActivity(new Intent(this, BluetoothSettingsActivity.class)); }
    private void openProgramLibrary() { startActivity(new Intent(this, ProgramsActivity.class)); }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == AUTO_PERMISSIONS && hasBluetoothPermissions()) autoConnectRemembered();
    }
    private void startWorkout() {
        if (service == null || selected == null || !service.isReady()) return;
        Intent intent = new Intent(this, TreadmillService.class);
        startForegroundService(intent);
        service.startWorkout(selected.workout);
    }
    private void showError(String message) { new AlertDialog.Builder(this).setTitle("Hiba").setMessage(message).setPositiveButton("OK", null).show(); }
    @Override protected void onDestroy() {
        if (service != null) service.removeListener(listener); unbindService(connection); super.onDestroy();
    }
}
