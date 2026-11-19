package kr.ac.ssu.zette.data.local;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import kr.ac.ssu.zette.R;

// 화면 모드 설정값
public enum ThemeMode {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.theme_mode_light),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.theme_mode_dark),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.theme_mode_system);

    public final int nightMode;
    @StringRes
    public final int labelRes;

    ThemeMode(int nightMode, @StringRes int labelRes) {
        this.nightMode = nightMode;
        this.labelRes = labelRes;
    }
}
