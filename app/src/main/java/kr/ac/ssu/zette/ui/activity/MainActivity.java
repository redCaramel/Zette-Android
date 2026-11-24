package kr.ac.ssu.zette.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.google.android.material.transition.platform.MaterialFadeThrough;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ActivityMainBinding;
import kr.ac.ssu.zette.ui.Network;
import kr.ac.ssu.zette.ui.fragment.CalendarFragment;
import kr.ac.ssu.zette.ui.fragment.HomeFragment;
import kr.ac.ssu.zette.ui.fragment.RoutineFragment;
import kr.ac.ssu.zette.ui.fragment.SettingFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean routineFabMode = false;
    // 네트워크 오류 표시 규칙: 연결이 없으면 맨 위 배너 + 일정·루틴 추가 버튼 비활성 (12-E·14-E, 홈은 03-E 배너)
    private int currentTab;
    private boolean online = true;
    private LocalTime offlineSince = LocalTime.now();
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        // BottomNavigationView를 눌렀을 때 Fragment 변경하기
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            currentTab = itemId;
            renderNetwork();

            if (itemId == R.id.homeFragment) {
                replaceFragment(new HomeFragment());
                setRoutineFabMode(false);
                binding.btnNewSchedule.setVisibility(VISIBLE);
                return true;
            }

            if (itemId == R.id.calendarFragment) {
                replaceFragment(new CalendarFragment());
                setRoutineFabMode(false);
                binding.btnNewSchedule.setVisibility(VISIBLE);
                return true;
            }

            if (itemId == R.id.routineFragment) {
                replaceFragment(new RoutineFragment());
                setRoutineFabMode(true);
                binding.btnNewSchedule.setVisibility(VISIBLE);
                return true;
            }

            if (itemId == R.id.settingFragment) {
                replaceFragment(new SettingFragment());
                setRoutineFabMode(false);
                binding.btnNewSchedule.setVisibility(GONE);
                return true;
            }

            return false;
        });

        binding.bottomNav.setOnItemReselectedListener(item -> { });

        setRoutineFabMode(getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container) instanceof RoutineFragment);
        hideFab(getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container) instanceof SettingFragment);
        currentTab = binding.bottomNav.getSelectedItemId();
        binding.networkBanner.btnRetry.setOnClickListener(v -> Network.get(this).recheck());
        Network.get(this).observe(this, isOnline -> {
            if (online && !isOnline) offlineSince = LocalTime.now();
            online = isOnline;
            renderNetwork();
        });

        binding.btnNewSchedule.setOnClickListener(v -> {
            if (routineFabMode) {
                onNewRoutineClick();
            } else {
                onNewScheduleClick();
            }
        });
    }

    private void renderNetwork() {
        binding.btnNewSchedule.setEnabled(online);
        boolean showBanner = !online && currentTab != R.id.settingFragment;
        binding.networkBanner.getRoot().setVisibility(showBanner ? VISIBLE : GONE);
        if (!showBanner) return;
        if (currentTab == R.id.homeFragment) {
            binding.networkBanner.tvBanner.setText(getString(R.string.network_unstable, offlineSince.format(TIME_FORMAT)));
        } else {
            binding.networkBanner.tvBanner.setText(currentTab == R.id.routineFragment
                    ? R.string.network_no_add_routine : R.string.network_no_add_schedule);
        }
    }

    private void setRoutineFabMode(boolean routine) {
        routineFabMode = routine;
        binding.btnNewSchedule.setText(routine ? R.string.fab_new_routine : R.string.fab_new_schedule);
    }
    private void hideFab(boolean gone) {
        if (gone)
            binding.btnNewSchedule.setVisibility(GONE);
        else
            binding.btnNewSchedule.setVisibility(VISIBLE);
    }

        private void onNewScheduleClick() {
        startActivity(new Intent(this, NewScheduleActivity.class));
    }

    private void onNewRoutineClick() {
        startActivity(new Intent(this, NewRoutineActivity.class));
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        if (current != null) {
            current.setExitTransition(new MaterialFadeThrough());
        }
        fragment.setEnterTransition(new MaterialFadeThrough());

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }
}
