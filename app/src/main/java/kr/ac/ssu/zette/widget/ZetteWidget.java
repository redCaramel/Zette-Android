package kr.ac.ssu.zette.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.data.local.ZetteDatabase;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.activity.MainActivity;
import kr.ac.ssu.zette.ui.activity.RealtimeArrivalActivity;
import kr.ac.ssu.zette.ui.fragment.HomeFragment;
import kr.ac.ssu.zette.ui.fragment.HomeFragment.HeroState;

// 홈 화면 위젯 (Figma 30·31·32). 홈 Hero 와 같은 상태를 보여준다
public class ZetteWidget extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "kr.ac.ssu.zette.widget.REFRESH";

    private static final DateTimeFormatter TITLE_TIME = DateTimeFormatter.ofPattern("a h시 mm분", Locale.KOREAN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        refreshAsync(context, goAsync());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            refreshAsync(context, goAsync());
        }
    }

    @Override
    public void onDisabled(Context context) {
        context.getSystemService(AlarmManager.class).cancel(refreshIntent(context));
    }

    private static void refreshAsync(@NonNull Context context, @Nullable PendingResult result) {
        Context app = context.getApplicationContext();
        ZetteDatabase.IO.execute(() -> {
            try {
                update(app);
            } finally {
                if (result != null) result.finish();
            }
        });
    }

    // 일정 저장·삭제 직후 등 작업 스레드에서 부른다
    @WorkerThread
    public static void update(@NonNull Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ZetteWidget.class));
        if (ids.length == 0) return;

        LocalDateTime now = LocalDateTime.now();
        List<Schedule> today = ZetteData.get(context).schedules.getSchedulesOn(now.toLocalDate());
        Schedule nearest = HomeFragment.findNearest(today, now);
        HeroState state = HomeFragment.heroStateOf(today, nearest, now);

        manager.updateAppWidget(ids, render(context, state, nearest, now));
        scheduleNext(context, nearest, now);
    }

    @NonNull
    private static RemoteViews render(@NonNull Context context, @NonNull HeroState state,
                                      @Nullable Schedule s, @NonNull LocalDateTime now) {
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_zette);
        v.setViewVisibility(R.id.layout_upcoming, state == HeroState.UPCOMING ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.layout_departing, state == HeroState.DEPARTING ? View.VISIBLE : View.GONE);
        boolean empty = s == null;
        v.setViewVisibility(R.id.layout_empty, empty ? View.VISIBLE : View.GONE);

        if (empty) {
            v.setTextViewText(R.id.tv_empty_body, context.getString(state == HeroState.EMPTY_DONE
                    ? R.string.hero_empty_body_done : R.string.hero_empty_body_none));
            v.setOnClickPendingIntent(R.id.widget_root, openApp(context));
            return v;
        }
        v.setOnClickPendingIntent(R.id.widget_root, openRealtime(context, s));
        String place = s.place != null ? s.place.name : "";

        if (state == HeroState.DEPARTING) {
            // TODO - api연결 구현: 실시간 버스 도착 정보 (지금은 홈 Hero 와 같은 샘플)
            String bus = context.getString(R.string.detail_sample_bus_number);
            int arrivalMinutes = 8;
            v.setTextViewText(R.id.tv_dep_title, context.getString(R.string.hero_departing_title, bus, arrivalMinutes));
            v.setTextViewText(R.id.tv_dep_arrival, context.getString(R.string.hero_departing_arrival, arrivalMinutes));
            v.setTextViewText(R.id.tv_dep_stop, context.getString(R.string.realtime_sample_stop));
            v.setTextViewText(R.id.tv_dep_walk, context.getString(R.string.hero_departing_walk, 4));
            return v;
        }

        v.setTextViewText(R.id.tv_title, context.getString(R.string.home_hero_title,
                s.arrivalTime.format(TITLE_TIME), s.name, HomeFragment.subjectParticle(s.name)));
        v.setTextViewText(R.id.tv_place, place);
        LocalDateTime departure = s.computed != null ? s.computed.expectedDeparture : null;
        if (departure == null) {
            // TODO - 출발 시각은 백그라운드 경로 계산 결과. 없으면 홈 Hero 와 같은 샘플
            v.setViewVisibility(R.id.tv_left, View.VISIBLE);
            v.setViewVisibility(R.id.ch_left, View.GONE);
            v.setTextViewText(R.id.tv_left, context.getString(R.string.home_sample_left_time));
            v.setTextViewText(R.id.tv_departure, context.getString(R.string.calendar_departure,
                    context.getString(R.string.detail_sample_departure)));
        } else {
            // 남은 시간은 위젯이 스스로 줄인다 (1분마다 다시 그리지 않음)
            long leftMs = Math.max(0, Duration.between(now, departure).toMillis());
            v.setViewVisibility(R.id.tv_left, View.GONE);
            v.setViewVisibility(R.id.ch_left, View.VISIBLE);
            v.setChronometer(R.id.ch_left, SystemClock.elapsedRealtime() + leftMs, null, true);
            v.setChronometerCountDown(R.id.ch_left, true);
            v.setTextViewText(R.id.tv_departure, context.getString(R.string.calendar_departure,
                    departure.toLocalTime().format(TIME)));
        }
        return v;
    }

    // 다음 상태가 바뀌는 시각(출발 시각·도착 시각·자정 중 가장 이른 때)에 다시 그린다
    private static void scheduleNext(@NonNull Context context, @Nullable Schedule nearest, @NonNull LocalDateTime now) {
        LocalDateTime next = LocalDate.now().plusDays(1).atStartOfDay();
        if (nearest != null) {
            LocalDateTime arrival = LocalDateTime.of(nearest.date, nearest.arrivalTime);
            if (arrival.isBefore(next)) next = arrival;
            LocalDateTime departure = nearest.computed != null ? nearest.computed.expectedDeparture : null;
            if (departure != null && departure.isAfter(now) && departure.isBefore(next)) next = departure;
        }
        long at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000;

        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent refresh = refreshIntent(context);
        // RTC: 화면이 꺼져 있으면 켜질 때 실행 (위젯은 그때 보이므로 충분)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setWindow(AlarmManager.RTC, at, 60_000, refresh);
        } else {
            alarms.setExact(AlarmManager.RTC, at, refresh);
        }
    }

    @NonNull
    private static PendingIntent refreshIntent(@NonNull Context context) {
        Intent intent = new Intent(context, ZetteWidget.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @NonNull
    private static PendingIntent openApp(@NonNull Context context) {
        Intent main = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 1, main,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // 뒤로 가기 하면 앱 홈이 나오도록 홈 위에 실시간 도착을 연다
    @NonNull
    private static PendingIntent openRealtime(@NonNull Context context, @NonNull Schedule s) {
        Intent main = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent realtime = RealtimeArrivalActivity.intent(context, s.key());
        return PendingIntent.getActivities(context, 2, new Intent[]{main, realtime},
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
