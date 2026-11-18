package kr.ac.ssu.zette.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ThemePreferences;
import kr.ac.ssu.zette.databinding.FragmentSettingBinding;
import kr.ac.ssu.zette.ui.activity.ThemeSettingActivity;

public class SettingFragment extends Fragment {

    // TODO - 샘플 데이터 삭제, 로컬 데이터 연결
    private static final int SAMPLE_ARRIVAL_BUFFER_MINUTES = 5;

    private FragmentSettingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);

        // TODO - 설정 메뉴별 텍스트 적용
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvArrivalBufferValue.setText(
                getString(R.string.setting_arrival_buffer_value, SAMPLE_ARRIVAL_BUFFER_MINUTES));

        binding.gotoRoute.setOnClickListener(v -> {
            // TODO - 경로 기준 선택 화면 이동
        });
        binding.gotoBuffer.setOnClickListener(v -> {
            // TODO - 도착 여유 선택 화면 이동
        });
        binding.gotoPermission.setOnClickListener(v -> {
            // TODO - 권한 상태 화면 이동
        });
        binding.gotoTheme.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemeSettingActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.tvThemeValue.setText(ThemePreferences.getThemeMode(requireContext()).labelRes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
