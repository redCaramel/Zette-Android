package kr.ac.ssu.zette.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;
import java.util.Objects;

import kr.ac.ssu.zette.dto.Schedule;

public class ScheduleCardItem {

    public enum Status {
        DONE,
        MOVING,
        LATE,
        SCHEDULED,
        UNRESERVED,
        MISSED
    }

    @NonNull
    public final Schedule schedule;
    @NonNull
    public final Status status;
    @Nullable
    public final LocalTime statusTime;
    @Nullable
    public final Integer travelMinutes;
    @Nullable
    public final String boardingRoute;

    public ScheduleCardItem(@NonNull Schedule schedule, @NonNull Status status,
                            @Nullable LocalTime statusTime, @Nullable Integer travelMinutes,
                            @Nullable String boardingRoute) {
        this.schedule = schedule;
        this.status = status;
        this.statusTime = statusTime;
        this.travelMinutes = travelMinutes;
        this.boardingRoute = boardingRoute;
    }

    public ScheduleCardItem withStatus(@NonNull Status newStatus) {
        return new ScheduleCardItem(schedule, newStatus, statusTime, travelMinutes, boardingRoute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleCardItem)) return false;
        ScheduleCardItem that = (ScheduleCardItem) o;
        return schedule.id == that.schedule.id
                && Objects.equals(schedule.name, that.schedule.name)
                && Objects.equals(schedule.date, that.schedule.date)
                && Objects.equals(schedule.arrivalTime, that.schedule.arrivalTime)
                && Objects.equals(placeName(schedule), placeName(that.schedule))
                && status == that.status
                && Objects.equals(statusTime, that.statusTime)
                && Objects.equals(travelMinutes, that.travelMinutes)
                && Objects.equals(boardingRoute, that.boardingRoute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schedule.id, schedule.name, schedule.date, schedule.arrivalTime,
                placeName(schedule), status, statusTime, travelMinutes, boardingRoute);
    }

    @Nullable
    private static String placeName(@NonNull Schedule s) {
        return s.place == null ? null : s.place.name;
    }
}
