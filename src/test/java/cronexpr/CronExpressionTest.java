package cronexpr;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public final class CronExpressionTest {
    private static int failures = 0;

    public static void main(String[] args) {
        testEveryMinute();
        testEveryFifteenMinutes();
        testWeekdays();
        testDayOfMonthOrDayOfWeek();
        testMonthNames();
        testSteppedRange();
        testAlias();
        testInvalidField();
        testInvalidFieldCount();
        testInvalidHour();
        testQuestionMarkWildcard();
        testSundayAsSeven();

        if (failures == 0) {
            System.out.println("All tests passed.");
        } else {
            System.out.println(failures + " test(s) failed.");
            System.exit(1);
        }
    }

    private static void testEveryMinute() {
        CronExpression cron = CronExpression.parse("* * * * *");
        List<ZonedDateTime> runs = cron.next(utc("2025-03-01T12:34:56"), 3);
        check("every minute",
                runs.get(0).equals(utc("2025-03-01T12:35:00"))
                        && runs.get(1).equals(utc("2025-03-01T12:36:00"))
                        && runs.get(2).equals(utc("2025-03-01T12:37:00")));
    }

    private static void testEveryFifteenMinutes() {
        CronExpression cron = CronExpression.parse("*/15 * * * *");
        List<ZonedDateTime> runs = cron.next(utc("2025-01-01T00:00:00"), 4);
        check("every fifteen minutes",
                runs.get(0).equals(utc("2025-01-01T00:15:00"))
                        && runs.get(1).equals(utc("2025-01-01T00:30:00"))
                        && runs.get(2).equals(utc("2025-01-01T00:45:00"))
                        && runs.get(3).equals(utc("2025-01-01T01:00:00")));
    }

    private static void testWeekdays() {
        CronExpression cron = CronExpression.parse("0 9 * * 1-5");
        List<ZonedDateTime> runs = cron.next(utc("2025-01-01T00:00:00"), 4);
        check("weekdays",
                runs.get(0).equals(utc("2025-01-01T09:00:00"))
                        && runs.get(1).equals(utc("2025-01-02T09:00:00"))
                        && runs.get(2).equals(utc("2025-01-03T09:00:00"))
                        && runs.get(3).equals(utc("2025-01-06T09:00:00")));
    }

    private static void testDayOfMonthOrDayOfWeek() {
        CronExpression cron = CronExpression.parse("0 0 1 * MON");
        List<ZonedDateTime> runs = cron.next(utc("2024-12-31T12:00:00"), 2);
        check("day-of-month or day-of-week",
                runs.get(0).equals(utc("2025-01-01T00:00:00"))
                        && runs.get(1).equals(utc("2025-01-06T00:00:00")));
    }

    private static void testMonthNames() {
        CronExpression cron = CronExpression.parse("0 0 * JAN,MAR *");
        List<ZonedDateTime> january = cron.next(utc("2025-01-15T12:00:00"), 1);
        List<ZonedDateTime> march = cron.next(utc("2025-02-15T12:00:00"), 1);
        check("month names",
                january.get(0).equals(utc("2025-01-16T00:00:00"))
                        && march.get(0).equals(utc("2025-03-01T00:00:00")));
    }

    private static void testSteppedRange() {
        CronExpression cron = CronExpression.parse("1-5/2 * * * *");
        List<ZonedDateTime> runs = cron.next(utc("2025-01-01T00:00:00"), 3);
        check("stepped range",
                runs.get(0).equals(utc("2025-01-01T00:01:00"))
                        && runs.get(1).equals(utc("2025-01-01T00:03:00"))
                        && runs.get(2).equals(utc("2025-01-01T00:05:00")));
    }

    private static void testAlias() {
        CronExpression cron = CronExpression.parse("@daily");
        List<ZonedDateTime> runs = cron.next(utc("2025-01-01T00:00:00"), 1);
        check("alias", runs.get(0).equals(utc("2025-01-02T00:00:00")));
    }

    private static void testInvalidField() {
        expectThrow(() -> CronExpression.parse("60 * * * *"));
    }

    private static void testInvalidFieldCount() {
        expectThrow(() -> CronExpression.parse("* * * *"));
    }

    private static void testInvalidHour() {
        expectThrow(() -> CronExpression.parse("0 24 * * *"));
    }

    private static void testQuestionMarkWildcard() {
        CronExpression cron = CronExpression.parse("0 0 ? * ?");
        List<ZonedDateTime> runs = cron.next(utc("2025-01-01T12:00:00"), 2);
        check("question-mark wildcard",
                runs.get(0).equals(utc("2025-01-02T00:00:00"))
                        && runs.get(1).equals(utc("2025-01-03T00:00:00")));
    }

    private static void testSundayAsSeven() {
        CronExpression cron = CronExpression.parse("0 0 * * 7");
        List<ZonedDateTime> runs = cron.next(utc("2025-01-04T12:00:00"), 1);
        check("sunday as seven", runs.get(0).equals(utc("2025-01-05T00:00:00")));
    }

    private static ZonedDateTime utc(String value) {
        return LocalDateTime.parse(value).atZone(ZoneId.of("UTC"));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + name);
        } else {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void expectThrow(Runnable action) {
        try {
            action.run();
            failures++;
            System.out.println("FAIL: expected exception");
        } catch (RuntimeException e) {
            System.out.println("PASS: expected exception");
        }
    }
}
