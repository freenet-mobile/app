package org.freenetproject.mobile.services.node;

import android.content.Intent;
import android.os.IBinder;

import org.freenetproject.mobile.ui.notification.Notification;

public class Service extends android.app.Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, Notification.show(this));

        return android.app.Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        Notification.remove(getApplicationContext());
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
