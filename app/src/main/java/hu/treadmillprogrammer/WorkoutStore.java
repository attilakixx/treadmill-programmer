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
    String save(Workout workout, String sourceCsv) throws Exception {
        String id = UUID.randomUUID().toString();
        File file = new File(directory, id + ".json");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(workout.toJson().put("sourceCsv", sourceCsv).toString().getBytes(StandardCharsets.UTF_8));
        }
        return id;
    }
    List<ProgramEntry> load() throws Exception {
        List<ProgramEntry> programs = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) for (File file : files) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            }
            String id = file.getName().substring(0, file.getName().length() - 5);
            programs.add(new ProgramEntry(id, Workout.fromJson(new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8))), "SAJÁT PROGRAM", "", false, false));
        }
        programs.sort(Comparator.comparing(w -> w.workout.name.toLowerCase(java.util.Locale.ROOT)));
        return programs;
    }
}
