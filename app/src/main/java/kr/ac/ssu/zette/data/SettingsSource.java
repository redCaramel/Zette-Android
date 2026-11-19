package kr.ac.ssu.zette.data;

import androidx.annotation.NonNull;

import kr.ac.ssu.zette.dto.Schedule;

// 백그라운드용 설정 읽기 입구 (읽기 전용)
public interface SettingsSource {

    boolean isVibrationEnabled();

    boolean isScreenAlertEnabled();

    boolean isVoiceEnabled();

    @NonNull
    Schedule.RouteType defaultRoutePreference();

    int defaultArrivalBufferMinutes();
}
