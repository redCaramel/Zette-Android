package kr.ac.ssu.zette;

import android.app.Application;

import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.data.local.debug.DebugSetup;

public class ZetteApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsStore.getInstance(this).applySavedThemeMode();
        DebugSetup.install(this);
    }
}
