package org.freenetproject.mobile.services.node;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.ui.main.activity.MainActivity;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for exposing data to the UI. It also exposes methods for the UI to interact with,
 * such as startService and stopService.
 */
public class Manager {

    private static Manager instance = null;
    private Runner runner = Runner.getInstance();

    public enum Status {
        STARTING_UP,
        STARTED,
        STOPPING,
        STOPPED,
        ERROR,
        INSTALLING,
        PAUSED,
        PAUSING // when adding a new state that has transitions be sure to update isTransitioning method
    }

    public static final String CONTEXT_NETWORK = "network";
    public static final String CONTEXT_BATTERY = "battery";

    private Map<String, Boolean> contextRunFlag = new HashMap<String, Boolean>() {{
        put(CONTEXT_NETWORK, true);
        put(CONTEXT_BATTERY, true);
    }};

    private MutableLiveData<Status> status = new MutableLiveData<Status>();
    private Manager() {
        status.postValue(
            runner.isStarted() ? Status.STARTED : Status.STOPPED
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
     * @return
     */
    public int startService(Context context) {
        if (!Installer.getInstance().isInstalled()) {
            status.postValue(Status.INSTALLING);
            try {
                Resources res = context.getResources();
                Installer.getInstance().install(
                        context.getDir("data", Context.MODE_PRIVATE).getAbsolutePath(),
                        res.openRawResource(R.raw.seednodes),
                        res.openRawResource(R.raw.freenet),
                        res.getConfiguration().locale.getDisplayLanguage()
                );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        int ret = startNode();
        if (ret == 0) {
            Intent serviceIntent = new Intent(context, Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            Log.e("Freenet", "Error starting freenet (" + ret + ")");
            status.postValue(Status.ERROR);
        }

        return 0;
    }

    private int startNode() {
        status.postValue(Status.STARTING_UP);
        int ret = -1;
        try {
            ret = runner.start(new String[]{Installer.getInstance().getFreenetIniPath()});
            if (ret == 0) {
                status.postValue(Status.STARTED);
            } else if (ret == -1) {
                // Already running
                status.postValue(Status.STARTED);
            } else {
                status.postValue(Status.ERROR);
            }
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }
        return ret;
    }

    /**
     * Stops the service through the Runner class. Also stops the Services.Node.Service.
     *
     * @param context Application context.
     * @return
     */
    public int stopService(Context context) {
        Intent serviceIntent = new Intent(context, Service.class);
        context.stopService(serviceIntent);

        try {
            if (runner.stop() != 0) {
                status.postValue(Status.ERROR);
            }

            status.postValue(Status.STOPPED);

            Log.i("Freenet", "Calling rebirth");
            ProcessPhoenix.triggerRebirth(
                    context,
                    new Intent(
                            context,
                            MainActivity.class
                    )
            );

        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

        return 0;
    }

    /**
     * Pauses the node running on the device while maintaining the service running on foreground.
     *
     * @param context
     * @return
     */
    public int pauseService(Context context, String serviceContext) {
        if (isPaused()) {
            return -1;
        }
        contextRunFlag.put(serviceContext, false);

        status.postValue(Status.PAUSING);
        try {
            if (runner.pause() == 0) {
                status.postValue(Status.PAUSED);
            } else {
                status.postValue(Status.ERROR);

            }
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

        return 0;
    }

    /**
     * Starts up or resume a service.
     *
     * @param context
     * @return
     */
    public int resumeService(Context context, String serviceContext) {
        if (!isPaused()) {
            return -2;
        }
        contextRunFlag.put(serviceContext, true);

        for (Boolean value : contextRunFlag.values()) {
           if (!value)
               return -3; // a given context has flagged not to run
        }

        status.postValue(Status.STARTING_UP);
        try {
            if (runner.resume() == 0) {
                status.postValue(Status.STARTED);
            } else {
                status.postValue(Status.ERROR);
            }
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

        return 0;
    }

    public Boolean isPaused() {
        return status.getValue().equals(Status.PAUSED);
    }

    public Boolean isRunning() {
        return status.getValue().equals(Status.STARTED);
    }

    public Boolean isTransitioning() {
        Status value = status.getValue();
        return !value.equals(Status.STARTED)
                && !value.equals(Status.STOPPED)
                && !value.equals(Status.PAUSED);
    }
}
