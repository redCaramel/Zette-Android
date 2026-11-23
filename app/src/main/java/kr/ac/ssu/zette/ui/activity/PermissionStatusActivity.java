package kr.ac.ssu.zette.ui.activity;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ActivityPermissionStatusBinding;

// 권한 상태 (Figma 18). 위치(항상 허용)·알림·정확한 알람 권한을 보여주고, 누르면 Android 설정으로 보낸다
public class PermissionStatusActivity extends AppCompatActivity {

    public enum LocationAccess { ALWAYS, FOREGROUND, OFF }

    private ActivityPermissionStatusBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPermissionStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.rowLocation.setOnClickListener(v -> startActivity(appDetailsIntent(this)));
        binding.rowNotification.setOnClickListener(v -> startActivity(notificationSettingsIntent()));
        binding.rowAlarm.setOnClickListener(v -> startActivity(exactAlarmSettingsIntent()));
    }

    // Android 설정에서 돌아오면 바뀐 상태를 다시 보여준다
    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        switch (locationAccess(this)) {
            case ALWAYS:
                apply(binding.tvLocationDesc, binding.tvLocationBadge, R.string.permission_location_on,
                        R.string.permission_badge_always, true);
                break;
            case FOREGROUND:
                apply(binding.tvLocationDesc, binding.tvLocationBadge, R.string.permission_location_foreground,
                        R.string.permission_badge_foreground, false);
                break;
            default:
                apply(binding.tvLocationDesc, binding.tvLocationBadge, R.string.permission_off_desc,
                        R.string.permission_badge_off, false);
                break;
        }
        boolean notification = notificationsEnabled(this);
        apply(binding.tvNotificationDesc, binding.tvNotificationBadge,
                notification ? R.string.permission_notification_on : R.string.permission_off_desc,
                notification ? R.string.permission_badge_on : R.string.permission_badge_off, notification);
        boolean alarm = exactAlarmAllowed(this);
        apply(binding.tvAlarmDesc, binding.tvAlarmBadge,
                alarm ? R.string.permission_alarm_on : R.string.permission_off_desc,
                alarm ? R.string.permission_badge_on : R.string.permission_badge_off, alarm);
    }

    // 허용은 초록 배지, 꺼짐·사용 중에만은 빨간 배지
    private void apply(@NonNull TextView desc, @NonNull TextView badge, @StringRes int descText,
                       @StringRes int badgeText, boolean ok) {
        desc.setText(descText);
        badge.setText(badgeText);
        @ColorRes int background = ok ? R.color.green_mint : R.color.semantic_error_tint;
        @ColorRes int content = ok ? R.color.green_brand : R.color.semantic_error;
        badge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        badge.setTextColor(ContextCompat.getColor(this, content));
    }

    // ===== 상태 읽기 (설정 화면 요약에서도 쓴다) =====

    @NonNull
    public static LocationAccess locationAccess(@NonNull Context context) {
        boolean foreground = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                || granted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (!foreground) return LocationAccess.OFF;
        // Android 9 이하는 위치 권한을 주면 백그라운드도 함께 허용된다
        boolean background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        return background ? LocationAccess.ALWAYS : LocationAccess.FOREGROUND;
    }

    public static boolean notificationsEnabled(@NonNull Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean exactAlarmAllowed(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    // "위치 항상 허용 · 알림 꺼짐 · 정확한 알람 허용"
    @NonNull
    public static String summary(@NonNull Context context) {
        LocationAccess location = locationAccess(context);
        int locationText = location == LocationAccess.ALWAYS ? R.string.permission_summary_location_always
                : location == LocationAccess.FOREGROUND ? R.string.permission_summary_location_foreground
                : R.string.permission_summary_location_off;
        return context.getString(R.string.permission_summary,
                context.getString(locationText),
                context.getString(notificationsEnabled(context)
                        ? R.string.permission_summary_notification_on : R.string.permission_summary_notification_off),
                context.getString(exactAlarmAllowed(context)
                        ? R.string.permission_summary_alarm_on : R.string.permission_summary_alarm_off));
    }

    private static boolean granted(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // ===== Android 설정으로 이동 =====

    // 위치 "항상 허용"은 앱 안에서 바로 요청할 수 없어 앱 정보 화면의 권한에서 바꾼다
    @NonNull
    private static Intent appDetailsIntent(@NonNull Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
    }

    @NonNull
    private Intent notificationSettingsIntent() {
        return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
    }

    @NonNull
    private Intent exactAlarmSettingsIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.fromParts("package", getPackageName(), null));
        }
        return appDetailsIntent(this);
    }
}
