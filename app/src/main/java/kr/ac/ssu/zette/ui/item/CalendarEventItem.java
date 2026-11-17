package kr.ac.ssu.zette.ui.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;
import java.util.Objects;

public class CalendarEventItem {

    public final long id;
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

    public CalendarEventItem(long id, @NonNull String title, @NonNull LocalTime time,
                             @Nullable LocalTime departureTime, @NonNull String placeName,
                             boolean alarmOn, @Nullable String routineLabel) {
        this.id = id;
        this.title = title;
        this.time = time;
        this.departureTime = departureTime;
        this.placeName = placeName;
        this.alarmOn = alarmOn;
        this.routineLabel = routineLabel;
    }

    public CalendarEventItem withAlarm(boolean on) {
        return new CalendarEventItem(id, title, time, departureTime, placeName, on, routineLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalendarEventItem)) return false;
        CalendarEventItem that = (CalendarEventItem) o;
        return id == that.id
                && alarmOn == that.alarmOn
                && title.equals(that.title)
                && time.equals(that.time)
                && Objects.equals(departureTime, that.departureTime)
                && placeName.equals(that.placeName)
                && Objects.equals(routineLabel, that.routineLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, time, departureTime, placeName, alarmOn, routineLabel);
    }
}
