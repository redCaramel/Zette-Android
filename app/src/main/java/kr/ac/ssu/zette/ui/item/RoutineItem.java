package kr.ac.ssu.zette.ui.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import kr.ac.ssu.zette.dto.Routine;

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
    @NonNull
    public final List<Integer> monthDays;
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
                        @NonNull Set<DayOfWeek> weekDays, @NonNull List<Integer> monthDays,
                        @NonNull LocalTime time, @NonNull String placeName,
                        @Nullable LocalTime departureAlarmTime, @Nullable Integer travelMinutes,
                        boolean enabled) {
        this.id = id;
        this.name = name;
        this.repeatType = repeatType;
        this.weekDays = Collections.unmodifiableSet(
                weekDays.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(weekDays));
        List<Integer> sortedDays = new ArrayList<>(monthDays);
        Collections.sort(sortedDays);
        this.monthDays = Collections.unmodifiableList(sortedDays);
        this.time = time;
        this.placeName = placeName;
        this.departureAlarmTime = departureAlarmTime;
        this.travelMinutes = travelMinutes;
        this.enabled = enabled;
    }

    // 저장소의 루틴 → 루틴 카드. 출발 알림 시각·이동 시간은 백그라운드 계산 전이라 비어 있다.
    @NonNull
    public static RoutineItem from(@NonNull Routine r) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (r.weekdays != null) {
            for (int i = 0; i < r.weekdays.length && i < 7; i++) {
                if (r.weekdays[i]) days.add(DayOfWeek.of(i + 1));
            }
        }
        List<Integer> monthDays = r.repeatDate != null ? r.repeatDate : Collections.emptyList();
        RepeatType type = r.repeatType == Routine.RepeatType.MONTHLY ? RepeatType.MONTHLY : RepeatType.WEEKLY;
        return new RoutineItem(r.id, r.name != null ? r.name : "", type, days, monthDays,
                r.arrivalTime != null ? r.arrivalTime : LocalTime.MIDNIGHT,
                r.place != null && r.place.name != null ? r.place.name : "",
                null, null, r.activated);
    }

    @NonNull
    public RoutineItem withEnabled(boolean on) {
        return new RoutineItem(id, name, repeatType, weekDays, monthDays, time, placeName,
                departureAlarmTime, travelMinutes, on);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutineItem)) return false;
        RoutineItem that = (RoutineItem) o;
        return id == that.id
                && enabled == that.enabled
                && name.equals(that.name)
                && repeatType == that.repeatType
                && weekDays.equals(that.weekDays)
                && monthDays.equals(that.monthDays)
                && time.equals(that.time)
                && placeName.equals(that.placeName)
                && Objects.equals(departureAlarmTime, that.departureAlarmTime)
                && Objects.equals(travelMinutes, that.travelMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, repeatType, weekDays, monthDays, time, placeName,
                departureAlarmTime, travelMinutes, enabled);
    }
}
