package cronexpr;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A parsed five-field cron expression.
 */
public final class CronExpression {
    private final CronField minute;
    private final CronField hour;
    private final CronField dayOfMonth;
    private final CronField month;
    private final CronField dayOfWeek;

    private final boolean dayOfMonthRestricted;
    private final boolean dayOfWeekRestricted;

    private CronExpression(CronField minute, CronField hour, CronField dayOfMonth,
                           CronField month, CronField dayOfWeek) {
        this.minute = minute;
        this.hour = hour;
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.dayOfWeek = dayOfWeek;
        this.dayOfMonthRestricted = !dayOfMonth.wildcard();
        this.dayOfWeekRestricted = !dayOfWeek.wildcard();
    }

    public static CronExpression parse(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("cron expression cannot be null");
        }
        String expr = expression.trim();
        if (expr.isEmpty()) {
            throw new IllegalArgumentException("cron expression cannot be empty");
        }

        String expanded = expandAlias(expr);
        String[] parts = expanded.trim().split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "expected 5 cron fields, got " + parts.length + ": " + expression);
        }

        CronField minute = CronField.parse(parts[0], 0, 59, null);
        CronField hour = CronField.parse(parts[1], 0, 23, null);
        CronField dayOfMonth = CronField.parse(parts[2], 1, 31, null);
        CronField month = CronField.parse(parts[3], 1, 12, monthNames());
        CronField dayOfWeek = CronField.parse(parts[4], 0, 7, dayNames());

        return new CronExpression(minute, hour, dayOfMonth, month, dayOfWeek);
    }

    public boolean matches(ZonedDateTime time) {
        if (!minute.contains(time.getMinute())) {
            return false;
        }
        if (!hour.contains(time.getHour())) {
            return false;
        }
        if (!month.contains(time.getMonthValue())) {
            return false;
        }
        return dayMatches(time.toLocalDate());
    }

    public ZonedDateTime nextAfter(ZonedDateTime from) {
        if (from == null) {
            throw new IllegalArgumentException("start time cannot be null");
        }

        ZonedDateTime time = from.withSecond(0).withNano(0).plusMinutes(1);
        int guard = 0;
        while (guard++ < 1_000_000) {
            if (!month.contains(time.getMonthValue())) {
                time = time.withDayOfMonth(1)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                        .plusMonths(1);
                continue;
            }

            if (!dayMatches(time.toLocalDate())) {
                time = time.plusDays(1)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);
                continue;
            }

            Integer nextHour = hour.nextAtOrAfter(time.getHour());
            if (nextHour == null) {
                time = time.plusDays(1)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);
                continue;
            }

            if (nextHour > time.getHour()) {
                return time.withHour(nextHour)
                        .withMinute(minute.first())
                        .withSecond(0)
                        .withNano(0);
            }

            Integer nextMinute = minute.nextAtOrAfter(time.getMinute());
            if (nextMinute == null) {
                Integer followingHour = hour.nextAfter(time.getHour());
                if (followingHour == null) {
                    time = time.plusDays(1)
                            .withHour(0)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    continue;
                }
                return time.withHour(followingHour)
                        .withMinute(minute.first())
                        .withSecond(0)
                        .withNano(0);
            }

            if (nextMinute > time.getMinute()) {
                return time.withMinute(nextMinute)
                        .withSecond(0)
                        .withNano(0);
            }

            return time.withSecond(0).withNano(0);
        }

        throw new IllegalStateException("unable to find a matching cron time");
    }

    public List<ZonedDateTime> next(ZonedDateTime from, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
        List<ZonedDateTime> result = new ArrayList<>(count);
        ZonedDateTime cursor = from;
        for (int i = 0; i < count; i++) {
            cursor = nextAfter(cursor);
            result.add(cursor);
        }
        return result;
    }

    private boolean dayMatches(LocalDate date) {
        int javaDayOfWeek = date.getDayOfWeek().getValue(); // Monday=1 ... Sunday=7
        int cronDayOfWeek = (javaDayOfWeek == 7) ? 0 : javaDayOfWeek;
        boolean sunday = javaDayOfWeek == 7;

        boolean domMatch = dayOfMonth.contains(date.getDayOfMonth());
        boolean dowMatch = dayOfWeek.contains(cronDayOfWeek) || (sunday && dayOfWeek.contains(7));

        if (dayOfMonthRestricted && dayOfWeekRestricted) {
            return domMatch || dowMatch;
        }
        if (dayOfMonthRestricted) {
            return domMatch;
        }
        if (dayOfWeekRestricted) {
            return dowMatch;
        }
        return true;
    }

    private static String expandAlias(String expression) {
        String key = expression.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "@yearly":
            case "@annually":
                return "0 0 1 1 *";
            case "@monthly":
                return "0 0 1 * *";
            case "@weekly":
                return "0 0 * * 0";
            case "@daily":
            case "@midnight":
                return "0 0 * * *";
            case "@hourly":
                return "0 * * * *";
            case "@reboot":
                throw new IllegalArgumentException("@reboot cannot be converted to a calendar time");
            default:
                return expression;
        }
    }

    private static Map<String, Integer> monthNames() {
        Map<String, Integer> names = new HashMap<>();
        String[] months = {
                "january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december"
        };
        for (int i = 0; i < months.length; i++) {
            String full = months[i];
            names.put(full, i + 1);
            names.put(full.substring(0, 3), i + 1);
        }
        return names;
    }

    private static Map<String, Integer> dayNames() {
        Map<String, Integer> names = new HashMap<>();
        String[] days = {
                "sunday", "monday", "tuesday", "wednesday",
                "thursday", "friday", "saturday"
        };
        for (int i = 0; i < days.length; i++) {
            String full = days[i];
            names.put(full, i);
            names.put(full.substring(0, 3), i);
        }
        return names;
    }
}
