package hu.treadmillprogrammer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class SpeedChart extends View {
    private static final int YELLOW = 0xfff7d436, MUTED = 0xff92978b, GRID = 0xff30352c;
    private Workout workout;
    private final Paint paint = new Paint(3);
    SpeedChart(Context context) { super(context); }
    void setWorkout(Workout workout) { this.workout = workout; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (workout == null) return;
        float density = getResources().getDisplayMetrics().density;
        float left = 38 * density, top = 20 * density, right = getWidth() - 12 * density, bottom = getHeight() - 27 * density;
        int max = 0;
        for (Workout.Step step : workout.steps) max = Math.max(max, step.speedHundredths);
        float scaleMax = (float) (Math.ceil(max / 500.0) * 500.0);
        if (scaleMax == 0) scaleMax = 500;
        paint.setStrokeWidth(density); paint.setColor(GRID); paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i <= 2; i++) {
            float y = bottom - (bottom - top) * i / 2f;
            canvas.drawLine(left, y, right, y, paint);
        }
        paint.setColor(MUTED); paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("0", 5 * density, bottom + 4 * density, paint);
        canvas.drawText(String.valueOf((int) (scaleMax / 100)), 5 * density, top + 4 * density, paint);
        canvas.drawText("km/h", left, getHeight() - 4 * density, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(workout.durationLabel(), right, getHeight() - 4 * density, paint);
        Path line = new Path();
        line.moveTo(left, bottom);
        float x = left;
        for (Workout.Step step : workout.steps) {
            float y = bottom - step.speedHundredths / scaleMax * (bottom - top);
            float nextX = x + (right - left) * (float) step.durationMs / workout.durationMs;
            line.lineTo(x, y);
            line.lineTo(nextX, y);
            x = nextX;
        }
        Path area = new Path(line);
        area.lineTo(right, bottom);
        area.close();
        paint.setStyle(Paint.Style.FILL); paint.setColor(0x22f7d436);
        canvas.drawPath(area, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f * density); paint.setColor(YELLOW);
        canvas.drawPath(line, paint);
    }
}
