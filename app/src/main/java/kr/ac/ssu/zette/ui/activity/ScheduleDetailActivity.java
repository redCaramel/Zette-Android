package kr.ac.ssu.zette.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.ActivityScheduleDetailBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.ui.DeleteConfirmDialog;
import kr.ac.ssu.zette.ui.KakaoMap;
import kr.ac.ssu.zette.ui.ShareDialog;

// 일정 상세 (Figma 06)
public class ScheduleDetailActivity extends AppCompatActivity {

    private static final String EXTRA_KEY = "schedule_key";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMAT =
            DateTimeFormatter.ofPattern("E", Locale.KOREAN);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN);

    private ActivityScheduleDetailBinding binding;
    @Nullable
    private Schedule lastSchedule;

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
        binding.btnShare.setOnClickListener(v -> {
            if (lastSchedule != null) ShareDialog.showScheduleSheet(this, lastSchedule);
        });

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
}
