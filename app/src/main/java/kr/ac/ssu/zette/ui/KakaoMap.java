package kr.ac.ssu.zette.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        // shortcut: 미설치면 바로 스토어로 보낸다. Figma 25 하단 시트를 만들면 그걸 띄운다
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE)));
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + PACKAGE)));
        }
    }
}
