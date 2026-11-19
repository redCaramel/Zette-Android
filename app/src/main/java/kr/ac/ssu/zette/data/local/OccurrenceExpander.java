package kr.ac.ssu.zette.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import kr.ac.ssu.zette.data.local.entity.RoutineEntity;
import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleStatus;

// 저장된 일정·회차 상태 행·루틴 → 날짜별 일정 목록 (순수 Java)
public final class OccurrenceExpander {

    public static final Comparator<Schedule> ORDER = Comparator
            .comparing((Schedule s) -> s.arrivalTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(s -> s.name, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(s -> s.key().encode());

    private OccurrenceExpander() {
    }

    // rows: date 또는 occurrence_date가 기간 안인 schedule 행 / routines: 소프트 삭제된 루틴 포함
    @NonNull
    public static Map<LocalDate, List<Schedule>> expand(@NonNull LocalDate from, @NonNull LocalDate to,
                                                        @NonNull Collection<ScheduleEntity> rows,
                                                        @NonNull Collection<RoutineEntity> routines,
                                                        @NonNull LocalDateTime now) {
        Map<LocalDate, List<Schedule>> result = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.put(d, new ArrayList<>());
        }

        Map<Long, RoutineEntity> routineById = new HashMap<>();
        for (RoutineEntity r : routines) {
            routineById.put(r.id, r);
        }

        Map<Long, Set<LocalDate>> rowOccurrences = new HashMap<>();
        for (ScheduleEntity row : rows) {
            if (row.routineId == null) {
                addIfInRange(result, row.date, EntityMapper.toSchedule(row));
                continue;
            }
            if (row.occurrenceDate == null) continue;
            rowOccurrences.computeIfAbsent(row.routineId, k -> new TreeSet<>()).add(row.occurrenceDate);
            if (row.skipped) continue;

            Schedule s = fromOccurrenceRow(row, routineById.get(row.routineId), now);
            addIfInRange(result, s.date, s);
        }

        for (RoutineEntity routine : routines) {
            Set<LocalDate> handled = rowOccurrences.get(routine.id);
            for (LocalDate d : result.keySet()) {
                if (!isCalculatedOccurrence(routine, d)) continue;
                if (handled != null && handled.contains(d)) continue;
                result.get(d).add(calculatedOccurrence(routine, d, now));
            }
        }

        for (List<Schedule> list : result.values()) {
            list.sort(ORDER);
        }
        return result;
    }

    @NonNull
    public static List<Schedule> expandDay(@NonNull LocalDate day, @NonNull Collection<ScheduleEntity> rows,
                                           @NonNull Collection<RoutineEntity> routines,
                                           @NonNull LocalDateTime now) {
        List<Schedule> list = expand(day, day, rows, routines, now).get(day);
        return list != null ? list : new ArrayList<>();
    }

    @NonNull
    public static List<Schedule> filterActive(@NonNull List<Schedule> schedules) {
        List<Schedule> active = new ArrayList<>();
        for (Schedule s : schedules) {
            if (s.activated) active.add(s);
        }
        return active;
    }

    // 생성일·삭제일·반복 규칙상 이 날짜에 회차가 있는지 (회차 상태 행 여부는 보지 않음)
    public static boolean isCalculatedOccurrence(@NonNull RoutineEntity routine, @NonNull LocalDate d) {
        if (routine.createdAt != null && d.isBefore(routine.createdAt.toLocalDate())) return false;
        if (routine.deletedAt != null && !d.isBefore(routine.deletedAt)) return false;
        return EntityMapper.toRoutine(routine).occursOn(d);
    }

    // 회차 상태 행 하나를 화면용 일정으로 만든다 (skipped 여부는 호출한 쪽에서 본다)
    @NonNull
    public static Schedule fromOccurrenceRow(@NonNull ScheduleEntity row, @Nullable RoutineEntity routine,
                                              @NonNull LocalDateTime now) {
        Schedule s;
        if (row.contentOverridden || routine == null) {
            s = EntityMapper.toSchedule(row);
        } else {
            s = EntityMapper.occurrenceOf(routine, row.occurrenceDate);
            s.id = row.id;
            EntityMapper.copyState(row, s);
        }
        boolean routineOn = routine == null || routine.activated;
        s.activated = routineOn && row.activated;
        if (s.status == null) {
            s.status = defaultStatus(s, now);
        }
        return s;
    }

    // 행이 없는 계산 회차
    @NonNull
    public static Schedule calculatedOccurrence(@NonNull RoutineEntity routine, @NonNull LocalDate d,
                                                 @NonNull LocalDateTime now) {
        Schedule s = EntityMapper.occurrenceOf(routine, d);
        s.activated = routine.activated;
        s.status = defaultStatus(s, now);
        return s;
    }

    // 행이 없는 회차의 상태: activated면 RESERVED(도착 일시가 지났으면 MISSED), 아니면 UNRESERVED
    @NonNull
    public static ScheduleStatus defaultStatus(@NonNull Schedule s, @NonNull LocalDateTime now) {
        if (!s.activated) return ScheduleStatus.UNRESERVED;
        if (s.date != null && s.arrivalTime != null
                && LocalDateTime.of(s.date, s.arrivalTime).isBefore(now)) {
            return ScheduleStatus.MISSED;
        }
        return ScheduleStatus.RESERVED;
    }

    // ===== 내부 =====

    private static void addIfInRange(@NonNull Map<LocalDate, List<Schedule>> result,
                                     @Nullable LocalDate date, @NonNull Schedule s) {
        if (date == null) return;
        List<Schedule> list = result.get(date);
        if (list != null) list.add(s);
    }
}
