package kr.ac.ssu.zette.data.local;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import kr.ac.ssu.zette.data.ScheduleChangeListener;
import kr.ac.ssu.zette.data.local.dao.RoutineDao;
import kr.ac.ssu.zette.data.local.dao.ScheduleDao;
import kr.ac.ssu.zette.data.local.entity.RoutineEntity;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;

// 화면용 저장소. 읽기는 LiveData, 쓰기는 작업 스레드에서 실행하고 콜백은 메인 스레드로 돌려준다.
public final class ScheduleRepository {

    private static final String TAG = "ScheduleRepository";

    public interface Callback<T> {
        void onComplete(@Nullable T result);
    }

    private final LocalScheduleSource core;
    private final ScheduleDao scheduleDao;
    private final RoutineDao routineDao;
    private final Executor io;
    private final Executor main;
    @Nullable
    private volatile ScheduleChangeListener changeListener;
    // 위젯 등 화면 쪽 갱신용 (백그라운드 리스너와 별개)
    private final List<Runnable> dataChangedListeners = new CopyOnWriteArrayList<>();

    private final Map<LocalDate, LiveData<List<Schedule>>> dayCache = new HashMap<>();
    private final Map<LocalDate, LiveData<List<Schedule>>> activeDayCache = new HashMap<>();
    private final Map<YearMonth, LiveData<Set<LocalDate>>> monthCache = new HashMap<>();
    // Splash가 미리 계산해 둔 날짜별 목록 (첫 화면을 바로 채우는 용도)
    private final Map<LocalDate, List<Schedule>> preloaded = new ConcurrentHashMap<>();

    ScheduleRepository(@NonNull ZetteDatabase db, @NonNull LocalScheduleSource core,
                       @NonNull Executor io, @NonNull Executor main) {
        this.core = core;
        this.scheduleDao = db.scheduleDao();
        this.routineDao = db.routineDao();
        this.io = io;
        this.main = main;
    }

    void setChangeListener(@Nullable ScheduleChangeListener listener) {
        changeListener = listener;
    }

    // 저장·삭제가 끝날 때마다 작업 스레드에서 호출된다
    public void addOnDataChangedListener(@NonNull Runnable listener) {
        dataChangedListeners.add(listener);
    }

    // ===== 읽기 =====

    // 그날 전체, 시간순 (위젯 등 화면 밖 동기 조회)
    @WorkerThread
    @NonNull
    public List<Schedule> getSchedulesOn(@NonNull LocalDate date) {
        return dayOf(core.expand(date, date), date);
    }

    // 그날 전체 (activated true/false 모두), 시간순
    @MainThread
    @NonNull
    public LiveData<List<Schedule>> observeSchedulesOn(@NonNull LocalDate date) {
        LiveData<List<Schedule>> cached = dayCache.get(date);
        if (cached == null) {
            cached = observeComputed(() -> dayOf(core.expand(date, date), date), preloaded.remove(date));
            dayCache.put(date, cached);
        }
        return cached;
    }

    // 그날 activated == true 만, 시간순
    @MainThread
    @NonNull
    public LiveData<List<Schedule>> observeActiveSchedulesOn(@NonNull LocalDate date) {
        LiveData<List<Schedule>> cached = activeDayCache.get(date);
        if (cached == null) {
            cached = map(observeSchedulesOn(date), OccurrenceExpander::filterActive);
            activeDayCache.put(date, cached);
        }
        return cached;
    }

    // 캘린더 점: 일정이 있는 날짜 (activated 무관)
    @MainThread
    @NonNull
    public LiveData<Set<LocalDate>> observeDatesWithSchedules(@NonNull YearMonth month) {
        LiveData<Set<LocalDate>> cached = monthCache.get(month);
        if (cached == null) {
            cached = observeComputed(() -> {
                Set<LocalDate> dates = new TreeSet<>();
                for (Map.Entry<LocalDate, List<Schedule>> day
                        : core.expand(month.atDay(1), month.atEndOfMonth()).entrySet()) {
                    if (!day.getValue().isEmpty()) dates.add(day.getKey());
                }
                return dates;
            }, null);
            monthCache.put(month, cached);
        }
        return cached;
    }

    @MainThread
    @NonNull
    public LiveData<Schedule> observeSchedule(@NonNull ScheduleKey key) {
        return observeComputed(() -> core.find(key), null);
    }

    // 삭제되지 않은 루틴
    @MainThread
    @NonNull
    public LiveData<List<Routine>> observeRoutines() {
        return map(routineDao.observeNotDeleted(), entities -> {
            List<Routine> routines = new ArrayList<>();
            for (RoutineEntity e : entities) routines.add(EntityMapper.toRoutine(e));
            return routines;
        });
    }

    // 화면이 모두 떠난 날짜·월 캐시를 비운다
    @MainThread
    public void clearCache() {
        dayCache.clear();
        activeDayCache.clear();
        monthCache.clear();
    }

    // Splash에서 오늘 목록을 미리 계산해 둔다 (작업 스레드)
    void warmUp(@NonNull LocalDate date) {
        preloaded.put(date, dayOf(core.expand(date, date), date));
    }

    // ===== 일정 쓰기 =====

    public void addSchedule(@NonNull Schedule s, @Nullable Callback<ScheduleKey> done) {
        write(() -> {
            ScheduleKey key = core.addSchedule(s);
            notify(l -> l.onScheduleSaved(key));
            return key;
        }, done);
    }

    public void updateSchedule(@NonNull Schedule s, @Nullable Callback<Void> done) {
        write(() -> {
            core.updateSchedule(s);
            notify(l -> l.onScheduleSaved(s.key()));
            return null;
        }, done);
    }

    // 회차면 그날만 skipped
    public void deleteSchedule(@NonNull ScheduleKey key, @Nullable Callback<Void> done) {
        write(() -> {
            core.deleteSchedule(key);
            notify(l -> l.onScheduleRemoved(key));
            return null;
        }, done);
    }

    // 예약됨 ↔ 미예약 (회차는 그날만)
    public void setScheduleActivated(@NonNull ScheduleKey key, boolean on) {
        write(() -> {
            core.setScheduleActivated(key, on);
            if (on) {
                notify(l -> l.onScheduleSaved(key));
            } else {
                notify(l -> l.onScheduleRemoved(key));
            }
            return null;
        }, null);
    }

    // 완료 처리
    public void markDone(@NonNull ScheduleKey key) {
        write(() -> {
            core.markDone(key);
            notify(l -> l.onScheduleRemoved(key));
            return null;
        }, null);
    }

    // 회차 하루만 수정·이동
    public void editOccurrence(@NonNull ScheduleKey key, @NonNull Schedule edited,
                               @Nullable Callback<Void> done) {
        write(() -> {
            core.editOccurrence(key, edited);
            notify(l -> l.onScheduleSaved(key));
            return null;
        }, done);
    }

    // ===== 루틴 쓰기 =====

    public void addRoutine(@NonNull Routine r, @Nullable Callback<Long> done) {
        write(() -> {
            long id = core.addRoutine(r);
            notify(l -> l.onRoutineChanged(id));
            return id;
        }, done);
    }

    // 지난 회차 보존 후 수정
    public void updateRoutine(@NonNull Routine r, @Nullable Callback<Void> done) {
        write(() -> {
            core.updateRoutine(r);
            notify(l -> l.onRoutineChanged(r.id));
            return null;
        }, done);
    }

    // 즉시 모든 회차에 반영
    public void setRoutineActivated(long routineId, boolean on) {
        write(() -> {
            core.setRoutineActivated(routineId, on);
            notify(l -> l.onRoutineChanged(routineId));
            return null;
        }, null);
    }

    // 소프트 삭제
    public void deleteRoutine(long routineId, @Nullable Callback<Void> done) {
        write(() -> {
            core.deleteRoutine(routineId);
            notify(l -> l.onRoutineChanged(routineId));
            return null;
        }, done);
    }

    // ===== 내부 =====

    // schedule·routine 테이블이 바뀔 때마다 작업 스레드에서 다시 계산해 postValue 한다.
    @NonNull
    private <T> LiveData<T> observeComputed(@NonNull Supplier<T> compute, @Nullable T initial) {
        MediatorLiveData<T> result = initial != null ? new MediatorLiveData<>(initial) : new MediatorLiveData<>();
        AtomicInteger generation = new AtomicInteger();
        Runnable recompute = () -> {
            int gen = generation.incrementAndGet();
            io.execute(() -> {
                T value = compute.get();
                if (gen == generation.get()) result.postValue(value);
            });
        };
        result.addSource(scheduleDao.observeTableChanges(), ignored -> recompute.run());
        result.addSource(routineDao.observeAll(), ignored -> recompute.run());
        return result;
    }

    @NonNull
    private static <A, B> LiveData<B> map(@NonNull LiveData<A> source,
                                           @NonNull java.util.function.Function<A, B> mapper) {
        MediatorLiveData<B> result = new MediatorLiveData<>();
        result.addSource(source, value -> result.setValue(value == null ? null : mapper.apply(value)));
        return result;
    }

    @NonNull
    private static List<Schedule> dayOf(@NonNull Map<LocalDate, List<Schedule>> map, @NonNull LocalDate date) {
        List<Schedule> list = map.get(date);
        return list != null ? list : Collections.emptyList();
    }

    private interface Work<T> {
        T run();
    }

    private <T> void write(@NonNull Work<T> work, @Nullable Callback<T> done) {
        io.execute(() -> {
            T result;
            try {
                result = work.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "저장 실패", e);
                result = null;
            }
            if (done != null) {
                T finalResult = result;
                main.execute(() -> done.onComplete(finalResult));
            }
        });
    }

    private void notify(@NonNull java.util.function.Consumer<ScheduleChangeListener> call) {
        for (Runnable r : dataChangedListeners) {
            try {
                r.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "변경 알림 실패", e);
            }
        }
        ScheduleChangeListener listener = changeListener;
        if (listener == null) return;
        try {
            call.accept(listener);
        } catch (RuntimeException e) {
            Log.e(TAG, "변경 알림 실패", e);
        }
    }
}
