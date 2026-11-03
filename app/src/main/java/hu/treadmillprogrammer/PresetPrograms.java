package hu.treadmillprogrammer;

import java.util.ArrayList;
import java.util.List;

final class PresetPrograms {
    private PresetPrograms() {}

    private static ProgramEntry preset(String id, String name, String category, String level, String rows) {
        return new ProgramEntry(id, Workout.parse(name, "timeInterval(min),speed(kmph)\n" + rows), category, level, true, false);
    }

    static List<ProgramEntry> all() {
        List<ProgramEntry> result = new ArrayList<>();
        result.add(preset("calorie_easy", "Kalória • könnyű", "KALÓRIA", "KÖNNYŰ",
            "5,3.5\n5,4.5\n5,5.0\n5,4.5\n5,5.0\n5,3.5"));
        result.add(preset("calorie_medium", "Kalória • közepes", "KALÓRIA", "KÖZEPES",
            "5,4.0\n5,5.5\n3,7.0\n4,5.5\n3,7.0\n4,5.5\n3,7.0\n3,5.5\n5,4.0"));
        result.add(preset("calorie_hard", "Kalória • nehéz", "KALÓRIA", "NEHÉZ",
            "5,4.0\n5,6.0\n4,8.5\n3,6.0\n4,9.0\n3,6.0\n4,9.0\n3,6.0\n4,8.5\n5,4.0"));
        result.add(preset("interval_easy", "Intervallum • könnyű", "INTERVALLUM", "KÖNNYŰ",
            "5,3.5\n1,6.5\n2,4.0\n1,6.5\n2,4.0\n1,6.5\n2,4.0\n1,6.5\n2,4.0\n5,3.5"));
        result.add(preset("interval_medium", "Intervallum • közepes", "INTERVALLUM", "KÖZEPES",
            "5,4.0\n2,8.0\n2,4.5\n2,8.0\n2,4.5\n2,8.0\n2,4.5\n2,8.0\n2,4.5\n5,4.0"));
        result.add(preset("interval_hard", "Intervallum • nehéz", "INTERVALLUM", "NEHÉZ",
            "5,4.0\n2,10.0\n2,5.0\n2,10.0\n2,5.0\n2,10.5\n2,5.0\n2,10.5\n2,5.0\n2,10.0\n2,5.0\n5,4.0"));
        result.add(preset("endurance_easy", "Állóképesség • könnyű", "ÁLLÓKÉPESSÉG", "KÖNNYŰ",
            "5,3.5\n15,5.0\n5,3.5"));
        result.add(preset("endurance_medium", "Állóképesség • közepes", "ÁLLÓKÉPESSÉG", "KÖZEPES",
            "5,4.0\n10,6.5\n10,7.0\n10,6.5\n5,4.0"));
        result.add(preset("endurance_hard", "Állóképesség • nehéz", "ÁLLÓKÉPESSÉG", "NEHÉZ",
            "5,4.0\n10,7.5\n20,8.0\n10,7.5\n5,4.0"));
        return result;
    }
}
