package kr.ac.ssu.zette.ui.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;

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

    // 저장소의 일정 → 홈 카드
    @NonNull
    public static ScheduleCardItem from(@NonNull Schedule s) {
        Status status = toCardStatus(s);
        LocalTime statusTime = null;
        String boardingRoute = null;
        switch (status) {
            case DONE:
                statusTime = timeOf(s.completedAt);
                break;
            case MOVING:
                statusTime = timeOf(s.expectedArrival);
                boardingRoute = s.computed != null ? s.computed.routeSummary : null;
                break;
            case SCHEDULED:
                statusTime = s.computed != null ? timeOf(s.computed.expectedDeparture) : null;
                break;
            default:
                break;
        }
        Integer travelMinutes = s.computed != null ? s.computed.travelMinutes : null;
        return new ScheduleCardItem(s, status, statusTime, travelMinutes, boardingRoute);
    }

    @NonNull
    public ScheduleKey key() {
        return schedule.key();
    }

    @NonNull
    private static Status toCardStatus(@NonNull Schedule s) {
        if (s.status == null) {
            return s.activated ? Status.SCHEDULED : Status.UNRESERVED;
        }
        switch (s.status) {
            case DONE:
                return Status.DONE;
            case MOVING:
                return Status.MOVING;
            case LATE:
                return Status.LATE;
            case MISSED:
                return Status.MISSED;
            case UNRESERVED:
                return Status.UNRESERVED;
            case RESERVED:
            default:
                return s.activated ? Status.SCHEDULED : Status.UNRESERVED;
        }
    }

    @Nullable
    private static LocalTime timeOf(@Nullable LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleCardItem)) return false;
        ScheduleCardItem that = (ScheduleCardItem) o;
        return key().equals(that.key())
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
        return Objects.hash(key(), schedule.name, schedule.date, schedule.arrivalTime,
                placeName(schedule), status, statusTime, travelMinutes, boardingRoute);
    }

    @Nullable
    private static String placeName(@NonNull Schedule s) {
        return s.place == null ? null : s.place.name;
    }
}
