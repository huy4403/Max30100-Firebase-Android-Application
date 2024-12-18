package com.iot.heartmonitor;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;

public class ActionNoti extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Dừng ringtone từ MainActivity
        Ringtone currentRingtone = MainActivity.currentRingtone; // Lấy tham chiếu tĩnh
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }

        NotificationManager notificationManager = MainActivity.notificationManager;
        int notificationId = intent.getIntExtra("notificationId", -1);
        if (notificationId != -1) {
            notificationManager.cancel(notificationId);
        }
    }
}

