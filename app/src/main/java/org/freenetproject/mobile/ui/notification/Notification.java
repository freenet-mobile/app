package org.freenetproject.mobile.ui.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.ui.main.activity.MainActivity;

/**
 * Class responsible for creating the notification in order to be able to run Freenet on background.
 */
public class Notification {
    /**
     * Create a notification to remain on background.
     *
     * @param context Application context
     * @return
     */
    public static android.app.Notification show(Context context) {
        final String CHANNEL_ID = "FREENET SERVICE";

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = null;
            nc = new NotificationChannel(CHANNEL_ID, String.valueOf(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            nc.setDescription(String.valueOf(R.string.app_name));
            nc.enableLights(true);
            nc.setLightColor(Color.BLUE);
            nm.createNotificationChannel(nc);
        }

        android.app.Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new android.app.Notification.Builder(context);
        } else {
            builder = new android.app.Notification.Builder(context, CHANNEL_ID);
        }

        return builder
            .setContentTitle(String.valueOf(R.string.node_running))
            .setContentText(String.valueOf(R.string.connected_to_the_network))
            .setSmallIcon(R.drawable.ic_freenet_logo_notification)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    new Intent(
                        context.getApplicationContext(), MainActivity.class
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build();
    }

    /**
     * Removes all notifications.
     * @param context
     */
    public static void remove(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }
}
