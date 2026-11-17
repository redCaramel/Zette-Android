package kr.ac.ssu.zette.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.platform.MaterialFadeThrough;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ActivityMainBinding;
import kr.ac.ssu.zette.ui.fragment.CalendarFragment;
import kr.ac.ssu.zette.ui.fragment.HomeFragment;
import kr.ac.ssu.zette.ui.fragment.RoutineFragment;
import kr.ac.ssu.zette.ui.fragment.SettingFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean routineFabMode = false;

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

            if (itemId == R.id.homeFragment) {
                replaceFragment(new HomeFragment());
                setRoutineFabMode(false);
                return true;
            }

            if (itemId == R.id.calendarFragment) {
                replaceFragment(new CalendarFragment());
                setRoutineFabMode(false);
                return true;
            }

            if (itemId == R.id.routineFragment) {
                replaceFragment(new RoutineFragment());
                setRoutineFabMode(true);
                return true;
            }

            if (itemId == R.id.settingFragment) {
                replaceFragment(new SettingFragment());
                setRoutineFabMode(false);
                return true;
            }

            return false;
        });

        binding.bottomNav.setOnItemReselectedListener(item -> { });

        setRoutineFabMode(getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container) instanceof RoutineFragment);
        hideFab(getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container) instanceof SettingFragment);
        binding.btnNewSchedule.setOnClickListener(v -> {
            if (routineFabMode) {
                onNewRoutineClick();
            } else {
                onNewScheduleClick();
            }
        });
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
        // TODO - 새 일정 버튼 클릭 로직
    }

    private void onNewRoutineClick() {
        // TODO - 새 루틴 버튼 클릭 로직
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
