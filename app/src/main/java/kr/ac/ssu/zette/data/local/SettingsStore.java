package kr.ac.ssu.zette.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import kr.ac.ssu.zette.data.ScheduleChangeListener;
import kr.ac.ssu.zette.data.SettingsSource;
import kr.ac.ssu.zette.dto.Schedule;

// 설정 6가지 저장소 (SharedPreferences). 화면은 observe()로, 백그라운드는 SettingsSource로 읽는다.
public final class SettingsStore implements SettingsSource {

    public static final String PREFS_NAME = "zette_settings";

    public static final String KEY_ALERT_VIBRATION = "alert_vibration";
    public static final String KEY_ALERT_SCREEN = "alert_screen";
    public static final String KEY_ALERT_VOICE = "alert_voice";
    public static final String KEY_DEFAULT_ROUTE_PREF = "default_route_pref";
    public static final String KEY_DEFAULT_BUFFER_MIN = "default_buffer_min";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";

    public static final boolean DEFAULT_ALERT_VIBRATION = true;
    public static final boolean DEFAULT_ALERT_SCREEN = true;
    // TODO - 명세 §12-17 확정 후 음성 알림 기본값 확인
    public static final boolean DEFAULT_ALERT_VOICE = false;
    public static final Schedule.RouteType DEFAULT_ROUTE_TYPE = Schedule.RouteType.MinTime;
    public static final int DEFAULT_BUFFER_MIN = 5;
    public static final ThemeMode DEFAULT_THEME_MODE = ThemeMode.SYSTEM;

    private static volatile SettingsStore instance;

    private final SharedPreferences prefs;
    private final MutableLiveData<Settings> settings;
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    @Nullable
    private volatile ScheduleChangeListener changeListener;

    private SettingsStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settings = new MutableLiveData<>(read());
        prefListener = (sharedPreferences, key) -> settings.postValue(read());
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @NonNull
    public static SettingsStore getInstance(@NonNull Context context) {
        SettingsStore local = instance;
        if (local == null) {
            synchronized (SettingsStore.class) {
                local = instance;
                if (local == null) {
                    local = new SettingsStore(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    void setChangeListener(@Nullable ScheduleChangeListener listener) {
        changeListener = listener;
    }

    @NonNull
    public LiveData<Settings> observe() {
        return settings;
    }

    @NonNull
    public Settings get() {
        return read();
    }

    // ===== 읽기 (SettingsSource) =====

    @Override
    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_ALERT_VIBRATION, DEFAULT_ALERT_VIBRATION);
    }

    @Override
    public boolean isScreenAlertEnabled() {
        return prefs.getBoolean(KEY_ALERT_SCREEN, DEFAULT_ALERT_SCREEN);
    }

    @Override
    public boolean isVoiceEnabled() {
        return prefs.getBoolean(KEY_ALERT_VOICE, DEFAULT_ALERT_VOICE);
    }

    @NonNull
    @Override
    public Schedule.RouteType defaultRoutePreference() {
        return parseEnum(Schedule.RouteType.class,
                prefs.getString(KEY_DEFAULT_ROUTE_PREF, null), DEFAULT_ROUTE_TYPE);
    }

    @Override
    public int defaultArrivalBufferMinutes() {
        return prefs.getInt(KEY_DEFAULT_BUFFER_MIN, DEFAULT_BUFFER_MIN);
    }

    @NonNull
    public ThemeMode getThemeMode() {
        return parseEnum(ThemeMode.class, prefs.getString(KEY_THEME_MODE, null), DEFAULT_THEME_MODE);
    }

    // ===== 쓰기 =====

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_VIBRATION, enabled).apply();
    }

    public void setScreenAlertEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_SCREEN, enabled).apply();
    }

    public void setVoiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_VOICE, enabled).apply();
    }

    public void setDefaultRoutePreference(@NonNull Schedule.RouteType routeType) {
        prefs.edit().putString(KEY_DEFAULT_ROUTE_PREF, routeType.name()).apply();
        notifyDefaultsChanged();
    }

    public void setDefaultArrivalBufferMinutes(int minutes) {
        prefs.edit().putInt(KEY_DEFAULT_BUFFER_MIN, minutes).apply();
        notifyDefaultsChanged();
    }

    // 온보딩(튜토리얼·권한 설정)을 마쳤는지
    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    // 데이터 초기화: 모든 설정을 기본값으로 (온보딩 완료 표시도 지운다)
    @WorkerThread
    void clearAll() {
        prefs.edit().clear().commit();
    }

    // 저장 후 즉시 앱 전체에 적용한다.
    public void setThemeMode(@NonNull ThemeMode mode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name()).apply();
        AppCompatDelegate.setDefaultNightMode(mode.nightMode);
    }

    public void applySavedThemeMode() {
        AppCompatDelegate.setDefaultNightMode(getThemeMode().nightMode);
    }

    // ===== 내부 =====

    @NonNull
    private Settings read() {
        return new Settings(isVibrationEnabled(), isScreenAlertEnabled(), isVoiceEnabled(),
                defaultRoutePreference(), defaultArrivalBufferMinutes(), getThemeMode());
    }

    private void notifyDefaultsChanged() {
        ScheduleChangeListener listener = changeListener;
        if (listener == null) return;
        ZetteDatabase.IO.execute(listener::onDefaultsChanged);
    }

    @NonNull
    private static <E extends Enum<E>> E parseEnum(@NonNull Class<E> type, @Nullable String name,
                                                   @NonNull E fallback) {
        if (name == null) return fallback;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
