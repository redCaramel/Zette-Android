package kr.ac.ssu.zette.ui.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.databinding.ActivityOnboardingPermissionBinding;
import kr.ac.ssu.zette.databinding.DialogSettingsGuideBinding;
import kr.ac.ssu.zette.databinding.ItemPermissionRowBinding;
import kr.ac.ssu.zette.ui.DeleteConfirmDialog;
import kr.ac.ssu.zette.ui.activity.PermissionStatusActivity.LocationAccess;

// 온보딩 권한 설정 (Figma 01-4 ~ 01-7). 5개를 모두 허용해야 시작할 수 있다
public class OnboardingPermissionActivity extends AppCompatActivity {

    private static final int TOTAL = 5;

    private ActivityOnboardingPermissionBinding binding;

    private final ActivityResultLauncher<String> notificationRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> onRuntimeResult(granted, Manifest.permission.POST_NOTIFICATIONS, R.string.perm_object_notification));
    private final ActivityResultLauncher<String[]> locationRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> onRuntimeResult(result.containsValue(true), Manifest.permission.ACCESS_FINE_LOCATION,
                    R.string.perm_object_location));
    private final ActivityResultLauncher<String> backgroundRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> render());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOnboardingPermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        setup(binding.rowNotification, R.drawable.ic_notifications, R.string.perm_notification, this::requestNotification);
        setup(binding.rowAlarm, R.drawable.ic_alarm, R.string.perm_alarm, this::requestExactAlarm);
        setup(binding.rowLocation, R.drawable.ic_my_location, R.string.perm_location,
                () -> locationRequest.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}));
        setup(binding.rowBackground, R.drawable.ic_place, R.string.perm_background, this::requestBackgroundLocation);
        setup(binding.rowBattery, R.drawable.ic_battery_saver, R.string.perm_battery, this::requestBattery);
        binding.btnStart.setOnClickListener(v -> start());
    }

    // 설정 화면에서 돌아올 때마다 다시 확인한다
    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void setup(@NonNull ItemPermissionRowBinding row, @DrawableRes int icon, @StringRes int title,
                       @NonNull Runnable request) {
        row.ivIcon.setImageResource(icon);
        row.tvTitle.setText(title);
        row.btnAllow.setOnClickListener(v -> request.run());
    }

    private void render() {
        LocationAccess location = PermissionStatusActivity.locationAccess(this);
        boolean notification = PermissionStatusActivity.notificationsEnabled(this);
        boolean alarm = PermissionStatusActivity.exactAlarmAllowed(this);
        boolean foreground = location != LocationAccess.OFF;
        boolean background = location == LocationAccess.ALWAYS;
        boolean battery = ignoringBatteryOptimizations();

        renderRow(binding.rowNotification, notification, true, R.string.perm_notification_desc);
        renderRow(binding.rowAlarm, alarm, true, R.string.perm_alarm_desc);
        renderRow(binding.rowLocation, foreground, true, R.string.perm_location_desc);
        // 항상 허용은 앱 사용 중 위치를 먼저 받아야 요청할 수 있다
        renderRow(binding.rowBackground, background, foreground,
                foreground ? R.string.perm_background_desc : R.string.perm_background_locked);
        renderRow(binding.rowBattery, battery, true, R.string.perm_battery_desc);

        int count = (notification ? 1 : 0) + (alarm ? 1 : 0) + (foreground ? 1 : 0) + (background ? 1 : 0) + (battery ? 1 : 0);
        binding.tvProgress.setText(getString(R.string.perm_progress, count, TOTAL));
        binding.progress.setProgressCompat(count, true);
        boolean all = count == TOTAL;
        binding.tvHelper.setVisibility(all ? View.INVISIBLE : View.VISIBLE);
        binding.btnStart.setEnabled(all);
    }

    private void renderRow(@NonNull ItemPermissionRowBinding row, boolean granted, boolean enabled, @StringRes int desc) {
        row.tvDesc.setText(desc);
        row.btnAllow.setVisibility(granted ? View.GONE : View.VISIBLE);
        row.layoutGranted.setVisibility(granted ? View.VISIBLE : View.GONE);
        row.btnAllow.setEnabled(enabled);
        row.tvTitle.setTextColor(ContextCompat.getColor(this, enabled ? R.color.text_primary : R.color.text_disabled));
        row.iconWrap.setAlpha(enabled ? 1f : 0.5f);
    }

    // ===== 요청 =====

    // 권한은 있는데 설정에서 알림을 꺼 둔 경우는 알림 설정으로 보낸다
    private void requestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        }
    }

    private void requestExactAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()));
        }
    }

    // Android 11+ 는 시스템이 앱 설정의 위치 권한 화면으로 보낸다
    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    private void requestBattery() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()));
        } catch (ActivityNotFoundException e) {
            open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    // 두 번 거부하면 더 물어볼 수 없어 설정으로 안내한다 (01-6)
    private void onRuntimeResult(boolean granted, @NonNull String permission, @StringRes int object) {
        if (!granted && !shouldShowRequestPermissionRationale(permission)) showDeniedGuide(object);
        render();
    }

    private void showDeniedGuide(@StringRes int object) {
        String name = getString(object);
        DialogSettingsGuideBinding guide = DialogSettingsGuideBinding.inflate(LayoutInflater.from(this));
        guide.tvTitle.setText(getString(R.string.perm_denied_title, name));
        guide.tvMessage.setText(getString(R.string.perm_denied_body, name));
        AlertDialog dialog = DeleteConfirmDialog.cardDialog(this, guide.getRoot());
        guide.btnOpen.setOnClickListener(v -> {
            dialog.dismiss();
            open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()));
        });
        guide.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ===== 완료 =====

    private void start() {
        SettingsStore.getInstance(this).setOnboardingDone(true);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private boolean ignoringBatteryOptimizations() {
        PowerManager power = getSystemService(PowerManager.class);
        return power != null && power.isIgnoringBatteryOptimizations(getPackageName());
    }

    @NonNull
    private Uri packageUri() {
        return Uri.fromParts("package", getPackageName(), null);
    }

    private void open(@NonNull Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()));
        }
    }
}
