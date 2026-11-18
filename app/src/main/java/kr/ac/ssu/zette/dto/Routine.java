package kr.ac.ssu.zette.dto;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 루틴 데이터 클래스
public class Routine {

    public long id;
    public String name;
    public RepeatType repeatType;
    public LocalTime arrivalTime;
    public Place place;
    public RouteType routeType; // 최소 시간 / 최소 도보 / 최소 환승
    public int arrivalBuffer; // 도착 여유 (분)

    // 요일 반복
    public boolean[] weekdays = new boolean[7];
    // 매월 반복
    public List<Integer> repeatDate;
    public Routine() {}

    public Routine(long id, String name, RepeatType repeatType, LocalTime arrivalTime, Place place,
                   RouteType routeType, int arrivalBuffer, boolean[] weekdays, List<Integer> repeatDate) {
        this.id = id;
        this.name = name;
        this.repeatType = repeatType;
        this.arrivalTime = arrivalTime;
        this.place = place;
        this.routeType = routeType;
        this.arrivalBuffer = arrivalBuffer;
        if (weekdays != null) {
            this.weekdays = weekdays;
        }
        this.repeatDate = repeatDate;
    }

    public enum RouteType { // 최소 시간 / 최소 도보 / 최소 환승
        MinTime, MinWalk, MinTransfer
    }
    public enum RepeatType {
        WEEKLY,
        MONTHLY
    }
}

