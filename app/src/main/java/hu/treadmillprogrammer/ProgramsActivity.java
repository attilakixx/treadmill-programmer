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
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ProgramsActivity extends Activity {
    private static final int BG = 0xff0b0d0b, SURFACE = 0xff181b17, PANEL = 0xff111410;
    private static final int YELLOW = 0xfff7d436, TEXT = 0xfff5f5ef, MUTED = 0xffa0a69a, BORDER = 0xff343a30;
    private static final int PICK_FILE = 1;
    private final List<ProgramEntry> programs = new ArrayList<>();
    private ProgramCatalog catalog;
    private TreadmillService service;
    private LinearLayout ownList, calorieList, intervalList, enduranceList;
    private TextView ownCount;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) { service = ((TreadmillService.LocalBinder) binder).service(); }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        catalog = new ProgramCatalog(this);
        buildUi();
        loadPrograms();
        bindService(new Intent(this, TreadmillService.class), connection, BIND_AUTO_CREATE);
        if (state == null && Intent.ACTION_SEND.equals(getIntent().getAction())) {
            Uri shared = getIntent().getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            if (shared == null && getIntent().getClipData() != null && getIntent().getClipData().getItemCount() > 0)
                shared = getIntent().getClipData().getItemAt(0).getUri();
            if (shared != null) importUri(shared);
            else showError("A megosztott fájl nem olvasható.");
        }
    }
    @Override protected void onResume() { super.onResume(); if (catalog != null) loadPrograms(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView result = new TextView(this); result.setText(value); result.setTextSize(size); result.setTextColor(color);
        result.setGravity(Gravity.CENTER_VERTICAL); return result;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable result = new GradientDrawable(); result.setColor(fill); result.setCornerRadius(dp(radius));
        if (stroke != 0) result.setStroke(dp(1), stroke); return result;
    }
    private Button button(String title, Runnable action, boolean primary) {
        Button result = new Button(this); result.setText(title); result.setAllCaps(false); result.setTextSize(15);
        result.setTypeface(Typeface.DEFAULT, Typeface.BOLD); result.setTextColor(primary ? BG : YELLOW);
        result.setMinHeight(dp(52)); result.setPadding(dp(16), dp(10), dp(16), dp(10));
        result.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55ffffff), shape(primary ? YELLOW : PANEL, primary ? 0 : BORDER, 14), null));
        result.setOnClickListener(v -> action.run()); return result;
    }
    private LinearLayout section(LinearLayout content, String title, String subtitle) {
        TextView heading = text(title, 23, TEXT); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(0, dp(30), 0, dp(4)); content.addView(heading);
        TextView sub = text(subtitle, 13, MUTED); content.addView(sub);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list); return list;
    }
    private void buildUi() {
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setNavigationBarContrastEnforced(false);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(30)); scroll.addView(content);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); content.addView(header);
        Button back = button("‹", this::finish, false); back.setTextSize(30); back.setContentDescription("Vissza");
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(14), 0, 0, 0);
        header.addView(titles);
        TextView title = text("Programkönyvtár", 26, TEXT); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); titles.addView(title);
        TextView subtitle = text("SAJÁT PROGRAMOK ÉS PRESETEK", 11, MUTED); subtitle.setLetterSpacing(.12f); titles.addView(subtitle);
        TextView intro = text("Válassz edzést vagy jelöld kedvencnek a csillaggal. A sebességek kiinduló minták; igazítsd őket a saját terhelhetőségedhez.", 14, MUTED);
        intro.setPadding(0, dp(22), 0, dp(18)); content.addView(intro);
        Button importButton = button("＋  CSV program importálása", this::pickFile, true); content.addView(importButton);
        ownList = section(content, "Saját programok", "A számítógépen írt és importált CSV-k");
        ownCount = text("", 13, MUTED); ownCount.setPadding(0, dp(10), 0, 0); content.addView(ownCount);
        calorieList = section(content, "Kalória", "Hosszabb, változó tempójú mozgás");
        intervalList = section(content, "Intervallum", "Gyors és könnyű szakaszok váltakozása");
        enduranceList = section(content, "Állóképesség", "Egyenletesebb, tartós terhelés");
        TextView note = text("Minden preset 5 perc bemelegítést és 5 perc levezetést tartalmaz. A nehézségi szintek tájékoztató jellegűek. A futópadot mindig a konzolon állítsd le.", 13, MUTED);
        note.setPadding(0, dp(28), 0, 0); content.addView(note);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            content.setPadding(dp(20) + bars.left, dp(18) + bars.top, dp(20) + bars.right, dp(30) + bars.bottom);
            return insets;
        });
        setContentView(scroll);
    }
    private void loadPrograms() {
        try {
            programs.clear(); programs.addAll(catalog.load()); refresh();
        } catch (Exception error) { showError("Programok olvasási hibája: " + error.getMessage()); }
    }
    private void refresh() {
        ownList.removeAllViews(); calorieList.removeAllViews(); intervalList.removeAllViews(); enduranceList.removeAllViews();
        String selectedId = catalog.selectedId(); int owned = 0;
        for (ProgramEntry entry : programs) {
            LinearLayout list;
            if (!entry.preset) { list = ownList; owned++; }
            else if (entry.category.equals("KALÓRIA")) list = calorieList;
            else if (entry.category.equals("INTERVALLUM")) list = intervalList;
            else list = enduranceList;
            LinearLayout card = ProgramCard.create(this, entry, entry.id.equals(selectedId), () -> choose(entry), () -> {
                catalog.toggleFavorite(entry.id); loadPrograms();
            });
            LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2); margins.topMargin = dp(14);
            list.addView(card, margins);
        }
        ownCount.setText(owned == 0 ? "Még nincs importált programod." : owned + " importált program");
    }
    private void choose(ProgramEntry entry) {
        if (service == null) { Toast.makeText(this, "A futópad szolgáltatás még indul. Próbáld újra.", Toast.LENGTH_SHORT).show(); return; }
        if (service.isRunning()) { Toast.makeText(this, "Edzés közben nem váltható program.", Toast.LENGTH_SHORT).show(); return; }
        catalog.select(entry.id); finish();
    }
    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
            DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/treadmill"));
        startActivityForResult(intent, PICK_FILE);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_FILE || result != RESULT_OK || data == null || data.getData() == null) return;
        importUri(data.getData());
    }
    private void importUri(Uri uri) {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("A fájl nem olvasható.");
            byte[] bytes = stream.readNBytes(256 * 1024 + 1);
            if (bytes.length > 256 * 1024) throw new IllegalArgumentException("A fájl túl nagy (maximum 256 KB).");
            String csv = new String(bytes, StandardCharsets.UTF_8);
            EditText nameInput = new EditText(this); nameInput.setSingleLine(true); nameInput.setHint("Program neve");
            new AlertDialog.Builder(this).setTitle("Program importálása").setView(nameInput)
                .setPositiveButton("Mentés", (dialog, which) -> {
                    try {
                        Workout workout = Workout.parse(nameInput.getText().toString(), csv);
                        String id = catalog.importCsv(workout, csv);
                        if (service != null && !service.isRunning()) catalog.select(id);
                        loadPrograms(); Toast.makeText(this, "Program elmentve", Toast.LENGTH_SHORT).show();
                    } catch (Exception error) { showError(error.getMessage()); }
                }).setNegativeButton("Mégse", null).show();
        } catch (Exception error) { showError(error.getMessage()); }
    }
    private void showError(String message) { new AlertDialog.Builder(this).setTitle("Hiba").setMessage(message).setPositiveButton("OK", null).show(); }
    @Override protected void onDestroy() { unbindService(connection); super.onDestroy(); }
}
