package hu.treadmillprogrammer;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class ProgramCard {
    private static final int SURFACE = 0xff181b17, PANEL = 0xff111410, YELLOW = 0xfff7d436;
    private static final int TEXT = 0xfff5f5ef, MUTED = 0xffa0a69a, BORDER = 0xff343a30;
    private ProgramCard() {}

    private static int dp(Activity activity, int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private static GradientDrawable shape(Activity activity, int fill, int stroke, int radius) {
        GradientDrawable result = new GradientDrawable(); result.setColor(fill); result.setCornerRadius(dp(activity, radius));
        if (stroke != 0) result.setStroke(dp(activity, 1), stroke);
        return result;
    }
    private static TextView text(Activity activity, String value, int size, int color) {
        TextView result = new TextView(activity); result.setText(value); result.setTextSize(size); result.setTextColor(color);
        result.setGravity(Gravity.CENTER_VERTICAL); return result;
    }
    private static LinearLayout stat(Activity activity, String title, String value) {
        LinearLayout box = new LinearLayout(activity); box.setOrientation(LinearLayout.VERTICAL);
        TextView caption = text(activity, title, 10, MUTED); caption.setLetterSpacing(.1f); box.addView(caption);
        TextView number = text(activity, value, 18, TEXT); number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setPadding(0, dp(activity, 4), 0, 0); box.addView(number); return box;
    }
    static LinearLayout create(Activity activity, ProgramEntry entry, boolean selected, Runnable onSelect, Runnable onFavorite) {
        int pad = dp(activity, 16);
        LinearLayout card = new LinearLayout(activity); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable background = shape(activity, SURFACE, selected ? YELLOW : BORDER, 20);
        if (selected) background.setStroke(dp(activity, 2), YELLOW);
        card.setBackground(background);

        LinearLayout top = new LinearLayout(activity); top.setGravity(Gravity.CENTER_VERTICAL); card.addView(top);
        TextView eyebrow = text(activity, entry.eyebrow(), 11, selected ? YELLOW : MUTED);
        eyebrow.setLetterSpacing(.1f); top.addView(eyebrow, new LinearLayout.LayoutParams(0, -2, 1));
        Button star = new Button(activity); star.setAllCaps(false); star.setText(entry.favorite ? "★" : "☆");
        star.setTextSize(26); star.setTextColor(YELLOW); star.setContentDescription(entry.favorite ? "Eltávolítás a kedvencekből" : "Kedvencekhez adás");
        star.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55ffffff), shape(activity, PANEL, BORDER, 12), null));
        star.setOnClickListener(v -> onFavorite.run()); top.addView(star, new LinearLayout.LayoutParams(dp(activity, 46), dp(activity, 46)));

        TextView name = text(activity, entry.workout.name, 20, TEXT); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setPadding(0, dp(activity, 5), 0, dp(activity, 14)); card.addView(name);
        LinearLayout stats = new LinearLayout(activity); card.addView(stats);
        stats.addView(stat(activity, "IDŐTARTAM", entry.workout.durationLabel()), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat(activity, "MAX. SEBESSÉG", entry.workout.maxSpeedLabel() + " km/h"), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout distance = new LinearLayout(activity); distance.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams distanceMargins = new LinearLayout.LayoutParams(-1, -2);
        distanceMargins.topMargin = dp(activity, 13); card.addView(distance, distanceMargins);
        TextView distanceCaption = text(activity, "BECSÜLT TÁVOLSÁG", 10, MUTED);
        distanceCaption.setLetterSpacing(.1f);
        distance.addView(distanceCaption, new LinearLayout.LayoutParams(0, -2, 1));
        TextView distanceValue = text(activity, entry.workout.estimatedDistanceLabel() + " km", 18, YELLOW);
        distanceValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD); distance.addView(distanceValue);
        SpeedChart chart = new SpeedChart(activity); chart.setWorkout(entry.workout);
        chart.setBackground(shape(activity, PANEL, 0, 14)); chart.setPadding(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6));
        LinearLayout.LayoutParams chartMargins = new LinearLayout.LayoutParams(-1, dp(activity, 150)); chartMargins.topMargin = dp(activity, 15);
        card.addView(chart, chartMargins);
        TextView choice = text(activity, selected ? "●  KIVÁLASZTVA" : "○  Program kiválasztása", 13, selected ? YELLOW : MUTED);
        choice.setTypeface(Typeface.DEFAULT, Typeface.BOLD); choice.setPadding(0, dp(activity, 14), 0, dp(activity, 2));
        choice.setOnClickListener(v -> onSelect.run()); card.addView(choice);
        card.setOnClickListener(v -> onSelect.run());
        return card;
    }
}
