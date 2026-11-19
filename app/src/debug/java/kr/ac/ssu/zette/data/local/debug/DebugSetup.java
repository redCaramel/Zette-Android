package kr.ac.ssu.zette.data.local.debug;

import android.content.Context;

import androidx.annotation.NonNull;

import kr.ac.ssu.zette.data.local.ZetteData;

// 디버그 빌드: 가짜 변경 리스너를 등록한다.
public final class DebugSetup {

    private DebugSetup() {
    }

    public static void install(@NonNull Context context) {
        ZetteData.get(context).setChangeListener(new LoggingChangeListener());
    }
}
