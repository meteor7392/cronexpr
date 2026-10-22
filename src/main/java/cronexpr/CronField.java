package cronexpr;

import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A single parsed cron field.
 */
public final class CronField {
    private final int min;
    private final int max;
    private final SortedSet<Integer> values;
    private final boolean wildcard;

    public CronField(int min, int max, SortedSet<Integer> values, boolean wildcard) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("cron field cannot be empty");
        }
        this.min = min;
        this.max = max;
        this.values = new TreeSet<>(values);
        this.wildcard = wildcard;
    }

    public boolean contains(int value) {
        return values.contains(value);
    }

    public SortedSet<Integer> values() {
        return values;
    }

    public boolean wildcard() {
        return wildcard;
    }

    public int first() {
        return values.first();
    }

    public Integer nextAtOrAfter(int value) {
        SortedSet<Integer> tail = values.tailSet(value);
        return tail.isEmpty() ? null : tail.first();
    }

    public Integer nextAfter(int value) {
        SortedSet<Integer> tail = values.tailSet(value + 1);
        return tail.isEmpty() ? null : tail.first();
    }

    public static CronField parse(String raw, int min, int max, Map<String, Integer> names) {
        String field = raw == null ? "" : raw.trim();
        if (field.isEmpty()) {
            throw new IllegalArgumentException("empty cron field");
        }

        boolean wildcard = field.equals("*") || field.equals("?");
        if (wildcard) {
            SortedSet<Integer> all = new TreeSet<>();
            for (int i = min; i <= max; i++) {
                all.add(i);
            }
            return new CronField(min, max, all, true);
        }

        SortedSet<Integer> collected = new TreeSet<>();
        for (String part : field.split(",")) {
            String piece = part.trim();
            if (piece.isEmpty()) {
                throw new IllegalArgumentException("empty list element in cron field: " + field);
            }

            String base = piece;
            int step = 1;
            int slash = piece.indexOf('/');
            if (slash >= 0) {
                base = piece.substring(0, slash).trim();
                step = parsePositiveInt(piece.substring(slash + 1).trim());
                if (base.isEmpty()) {
                    throw new IllegalArgumentException("missing range before step: " + piece);
                }
            }

            int rangeMin;
            int rangeMax;
            if (base.equals("*") || base.equals("?")) {
                rangeMin = min;
                rangeMax = max;
            } else if (base.contains("-")) {
                String[] range = base.split("-", -1);
                if (range.length != 2) {
                    throw new IllegalArgumentException("invalid range: " + base);
                }
                rangeMin = parseValue(range[0].trim(), names, min, max);
                rangeMax = parseValue(range[1].trim(), names, min, max);
            } else {
                int value = parseValue(base, names, min, max);
                rangeMin = value;
                rangeMax = slash >= 0 ? max : value;
            }

            if (rangeMin < min || rangeMax > max || rangeMin > rangeMax) {
                throw new IllegalArgumentException("cron field value out of range: " + piece);
            }

            for (int value = rangeMin; value <= rangeMax; value++) {
                if ((value - rangeMin) % step == 0) {
                    collected.add(value);
                }
            }
        }

        if (collected.isEmpty()) {
            throw new IllegalArgumentException("cron field produced no values: " + field);
        }
        return new CronField(min, max, collected, false);
    }

    private static int parseValue(String token, Map<String, Integer> names, int min, int max) {
        String text = token.trim().toLowerCase(Locale.ROOT);
        if (names != null && names.containsKey(text)) {
            int value = names.get(text);
            if (value < min || value > max) {
                throw new IllegalArgumentException("named value out of range: " + token);
            }
            return value;
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid cron field value: " + token, e);
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException("cron field value out of range: " + token);
        }
        return value;
    }

    private static int parsePositiveInt(String text) {
        try {
            int value = Integer.parseInt(text);
            if (value <= 0) {
                throw new IllegalArgumentException("step must be positive: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid step value: " + text, e);
        }
    }
}
