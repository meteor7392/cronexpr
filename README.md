# cronexpr

`cronexpr` is a small, dependency-free Java library and command-line tool for parsing
standard five-field cron expressions and computing their next run times.

## Features

- Five-field cron expressions: minute, hour, day of month, month, day of week
- Ranges, lists, steps, and wildcards
- Month and weekday names
- Common `@` aliases such as `@daily` and `@hourly`
- Correct day-of-month / day-of-week OR semantics
- Next-run computation in a chosen time zone
- No third-party dependencies

## Build

Compile the project with the included Makefile:

```sh
make
```

Run the tests:

```sh
make test
```

## Command-line usage

```sh
java -cp build cronexpr.Main "0 9 * * 1-5" --count 5
```

Options:

- `--count N` - number of future run times to print (default: 5)
- `--from DATETIME` - start time in ISO-8601 local or zoned form
- `--zone ZONE` - time zone for local start times (default: system time zone)
- `--validate` - validate the expression without computing run times

Example:

```sh
java -cp build cronexpr.Main "*/15 * * * *" --count 4 --zone UTC
```

## Library usage

```java
import cronexpr.CronExpression;
import java.time.*;

CronExpression cron = CronExpression.parse("0 9 * * 1-5");
ZonedDateTime from = LocalDateTime.parse("2025-01-01T00:00:00").atZone(ZoneId.of("UTC"));

for (ZonedDateTime run : cron.next(from, 3)) {
    System.out.println(run);
}
```

## Expression format

```text
minute hour day-of-month month day-of-week
```

- Minute: `0-59`
- Hour: `0-23`
- Day of month: `1-31`
- Month: `1-12` or `JAN`-`DEC`
- Day of week: `0-7` (`0` and `7` are Sunday) or `SUN`-`SAT`

Supported syntax includes `*`, `?`, ranges (`1-5`), steps (`*/15`, `1-10/2`),
and comma-separated lists (`MON,WED,FRI`).

## Limitations

- Standard five-field cron only; seconds are not supported.
- Quartz-specific extensions such as `L`, `W`, and `#` are not supported.
- Impossible combinations, such as day-of-month `31` in February, never match.
