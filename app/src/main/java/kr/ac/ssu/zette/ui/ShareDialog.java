package kr.ac.ssu.zette.ui;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;

import com.kakao.sdk.share.ShareClient;
import com.kakao.sdk.share.WebSharerClient;
import com.kakao.sdk.template.model.Button;
import com.kakao.sdk.template.model.Link;
import com.kakao.sdk.template.model.TextTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kr.ac.ssu.zette.BuildConfig;
import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.DialogShareBinding;
import kr.ac.ssu.zette.dto.Routine;
import kr.ac.ssu.zette.dto.Schedule;

// 일정·루틴 공유 (Figma 12-1, 14-2, 26)
public final class ShareDialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    private ShareDialog() {
    }

    public static void showSchedule(@NonNull Context context, @NonNull Schedule s) {
        showSchedule(context, s, false);
    }

    // 일정 상세에서 여는 하단 시트 (26)
    public static void showScheduleSheet(@NonNull Context context, @NonNull Schedule s) {
        showSchedule(context, s, true);
    }

    private static void showSchedule(@NonNull Context context, @NonNull Schedule s, boolean sheet) {
        String when = context.getString(R.string.share_until, s.date.format(DATE_FORMAT), s.arrivalTime.format(TIME_FORMAT));
        String place = s.place != null ? s.place.name : null;
        Map<String, String> params = ShareLink.toParams(s);
        String link = ShareLink.toUri(ShareLink.HOST_SCHEDULE, params).toString();
        String message = s.name + "\n" + when + (place != null ? "\n" + place : "");

        show(context, sheet,
                sheet ? R.string.send_schedule_title : R.string.share_schedule_title,
                sheet ? R.string.send_schedule_label : R.string.share_schedule_label, s.name,
                R.drawable.ic_schedule, when, place, R.string.share_schedule_note,
                sheet ? R.string.send_kakao : R.string.share_kakao,
                () -> shareToKakao(context, message, params, link, R.string.share_kakao_button),
                () -> copyLink(context, link));
    }

    // repeat = "매주 월 · 수 · 금"
    public static void showRoutine(@NonNull Context context, @NonNull Routine r, @NonNull String repeat) {
        String when = context.getString(R.string.share_until, repeat, r.arrivalTime.format(TIME_FORMAT));
        String place = r.place != null ? r.place.name : null;
        Map<String, String> params = ShareLink.toParams(r);
        String link = ShareLink.toUri(ShareLink.HOST_ROUTINE, params).toString();
        String message = r.name + "\n" + when + (place != null ? "\n" + place : "");

        show(context, false, R.string.share_routine_title, R.string.share_routine_label, r.name,
                R.drawable.ic_bottom_nav_routine, when, place, R.string.share_routine_note, R.string.share_kakao,
                () -> shareToKakao(context, message, params, link, R.string.share_kakao_button_routine),
                () -> copyLink(context, link));
    }

    // sheet = 하단 시트(닫기 버튼 없음), 아니면 카드 다이얼로그
    private static void show(@NonNull Context context, boolean sheet, @StringRes int title, @StringRes int label,
                             @NonNull String name, @DrawableRes int whenIcon, @NonNull String when,
                             @Nullable String place, @StringRes int note, @StringRes int kakaoLabel,
                             @NonNull Runnable onKakao, @NonNull Runnable onCopyLink) {
        DialogShareBinding binding = DialogShareBinding.inflate(LayoutInflater.from(context));
        binding.tvTitle.setText(title);
        binding.tvLabel.setText(label);
        binding.tvName.setText(name);
        binding.ivWhen.setImageResource(whenIcon);
        binding.tvWhen.setText(when);
        binding.tvPlace.setText(place);
        binding.tvNote.setText(note);
        binding.btnKakao.setText(kakaoLabel);

        Dialog dialog;
        if (sheet) {
            LinearLayout root = binding.getRoot();
            root.setBackground(null);
            root.setPadding(root.getPaddingLeft(), 0, root.getPaddingRight(), root.getPaddingBottom());
            root.addView(new BottomSheetDragHandleView(context), 0);
            binding.btnClose.setVisibility(View.GONE);
            BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
            bottomSheet.setContentView(root);
            dialog = bottomSheet;
        } else {
            dialog = DeleteConfirmDialog.cardDialog(context, binding.getRoot());
        }
        binding.btnKakao.setOnClickListener(v -> {
            dialog.dismiss();
            onKakao.run();
        });
        binding.btnCopyLink.setOnClickListener(v -> {
            dialog.dismiss();
            onCopyLink.run();
        });
        binding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // 버튼을 누르면 kakao{앱키}://kakaolink?params 로 앱이 열린다 (ShareLinkActivity)
    private static void shareToKakao(@NonNull Context context, @NonNull String message,
                                     @NonNull Map<String, String> params, @NonNull String link,
                                     @StringRes int buttonText) {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isEmpty()) {
            shareAsText(context, message + "\n" + link);    // 앱 키 전: 기본 공유 시트
            return;
        }
        Link appLink = new Link(null, null, params, null);
        TextTemplate template = new TextTemplate(message, appLink,
                Collections.singletonList(new Button(context.getString(buttonText), appLink)), null);

        if (ShareClient.getInstance().isKakaoTalkSharingAvailable(context)) {
            ShareClient.getInstance().shareDefault(context, template, (result, error) -> {
                if (error != null || result == null) {
                    Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show();
                } else {
                    context.startActivity(result.getIntent());
                }
                return Unit.INSTANCE;
            });
            return;
        }
        // 카카오톡이 없으면 웹 공유
        openUri(context, WebSharerClient.getInstance().makeDefaultUrl(template));
    }

    private static void shareAsText(@NonNull Context context, @NonNull String text) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(send, null));
    }

    private static void copyLink(@NonNull Context context, @NonNull String link) {
        ClipboardManager clipboard = context.getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText("zette", link));
        // Android 13+ 는 시스템이 복사 알림을 띄운다
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.share_link_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static void openUri(@NonNull Context context, @NonNull Uri uri) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
