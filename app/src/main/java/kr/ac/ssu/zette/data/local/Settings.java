package kr.ac.ssu.zette.data.local;

import androidx.annotation.NonNull;

import java.util.Objects;

import kr.ac.ssu.zette.dto.Schedule;

// 화면용 설정 묶음 (읽기 전용 값)
public final class Settings {

    public final boolean vibrationEnabled;
    public final boolean screenAlertEnabled;
    public final boolean voiceEnabled;
    @NonNull
    public final Schedule.RouteType defaultRouteType;
    public final int defaultArrivalBufferMinutes;
    @NonNull
    public final ThemeMode themeMode;

    public Settings(boolean vibrationEnabled, boolean screenAlertEnabled, boolean voiceEnabled,
                    @NonNull Schedule.RouteType defaultRouteType, int defaultArrivalBufferMinutes,
                    @NonNull ThemeMode themeMode) {
        this.vibrationEnabled = vibrationEnabled;
        this.screenAlertEnabled = screenAlertEnabled;
        this.voiceEnabled = voiceEnabled;
        this.defaultRouteType = defaultRouteType;
        this.defaultArrivalBufferMinutes = defaultArrivalBufferMinutes;
        this.themeMode = themeMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Settings)) return false;
        Settings that = (Settings) o;
        return vibrationEnabled == that.vibrationEnabled
                && screenAlertEnabled == that.screenAlertEnabled
                && voiceEnabled == that.voiceEnabled
                && defaultArrivalBufferMinutes == that.defaultArrivalBufferMinutes
                && defaultRouteType == that.defaultRouteType
                && themeMode == that.themeMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vibrationEnabled, screenAlertEnabled, voiceEnabled, defaultRouteType,
                defaultArrivalBufferMinutes, themeMode);
    }
}
