package kr.ac.ssu.zette.ui.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;
import java.util.Objects;

import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;

public class CalendarEventItem {

    @NonNull
    public final ScheduleKey key;
    @NonNull
    public final String title;
    @NonNull
    public final LocalTime time;
    @Nullable
    public final LocalTime departureTime;
    @NonNull
    public final String placeName;
    public final boolean alarmOn;
    @Nullable
    public final String routineLabel;

    public CalendarEventItem(@NonNull ScheduleKey key, @NonNull String title, @NonNull LocalTime time,
                             @Nullable LocalTime departureTime, @NonNull String placeName,
                             boolean alarmOn, @Nullable String routineLabel) {
        this.key = key;
        this.title = title;
        this.time = time;
        this.departureTime = departureTime;
        this.placeName = placeName;
        this.alarmOn = alarmOn;
        this.routineLabel = routineLabel;
    }

    // 저장소의 일정 → 캘린더 항목. 루틴 회차면 routineLabel을 붙인다.
    @NonNull
    public static CalendarEventItem from(@NonNull Schedule s, @NonNull String routineLabel) {
        LocalTime departure = s.computed != null && s.computed.expectedDeparture != null
                ? s.computed.expectedDeparture.toLocalTime()
                : null;
        return new CalendarEventItem(s.key(),
                s.name != null ? s.name : "",
                s.arrivalTime != null ? s.arrivalTime : LocalTime.MIDNIGHT,
                departure,
                s.place != null && s.place.name != null ? s.place.name : "",
                s.activated,
                s.isOccurrence() ? routineLabel : null);
    }

    public CalendarEventItem withAlarm(boolean on) {
        return new CalendarEventItem(key, title, time, departureTime, placeName, on, routineLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalendarEventItem)) return false;
        CalendarEventItem that = (CalendarEventItem) o;
        return key.equals(that.key)
                && alarmOn == that.alarmOn
                && title.equals(that.title)
                && time.equals(that.time)
                && Objects.equals(departureTime, that.departureTime)
                && placeName.equals(that.placeName)
                && Objects.equals(routineLabel, that.routineLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, title, time, departureTime, placeName, alarmOn, routineLabel);
    }
}
