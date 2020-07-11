package org.freenetproject.mobile.ui.main.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.freenetproject.mobile.R;
import org.freenetproject.mobile.services.node.Installer;
import org.freenetproject.mobile.services.node.Runner;
import org.freenetproject.mobile.services.node.Service;

import java.io.FileNotFoundException;

/**
 * Class responsible for exposing data to the UI. It also exposes methods for the UI to interact with,
 * such as startService and stopService.
 */
public class MainViewModel extends ViewModel {

    private Runner runner = Runner.getInstance();

    public enum Status {
        STARTING_UP,
        STARTED,
        STOPPING,
        STOPPED,
        ERROR,
        INSTALLING
    }

    private MutableLiveData<Status> status = new MutableLiveData<Status>();
    public MainViewModel() {
        super();
        status.setValue(Status.STOPPED);
    }

    public void init(Status value) {
        status.setValue(value);
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
                    context.getDir("data", android.content.Context.MODE_PRIVATE).getAbsolutePath(),
                    res.openRawResource(R.raw.seednodes),
                    res.openRawResource(R.raw.freenet),
                    res.getConfiguration().locale.getDisplayLanguage()
                );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        status.postValue(Status.STARTING_UP);
        int ret = -1;
        try {
            ret = runner.start(new String[]{Installer.getInstance().getFreenetIniPath()});
            if (ret == 0) {
                status.postValue(Status.STARTED);
            } else {
                status.postValue(Status.ERROR);
            }
        } catch (Exception e) {
            status.postValue(Status.ERROR);
        }

        if (ret == 0) {
            Intent serviceIntent = new Intent(context, Service.class);
            context.startService(serviceIntent);
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

    public Boolean isRunning() {
        return status.getValue().equals(Status.STARTED);
    }

    public Boolean isTransitioning() {
        Status value = status.getValue();
        return !value.equals(Status.STARTED) && !value.equals(Status.STOPPED);
    }
}

