package kr.ac.ssu.zette.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import kr.ac.ssu.zette.dto.ComputedRoute;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.dto.ScheduleStatus;

// 백그라운드(알림 예약·상태 판정)용 일정 조회·상태 쓰기 입구. 작업 스레드에서만 호출한다.
public interface ScheduleSource {

    @WorkerThread
    @Nullable
    Schedule get(@NonNull ScheduleKey key);

    // 실제 activated && status == RESERVED 인 일정 (일반 일정 + 루틴 회차), 시간순
    @WorkerThread
    @NonNull
    List<Schedule> getReservedBetween(@NonNull LocalDate from, @NonNull LocalDate to);

    @WorkerThread
    void updateStatus(@NonNull ScheduleKey key, @NonNull ScheduleStatus status,
                      @Nullable LocalDateTime expectedArrival);

    // 예상 출발 시각, 이동 시간, 경로 요약, 조회 시각
    @WorkerThread
    void updateComputed(@NonNull ScheduleKey key, @NonNull ComputedRoute computed);

    @WorkerThread
    @NonNull
    List<Routine> getActiveRoutines();
}
