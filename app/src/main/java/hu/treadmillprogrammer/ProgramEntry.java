package hu.treadmillprogrammer;

final class ProgramEntry {
    final String id;
    final Workout workout;
    final String category;
    final String level;
    final boolean preset;
    final boolean favorite;

    ProgramEntry(String id, Workout workout, String category, String level, boolean preset, boolean favorite) {
        this.id = id;
        this.workout = workout;
        this.category = category;
        this.level = level;
        this.preset = preset;
        this.favorite = favorite;
    }

    String eyebrow() { return category + (level.isEmpty() ? "" : "  /  " + level); }
}
