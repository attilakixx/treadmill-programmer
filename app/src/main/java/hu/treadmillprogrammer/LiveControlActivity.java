package hu.treadmillprogrammer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class LiveControlActivity extends Activity {
    private static final int BG = 0xff0b0d0b, SURFACE = 0xff181b17, PANEL = 0xff111410;
    private static final int YELLOW = 0xfff7d436, TEXT = 0xfff5f5ef, MUTED = 0xffa0a69a, BORDER = 0xff343a30;
    private static final String PREFS = "live_control";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TreadmillService service;
    private int[] speeds;
    private int initialSlot;
    private float weightKg;
    private TextView stateText, timerText, speedText, weightText;
    private Button startButton, stopButton;
    private Button[] speedButtons;
    private final Runnable timerTick = new Runnable() {
        @Override public void run() {
            if (service != null && service.isLiveSession()) {
                timerText.setText(formatElapsed(service.liveElapsedMs()));
                handler.postDelayed(this, 250);
            }
        }
    };
    private final TreadmillService.Listener listener = (state, ready, running, step, remaining) -> runOnUiThread(() -> {
        stateText.setText(state);
        stateText.setTextColor(ready ? TEXT : MUTED);
        updateControls();
    });
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((TreadmillService.LocalBinder) binder).service();
            service.addListener(listener); updateControls();
            if (service.isLiveSession()) handler.post(timerTick);
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; stateText.setText("Futópad-kapcsolat megszakadt"); updateControls(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        var prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        speeds = new int[]{prefs.getInt("speed_0", 400), prefs.getInt("speed_1", 600), prefs.getInt("speed_2", 800)};
        weightKg = prefs.getFloat("weight_kg", 70f);
        buildUi(); renderSpeedButtons();
        bindService(new Intent(this, TreadmillService.class), connection, BIND_AUTO_CREATE);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL); return view;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable result = new GradientDrawable(); result.setColor(fill); result.setCornerRadius(dp(radius));
        if (stroke != 0) result.setStroke(dp(1), stroke); return result;
    }
    private Button button(String title, Runnable action, boolean primary) {
        Button view = new Button(this); view.setText(title); view.setAllCaps(false); view.setTextSize(16);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); view.setTextColor(primary ? BG : YELLOW);
        view.setMinHeight(dp(58)); view.setPadding(dp(15), dp(10), dp(15), dp(10));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55ffffff), shape(primary ? YELLOW : PANEL, primary ? 0 : BORDER, 16), null));
        view.setOnClickListener(v -> action.run()); return view;
    }
    private void buildUi() {
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setNavigationBarContrastEnforced(false);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(30)); scroll.addView(content);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); content.addView(header);
        Button back = button("‹", this::back, false); back.setTextSize(30); back.setContentDescription("Vissza");
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text("Élő vezérlés", 27, TEXT); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(dp(14), 0, 0, 0); header.addView(title);
        LinearLayout statusCard = new LinearLayout(this); statusCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        statusCard.setBackground(shape(SURFACE, BORDER, 16)); LinearLayout.LayoutParams statusMargins = new LinearLayout.LayoutParams(-1, -2);
        statusMargins.topMargin = dp(20); content.addView(statusCard, statusMargins);
        stateText = text("Futópad-kapcsolat ellenőrzése…", 14, MUTED); statusCard.addView(stateText);

        timerText = text("00:00:00", 52, TEXT); timerText.setGravity(Gravity.CENTER);
        timerText.setTypeface(Typeface.DEFAULT, Typeface.BOLD); LinearLayout.LayoutParams timerMargins = new LinearLayout.LayoutParams(-1, -2);
        timerMargins.topMargin = dp(38); content.addView(timerText, timerMargins);
        TextView elapsedLabel = text("ELTELT EDZÉS IDŐ", 11, MUTED); elapsedLabel.setGravity(Gravity.CENTER);
        elapsedLabel.setLetterSpacing(.15f); content.addView(elapsedLabel);
        speedText = text("— km/h", 26, YELLOW); speedText.setGravity(Gravity.CENTER); speedText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams speedMargins = new LinearLayout.LayoutParams(-1, -2); speedMargins.topMargin = dp(22); content.addView(speedText, speedMargins);

        LinearLayout weightRow = new LinearLayout(this); weightRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams weightMargins = new LinearLayout.LayoutParams(-1, -2); weightMargins.topMargin = dp(24); content.addView(weightRow, weightMargins);
        weightText = text("Kalóriabecslés · " + weightLabel() + " kg", 13, MUTED); weightRow.addView(weightText, new LinearLayout.LayoutParams(0, -2, 1));
        Button weightButton = button("Testsúly", this::editWeight, false); weightButton.setTextSize(13); weightButton.setMinHeight(dp(42)); weightRow.addView(weightButton);

        TextView hint = text("Kedvenc sebességek  ·  hosszan nyomva módosítható", 13, MUTED);
        hint.setPadding(0, dp(28), 0, dp(8)); content.addView(hint);
        speedButtons = new Button[3];
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            Button speedButton = button("", () -> speedTapped(slot), false);
            speedButton.setTextSize(24); speedButtons[i] = speedButton;
            speedButton.setOnLongClickListener(v -> { editSpeed(slot); return true; });
            LinearLayout.LayoutParams buttonMargins = new LinearLayout.LayoutParams(-1, dp(78)); buttonMargins.topMargin = dp(9);
            content.addView(speedButton, buttonMargins);
        }
        startButton = button("Élő edzés indítása", this::startSession, true);
        LinearLayout.LayoutParams startMargins = new LinearLayout.LayoutParams(-1, -2); startMargins.topMargin = dp(22); content.addView(startButton, startMargins);
        stopButton = button("Edzés befejezése és statisztika", this::confirmFinish, false);
        LinearLayout.LayoutParams stopMargins = new LinearLayout.LayoutParams(-1, -2); stopMargins.topMargin = dp(10); content.addView(stopButton, stopMargins);
        TextView safety = text("A sebesség gombok vezérlik a futópadot. A befejezés nem állítja le a szalagot; azt a konzolon vagy a biztonsági kulccsal állítsd le.", 13, MUTED);
        safety.setPadding(0, dp(20), 0, 0); content.addView(safety);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            content.setPadding(dp(20) + bars.left, dp(18) + bars.top, dp(20) + bars.right, dp(30) + bars.bottom); return insets;
        });
        setContentView(scroll);
    }
    private void renderSpeedButtons() {
        for (int i = 0; i < speedButtons.length; i++) {
            String prefix = service != null && !service.isLiveSession() && initialSlot == i ? "●  " : "";
            speedButtons[i].setText(prefix + String.format(Locale.ROOT, "%.1f km/h", speeds[i] / 100.0));
            boolean canChoose = service != null && service.isReady() && (!service.isRunning() || service.isLiveSession() && !service.commandPending());
            speedButtons[i].setEnabled(canChoose);
            speedButtons[i].setAlpha(speedButtons[i].isEnabled() ? 1f : .6f);
        }
    }
    private void updateControls() {
        if (service == null) return;
        boolean live = service.isLiveSession();
        if (live) {
            timerText.setText(formatElapsed(service.liveElapsedMs()));
            speedText.setText(service.liveSpeed() > 0 ? String.format(Locale.ROOT, "%.2f km/h", service.liveSpeed() / 100.0) : "Sebesség beállítása…");
            handler.removeCallbacks(timerTick); handler.post(timerTick);
        } else {
            speedText.setText(String.format(Locale.ROOT, "Induló sebesség · %.1f km/h", speeds[initialSlot] / 100.0));
        }
        startButton.setVisibility(live ? View.GONE : View.VISIBLE);
        startButton.setEnabled(service.isReady() && !service.isRunning());
        startButton.setAlpha(startButton.isEnabled() ? 1f : .45f);
        stopButton.setVisibility(live ? View.VISIBLE : View.GONE);
        stopButton.setEnabled(live && !service.commandPending());
        renderSpeedButtons();
    }
    private String formatElapsed(long ms) {
        long seconds = ms / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
    private String weightLabel() { return String.format(Locale.ROOT, "%.1f", weightKg); }
    private void startSession() {
        if (service == null || !service.isReady() || service.isRunning()) return;
        new AlertDialog.Builder(this).setTitle("Élő edzés indítása")
            .setMessage("A futópadot előbb indítsd el a konzolon. A PaceLab ezután beállítja a kiválasztott sebességet.")
            .setNegativeButton("Mégse", null).setPositiveButton("Indítás", (d, w) -> {
                startForegroundService(new Intent(this, TreadmillService.class));
                service.startLiveSession(speeds[initialSlot]); updateControls();
            }).show();
    }
    private void speedTapped(int slot) {
        if (service == null || !service.isLiveSession()) { initialSlot = slot; updateControls(); return; }
        if (!service.setLiveSpeed(speeds[slot])) Toast.makeText(this, "A gép nem támogatja ezt a sebességet vagy még dolgozza fel az előző parancsot.", Toast.LENGTH_SHORT).show();
        updateControls();
    }
    private void editSpeed(int slot) {
        EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.ROOT, "%.1f", speeds[slot] / 100.0));
        new AlertDialog.Builder(this).setTitle("Kedvenc sebesség " + (slot + 1)).setMessage("Sebesség km/h-ban")
            .setView(input).setNegativeButton("Mégse", null).setPositiveButton("Mentés", (d, w) -> {
                try {
                    int value = Math.round(Float.parseFloat(input.getText().toString().trim().replace(',', '.')) * 100);
                    if (value < 100 || value > 2500 || value % 10 != 0 || service != null && service.isReady() && !service.supportsSpeed(value))
                        throw new IllegalArgumentException();
                    speeds[slot] = value; getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("speed_" + slot, value).apply(); renderSpeedButtons();
                } catch (Exception error) { Toast.makeText(this, "Adj meg 1–25 km/h közötti, tized km/h-ra végződő támogatott sebességet.", Toast.LENGTH_LONG).show(); }
            }).show();
    }
    private void editWeight() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(weightLabel());
        new AlertDialog.Builder(this).setTitle("Testsúly a kalóriabecsléshez").setMessage("Testsúly kilogrammban")
            .setView(input).setNegativeButton("Mégse", null).setPositiveButton("Mentés", (d, w) -> {
                try {
                    float value = Float.parseFloat(input.getText().toString().trim().replace(',', '.'));
                    if (value < 30 || value > 250) throw new IllegalArgumentException();
                    weightKg = value; getSharedPreferences(PREFS, MODE_PRIVATE).edit().putFloat("weight_kg", value).apply();
                    weightText.setText("Kalóriabecslés · " + weightLabel() + " kg");
                } catch (Exception error) { Toast.makeText(this, "A testsúly 30 és 250 kg között lehet.", Toast.LENGTH_LONG).show(); }
            }).show();
    }
    private void confirmFinish() {
        new AlertDialog.Builder(this).setTitle("Edzés befejezése?")
            .setMessage("A PaceLab lezárja a mérést és megmutatja az összesítést. A futópad szalagja tovább járhat.")
            .setNegativeButton("Vissza", null).setPositiveButton("Befejezés", (d, w) -> finishSession()).show();
    }
    private void finishSession() {
        if (service == null) return;
        SessionStats stats = service.finishLiveSession(weightKg); handler.removeCallbacks(timerTick); updateControls();
        if (stats == null) return;
        String message = "Időtartam: " + stats.durationLabel()
            + "\nTávolság: " + String.format(Locale.ROOT, "%.2f km", stats.distanceKm)
            + "\nÁtlagsebesség: " + String.format(Locale.ROOT, "%.2f km/h", stats.averageSpeedKmh)
            + "\nÁtlagtempó: " + stats.paceLabel()
            + "\nMax. sebesség: " + String.format(Locale.ROOT, "%.2f km/h", stats.maxSpeedHundredths / 100.0)
            + "\nBecsült energia: " + String.format(Locale.ROOT, "%.0f kcal", stats.calories)
            + "\nSebességváltások: " + stats.speedChanges
            + "\n\nA távolság a küldött célsebességekből, a kalória a testsúlyból és MET aktivitásbecslésből készült.";
        new AlertDialog.Builder(this).setTitle("Edzés összesítése").setMessage(message).setPositiveButton("Rendben", null).show();
    }
    private void back() {
        if (service != null && service.isLiveSession()) confirmFinish(); else finish();
    }
    @Override public void onBackPressed() { back(); }
    @Override protected void onDestroy() {
        handler.removeCallbacks(timerTick);
        if (service != null) service.removeListener(listener);
        unbindService(connection); super.onDestroy();
    }
}
