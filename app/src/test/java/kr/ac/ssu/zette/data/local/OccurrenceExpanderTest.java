package kr.ac.ssu.zette.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import kr.ac.ssu.zette.data.local.entity.RoutineEntity;
import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleStatus;

public class OccurrenceExpanderTest {

    // 2026-11-18 = 수요일
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);
    private static final LocalDateTime NOW = LocalDateTime.of(TODAY, LocalTime.NOON);
    private static final LocalDate MON_16 = LocalDate.of(2026, 11, 16);
    private static final LocalDate TUE_17 = LocalDate.of(2026, 11, 17);
    private static final LocalDate THU_19 = LocalDate.of(2026, 11, 19);
    private static final LocalDate FRI_20 = LocalDate.of(2026, 11, 20);
    private static final LocalDate WED_25 = LocalDate.of(2026, 11, 25);
    private static final LocalDate THU_26 = LocalDate.of(2026, 11, 26);
    private static final LocalDate FRI_27 = LocalDate.of(2026, 11, 27);
    private static final LocalDate CREATED = LocalDate.of(2026, 11, 1);

    private static final List<ScheduleEntity> NO_ROWS = Collections.emptyList();
    private static final List<RoutineEntity> NO_ROUTINES = Collections.emptyList();

    // 1. 일반 일정 3개(시각 뒤섞음) → 시간순
    @Test
    public void normalSchedules_sortedByArrivalTime() {
        List<ScheduleEntity> rows = Arrays.asList(
                schedule(1, "C", FRI_20, LocalTime.of(15, 0)),
                schedule(2, "A", FRI_20, LocalTime.of(9, 0)),
                schedule(3, "B", FRI_20, LocalTime.of(12, 0)));

        List<Schedule> day = OccurrenceExpander.expandDay(FRI_20, rows, NO_ROUTINES, NOW);

        assertEquals(3, day.size());
        assertEquals(LocalTime.of(9, 0), day.get(0).arrivalTime);
        assertEquals(LocalTime.of(12, 0), day.get(1).arrivalTime);
        assertEquals(LocalTime.of(15, 0), day.get(2).arrivalTime);
        assertEquals("s:2", day.get(0).key().encode());
    }

    // 2. 월·수·금 루틴, 수요일 조회 → 회차 1개, 키 r:id:날짜
    @Test
    public void weeklyRoutine_wednesday_oneOccurrence() {
        RoutineEntity routine = mwfRoutine(3);

        List<Schedule> day = OccurrenceExpander.expandDay(WED_25, NO_ROWS, list(routine), NOW);

        assertEquals(1, day.size());
        assertEquals("r:3:2026-11-25", day.get(0).key().encode());
        assertEquals(ScheduleStatus.RESERVED, day.get(0).status);
        assertEquals(0, day.get(0).id);
    }

    // 3. 루틴 생성일 이전 날짜 → 회차 없음
    @Test
    public void beforeCreatedAt_noOccurrence() {
        RoutineEntity routine = mwfRoutine(3);
        routine.createdAt = LocalDateTime.of(FRI_20, LocalTime.of(10, 0));

        assertTrue(OccurrenceExpander.expandDay(TODAY, NO_ROWS, list(routine), NOW).isEmpty());
        assertEquals(1, OccurrenceExpander.expandDay(FRI_20, NO_ROWS, list(routine), NOW).size());
    }

    // 4. 루틴 activated=false → 회차 있음, activated=false
    @Test
    public void inactiveRoutine_occurrenceShownInactive() {
        RoutineEntity routine = mwfRoutine(3);
        routine.activated = false;

        List<Schedule> day = OccurrenceExpander.expandDay(WED_25, NO_ROWS, list(routine), NOW);

        assertEquals(1, day.size());
        assertFalse(day.get(0).activated);
        assertEquals(ScheduleStatus.UNRESERVED, day.get(0).status);
    }

    // 5. 회차 하루만 알림 끔 → 그날만 false
    @Test
    public void occurrenceAlarmOff_onlyThatDay() {
        RoutineEntity routine = mwfRoutine(3);
        ScheduleEntity row = EntityMapper.occurrenceRowOf(routine, WED_25);
        row.id = 10;
        row.activated = false;
        row.status = ScheduleStatus.UNRESERVED;

        Map<LocalDate, List<Schedule>> map =
                OccurrenceExpander.expand(WED_25, FRI_27, list(row), list(routine), NOW);

        assertEquals(1, map.get(WED_25).size());
        assertFalse(map.get(WED_25).get(0).activated);
        assertEquals("r:3:2026-11-25", map.get(WED_25).get(0).key().encode());
        assertEquals(1, map.get(FRI_27).size());
        assertTrue(map.get(FRI_27).get(0).activated);
    }

    // 6. 회차를 수요일 → 목요일로 이동 → 수요일 없음, 목요일에 1개
    @Test
    public void occurrenceMoved_wednesdayToThursday() {
        RoutineEntity routine = mwfRoutine(3);
        ScheduleEntity row = EntityMapper.occurrenceRowOf(routine, WED_25);
        row.id = 11;
        row.contentOverridden = true;
        row.date = THU_26;
        row.arrivalTime = LocalTime.of(10, 0);
        row.status = ScheduleStatus.RESERVED;

        Map<LocalDate, List<Schedule>> map =
                OccurrenceExpander.expand(WED_25, THU_26, list(row), list(routine), NOW);

        assertTrue(map.get(WED_25).isEmpty());
        assertEquals(1, map.get(THU_26).size());
        Schedule moved = map.get(THU_26).get(0);
        assertEquals("r:3:2026-11-25", moved.key().encode());
        assertEquals(LocalTime.of(10, 0), moved.arrivalTime);
    }

    // 7. 회차 하루만 삭제 → 그날 없음, 다른 날 그대로
    @Test
    public void occurrenceSkipped_onlyThatDayRemoved() {
        RoutineEntity routine = mwfRoutine(3);
        ScheduleEntity row = EntityMapper.occurrenceRowOf(routine, WED_25);
        row.id = 12;
        row.skipped = true;

        Map<LocalDate, List<Schedule>> map =
                OccurrenceExpander.expand(WED_25, FRI_27, list(row), list(routine), NOW);

        assertTrue(map.get(WED_25).isEmpty());
        assertEquals(1, map.get(FRI_27).size());
    }

    // 8. 루틴 삭제(deleted_at=오늘) → 어제는 있음, 오늘·내일 없음
    @Test
    public void deletedRoutine_pastKept_todayAndFutureRemoved() {
        RoutineEntity routine = dailyRoutine(4);
        routine.deletedAt = TODAY;

        Map<LocalDate, List<Schedule>> map =
                OccurrenceExpander.expand(TUE_17, THU_19, NO_ROWS, list(routine), NOW);

        assertEquals(1, map.get(TUE_17).size());
        assertTrue(map.get(TODAY).isEmpty());
        assertTrue(map.get(THU_19).isEmpty());
    }

    // 9. 매월 31일 루틴, 11월 → 회차 없음
    @Test
    public void monthly31_november_noOccurrence() {
        RoutineEntity routine = baseRoutine(5);
        routine.repeatType = Routine.RepeatType.MONTHLY;
        routine.repeatDate = list(31);

        Map<LocalDate, List<Schedule>> november = OccurrenceExpander.expand(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), NO_ROWS, list(routine), NOW);
        for (List<Schedule> day : november.values()) {
            assertTrue(day.isEmpty());
        }
        assertEquals(1, OccurrenceExpander.expandDay(
                LocalDate.of(2026, 12, 31), NO_ROWS, list(routine), NOW).size());
    }

    // 10. 과거 날짜, 행 없는 activated 회차 → MISSED
    @Test
    public void pastActivatedOccurrenceWithoutRow_missed() {
        RoutineEntity routine = mwfRoutine(3);

        List<Schedule> day = OccurrenceExpander.expandDay(MON_16, NO_ROWS, list(routine), NOW);

        assertEquals(1, day.size());
        assertTrue(day.get(0).activated);
        assertEquals(ScheduleStatus.MISSED, day.get(0).status);
    }

    // 11. observeActiveSchedulesOn용 거르기 → activated 회차만
    @Test
    public void filterActive_onlyActivated() {
        RoutineEntity on = mwfRoutine(3);
        RoutineEntity off = mwfRoutine(6);
        off.name = "꺼진 루틴";
        off.activated = false;

        List<Schedule> all = OccurrenceExpander.expandDay(WED_25, NO_ROWS, list(on, off), NOW);
        List<Schedule> active = OccurrenceExpander.filterActive(all);

        assertEquals(2, all.size());
        assertEquals(1, active.size());
        assertEquals("r:3:2026-11-25", active.get(0).key().encode());
    }

    // ===== 테스트 데이터 =====

    private static ScheduleEntity schedule(long id, String name, LocalDate date, LocalTime time) {
        ScheduleEntity e = new ScheduleEntity();
        e.id = id;
        e.name = name;
        e.date = date;
        e.arrivalTime = time;
        e.routeType = Schedule.RouteType.MinTime;
        e.arrivalBuffer = 5;
        e.activated = true;
        e.status = ScheduleStatus.RESERVED;
        return e;
    }

    private static RoutineEntity baseRoutine(long id) {
        RoutineEntity r = new RoutineEntity();
        r.id = id;
        r.name = "루틴 " + id;
        r.arrivalTime = LocalTime.of(8, 30);
        r.routeType = Schedule.RouteType.MinTime;
        r.arrivalBuffer = 5;
        r.activated = true;
        r.createdAt = LocalDateTime.of(CREATED, LocalTime.of(9, 0));
        return r;
    }

    private static RoutineEntity mwfRoutine(long id) {
        RoutineEntity r = baseRoutine(id);
        r.repeatType = Routine.RepeatType.WEEKLY;
        r.weekdays = new boolean[]{true, false, true, false, true, false, false};
        return r;
    }

    private static RoutineEntity dailyRoutine(long id) {
        RoutineEntity r = baseRoutine(id);
        r.repeatType = Routine.RepeatType.WEEKLY;
        r.weekdays = new boolean[]{true, true, true, true, true, true, true};
        return r;
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }
}
