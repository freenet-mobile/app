package org.freenetproject.mobile.services.node;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.freenetproject.mobile.NodeController;
import org.freenetproject.mobile.NodeControllerImpl;
import org.freenetproject.mobile.R;
import org.freenetproject.mobile.ui.main.activity.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class responsible for exposing data to the UI. It also exposes methods for the UI to interact with,
 * such as startService and stopService.
 */
public class Manager {
    public static final String CONTEXT_NETWORK = "network";
    public static final String CONTEXT_BATTERY = "battery";
    private static Manager instance = null;
    private final Map<String, Boolean> contextRunFlag = new HashMap<String, Boolean>() {{
        put(CONTEXT_NETWORK, true);
        put(CONTEXT_BATTERY, true);
    }};

    // when adding a new state that has transitions be sure to update isTransitioning method
    public enum Status {
        STARTING_UP,
        STARTED,
        STOPPING,
        STOPPED,
        ERROR,
        PAUSED,
        PAUSING
    }

    private NodeController nc;
    private final MutableLiveData<Status> status = new MutableLiveData<Status>();

    private Manager() {
        status.postValue(
            nc != null && nc.isRunning() ? Status.STARTED : Status.STOPPED
        );
    }

    public static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }

        return instance;
    }

    public LiveData<Status> getStatus() {
        return status;
    }

    /**
     * Start the service through the Runner class. Also install the node if it's not already installed.
     *
     * On one hand it starts the actual node through the Runner class and on the other hand it starts
     * the Services.Node.Service service to be able to keep running on the background.
     *
     * Checks if the node is installed and install it otherwise.
     *
     * @param context Application context.
     */
    public void startService(Context context) throws IOException {
        File path = context.getDir("data", Context.MODE_PRIVATE);

        status.postValue(Status.STARTING_UP);

        nc = new NodeControllerImpl(path.toPath());

        SharedPreferences prefs = context.getSharedPreferences(
            context.getPackageName(), Context.MODE_PRIVATE
        );

        Resources res = context.getResources();
        // Setup first-run configuration
        if (prefs.getBoolean("first-run", true)) {
            nc.setConfig("seednodes.fref", res.openRawResource(R.raw.seednodes));
            nc.setConfig("bookmarks.dat", res.openRawResource(R.raw.bookmarks));
        }

        nc.start();

        if (nc.isRunning()) {

            if (prefs.getBoolean("first-run", true)) {
                // Setup first-run runtime configuration
                nc.setConfig("node.l10n", res.getConfiguration().getLocales().get(0).toLanguageTag());
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("first-run", false);
            editor.apply();

            Intent serviceIntent = new Intent(context, Service.class);
            context.startForegroundService(serviceIntent);
            status.postValue(Status.STARTED);

        } else {
            status.postValue(Status.ERROR);
        }

    }

    /**
     * Stops the service through the Runner class. Also stops the Services.Node.Service.
     *
     * @param context Application context.
     */
    public void stopService(Context context) {
        Intent serviceIntent = new Intent(context, Service.class);
        context.stopService(serviceIntent);

        try {
            nc.shutdown();
            status.postValue(Status.STOPPED);

        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

    }

    /**
     * Stop the node and restart the application.
     *
     * @param context Application context
     */
    public void restartService(Context context) {
        stopService(context);

        Log.i("Freenet", "Calling rebirth");
        ProcessPhoenix.triggerRebirth(
                context,
                new Intent(
                        context,
                        MainActivity.class
                )
        );

    }

    /**
     * Pauses the node running on the device while maintaining the service running on foreground.
     *
     * @param context Application context
     * @param serviceContext String description for the calling context
     */
    public void pauseService(Context context, String serviceContext) {
        if (isPaused()) {
            return;
        }
        contextRunFlag.put(serviceContext, false);

        status.postValue(Status.PAUSING);
        try {
            nc.pause();
            status.postValue(Status.PAUSED);
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

    }

    /**
     * Starts up or resume a service.
     *
     * @param context Application context
     * @param serviceContext String description for the calling context
     */
    public void resumeService(Context context, String serviceContext) {
        if (!isPaused()) {
            return;
        }
        contextRunFlag.put(serviceContext, true);

        for (Boolean value : contextRunFlag.values()) {
            if (!value) {
                return; // a given context has flagged not to run
            }
        }

        status.postValue(Status.STARTING_UP);
        try {
            nc.resume();
            status.postValue(Status.STARTED);
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

    }

    public void resetService(Context context) {
        Intent serviceIntent = new Intent(context, Service.class);
        context.stopService(serviceIntent);

        try {
            nc.shutdown();
        } catch (Exception e) {
            Log.e("Freenet", "Error stopping node: " + e.getMessage());
        }

        Log.i("Freenet", "Calling rebirth");
        ProcessPhoenix.triggerRebirth(
                context,
                new Intent(
                        context,
                        MainActivity.class
                )
        );
    }

    public Boolean isStopped() {
        return Objects.equals(status.getValue(), Status.STOPPED);
    }
    public Boolean isPaused() {
        return Objects.equals(status.getValue(), Status.PAUSED);
    }

    public Boolean isRunning() {
        return Objects.equals(status.getValue(), Status.STARTED);
    }

    public Boolean hasError() {
        return Objects.equals(status.getValue(), Status.ERROR);
    }

    public Boolean isTransitioning() {
        Status value = status.getValue();
        assert value != null;
        return !value.equals(Status.STARTED)
                && !value.equals(Status.STOPPED)
                && !value.equals(Status.PAUSED);
    }
}
