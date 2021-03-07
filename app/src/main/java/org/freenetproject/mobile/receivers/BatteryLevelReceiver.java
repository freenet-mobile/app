package org.freenetproject.mobile.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.freenetproject.mobile.services.node.Manager;

public class BatteryLevelReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("Freenet", "Stopping service from low battery level");
        Manager.getInstance().stopService(context);
    }
}

