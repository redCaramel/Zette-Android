package kr.ac.ssu.zette.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.ui.ShareLink;

// 받은 일정 추가 (Figma 28)
public class ReceivedScheduleActivity extends NewScheduleActivity {

    @Override
    protected void onSetupMode() {
        Uri data = getIntent().getData();
        Schedule s = data != null ? ShareLink.scheduleFrom(ShareLink.paramsOf(data)) : null;
        if (s == null) {
            Toast.makeText(this, R.string.received_invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.tvToolbarTitle.setText(R.string.received_title);
        binding.tvEditBanner.setText(R.string.received_banner);
        binding.layoutEditBanner.setVisibility(View.VISIBLE);
        binding.btnSave.setText(R.string.received_save);
        fillForm(s.name, s.date, s.arrivalTime, s.place, s.routeType, s.arrivalBuffer);
    }

    // 앱 화면이 없으면 홈으로
    @Override
    public void finish() {
        if (isTaskRoot()) startActivity(new Intent(this, MainActivity.class));
        super.finish();
    }
}
