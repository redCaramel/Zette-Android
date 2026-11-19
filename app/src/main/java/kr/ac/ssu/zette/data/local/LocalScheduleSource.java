package kr.ac.ssu.zette.data.local;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import kr.ac.ssu.zette.data.ScheduleSource;
import kr.ac.ssu.zette.data.local.dao.RoutineDao;
import kr.ac.ssu.zette.data.local.dao.ScheduleDao;
import kr.ac.ssu.zette.data.local.entity.RoutineEntity;
import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;
import kr.ac.ssu.zette.dto.ComputedRoute;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.dto.ScheduleStatus;

// 동기 조회·쓰기 구현. 백그라운드는 ScheduleSource로, 화면용 ScheduleRepository는 작업 스레드에서 이 클래스를 부른다.
public final class LocalScheduleSource implements ScheduleSource {

    private final ZetteDatabase db;
    private final ScheduleDao scheduleDao;
    private final RoutineDao routineDao;
    private final Supplier<LocalDateTime> clock;

    LocalScheduleSource(@NonNull ZetteDatabase db, @NonNull Supplier<LocalDateTime> clock) {
        this.db = db;
        this.scheduleDao = db.scheduleDao();
        this.routineDao = db.routineDao();
        this.clock = clock;
    }

    // ===== ScheduleSource (백그라운드용) =====

    @WorkerThread
    @Nullable
    @Override
    public Schedule get(@NonNull ScheduleKey key) {
        assertWorkerThread();
        return find(key);
    }

    @WorkerThread
    @NonNull
    @Override
    public List<Schedule> getReservedBetween(@NonNull LocalDate from, @NonNull LocalDate to) {
        assertWorkerThread();
        List<Schedule> reserved = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Schedule>> day : expand(from, to).entrySet()) {
            for (Schedule s : day.getValue()) {
                if (s.activated && s.status == ScheduleStatus.RESERVED) reserved.add(s);
            }
        }
        reserved.sort(Comparator.comparing((Schedule s) -> s.date).thenComparing(OccurrenceExpander.ORDER));
        return reserved;
    }

    @WorkerThread
    @Override
    public void updateStatus(@NonNull ScheduleKey key, @NonNull ScheduleStatus status,
                             @Nullable LocalDateTime expectedArrival) {
        assertWorkerThread();
        db.runInTransaction(() -> {
            ScheduleEntity row = loadWritableRow(key);
            if (row == null) return;
            row.status = status;
            row.expectedArrival = expectedArrival;
            if (status == ScheduleStatus.DONE && row.completedAt == null) {
                row.completedAt = clock.get();
            }
            saveRow(row);
        });
    }

    @WorkerThread
    @Override
    public void updateComputed(@NonNull ScheduleKey key, @NonNull ComputedRoute computed) {
        assertWorkerThread();
        db.runInTransaction(() -> {
            ScheduleEntity row = loadWritableRow(key);
            if (row == null) return;
            row.expectedDeparture = computed.expectedDeparture;
            row.travelMinutes = computed.travelMinutes;
            row.routeSummary = computed.routeSummary;
            row.computedAt = computed.computedAt;
            saveRow(row);
        });
    }

    @WorkerThread
    @NonNull
    @Override
    public List<Routine> getActiveRoutines() {
        assertWorkerThread();
        List<Routine> routines = new ArrayList<>();
        for (RoutineEntity e : routineDao.getActive()) {
            routines.add(EntityMapper.toRoutine(e));
        }
        return routines;
    }

    // ===== 조회 (ScheduleRepository가 작업 스레드에서 사용) =====

    @NonNull
    Map<LocalDate, List<Schedule>> expand(@NonNull LocalDate from, @NonNull LocalDate to) {
        return OccurrenceExpander.expand(from, to, scheduleDao.getInRange(from, to),
                routineDao.getAll(), clock.get());
    }

    @Nullable
    Schedule find(@NonNull ScheduleKey key) {
        if (!key.isOccurrence()) {
            ScheduleEntity row = scheduleDao.findById(key.scheduleId);
            return row == null || row.routineId != null ? null : EntityMapper.toSchedule(row);
        }
        RoutineEntity routine = routineDao.findById(key.routineId);
        ScheduleEntity row = scheduleDao.findOccurrence(key.routineId, key.occurrenceDate);
        if (row != null) {
            return row.skipped ? null : OccurrenceExpander.fromOccurrenceRow(row, routine, clock.get());
        }
        if (routine == null || !OccurrenceExpander.isCalculatedOccurrence(routine, key.occurrenceDate)) {
            return null;
        }
        return OccurrenceExpander.calculatedOccurrence(routine, key.occurrenceDate, clock.get());
    }

    // ===== 일정 쓰기 =====

    @NonNull
    ScheduleKey addSchedule(@NonNull Schedule s) {
        ScheduleEntity e = EntityMapper.toEntity(s);
        e.id = 0;
        e.routineId = null;
        e.occurrenceDate = null;
        e.contentOverridden = false;
        e.skipped = false;
        if (e.status == null) {
            e.status = s.activated ? ScheduleStatus.RESERVED : ScheduleStatus.UNRESERVED;
        }
        long id = scheduleDao.insert(e);
        return ScheduleKey.ofSchedule(id);
    }

    void updateSchedule(@NonNull Schedule s) {
        if (s.isOccurrence()) {
            throw new IllegalArgumentException("루틴 회차는 editOccurrence()로 수정합니다.");
        }
        ScheduleEntity e = EntityMapper.toEntity(s);
        e.routineId = null;
        e.occurrenceDate = null;
        scheduleDao.update(e);
    }

    void deleteSchedule(@NonNull ScheduleKey key) {
        if (!key.isOccurrence()) {
            scheduleDao.deleteById(key.scheduleId);
            return;
        }
        db.runInTransaction(() -> {
            ScheduleEntity row = loadWritableRow(key);
            if (row == null) return;
            row.skipped = true;
            saveRow(row);
        });
    }

    void setScheduleActivated(@NonNull ScheduleKey key, boolean on) {
        db.runInTransaction(() -> {
            ScheduleEntity row = loadWritableRow(key);
            if (row == null) return;
            row.activated = on;
            if (row.status == null || row.status == ScheduleStatus.RESERVED
                    || row.status == ScheduleStatus.UNRESERVED) {
                row.status = on ? ScheduleStatus.RESERVED : ScheduleStatus.UNRESERVED;
            }
            saveRow(row);
        });
    }

    void markDone(@NonNull ScheduleKey key) {
        db.runInTransaction(() -> {
            ScheduleEntity row = loadWritableRow(key);
            if (row == null) return;
            row.status = ScheduleStatus.DONE;
            row.completedAt = clock.get();
            saveRow(row);
        });
    }

    void editOccurrence(@NonNull ScheduleKey key, @NonNull Schedule edited) {
        if (!key.isOccurrence()) {
            throw new IllegalArgumentException("일반 일정은 updateSchedule()로 수정합니다.");
        }
        db.runInTransaction(() -> {
            ScheduleEntity row = loadWritableRow(key);
            if (row == null) return;
            ScheduleEntity content = EntityMapper.toEntity(edited);
            row.name = content.name;
            row.date = content.date;
            row.arrivalTime = content.arrivalTime;
            row.placeId = content.placeId;
            row.placeName = content.placeName;
            row.placeAddress = content.placeAddress;
            row.placeLat = content.placeLat;
            row.placeLng = content.placeLng;
            row.routeType = content.routeType;
            row.arrivalBuffer = content.arrivalBuffer;
            row.contentOverridden = true;
            row.skipped = false;
            saveRow(row);
        });
    }

    // ===== 루틴 쓰기 =====

    long addRoutine(@NonNull Routine r) {
        RoutineEntity e = EntityMapper.toEntity(r);
        e.id = 0;
        e.deletedAt = null;
        if (e.createdAt == null) e.createdAt = clock.get();
        return routineDao.insert(e);
    }

    // 지난 회차를 수정 전 내용으로 고정한 뒤 루틴을 수정한다 (한 트랜잭션)
    void updateRoutine(@NonNull Routine r) {
        db.runInTransaction(() -> {
            RoutineEntity old = routineDao.findById(r.id);
            if (old == null) return;
            preservePastOccurrences(old);

            RoutineEntity updated = EntityMapper.toEntity(r);
            updated.id = old.id;
            updated.createdAt = old.createdAt;
            updated.deletedAt = old.deletedAt;
            routineDao.update(updated);
        });
    }

    void setRoutineActivated(long routineId, boolean on) {
        routineDao.setActivated(routineId, on);
    }

    // 소프트 삭제: 오늘부터 회차가 사라지고, 오늘 이후 회차 상태 행은 지운다 (한 트랜잭션)
    void deleteRoutine(long routineId) {
        LocalDate today = clock.get().toLocalDate();
        db.runInTransaction(() -> {
            routineDao.setDeletedAt(routineId, today);
            scheduleDao.deleteOccurrencesFrom(routineId, today);
        });
    }

    // ===== 내부 =====

    private void preservePastOccurrences(@NonNull RoutineEntity old) {
        LocalDateTime now = clock.get();
        LocalDate yesterday = now.toLocalDate().minusDays(1);

        for (ScheduleEntity row : scheduleDao.getOccurrenceRows(old.id)) {
            if (row.contentOverridden || row.occurrenceDate == null
                    || !row.occurrenceDate.isBefore(now.toLocalDate())) {
                continue;
            }
            EntityMapper.copyRoutineContent(old, row);
            row.date = row.occurrenceDate;
            row.contentOverridden = true;
            scheduleDao.update(row);
        }

        if (old.createdAt == null) return;
        for (LocalDate d = old.createdAt.toLocalDate(); !d.isAfter(yesterday); d = d.plusDays(1)) {
            if (!OccurrenceExpander.isCalculatedOccurrence(old, d)) continue;
            if (scheduleDao.findOccurrence(old.id, d) != null) continue;

            ScheduleEntity row = EntityMapper.occurrenceRowOf(old, d);
            row.contentOverridden = true;
            row.status = OccurrenceExpander.calculatedOccurrence(old, d, now).status;
            scheduleDao.insert(row);
        }
    }

    // 쓰기 대상 행. 일반 일정은 그 행, 회차는 기존 상태 행 또는 루틴 내용으로 채운 새 행.
    @Nullable
    private ScheduleEntity loadWritableRow(@NonNull ScheduleKey key) {
        if (!key.isOccurrence()) {
            return scheduleDao.findById(key.scheduleId);
        }
        ScheduleEntity row = scheduleDao.findOccurrence(key.routineId, key.occurrenceDate);
        if (row != null) return row;

        RoutineEntity routine = routineDao.findById(key.routineId);
        if (routine == null) return null;
        row = EntityMapper.occurrenceRowOf(routine, key.occurrenceDate);
        row.status = OccurrenceExpander.calculatedOccurrence(routine, key.occurrenceDate, clock.get()).status;
        return row;
    }

    private void saveRow(@NonNull ScheduleEntity row) {
        if (row.routineId != null) {
            scheduleDao.upsertOccurrence(row);
        } else {
            scheduleDao.update(row);
        }
    }

    private static void assertWorkerThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("ScheduleSource는 작업 스레드에서만 호출해야 합니다.");
        }
    }
}
