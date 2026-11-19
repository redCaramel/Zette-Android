package kr.ac.ssu.zette.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;

// 루틴 규칙. 실제로 지우지 않고 deleted_at으로 소프트 삭제한다.
@Entity(tableName = "routine")
public class RoutineEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    @ColumnInfo(name = "repeat_type")
    public Routine.RepeatType repeatType;

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

    @ColumnInfo(name = "created_at")
    public LocalDateTime createdAt;

    @Nullable
    public boolean[] weekdays;

    @Nullable
    @ColumnInfo(name = "repeat_date")
    public List<Integer> repeatDate;

    // 삭제한 날짜. 이 날짜 전까지만 회차를 계산한다.
    @Nullable
    @ColumnInfo(name = "deleted_at")
    public LocalDate deletedAt;
}
