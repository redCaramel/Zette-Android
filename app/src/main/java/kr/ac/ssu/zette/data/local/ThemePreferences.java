package kr.ac.ssu.zette.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import kr.ac.ssu.zette.R;

public final class ThemePreferences {

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

    private static final String PREFS_NAME = "zette_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final ThemeMode DEFAULT_MODE = ThemeMode.SYSTEM;

    private ThemePreferences() {
    }

    @NonNull
    public static ThemeMode getThemeMode(@NonNull Context context) {
        String saved = prefs(context).getString(KEY_THEME_MODE, DEFAULT_MODE.name());
        try {
            return ThemeMode.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return DEFAULT_MODE;
        }
    }

    public static void setThemeMode(@NonNull Context context, @NonNull ThemeMode mode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name()).apply();
        AppCompatDelegate.setDefaultNightMode(mode.nightMode);
    }

    public static void applySavedThemeMode(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(getThemeMode(context).nightMode);
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
