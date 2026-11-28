package kr.ac.ssu.zette;

import android.app.Application;

import com.kakao.sdk.common.KakaoSdk;

import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.data.local.debug.DebugSetup;

public class ZetteApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsStore.getInstance(this).applySavedThemeMode();
        DebugSetup.install(this);
        // 키가 없으면 카카오 공유 대신 기본 공유 시트를 쓴다 (ShareDialog)
        if (!BuildConfig.KAKAO_NATIVE_APP_KEY.isEmpty()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY);
        }
    }
}
