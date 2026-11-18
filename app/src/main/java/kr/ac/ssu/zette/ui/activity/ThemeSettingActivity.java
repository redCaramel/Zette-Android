package kr.ac.ssu.zette.ui.activity;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import kr.ac.ssu.zette.data.local.ThemePreferences;
import kr.ac.ssu.zette.data.local.ThemePreferences.ThemeMode;
import kr.ac.ssu.zette.databinding.ActivityThemeSettingBinding;

public class ThemeSettingActivity extends AppCompatActivity {

    private static final int HALF_CLIP_LEVEL = 5000;
    private static final float DIMMED_ALPHA = 0.55f;

    private ActivityThemeSettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityThemeSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.ivPreviewLight.setImageLevel(HALF_CLIP_LEVEL);
        binding.ivPreviewDark.setImageLevel(HALF_CLIP_LEVEL);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.optionLight.setOnClickListener(v -> selectMode(ThemeMode.LIGHT));
        binding.optionDark.setOnClickListener(v -> selectMode(ThemeMode.DARK));
        binding.optionSystem.setOnClickListener(v -> selectMode(ThemeMode.SYSTEM));

        render(ThemePreferences.getThemeMode(this));
    }

    private void selectMode(@NonNull ThemeMode mode) {
        if (mode == ThemePreferences.getThemeMode(this)) return;
        render(mode);
        ThemePreferences.setThemeMode(this, mode);
    }

    private void render(@NonNull ThemeMode mode) {
        binding.optionLight.setSelected(mode == ThemeMode.LIGHT);
        binding.optionDark.setSelected(mode == ThemeMode.DARK);
        binding.optionSystem.setSelected(mode == ThemeMode.SYSTEM);

        boolean darkActive = mode == ThemeMode.DARK
                || (mode == ThemeMode.SYSTEM && isSystemInDarkMode());
        binding.ivPreviewLight.setAlpha(darkActive ? DIMMED_ALPHA : 1f);
        binding.ivPreviewDark.setAlpha(darkActive ? 1f : DIMMED_ALPHA);
    }

    private boolean isSystemInDarkMode() {
        int nightMask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMask == Configuration.UI_MODE_NIGHT_YES;
    }
}
