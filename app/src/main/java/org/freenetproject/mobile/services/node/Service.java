package org.freenetproject.mobile.services.node;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.freenetproject.mobile.receivers.PowerConnectionReceiver;
import org.freenetproject.mobile.receivers.WifiReceiver;
import org.freenetproject.mobile.ui.notification.Notification;

public class Service extends android.app.Service {
    PowerConnectionReceiver pcr = new PowerConnectionReceiver();
    WifiReceiver wr = new WifiReceiver();
    ConnectivityManager connectivityManager;
    ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Freenet", "Called service onStartCommand");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Register power connection receiver
            IntentFilter powerConnectedFilter = new IntentFilter();
            powerConnectedFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            powerConnectedFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(pcr, powerConnectedFilter);

            // register network callback
            Context context = getApplicationContext();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.i("Freenet", "onAvailable");
                    if (Manager.getInstance().isPaused()) {
                        Log.d("Freenet", "Resuming service");
                        Manager.getInstance().resumeService(context);
                    }
                }
                @Override
                public void onLost(Network network) {
                    Log.i("Freenet", "onLost");
                    if (Manager.getInstance().isRunning()) {
                        Log.d("Freenet", "Pausing service");
                        Manager.getInstance().pauseService(context);
                    }
                }
            };

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } else {
            // Register wifi changed intent, only available on Android < O
            IntentFilter wifiFilter = new IntentFilter();
            wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(wr, wifiFilter);
        }

        startForeground(1, Notification.show(this));

        return android.app.Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i("Freenet", "Stopping service");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unregisterReceiver(pcr);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            unregisterReceiver(wr);
        }

        Notification.remove(getApplicationContext());
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
