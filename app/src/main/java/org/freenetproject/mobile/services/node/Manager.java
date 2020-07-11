package org.freenetproject.mobile.services.node;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.freenetproject.mobile.R;

import java.io.FileNotFoundException;

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

    private MutableLiveData<Status> status = new MutableLiveData<Status>();
    private Manager() {
    }

    public static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }

        return instance;
    }

    public void init(Status value, Context context) {
        if (value == Manager.Status.ERROR) {
            new Thread(() -> {
                reset(context);
                status.postValue(Status.STOPPED);
            }).start();
            status.setValue(Status.STOPPED);
            return;
        }

        status.setValue(value);
    }

    public void reset(Context context) {
        try {
            stopService(context);
        } catch (Exception e) {
            Log.e("Freenet", "Error stopping service: " + e.getMessage());
        }
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

        int ret = resumeService(context);

        if (ret == 0) {
            Intent serviceIntent = new Intent(context, Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            Log.e("Freenet", "Error starting freenet");
        }

        return 0;
    }

    /**
     * Stops the service through the Runner class. Also stops the Services.Node.Service.
     *
     * @param context Application context.
     * @return
     */
    public int stopService(Context context) {
        status.postValue(Status.STOPPING);
        try {
            if (runner.stop() == 0) {
                status.postValue(Status.STOPPED);
            } else {
                status.postValue(Status.ERROR);

            }
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

        Intent serviceIntent = new Intent(context, Service.class);
        context.stopService(serviceIntent);

        return 0;
    }

    /**
     * Pauses the node running on the device while maintaining the service running on foreground.
     *
     * @param context
     * @return
     */
    public int pauseService(Context context) {
        if (isPaused()) {
            return -1;
        }

        status.postValue(Status.PAUSING);
        try {
            if (runner.stop() == 0) {
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
    public int resumeService(Context context) {
        if (isPaused()) {
            return -1;
        }

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
