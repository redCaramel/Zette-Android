package kr.ac.ssu.zette.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// 로딩 상태 공용 코드 (Figma Mobile Screens "로딩 표시 규칙", 02-L·04-L·06-L·07-L·12-L·14-L)
public final class Loading {

    // TODO - api연결 구현: 서버 응답이 없어 로딩을 3초 대기로 흉내 낸다
    public static final long FAKE_DELAY_MS = 3000;

    private Loading() {}

    // 스켈레톤: alpha 1 ↔ 0.5 를 약 1초 주기로 반복
    public static void pulse(@NonNull View skeleton) {
        stopPulse(skeleton);
        ObjectAnimator anim = ObjectAnimator.ofFloat(skeleton, View.ALPHA, 1f, 0.5f);
        anim.setDuration(1000);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        skeleton.setTag(anim);
        anim.start();
    }

    public static void stopPulse(@NonNull View skeleton) {
        if (skeleton.getTag() instanceof Animator) ((Animator) skeleton.getTag()).cancel();
        skeleton.setTag(null);
        skeleton.setAlpha(1f);
    }

    // 깜빡임 방지: 300ms 안에 끝나면 스켈레톤을 띄우지 않고, 한 번 띄우면 최소 500ms 유지한다.
    // finish 의 onDone(실제 화면 그리기)은 스켈레톤이 사라진 뒤에 실행된다
    public static final class Gate {
        private static final long SHOW_AFTER_MS = 300;
        private static final long MIN_SHOWN_MS = 500;

        private final View[] skeletons;
        // View.postDelayed 는 View 가 떨어진 뒤 취소되지 않아 Handler 에 예약한다
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean waiting;
        private long shownAt = -1;
        @Nullable
        private Runnable pendingDone;

        private final Runnable show = this::showNow;
        private final Runnable hide = () -> {
            hideNow();
            Runnable done = pendingDone;
            pendingDone = null;
            if (done != null) done.run();
        };

        public Gate(@NonNull View... skeletons) {
            this.skeletons = skeletons;
        }

        public void start() {
            cancel();
            waiting = true;
            handler.postDelayed(show, SHOW_AFTER_MS);
        }

        public void finish(@NonNull Runnable onDone) {
            if (pendingDone != null) {      // 최소 표시 시간을 기다리는 중이면 마지막 결과만 그린다
                pendingDone = onDone;
                return;
            }
            if (!waiting) {
                onDone.run();
                return;
            }
            waiting = false;
            handler.removeCallbacks(show);
            if (shownAt < 0) {
                onDone.run();
                return;
            }
            pendingDone = onDone;
            handler.postDelayed(hide, Math.max(0, shownAt + MIN_SHOWN_MS - SystemClock.uptimeMillis()));
        }

        public void cancel() {
            handler.removeCallbacks(show);
            handler.removeCallbacks(hide);
            pendingDone = null;
            waiting = false;
            hideNow();
        }

        private void showNow() {
            shownAt = SystemClock.uptimeMillis();
            for (View v : skeletons) {
                v.setVisibility(View.VISIBLE);
                pulse(v);
            }
        }

        private void hideNow() {
            shownAt = -1;
            for (View v : skeletons) {
                stopPulse(v);
                v.setVisibility(View.GONE);
            }
        }
    }
}
