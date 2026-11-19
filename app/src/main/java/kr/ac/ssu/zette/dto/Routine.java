package kr.ac.ssu.zette.dto;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// 루틴 데이터 클래스
public class Routine {

    public long id;
    public String name;
    public RepeatType repeatType;
    public LocalTime arrivalTime;
    public Place place;
    public Schedule.RouteType routeType; // 최소 시간 / 최소 도보 / 최소 환승
    public int arrivalBuffer; // 도착 여유 (분)
    public boolean activated;
    public LocalDateTime createdAt; // 회차 계산 시작 기준

    // 요일 반복: [0]=월 [1]=화 ... [6]=일 (DayOfWeek.getValue() - 1)
    public boolean[] weekdays = new boolean[7];
    // 매월 반복: 반복할 날짜(1~31)
    public List<Integer> repeatDate;

    public Routine() {}

    public Routine(long id, String name, RepeatType repeatType, LocalTime arrivalTime, Place place,
                   Schedule.RouteType routeType, int arrivalBuffer, boolean[] weekdays,
                   List<Integer> repeatDate) {
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
        activated = true;
        createdAt = LocalDateTime.now();
    }

    // 규칙상 해당 날짜에 회차가 있는지 (생성일·삭제일은 회차 계산기에서 따로 판단)
    public boolean occursOn(@NonNull LocalDate date) {
        if (repeatType == RepeatType.WEEKLY) {
            int index = date.getDayOfWeek().getValue() - 1;
            return weekdays != null && index < weekdays.length && weekdays[index];
        }
        if (repeatType == RepeatType.MONTHLY) {
            return repeatDate != null && repeatDate.contains(date.getDayOfMonth());
        }
        return false;
    }

    public enum RepeatType {
        WEEKLY,
        MONTHLY
    }
}
