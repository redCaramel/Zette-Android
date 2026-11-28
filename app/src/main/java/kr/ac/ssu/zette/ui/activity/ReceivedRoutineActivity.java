package kr.ac.ssu.zette.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.ui.ShareLink;

// 받은 루틴 추가 (Figma 29)
public class ReceivedRoutineActivity extends NewRoutineActivity {

    @Override
    protected void onSetupMode() {
        super.onSetupMode();
        Uri data = getIntent().getData();
        Routine r = data != null ? ShareLink.routineFrom(ShareLink.paramsOf(data)) : null;
        if (r == null) {
            Toast.makeText(this, R.string.received_invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.tvToolbarTitle.setText(R.string.received_routine_title);
        binding.tvEditBanner.setText(R.string.received_routine_banner);
        binding.layoutEditBanner.setVisibility(View.VISIBLE);
        binding.btnSave.setText(R.string.received_routine_save);
        fillRoutine(r);
    }

    // 앱 화면이 없으면 홈으로
    @Override
    public void finish() {
        if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
        super.finish();
    }
}
