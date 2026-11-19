package kr.ac.ssu.zette.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 일정 데이터 클래스
public class Schedule {
    public long id;
    public String name;
    public LocalDate date;
    public LocalTime arrivalTime;
    public Place place;
    public RouteType routeType; // 최소 시간 / 최소 도보 / 최소 환승
    public int arrivalBuffer; // 도착 여유 (분)
    public boolean activated;

    // 루틴 회차 (일반 일정이면 둘 다 null)
    @Nullable
    public Long routineId;
    @Nullable
    public LocalDate occurrenceDate; // 회차의 원래 날짜

    // 상태 · 계산 결과
    public ScheduleStatus status;
    @Nullable
    public LocalDateTime completedAt;
    @Nullable
    public LocalDateTime expectedArrival;
    @Nullable
    public ComputedRoute computed;

    public Schedule() {}

    public Schedule(long id, String name, LocalDate date, LocalTime arrivalTime, Place place,
                    RouteType routeType, int arrivalBuffer) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.arrivalTime = arrivalTime;
        this.place = place;
        this.routeType = routeType;
        this.arrivalBuffer = arrivalBuffer;
        activated = true;
        status = ScheduleStatus.RESERVED;
    }

    @NonNull
    public ScheduleKey key() {
        if (routineId != null) {
            return ScheduleKey.ofOccurrence(routineId, occurrenceDate);
        }
        return ScheduleKey.ofSchedule(id);
    }

    public boolean isOccurrence() {
        return routineId != null;
    }

    public enum RouteType { // 최소 시간 / 최소 도보 / 최소 환승
        MinTime, MinWalk, MinTransfer
    }
}
