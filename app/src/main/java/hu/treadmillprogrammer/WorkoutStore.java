package hu.treadmillprogrammer;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class WorkoutStore {
    private final File directory;
    WorkoutStore(Context context) {
        directory = new File(context.getFilesDir(), "programs");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Nem hozható létre a programkönyvtár.");
    }
    void save(Workout workout, String sourceCsv) throws Exception {
        File file = new File(directory, UUID.randomUUID() + ".json");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(workout.toJson().put("sourceCsv", sourceCsv).toString().getBytes(StandardCharsets.UTF_8));
        }
    }
    List<Workout> load() throws Exception {
        List<Workout> programs = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) for (File file : files) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            }
            programs.add(Workout.fromJson(new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8))));
        }
        programs.sort(Comparator.comparing(w -> w.name.toLowerCase()));
        return programs;
    }
}
