package kr.ac.ssu.zette.data.local;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleStatus;

// Room 타입 변환: LocalDate → epochDay, LocalTime → 분, LocalDateTime → ISO 문자열,
// enum → 이름, boolean[] weekdays → "1010100", List<Integer> → "1,15,31"
public final class Converters {

    private static final int WEEK_DAYS = 7;
    private static final String LIST_SEPARATOR = ",";

    private Converters() {
    }

    @TypeConverter
    @Nullable
    public static Long fromLocalDate(@Nullable LocalDate date) {
        return date == null ? null : date.toEpochDay();
    }

    @TypeConverter
    @Nullable
    public static LocalDate toLocalDate(@Nullable Long epochDay) {
        return epochDay == null ? null : LocalDate.ofEpochDay(epochDay);
    }

    @TypeConverter
    @Nullable
    public static Integer fromLocalTime(@Nullable LocalTime time) {
        return time == null ? null : time.getHour() * 60 + time.getMinute();
    }

    @TypeConverter
    @Nullable
    public static LocalTime toLocalTime(@Nullable Integer minutes) {
        return minutes == null ? null : LocalTime.of(minutes / 60, minutes % 60);
    }

    @TypeConverter
    @Nullable
    public static String fromLocalDateTime(@Nullable LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }

    @TypeConverter
    @Nullable
    public static LocalDateTime toLocalDateTime(@Nullable String iso) {
        return iso == null ? null : LocalDateTime.parse(iso);
    }

    @TypeConverter
    @Nullable
    public static String fromRouteType(@Nullable Schedule.RouteType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    @Nullable
    public static Schedule.RouteType toRouteType(@Nullable String name) {
        return name == null ? null : Schedule.RouteType.valueOf(name);
    }

    @TypeConverter
    @Nullable
    public static String fromRepeatType(@Nullable Routine.RepeatType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    @Nullable
    public static Routine.RepeatType toRepeatType(@Nullable String name) {
        return name == null ? null : Routine.RepeatType.valueOf(name);
    }

    @TypeConverter
    @Nullable
    public static String fromStatus(@Nullable ScheduleStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    @Nullable
    public static ScheduleStatus toStatus(@Nullable String name) {
        return name == null ? null : ScheduleStatus.valueOf(name);
    }

    @TypeConverter
    @Nullable
    public static String fromWeekdays(@Nullable boolean[] weekdays) {
        if (weekdays == null) return null;
        StringBuilder sb = new StringBuilder(WEEK_DAYS);
        for (int i = 0; i < WEEK_DAYS; i++) {
            sb.append(i < weekdays.length && weekdays[i] ? '1' : '0');
        }
        return sb.toString();
    }

    @TypeConverter
    @Nullable
    public static boolean[] toWeekdays(@Nullable String value) {
        if (value == null) return null;
        boolean[] weekdays = new boolean[WEEK_DAYS];
        for (int i = 0; i < WEEK_DAYS && i < value.length(); i++) {
            weekdays[i] = value.charAt(i) == '1';
        }
        return weekdays;
    }

    @TypeConverter
    @Nullable
    public static String fromIntList(@Nullable List<Integer> values) {
        if (values == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Integer v : values) {
            if (v == null) continue;
            if (sb.length() > 0) sb.append(LIST_SEPARATOR);
            sb.append(v);
        }
        return sb.toString();
    }

    @TypeConverter
    @Nullable
    public static List<Integer> toIntList(@Nullable String value) {
        if (value == null) return null;
        List<Integer> values = new ArrayList<>();
        if (value.isEmpty()) return values;
        for (String part : value.split(LIST_SEPARATOR)) {
            values.add(Integer.parseInt(part.trim()));
        }
        return values;
    }
}
