package kr.ac.ssu.zette.ui.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class RoutineItem {

    public enum RepeatType {
        WEEKLY,
        MONTHLY
    }

    public final long id;
    @NonNull
    public final String name;
    @NonNull
    public final RepeatType repeatType;
    @NonNull
    public final Set<DayOfWeek> weekDays;
    public final int monthDay;
    @NonNull
    public final LocalTime time;
    @NonNull
    public final String placeName;
    @Nullable
    public final LocalTime departureAlarmTime;
    @Nullable
    public final Integer travelMinutes;
    public final boolean enabled;

    private RoutineItem(long id, @NonNull String name, @NonNull RepeatType repeatType,
                        @NonNull Set<DayOfWeek> weekDays, int monthDay, @NonNull LocalTime time,
                        @NonNull String placeName, @Nullable LocalTime departureAlarmTime,
                        @Nullable Integer travelMinutes, boolean enabled) {
        this.id = id;
        this.name = name;
        this.repeatType = repeatType;
        this.weekDays = Collections.unmodifiableSet(
                weekDays.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(weekDays));
        this.monthDay = monthDay;
        this.time = time;
        this.placeName = placeName;
        this.departureAlarmTime = departureAlarmTime;
        this.travelMinutes = travelMinutes;
        this.enabled = enabled;
    }

    @NonNull
    public static RoutineItem weekly(long id, @NonNull String name, @NonNull Set<DayOfWeek> weekDays,
                                     @NonNull LocalTime time, @NonNull String placeName,
                                     @Nullable LocalTime departureAlarmTime,
                                     @Nullable Integer travelMinutes, boolean enabled) {
        return new RoutineItem(id, name, RepeatType.WEEKLY, weekDays, 0, time, placeName,
                departureAlarmTime, travelMinutes, enabled);
    }

    @NonNull
    public static RoutineItem monthly(long id, @NonNull String name, int monthDay,
                                      @NonNull LocalTime time, @NonNull String placeName,
                                      @Nullable LocalTime departureAlarmTime,
                                      @Nullable Integer travelMinutes, boolean enabled) {
        return new RoutineItem(id, name, RepeatType.MONTHLY, EnumSet.noneOf(DayOfWeek.class),
                monthDay, time, placeName, departureAlarmTime, travelMinutes, enabled);
    }

    @NonNull
    public RoutineItem withEnabled(boolean on) {
        return new RoutineItem(id, name, repeatType, weekDays, monthDay, time, placeName,
                departureAlarmTime, travelMinutes, on);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutineItem)) return false;
        RoutineItem that = (RoutineItem) o;
        return id == that.id
                && monthDay == that.monthDay
                && enabled == that.enabled
                && name.equals(that.name)
                && repeatType == that.repeatType
                && weekDays.equals(that.weekDays)
                && time.equals(that.time)
                && placeName.equals(that.placeName)
                && Objects.equals(departureAlarmTime, that.departureAlarmTime)
                && Objects.equals(travelMinutes, that.travelMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, repeatType, weekDays, monthDay, time, placeName,
                departureAlarmTime, travelMinutes, enabled);
    }
}
