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
                Log.d("Freenet", "Power connected");
                if (Manager.getInstance().isPaused()) {
                    Log.i("Freenet", "Resuming service");
                    Manager.getInstance().resumeService(context);
                }
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                Log.d("Freenet", "Power disconnected");
                if (Manager.getInstance().isRunning()) {
                    Log.i("Freenet", "Pausing service");
                    Manager.getInstance().pauseService(context);
                }
                break;
        }
    }
}
