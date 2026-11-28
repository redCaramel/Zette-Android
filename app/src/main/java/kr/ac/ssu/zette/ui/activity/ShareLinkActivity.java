package kr.ac.ssu.zette.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.ui.ShareLink;

// 공유 링크 진입점. k 값에 따라 받은 일정·루틴 화면으로 보낸다
public class ShareLinkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent().getData();
        String kind = data != null ? ShareLink.paramsOf(data).get(ShareLink.KIND) : null;
        Class<?> target = ShareLink.KIND_SCHEDULE.equals(kind) ? ReceivedScheduleActivity.class
                : ShareLink.KIND_ROUTINE.equals(kind) ? ReceivedRoutineActivity.class
                : null;
        if (target == null) {
            Toast.makeText(this, R.string.received_invalid, Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(this, target).setData(data));
        }
        finish();
    }
}
