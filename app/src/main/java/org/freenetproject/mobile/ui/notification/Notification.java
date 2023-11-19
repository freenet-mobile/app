package org.freenetproject.mobile.ui.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.services.node.Manager;
import org.freenetproject.mobile.ui.main.activity.MainActivity;

import java.util.Observer;

/**
 * Class responsible for creating the notification in order to be able to run Freenet on background.
 */
public class Notification {
    private static android.app.Notification.Builder builder;
    private static NotificationManager nm;
    private static Manager manager = Manager.getInstance();
    private static final String CHANNEL_ID = "FREENET SERVICE";

    /**
     * Create a notification to remain on background.
     *
     * @param context Application context
     * @return
     */
    public static android.app.Notification show(Context context) {
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = null;
            nc = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            nc.setDescription(context.getString(R.string.app_name));
            nc.enableLights(true);
            nc.setLightColor(Color.BLUE);
            nm.createNotificationChannel(nc);
        }

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new android.app.Notification.Builder(context);
        } else {
            builder = new android.app.Notification.Builder(context, CHANNEL_ID);
        }

        builder
            .setContentTitle(context.getString(R.string.node_running))
            .setContentText(context.getString(R.string.connected_to_the_network))
            .setSmallIcon(R.drawable.ic_freenet_logo_notification)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    new Intent(
                        context.getApplicationContext(), MainActivity.class
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                )
            );

        manager.getStatus().observeForever(status -> {
            if (status.equals(Manager.Status.STARTED)) {
                builder.setContentTitle(context.getString(R.string.node_running));
                builder.setContentText(context.getString(R.string.connected_to_the_network));
            } else if (status.equals(Manager.Status.PAUSED)) {
                builder.setContentTitle(context.getString(R.string.node_paused));
                builder.setContentText(context.getString(R.string.battery_and_data));
            } else if (status.equals(Manager.Status.ERROR)) {
                builder.setContentTitle(context.getString(R.string.error_starting_up));
                builder.setContentText(context.getString(R.string.error_detail));
            }

            nm.notify(1, builder.build());
        });


        return builder.build();
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
