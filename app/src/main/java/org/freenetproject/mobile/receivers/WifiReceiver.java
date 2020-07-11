package org.freenetproject.mobile.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.freenetproject.mobile.services.node.Manager;

/**
 * This receiver is only available for android < 21
 */
public class WifiReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Freenet", "Network change detected");
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isConnected = activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();
        if (isConnected) {
            Log.i("Freenet", "Resuming service");
            Manager.getInstance().resumeService(context);
        } else {
            Log.i("Freenet", "Pausing service");
            Manager.getInstance().pauseService(context);
        }
    }
}
