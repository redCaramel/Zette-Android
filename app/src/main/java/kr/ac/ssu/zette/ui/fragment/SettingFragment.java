package kr.ac.ssu.zette.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.Settings;
import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.databinding.FragmentSettingBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.BufferDialogs;
import kr.ac.ssu.zette.ui.activity.PermissionStatusActivity;
import kr.ac.ssu.zette.ui.activity.ThemeSettingActivity;

public class SettingFragment extends Fragment {

    private FragmentSettingBinding binding;
    private SettingsStore settingsStore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settingsStore = SettingsStore.getInstance(requireContext());

        binding.switchVibration.setOnCheckedChangeListener(
                (button, checked) -> settingsStore.setVibrationEnabled(checked));
        binding.switchScreen.setOnCheckedChangeListener(
                (button, checked) -> settingsStore.setScreenAlertEnabled(checked));
        binding.switchVoice.setOnCheckedChangeListener(
                (button, checked) -> settingsStore.setVoiceEnabled(checked));

        settingsStore.observe().observe(getViewLifecycleOwner(), this::render);

        binding.gotoRoute.setOnClickListener(v -> BufferDialogs.showRouteType(requireContext(),
                settingsStore.defaultRoutePreference(), settingsStore::setDefaultRoutePreference));
        binding.gotoBuffer.setOnClickListener(v -> BufferDialogs.showDefault(requireContext(),
                settingsStore.defaultArrivalBufferMinutes(), settingsStore::setDefaultArrivalBufferMinutes));
        binding.gotoPermission.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PermissionStatusActivity.class)));
        binding.gotoTheme.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemeSettingActivity.class)));
    }

    // 권한은 Android 설정에서 바뀌므로 화면에 돌아올 때마다 요약을 다시 읽는다
    @Override
    public void onResume() {
        super.onResume();
        binding.tvPermissionValue.setText(PermissionStatusActivity.summary(requireContext()));
    }

    private void render(@NonNull Settings settings) {
        syncChecked(binding.switchVibration, settings.vibrationEnabled);
        syncChecked(binding.switchScreen, settings.screenAlertEnabled);
        syncChecked(binding.switchVoice, settings.voiceEnabled);

        binding.tvRouteTypeValue.setText(routeTypeLabel(settings.defaultRouteType));
        binding.tvArrivalBufferValue.setText(settings.defaultArrivalBufferMinutes > 0
                ? getString(R.string.setting_arrival_buffer_value, settings.defaultArrivalBufferMinutes)
                : getString(R.string.setting_arrival_buffer_none));
        binding.tvThemeValue.setText(settings.themeMode.labelRes);
    }

    // 저장값과 다를 때만 바꿔서, 저장 → LiveData 갱신 → 다시 저장으로 이어지지 않게 한다.
    private static void syncChecked(@NonNull MaterialSwitch view, boolean checked) {
        if (view.isChecked() != checked) {
            view.setChecked(checked);
        }
    }

    @StringRes
    private static int routeTypeLabel(@NonNull Schedule.RouteType routeType) {
        switch (routeType) {
            case MinWalk:
                return R.string.setting_route_type_min_walk;
            case MinTransfer:
                return R.string.setting_route_type_min_transfer;
            case MinTime:
            default:
                return R.string.setting_route_type_min_time;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
