package hu.treadmillprogrammer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Workout {
    final String name;
    final List<Step> steps;
    final long durationMs;

    static final class Step {
        final long durationMs;
        final int speedHundredths;
        Step(long durationMs, int speedHundredths) {
            this.durationMs = durationMs;
            this.speedHundredths = speedHundredths;
        }
        String speedLabel() { return String.format(Locale.ROOT, "%.2f", speedHundredths / 100.0); }
    }

    Workout(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
        long sum = 0;
        for (Step step : steps) sum += step.durationMs;
        durationMs = sum;
    }

    static Workout parse(String name, String csv) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Adj nevet a programnak.");
        if (name.length() > 80) throw new IllegalArgumentException("A név legfeljebb 80 karakter lehet.");
        String[] lines = csv.replace("\uFEFF", "").split("\\R");
        List<Step> result = new ArrayList<>();
        boolean firstLineSeen = false;
        long total = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (!firstLineSeen) {
                firstLineSeen = true;
                if (line.replace(" ", "").equalsIgnoreCase("timeInterval(min),speed(kmph)")) continue;
            }
            String[] values = line.split(",", -1);
            if (values.length != 2) throw new IllegalArgumentException((i + 1) + ". sor: két, vesszővel elválasztott szám kell.");
            try {
                long ms = new BigDecimal(values[0].trim()).multiply(BigDecimal.valueOf(60000)).longValueExact();
                int speed = new BigDecimal(values[1].trim()).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
                if (ms <= 0 || speed <= 0 || speed > 2500) throw new ArithmeticException();
                total = Math.addExact(total, ms);
                if (total > 8 * 60 * 60 * 1000L) throw new IllegalArgumentException("A program legfeljebb 8 órás lehet.");
                result.add(new Step(ms, speed));
            } catch (NumberFormatException | ArithmeticException ex) {
                throw new IllegalArgumentException((i + 1) + ". sor: pozitív idő és 0–25 km/h sebesség kell (ponttal írt tizedesek).", ex);
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("A program üres.");
        return new Workout(name.trim(), result);
    }

    JSONObject toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("name", name);
        JSONArray rows = new JSONArray();
        for (Step step : steps) rows.put(new JSONObject().put("durationMs", step.durationMs).put("speedHundredths", step.speedHundredths));
        root.put("steps", rows);
        return root;
    }

    static Workout fromJson(JSONObject root) throws JSONException {
        List<Step> rows = new ArrayList<>();
        JSONArray data = root.getJSONArray("steps");
        for (int i = 0; i < data.length(); i++) {
            JSONObject row = data.getJSONObject(i);
            rows.add(new Step(row.getLong("durationMs"), row.getInt("speedHundredths")));
        }
        return new Workout(root.getString("name"), rows);
    }

    String durationLabel() {
        long seconds = durationMs / 1000;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    String maxSpeedLabel() {
        int maximum = 0;
        for (Step step : steps) maximum = Math.max(maximum, step.speedHundredths);
        return String.format(Locale.ROOT, "%.2f", maximum / 100.0);
    }

    String estimatedDistanceLabel() {
        long speedTime = 0;
        for (Step step : steps) speedTime += (long) step.speedHundredths * step.durationMs;
        return BigDecimal.valueOf(speedTime)
            .divide(BigDecimal.valueOf(360_000_000L), 2, RoundingMode.HALF_UP)
            .toPlainString();
    }

    boolean samePlan(Workout other) {
        if (other == null || !name.equals(other.name) || steps.size() != other.steps.size()) return false;
        for (int i = 0; i < steps.size(); i++) {
            Step left = steps.get(i), right = other.steps.get(i);
            if (left.durationMs != right.durationMs || left.speedHundredths != right.speedHundredths) return false;
        }
        return true;
    }
}
