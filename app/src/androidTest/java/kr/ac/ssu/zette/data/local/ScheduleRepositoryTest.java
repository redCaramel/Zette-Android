package kr.ac.ssu.zette.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import kr.ac.ssu.zette.data.local.entity.ScheduleEntity;
import kr.ac.ssu.zette.dto.Place;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;

@RunWith(AndroidJUnit4.class)
public class ScheduleRepositoryTest {

    // 2026-11-18 = 수요일
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);
    private static final LocalDateTime NOW = LocalDateTime.of(TODAY, LocalTime.NOON);

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ZetteDatabase db;
    private ZetteData data;
    private LocalScheduleSource core;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = ZetteDatabase.createInMemory(context);
        data = ZetteData.createForTest(db, SettingsStore.getInstance(context), () -> NOW,
                Runnable::run, Runnable::run);
        core = (LocalScheduleSource) data.scheduleSource();
    }

    @After
    public void tearDown() {
        db.close();
    }

    // 일정 추가 → 같은 날 조회에 나옴
    @Test
    public void addSchedule_appearsOnThatDay() {
        Schedule s = new Schedule(0, "회의", TODAY.plusDays(2), LocalTime.of(15, 0),
                new Place("p1", "강남역", "서울", 37.5, 127.0, null), Schedule.RouteType.MinTime, 5);

        ScheduleKey key = core.addSchedule(s);
        List<Schedule> day = core.expand(TODAY.plusDays(2), TODAY.plusDays(2)).get(TODAY.plusDays(2));

        assertEquals(1, day.size());
        assertEquals(key, day.get(0).key());
        assertEquals("강남역", day.get(0).place.name);
    }

    // 회차 상태 행 유니크: 같은 (routine_id, occurrence_date)를 두 번 저장해도 하나로 유지
    @Test
    public void occurrenceRow_upsertKeepsSingleRow() {
        long routineId = core.addRoutine(mwRoutine());
        LocalDate wed = LocalDate.of(2026, 11, 25);

        core.setScheduleActivated(ScheduleKey.ofOccurrence(routineId, wed), false);
        core.setScheduleActivated(ScheduleKey.ofOccurrence(routineId, wed), true);

        List<ScheduleEntity> rows = db.scheduleDao().getOccurrenceRows(routineId);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).activated);
    }

    // 루틴 수정 시 지난 회차 보존: 월·수 → 화. 지난 월·수 기록은 그대로, 앞으로는 화요일만
    @Test
    public void updateRoutine_keepsPastOccurrences() {
        Routine routine = mwRoutine();
        long routineId = core.addRoutine(routine);

        Routine updated = mwRoutine();
        updated.id = routineId;
        updated.weekdays = new boolean[]{false, true, false, false, false, false, false};
        core.updateRoutine(updated);

        Map<LocalDate, List<Schedule>> past = core.expand(LocalDate.of(2026, 11, 2), TODAY.minusDays(1));
        assertEquals(1, past.get(LocalDate.of(2026, 11, 16)).size());   // 월
        assertEquals(1, past.get(LocalDate.of(2026, 11, 11)).size());   // 수
        assertTrue(past.get(LocalDate.of(2026, 11, 17)).isEmpty());     // 화 (수정 전 규칙엔 없음)

        Map<LocalDate, List<Schedule>> future = core.expand(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 25));
        assertTrue(future.get(LocalDate.of(2026, 11, 23)).isEmpty());   // 월
        assertEquals(1, future.get(LocalDate.of(2026, 11, 24)).size()); // 화
        assertTrue(future.get(LocalDate.of(2026, 11, 25)).isEmpty());   // 수
    }

    // LiveData 갱신: 구독 중인 날짜에 일정을 추가하면 목록이 바뀐다
    @Test
    public void observeSchedulesOn_updatesAfterAdd() throws InterruptedException {
        LocalDate day = TODAY.plusDays(3);
        LiveData<List<Schedule>> live = data.schedules.observeSchedulesOn(day);
        CountDownLatch latch = new CountDownLatch(1);
        Observer<List<Schedule>> observer = list -> {
            if (list != null && list.size() == 1) latch.countDown();
        };
        live.observeForever(observer);

        data.schedules.addSchedule(new Schedule(0, "약속", day, LocalTime.of(18, 0), null,
                Schedule.RouteType.MinTime, 5), null);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(live.getValue());
        live.removeObserver(observer);
    }

    private static Routine mwRoutine() {
        boolean[] monWed = {true, false, true, false, false, false, false};
        Routine r = new Routine(0, "출근", Routine.RepeatType.WEEKLY, LocalTime.of(8, 30), null,
                Schedule.RouteType.MinTime, 5, monWed, null);
        r.createdAt = LocalDateTime.of(2026, 11, 1, 9, 0);
        return r;
    }
}
