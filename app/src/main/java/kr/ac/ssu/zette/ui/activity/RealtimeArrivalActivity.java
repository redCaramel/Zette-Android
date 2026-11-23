package kr.ac.ssu.zette.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

// 실시간 도착 (Figma 07). 홈 Hero 에서 들어온다.
// shortcut: 버스 도착 정보(도착 카드·도착 예정 목록·승차 정류장)는 샘플 문자열이다. 실시간 API 연결 시 교체
public class RealtimeArrivalActivity extends AppCompatActivity {

    private static final String EXTRA_KEY = "schedule_key";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    private ActivityRealtimeArrivalBinding binding;

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
        binding.btnRefresh.setOnClickListener(v -> showUpdatedNow());
        showUpdatedNow();

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
        binding.btnOpenMap.setEnabled(KakaoMap.canRoute(s.place));
        binding.btnOpenMap.setOnClickListener(v -> KakaoMap.openRoute(this, s.place));
        LocalDateTime departure = s.computed != null ? s.computed.expectedDeparture : null;
        binding.tvDeparture.setText(departure != null
                ? departure.toLocalTime().format(TIME_FORMAT)
                : getString(R.string.detail_sample_departure));
    }

    private void showUpdatedNow() {
        binding.tvUpdated.setText(getString(R.string.realtime_updated, LocalTime.now().format(TIME_FORMAT)));
    }
}
