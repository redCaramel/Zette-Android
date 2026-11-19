package kr.ac.ssu.zette.dto;

// 일정 상태 (홈 Schedule Card 상태와 같음)
public enum ScheduleStatus {
    RESERVED,   // 예약됨 (출발 알림 예약)
    UNRESERVED, // 미예약 (알림 꺼짐)
    MOVING,     // 이동 중
    LATE,       // 지각 예상
    DONE,       // 완료
    MISSED      // 시간 경과
}
