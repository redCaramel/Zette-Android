package kr.ac.ssu.zette.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.ActivityRealtimeArrivalBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.ui.KakaoMap;
import kr.ac.ssu.zette.ui.Loading;
import kr.ac.ssu.zette.ui.Network;

// 실시간 도착 (Figma 07). 홈 Hero 에서 들어온다.
// 출발 1시간 전보다 이르면 예상 출발만 보여주는 07-1 화면, 그 뒤로는 실시간 도착 화면. 매 분 다시 판단한다
// shortcut: 버스 도착 정보(도착 카드·도착 예정 목록·승차 정류장)는 샘플 문자열이다. 실시간 API 연결 시 교체
public class RealtimeArrivalActivity extends AppCompatActivity {

    private static final String EXTRA_KEY = "schedule_key";
    static final long REALTIME_WINDOW_MINUTES = 60;   // 출발 몇 분 전부터 실시간 도착을 보여줄지

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMAT =
            DateTimeFormatter.ofPattern("E", Locale.KOREAN);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN);

    enum Mode { FAR, REALTIME }

    private ActivityRealtimeArrivalBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private Schedule lastSchedule;
    @Nullable
    private Mode mode;   // 아직 일정을 받기 전이면 null

    // 07-E: 보여줄 값이 없으면 전체 화면 오류, 이미 보여주던 값이 있으면 "몇 시 기준" 배너만 띄운다
    private boolean hasContent;
    private LocalTime lastUpdated;

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull ScheduleKey key) {
        return new Intent(context, RealtimeArrivalActivity.class).putExtra(EXTRA_KEY, key.encode());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRealtimeArrivalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String encodedKey = getIntent().getStringExtra(EXTRA_KEY);
        if (encodedKey == null) {
            finish();
            return;
        }
        ScheduleKey key = ScheduleKey.decode(encodedKey);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.arrivalSkeleton.layoutSkeletonChips.setVisibility(View.GONE);
        binding.layoutArrivalLoading.tvLoading.setText(R.string.realtime_loading);
        binding.btnOfflineRetry.setOnClickListener(v -> loadArrivals());
        binding.networkBanner.btnRetry.setOnClickListener(v -> loadArrivals());
        // 연결되면 배너·오류 화면은 스스로 다시 불러온다 (07-1 은 저장된 값만 써서 네트워크와 무관)
        Network.get(this).observe(this, online -> {
            boolean showingError = binding.networkBanner.getRoot().getVisibility() == View.VISIBLE
                    || binding.layoutOffline.getVisibility() == View.VISIBLE;
            if (online && showingError && mode == Mode.REALTIME) loadArrivals();
        });

        // 삭제되었거나 찾을 수 없는 일정이면 닫는다. 예상 출발이 다시 계산되면 모드도 다시 정한다
        ZetteData.get(this).schedules.observeSchedule(key).observe(this, schedule -> {
            if (schedule == null) {
                finish();
                return;
            }
            lastSchedule = schedule;
            refreshForNow(false);
        });
    }

    // 매 분 정각에 모드를 다시 정하고 화면을 새로 그린다. 화면이 보일 때만 받는다
    private final BroadcastReceiver minuteTick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshForNow(true);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) return;
        refreshForNow(true);
        ContextCompat.registerReceiver(this, minuteTick,
                new IntentFilter(Intent.ACTION_TIME_TICK), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding != null) unregisterReceiver(minuteTick);
    }

    // tick: 1분 갱신·화면 복귀면 true (실시간 도착을 다시 불러온다), 일정 값만 바뀌었으면 false
    private void refreshForNow(boolean tick) {
        Schedule s = lastSchedule;
        if (s == null) return;
        LocalDateTime departure = departureOf(s);
        Mode next = modeAt(LocalDateTime.now(), departure);
        boolean changed = next != mode;
        mode = next;
        if (changed) showMode(next);

        if (next == Mode.FAR) {
            applyFar(s, departure);
        } else {
            applyRealtimeInfo(s, departure);
            if (changed || tick) loadArrivals();
        }
    }

    // 출발 1시간 전이 되기 전이면 07-1
    @NonNull
    static Mode modeAt(@NonNull LocalDateTime now, @NonNull LocalDateTime departure) {
        return now.isBefore(departure.minusMinutes(REALTIME_WINDOW_MINUTES)) ? Mode.FAR : Mode.REALTIME;
    }

    @NonNull
    private static LocalDateTime departureOf(@NonNull Schedule s) {
        if (s.computed != null && s.computed.expectedDeparture != null) return s.computed.expectedDeparture;
        // TODO - api연결 구현: 백그라운드가 예상 출발을 계산하기 전엔 도착 45분 전으로 가정
        return LocalDateTime.of(s.date, s.arrivalTime).minusMinutes(45);
    }

    // 두 모드 화면 전환. 07-1 로 가면 진행 중인 불러오기·오류 표시를 접는다
    private void showMode(@NonNull Mode m) {
        boolean far = m == Mode.FAR;
        binding.layoutFar.setVisibility(far ? View.VISIBLE : View.GONE);
        binding.btnOpenMap.setVisibility(far ? View.GONE : View.VISIBLE);
        if (far) {
            handler.removeCallbacks(arrivalsLoaded);
            Loading.stopPulse(binding.layoutLoading);
            binding.layoutLoading.setVisibility(View.GONE);
            binding.layoutContent.setVisibility(View.GONE);
            binding.layoutOffline.setVisibility(View.GONE);
            binding.networkBanner.getRoot().setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.VISIBLE);
            hasContent = false;   // 다시 07 로 오면 스켈레톤부터
        }
    }

    // 07-1 화면 편집: 예상 출발 카드·안내·일정 정보
    private void applyFar(@NonNull Schedule s, @NonNull LocalDateTime departure) {
        LocalDateTime basis = s.computed != null && s.computed.computedAt != null
                ? s.computed.computedAt : LocalDateTime.now();
        binding.tvFarBasis.setText(getString(R.string.realtime_far_basis, basis.format(TIME_FORMAT)));
        binding.tvFarDeparture.setText(departure.format(TIME_FORMAT));
        binding.tvFarGuide.setText(getString(R.string.realtime_far_guide_body,
                departure.minusMinutes(REALTIME_WINDOW_MINUTES).format(TIME_FORMAT)));
        binding.tvFarName.setText(s.name);
        binding.tvFarArrival.setText(getString(R.string.detail_arrival_value,
                formatDay(s.date), s.arrivalTime.format(TIME_FORMAT)));
        binding.tvFarPlace.setText(s.place != null ? s.place.name : null);
        binding.tvFarAlarm.setText(getString(R.string.realtime_far_alarm_value, departure.format(TIME_FORMAT),
                getString(s.activated ? R.string.schedule_status_scheduled : R.string.schedule_status_unreserved)));
    }

    // 07 화면 편집 (일정 값): 지도 버튼·출발 알림 시각
    private void applyRealtimeInfo(@NonNull Schedule s, @NonNull LocalDateTime departure) {
        binding.btnOpenMap.setEnabled(KakaoMap.canRoute(s.place));
        binding.btnOpenMap.setOnClickListener(v -> KakaoMap.openRoute(this, s.place));
        binding.tvDeparture.setText(departure.format(TIME_FORMAT));
    }

    // 07 화면 편집 (실시간 응답): 기준 시각과 도착 정보
    // TODO - api연결 구현: 응답의 도착 카드·도착 예정 목록·승차 정류장을 여기서 채운다
    private void applyArrivals(@NonNull LocalTime updatedAt) {
        hasContent = true;
        lastUpdated = updatedAt;
        Loading.stopPulse(binding.layoutLoading);
        binding.networkBanner.getRoot().setVisibility(View.GONE);
        binding.layoutOffline.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.VISIBLE);
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);
        binding.tvUpdated.setText(getString(R.string.realtime_updated, updatedAt.format(TIME_FORMAT)));
    }

    // 07-L: 처음엔 스켈레톤 + "실시간 도착 정보 불러오는 중…".
    // 1분 갱신은 보이는 값을 지우지 않고 뒤에서 바꾼다 (로딩 표시 규칙 "다시 불러올 때")
    private void loadArrivals() {
        handler.removeCallbacks(arrivalsLoaded);
        if (!hasContent) {
            binding.layoutOffline.setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.VISIBLE);
            binding.layoutLoading.setVisibility(View.VISIBLE);
            Loading.pulse(binding.layoutLoading);
        }
        // TODO - api연결 구현: 실시간 버스 도착 응답 대신 3초 대기
        handler.postDelayed(arrivalsLoaded, Loading.FAKE_DELAY_MS);
    }

    private final Runnable arrivalsLoaded = () -> {
        if (binding == null || mode != Mode.REALTIME) return;
        // TODO - api연결 구현: 실제 응답 실패(10초 초과·5xx)도 오류로 처리
        if (!Network.get(this).isOnline()) {
            if (hasContent) {
                binding.networkBanner.tvBanner.setText(
                        getString(R.string.network_unstable, lastUpdated.format(TIME_FORMAT)));
                binding.networkBanner.getRoot().setVisibility(View.VISIBLE);
            } else {
                Loading.stopPulse(binding.layoutLoading);
                binding.scrollContent.setVisibility(View.GONE);
                binding.layoutOffline.setVisibility(View.VISIBLE);
            }
            return;
        }
        applyArrivals(LocalTime.now());
    };

    @NonNull
    private String formatDay(@NonNull LocalDate date) {
        LocalDate today = LocalDate.now();
        String dayOfWeek = date.format(DAY_OF_WEEK_FORMAT);
        if (date.equals(today)) return getString(R.string.detail_day_today, dayOfWeek);
        if (date.equals(today.plusDays(1))) return getString(R.string.detail_day_tomorrow, dayOfWeek);
        return date.format(DATE_FORMAT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(arrivalsLoaded);
        if (binding != null) Loading.stopPulse(binding.layoutLoading);
    }
}
