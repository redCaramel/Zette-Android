package kr.ac.ssu.zette.ui;

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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 하단 inset은 BottomNavigationView가 직접 처리하므로 루트에서는 제외 (이중 padding 방지)
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
                return true;
            }

            if (itemId == R.id.calendarFragment) {
                replaceFragment(new CalendarFragment());
                return true;
            }

            if (itemId == R.id.routineFragment) {
                replaceFragment(new RoutineFragment());
                return true;
            }

            if (itemId == R.id.settingFragment) {
                replaceFragment(new SettingFragment());
                return true;
            }

            return false;
        });

        binding.bottomNav.setOnItemReselectedListener(item -> { });
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
