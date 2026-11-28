package kr.ac.ssu.zette.data.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import kr.ac.ssu.zette.data.ScheduleChangeListener;
import kr.ac.ssu.zette.data.ScheduleSource;
import kr.ac.ssu.zette.data.SettingsSource;

// 로컬 데이터 묶음 (DB, 일정 저장소, 설정). 어디서 처음 불리든 그 자리에서 초기화된다.
// Splash를 거치지 않는 Receiver·Worker·Service에서도 get()으로 바로 쓸 수 있다.
public final class ZetteData {

    private static volatile ZetteData instance;

    @NonNull
    public final ZetteDatabase database;
    @NonNull
    public final SettingsStore settings;
    @NonNull
    public final ScheduleRepository schedules;
    @NonNull
    private final LocalScheduleSource scheduleSource;

    private ZetteData(@NonNull ZetteDatabase database, @NonNull SettingsStore settings,
                      @NonNull Supplier<LocalDateTime> clock, @NonNull Executor io,
                      @NonNull Executor main) {
        this.database = database;
        this.settings = settings;
        this.scheduleSource = new LocalScheduleSource(database, clock);
        this.schedules = new ScheduleRepository(database, scheduleSource, io, main);
    }

    @NonNull
    public static ZetteData get(@NonNull Context context) {
        ZetteData local = instance;
        if (local == null) {
            synchronized (ZetteData.class) {
                local = instance;
                if (local == null) {
                    Context app = context.getApplicationContext();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    local = new ZetteData(ZetteDatabase.create(app), SettingsStore.getInstance(app),
                            LocalDateTime::now, ZetteDatabase.IO, mainHandler::post);
                    instance = local;
                }
            }
        }
        return local;
    }

    // 데이터 초기화 (17-3): 일정·루틴·설정을 모두 지운다. 화면 캐시는 호출한 쪽이 메인에서 비운다
    @WorkerThread
    public void resetAll() {
        database.clearAllTables();
        settings.clearAll();
        // TODO - 알림 모듈 연결: 예약된 출발 알림 모두 취소
    }

    // Splash용 이름. get()과 같다.
    @NonNull
    public static ZetteData init(@NonNull Context context) {
        return get(context);
    }

    // 테스트용: 메모리 DB 등 직접 만든 구성으로 생성 (싱글톤에 등록하지 않음)
    @NonNull
    public static ZetteData createForTest(@NonNull ZetteDatabase database, @NonNull SettingsStore settings,
                                          @NonNull Supplier<LocalDateTime> clock,
                                          @NonNull Executor io, @NonNull Executor main) {
        return new ZetteData(database, settings, clock, io, main);
    }

    @NonNull
    public ScheduleSource scheduleSource() {
        return scheduleSource;
    }

    @NonNull
    public SettingsSource settingsSource() {
        return settings;
    }

    // 백그라운드 모듈의 변경 알림 리스너 등록. null이면 알림을 보내지 않는다.
    public void setChangeListener(@Nullable ScheduleChangeListener listener) {
        schedules.setChangeListener(listener);
        settings.setChangeListener(listener);
    }

    // DB를 실제로 열고(첫 실행이면 테이블 생성) 오늘 목록을 미리 계산한다.
    @WorkerThread
    public void warmUp() {
        database.scheduleDao().count();
        schedules.warmUp(LocalDate.now());
    }
}
