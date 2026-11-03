package hu.treadmillprogrammer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ProgramCatalog {
    private static final String FAVORITES = "favorites", SELECTED = "selected_program_id";
    private final SharedPreferences preferences;
    private final WorkoutStore store;

    ProgramCatalog(Context context) {
        preferences = context.getSharedPreferences("program_catalog", Context.MODE_PRIVATE);
        store = new WorkoutStore(context);
    }

    List<ProgramEntry> load() throws Exception {
        List<ProgramEntry> all = new ArrayList<>(store.load());
        all.addAll(PresetPrograms.all());
        Set<String> favorites = preferences.getStringSet(FAVORITES, Set.of());
        List<ProgramEntry> result = new ArrayList<>();
        for (ProgramEntry entry : all) result.add(new ProgramEntry(entry.id, entry.workout, entry.category, entry.level, entry.preset, favorites.contains(entry.id)));
        return result;
    }

    String importCsv(Workout workout, String csv) throws Exception { return store.save(workout, csv); }
    String selectedId() { return preferences.getString(SELECTED, null); }
    void select(String id) { preferences.edit().putString(SELECTED, id).apply(); }
    void toggleFavorite(String id) {
        Set<String> favorites = new HashSet<>(preferences.getStringSet(FAVORITES, Set.of()));
        if (!favorites.add(id)) favorites.remove(id);
        preferences.edit().putStringSet(FAVORITES, favorites).apply();
    }
}
