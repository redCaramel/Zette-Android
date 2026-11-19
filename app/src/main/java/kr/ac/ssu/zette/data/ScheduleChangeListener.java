package kr.ac.ssu.zette.data;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import kr.ac.ssu.zette.dto.ScheduleKey;

// 일정·루틴·설정 변경 알림. 백그라운드 모듈이 구현해 ZetteData.setChangeListener()로 등록한다.
// DB 반영이 끝난 뒤 작업 스레드에서 호출된다.
public interface ScheduleChangeListener {

    // 추가·수정·하루만 수정·알림 켜기
    @WorkerThread
    void onScheduleSaved(@NonNull ScheduleKey key);

    // 삭제·알림 끄기·완료 처리
    @WorkerThread
    void onScheduleRemoved(@NonNull ScheduleKey key);

    // 루틴 추가·수정·켜기/끄기·삭제
    @WorkerThread
    void onRoutineChanged(long routineId);

    // 기본 경로 기준·기본 도착 여유 변경
    @WorkerThread
    void onDefaultsChanged();
}
