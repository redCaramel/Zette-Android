package kr.ac.ssu.zette;

import android.app.Application;

import kr.ac.ssu.zette.data.local.ThemePreferences;

public class ZetteApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedThemeMode(this);
    }
}
