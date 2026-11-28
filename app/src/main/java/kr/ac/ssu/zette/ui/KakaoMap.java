package kr.ac.ssu.zette.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.SheetMapAppMissingBinding;
import kr.ac.ssu.zette.dto.Place;

// 카카오맵 앱으로 현재 위치 → 도착 장소 대중교통 경로를 연다 (06·07 "카카오맵에서 경로 보기")
public final class KakaoMap {

    private static final String PACKAGE = "net.daum.android.map";

    private KakaoMap() {}

    // 좌표가 없으면(EntityMapper 가 null 좌표를 0,0 으로 바꾼다) 경로를 열 수 없다
    public static boolean canRoute(@Nullable Place place) {
        return place != null && (place.lat != 0 || place.lng != 0);
    }

    public static void openRoute(@NonNull Context context, @NonNull Place place) {
        Intent route = new Intent(Intent.ACTION_VIEW,
                Uri.parse("kakaomap://route?ep=" + place.lat + "," + place.lng + "&by=PUBLICTRANSIT"));
        if (route.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(route);
            return;
        }
        showAppMissing(context, place);
    }

    // 앱 미설치 (Figma 25)
    private static void showAppMissing(@NonNull Context context, @NonNull Place place) {
        SheetMapAppMissingBinding binding = SheetMapAppMissingBinding.inflate(LayoutInflater.from(context));
        BottomSheetDialog sheet = new BottomSheetDialog(context);
        sheet.setContentView(binding.getRoot());

        binding.btnWeb.setOnClickListener(v -> {
            sheet.dismiss();
            // 모바일 웹 길찾기: map.kakao.com/link/to/이름,위도,경도
            String name = place.name != null ? place.name : "";
            Uri web = new Uri.Builder().scheme("https").authority("map.kakao.com")
                    .appendPath("link").appendPath("to")
                    .appendPath(name.replace(",", " ") + "," + place.lat + "," + place.lng)
                    .build();
            if (!open(context, web)) failed(context);
        });
        binding.btnInstall.setOnClickListener(v -> {
            sheet.dismiss();
            if (!open(context, Uri.parse("market://details?id=" + PACKAGE))
                    && !open(context, Uri.parse("https://play.google.com/store/apps/details?id=" + PACKAGE))) {
                failed(context);
            }
        });
        sheet.show();
    }

    private static boolean open(@NonNull Context context, @NonNull Uri uri) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private static void failed(@NonNull Context context) {
        Toast.makeText(context, R.string.map_open_failed, Toast.LENGTH_SHORT).show();
    }
}
