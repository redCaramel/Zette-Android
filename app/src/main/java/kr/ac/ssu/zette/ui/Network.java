package kr.ac.ssu.zette.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

// 인터넷 연결 여부. 관찰 중에는 연결이 바뀔 때마다 true/false 를 알려준다 (Figma "네트워크 오류 표시 규칙")
public final class Network extends LiveData<Boolean> {

    private static Network instance;

    private final ConnectivityManager cm;
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull android.net.Network network) {
            postValue(true);
        }

        @Override
        public void onLost(@NonNull android.net.Network network) {
            postValue(false);
        }
    };

    @NonNull
    public static synchronized Network get(@NonNull Context context) {
        if (instance == null) instance = new Network(context.getApplicationContext());
        return instance;
    }

    private Network(@NonNull Context context) {
        cm = context.getSystemService(ConnectivityManager.class);
        setValue(isOnline());
    }

    public boolean isOnline() {
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // "다시 시도": 콜백을 기다리지 않고 지금 상태로 다시 알린다
    public void recheck() {
        setValue(isOnline());
    }

    @Override
    protected void onActive() {
        setValue(isOnline());
        cm.registerDefaultNetworkCallback(callback);
    }

    @Override
    protected void onInactive() {
        cm.unregisterNetworkCallback(callback);
    }
}
