package kr.ac.ssu.zette.data.local.debug;

import android.util.Log;

import androidx.annotation.NonNull;

import kr.ac.ssu.zette.data.ScheduleChangeListener;
import kr.ac.ssu.zette.dto.ScheduleKey;

// 디버그 빌드 전용: 변경 알림 호출만 Logcat에 남긴다. 백그라운드 모듈이 들어오면 그 리스너로 바꾼다.
public class LoggingChangeListener implements ScheduleChangeListener {

    private static final String TAG = "ZetteChange";

    @Override
    public void onScheduleSaved(@NonNull ScheduleKey key) {
        Log.d(TAG, "onScheduleSaved " + key.encode());
    }

    @Override
    public void onScheduleRemoved(@NonNull ScheduleKey key) {
        Log.d(TAG, "onScheduleRemoved " + key.encode());
    }

    @Override
    public void onRoutineChanged(long routineId) {
        Log.d(TAG, "onRoutineChanged " + routineId);
    }

    @Override
    public void onDefaultsChanged() {
        Log.d(TAG, "onDefaultsChanged");
    }
}
