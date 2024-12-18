package com.iot.heartmonitor;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Date;

public class MyForegroundService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Log.e("TAG", "Service is running...");
                    try{
                        Thread.sleep(2000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        final String CHENNELID = "Foreground Service ID";
        NotificationChannel chennel = new NotificationChannel(
                CHENNELID,
                CHENNELID,
                NotificationManager.IMPORTANCE_LOW
        );
        getSystemService(NotificationManager.class).createNotificationChannel(chennel);
        Notification.Builder notification = new Notification.Builder(this, CHENNELID)
                .setContentText("Service is running")
                .setContentTitle("Service").setSmallIcon(R.mipmap.heartmonitorlogo);
        startForeground(1001, notification.build());
        return super.onStartCommand(intent, flags, startId);
    }
}
