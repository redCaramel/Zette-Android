package kr.ac.ssu.zette.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleStatus;

// 일반 일정(routine_id = null)과 루틴 회차 상태 행(routine_id != null)을 함께 저장한다.
@Entity(
        tableName = "schedule",
        indices = {
                @Index(value = {"date"}),
                @Index(value = {"routine_id", "occurrence_date"}, unique = true)
        }
)
public class ScheduleEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    public LocalDate date;

    @ColumnInfo(name = "arrival_time")
    public LocalTime arrivalTime;

    @Nullable
    @ColumnInfo(name = "place_id")
    public String placeId;
    @Nullable
    @ColumnInfo(name = "place_name")
    public String placeName;
    @Nullable
    @ColumnInfo(name = "place_address")
    public String placeAddress;
    @Nullable
    @ColumnInfo(name = "place_lat")
    public Double placeLat;
    @Nullable
    @ColumnInfo(name = "place_lng")
    public Double placeLng;

    @ColumnInfo(name = "route_type")
    public Schedule.RouteType routeType;

    @ColumnInfo(name = "arrival_buffer")
    public int arrivalBuffer;

    public boolean activated;

    public ScheduleStatus status;

    @Nullable
    @ColumnInfo(name = "completed_at")
    public LocalDateTime completedAt;

    @Nullable
    @ColumnInfo(name = "expected_arrival")
    public LocalDateTime expectedArrival;

    @Nullable
    @ColumnInfo(name = "expected_departure")
    public LocalDateTime expectedDeparture;

    @Nullable
    @ColumnInfo(name = "travel_minutes")
    public Integer travelMinutes;

    @Nullable
    @ColumnInfo(name = "route_summary")
    public String routeSummary;

    @Nullable
    @ColumnInfo(name = "computed_at")
    public LocalDateTime computedAt;

    // 루틴 회차 상태 행
    @Nullable
    @ColumnInfo(name = "routine_id")
    public Long routineId;

    @Nullable
    @ColumnInfo(name = "occurrence_date")
    public LocalDate occurrenceDate;

    // true: 이 행의 내용(이름·날짜·시각·장소·경로)을 쓴다 / false: 내용은 루틴에서, 상태만 이 행에서
    @ColumnInfo(name = "content_overridden")
    public boolean contentOverridden;

    // 이 회차 하루만 삭제
    public boolean skipped;
}
