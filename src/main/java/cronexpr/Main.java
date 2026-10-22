package cronexpr;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }

        List<String> positional = new ArrayList<>();
        int count = 5;
        String from = null;
        String zone = ZoneId.systemDefault().getId();
        boolean validate = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--count":
                    if (i + 1 >= args.length) {
                        fail("missing value for --count");
                    }
                    try {
                        count = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        fail("invalid count: " + args[i]);
                    }
                    if (count < 0) {
                        fail("count cannot be negative");
                    }
                    break;
                case "--from":
                    if (i + 1 >= args.length) {
                        fail("missing value for --from");
                    }
                    from = args[++i];
                    break;
                case "--zone":
                    if (i + 1 >= args.length) {
                        fail("missing value for --zone");
                    }
                    zone = args[++i];
                    break;
                case "--validate":
                    validate = true;
                    break;
                case "--help":
                case "-h":
                    usage();
                    return;
                default:
                    positional.add(args[i]);
                    break;
            }
        }

        if (positional.isEmpty()) {
            fail("missing cron expression");
        }

        String expression = String.join(" ", positional);
        CronExpression cron;
        try {
            cron = CronExpression.parse(expression);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
            return;
        }

        if (validate) {
            System.out.println("valid");
            return;
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zone);
        } catch (Exception e) {
            fail("invalid time zone: " + zone);
            return;
        }

        ZonedDateTime start;
        if (from == null) {
            start = ZonedDateTime.now(zoneId);
        } else {
            try {
                start = ZonedDateTime.parse(from);
            } catch (DateTimeParseException ignored) {
                try {
                    start = LocalDateTime.parse(from).atZone(zoneId);
                } catch (DateTimeParseException e) {
                    fail("invalid start time: " + from);
                    return;
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
        for (ZonedDateTime run : cron.next(start, count)) {
            System.out.println(run.format(formatter));
        }
    }

    private static void usage() {
        System.out.println("Usage: cronexpr.Main EXPRESSION [--count N] [--from DATETIME] [--zone ZONE] [--validate]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  cronexpr.Main \"0 9 * * 1-5\" --count 5");
        System.out.println("  cronexpr.Main \"*/15 * * * *\" --count 4 --zone UTC");
    }

    private static void fail(String message) {
        System.err.println("error: " + message);
        System.exit(2);
    }
}
