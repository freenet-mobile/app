package org.freenetproject.mobile.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.freenetproject.mobile.services.node.Manager;

import java.util.Objects;

public class PowerConnectionReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Freenet", "onReceive");

        // only if configured to manage start/stop
        switch(Objects.requireNonNull(intent.getAction())){
            case Intent.ACTION_POWER_CONNECTED:
                Log.i("Freenet", "Resuming service from power change");
                new Thread(() -> {
                    Manager.getInstance().resumeService(context, Manager.CONTEXT_BATTERY);
                }).start();

                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                Log.i("Freenet", "Pausing service from power change");
                new Thread(() -> {
                    Manager.getInstance().pauseService(context, Manager.CONTEXT_BATTERY);
                }).start();
                break;
        }
    }
}
