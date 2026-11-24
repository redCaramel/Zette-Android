package kr.ac.ssu.zette.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.ActivityScheduleDetailBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.ui.DeleteConfirmDialog;
import kr.ac.ssu.zette.ui.KakaoMap;
import kr.ac.ssu.zette.ui.Loading;
import kr.ac.ssu.zette.ui.Network;
import kr.ac.ssu.zette.ui.item.ScheduleCardItem;

// 일정 상세 (Figma 06). 승차 정보 카드와 경로가 계산되지 않은 값은 아직 샘플로 보여준다
public class ScheduleDetailActivity extends AppCompatActivity {

    private static final String EXTRA_KEY = "schedule_key";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMAT =
            DateTimeFormatter.ofPattern("E", Locale.KOREAN);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN);

    private ActivityScheduleDetailBinding binding;
    // 06-L: 경로 계산이 끝나기 전엔 이동 시간·출발 알림 "계산 중…", 승차 정보·지도는 스피너
    private boolean routeLoading = true;
    // 06-E: 경로를 못 받으면 이동 시간 "불러오지 못했어요", 출발 알림은 마지막 계산값, 승차 카드 자리에 영역 오류
    private boolean routeError;
    @Nullable
    private Schedule lastSchedule;
    private final Runnable routeLoaded = () -> {
        routeLoading = false;
        // TODO - api연결 구현: 실제 응답 실패(10초 초과·5xx)도 오류로 처리
        routeError = !Network.get(this).isOnline();
        renderRouteState();
        if (lastSchedule != null) bind(lastSchedule);
    };

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull ScheduleKey key) {
        return new Intent(context, ScheduleDetailActivity.class).putExtra(EXTRA_KEY, key.encode());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityScheduleDetailBinding.inflate(getLayoutInflater());
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

        binding.layoutBoardingLoading.tvLoading.setText(R.string.detail_boarding_loading);
        binding.boardingError.tvErrorTitle.setText(R.string.detail_route_error_title);
        binding.boardingError.tvErrorBody.setText(R.string.detail_route_error_body);
        binding.boardingError.btnRetry.setOnClickListener(v -> loadRoute());
        loadRoute();

        binding.btnEdit.setOnClickListener(v -> EditScheduleActivity.open(this, key));
        // 삭제되면 아래 observeSchedule 이 null 을 받아 화면을 닫는다
        binding.btnDelete.setOnClickListener(v -> DeleteConfirmDialog.confirmSchedule(this, key));

        // 삭제되었거나 찾을 수 없는 일정이면 닫는다
        ZetteData.get(this).schedules.observeSchedule(key).observe(this, schedule -> {
            if (schedule == null) {
                finish();
                return;
            }
            bind(schedule);
        });
    }

    private void loadRoute() {
        binding.getRoot().removeCallbacks(routeLoaded);
        routeLoading = true;
        routeError = false;
        renderRouteState();
        if (lastSchedule != null) bind(lastSchedule);
        // TODO - api연결 구현: 경로·승차 정보 응답 대신 3초 대기. 승차 카드 높이는 invisible 로 유지
        binding.getRoot().postDelayed(routeLoaded, Loading.FAKE_DELAY_MS);
    }

    private void renderRouteState() {
        binding.cardBoarding.setVisibility(routeError ? View.GONE : View.VISIBLE);
        binding.boardingError.getRoot().setVisibility(routeError ? View.VISIBLE : View.GONE);
        binding.layoutBoardingLoading.getRoot().setVisibility(routeLoading ? View.VISIBLE : View.GONE);
        binding.layoutBoarding.setVisibility(routeLoading ? View.INVISIBLE : View.VISIBLE);
        binding.pbMap.setVisibility(routeLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) binding.getRoot().removeCallbacks(routeLoaded);
    }

    private void bind(@NonNull Schedule s) {
        lastSchedule = s;
        binding.tvName.setText(s.name);
        binding.tvArrival.setText(getString(R.string.detail_arrival_value,
                formatDay(s.date), s.arrivalTime.format(TIME_FORMAT)));
        binding.tvPlace.setText(s.place != null ? s.place.name : null);
        binding.btnOpenMap.setEnabled(KakaoMap.canRoute(s.place));
        binding.btnOpenMap.setOnClickListener(v -> KakaoMap.openRoute(this, s.place));
        binding.tvRoute.setText(getString(R.string.detail_route_value,
                getString(routeTypeLabel(s.routeType)), formatBuffer(s.arrivalBuffer)));
        bindStatus(ScheduleCardItem.from(s).status);

        if (routeLoading) {
            binding.tvTravel.setText(R.string.detail_calculating);
            binding.tvDeparture.setText(R.string.detail_calculating);
            return;
        }
        LocalDateTime departure = s.computed != null ? s.computed.expectedDeparture : null;
        String departureText = departure != null
                ? departure.toLocalTime().format(TIME_FORMAT)
                : getString(R.string.detail_sample_departure);
        if (routeError) {
            binding.tvTravel.setText(R.string.detail_load_failed);
            binding.tvDeparture.setText(getString(R.string.detail_last_computed, departureText));
            return;
        }
        binding.tvTravel.setText(formatTravel(s));
        binding.tvDeparture.setText(departureText);
    }

    @NonNull
    private String formatDay(@NonNull LocalDate date) {
        LocalDate today = LocalDate.now();
        String dayOfWeek = date.format(DAY_OF_WEEK_FORMAT);
        if (date.equals(today)) return getString(R.string.detail_day_today, dayOfWeek);
        if (date.equals(today.plusDays(1))) return getString(R.string.detail_day_tomorrow, dayOfWeek);
        return date.format(DATE_FORMAT);
    }

    @NonNull
    private String formatTravel(@NonNull Schedule s) {
        Integer minutes = s.computed != null ? s.computed.travelMinutes : null;
        String summary = s.computed != null ? s.computed.routeSummary : null;
        if (minutes == null) return getString(R.string.detail_sample_travel);
        if (summary == null || summary.isEmpty()) return getString(R.string.detail_travel_minutes, minutes);
        return getString(R.string.detail_travel_value, minutes, summary);
    }

    @NonNull
    private String formatBuffer(int minutes) {
        return minutes > 0
                ? getString(R.string.setting_arrival_buffer_value, minutes)
                : getString(R.string.setting_arrival_buffer_none);
    }

    @StringRes
    private static int routeTypeLabel(@Nullable Schedule.RouteType type) {
        if (type == Schedule.RouteType.MinWalk) return R.string.setting_route_type_min_walk;
        if (type == Schedule.RouteType.MinTransfer) return R.string.setting_route_type_min_transfer;
        return R.string.setting_route_type_min_time;
    }

    // 홈 카드의 상태 배지와 같은 글자·색
    private void bindStatus(@NonNull ScheduleCardItem.Status status) {
        switch (status) {
            case DONE:
                applyStatus(R.string.schedule_status_done, R.color.surface_ceramic, R.color.text_secondary);
                break;
            case MOVING:
                applyStatus(R.string.schedule_status_moving, R.color.green_mint, R.color.green_brand);
                break;
            case LATE:
                applyStatus(R.string.schedule_status_late, R.color.semantic_error_tint, R.color.semantic_error);
                break;
            case UNRESERVED:
                applyStatus(R.string.schedule_status_unreserved, R.color.surface_ceramic, R.color.text_secondary);
                break;
            case MISSED:
                applyStatus(R.string.schedule_status_missed, R.color.semantic_error_tint, R.color.semantic_error);
                break;
            case SCHEDULED:
            default:
                applyStatus(R.string.schedule_status_scheduled, R.color.green_mint, R.color.green_brand);
                break;
        }
    }

    private void applyStatus(@StringRes int label, @ColorRes int background, @ColorRes int content) {
        binding.tvStatus.setText(label);
        binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, content));
    }
}
