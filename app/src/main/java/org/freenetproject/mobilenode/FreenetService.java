package org.freenetproject.mobilenode;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Security;

import freenet.node.NodeStarter;

public class FreenetService extends Service {
    private String FREENET_CONFIG_FILE = "freenet.ini";
    private String DATA_PATH = "data";
    private String LOG_PATH = "logs";

    private File freenet_config;
    private File data_dir;
    private File log_dir;

    enum ServiceStatus {
        SETUP,
        STARTING_UP,
        STARTED,
        STOPPING,
        STOPPED
    }

    private Boolean isServiceStarted = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isServiceStarted) {
            return Service.START_STICKY;
        }
        isServiceStarted = true;

        updateStatus(ServiceStatus.SETUP, "Setting up...");
        Thread t = new Thread(() -> {
            updateStatus(ServiceStatus.STARTING_UP, "Starting node...");
            NodeStarter.start_osgi(new String[] { freenet_config.getAbsolutePath() });
            updateStatus(ServiceStatus.STARTED, "Running");

            startForeground(1, createNotification());
        });
        t.start();

        return Service.START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        freenet_config = getFileStreamPath(FREENET_CONFIG_FILE);
        data_dir = getDir(DATA_PATH, MODE_PRIVATE);

        log_dir = getDir(LOG_PATH, MODE_PRIVATE);

        if (!freenet_config.exists()) {
            try {
                createDefaultConfiguration();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        updateStatus(ServiceStatus.SETUP, "Setting up dependencies...");
        this.setupBundledBCProvider();
        this.createSeednodesFile();

    }

    private void createDefaultConfiguration() throws FileNotFoundException {
        final FileOutputStream ini = openFileOutput(FREENET_CONFIG_FILE, MODE_PRIVATE);
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(ini));
        final String data = data_dir.getAbsolutePath();
        final String log = log_dir.getAbsolutePath();

        out.println("node.install.persistentTempDir=" + data + "/persistent-temp");
        out.println("node.install.cfgDir=" + data);
        out.println("node.install.storeDir=" + data + "/datastore");
        out.println("node.install.userDir=" + data);
        out.println("node.install.pluginStoresDir=" + data + "/plugin-data");
        out.println("node.install.tempDir=" + data + "/temp");
        out.println("node.install.nodeDir=" + data);
        out.println("node.install.pluginDir=" + data + "/plugins");
        out.println("node.install.runDir=" + data);
        out.println("logger.dirname=" + log);
        out.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification() {
        final String CHANNEL_ID = "FREENET SERVICE";

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel nc = new NotificationChannel(CHANNEL_ID, "Freenet service", NotificationManager.IMPORTANCE_HIGH);
        nc.setDescription("Freenet service");
        nc.enableLights(true);
        nc.setLightColor(Color.BLUE);
        nm.createNotificationChannel(nc);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this);
        } else {
            builder = new Notification.Builder(this, CHANNEL_ID);
        }

        return builder
                .setContentTitle("Freenet service")
                .setContentText("Freenet service is running in background.")
                .setSmallIcon(R.drawable.ic_freenet_logo)
                .build();
    }

    /**
     * Replace Android's BC provider with the bundled version.
     *
     * @return
     */
    private int setupBundledBCProvider() {
        Security.removeProvider("BC");

        return Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    /**
     * Creates the seednodes.fref file in order for the node to bootstrap into the network.
     *
     * @return Boolean true if the seednodes file exists or was created successfully, false otherwise.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean createSeednodesFile() {
        InputStream seednodes = this.getResources().openRawResource(R.raw.seednodes);

        File f = new File(data_dir + "/seednodes.fref");
        if (f.exists()) {
            return true;
        }

        try {
            if (!f.createNewFile()) {
                Log.e("Feeenet", "Failed to call createNewFile.");
                return false;
            }
        } catch (IOException e) {
            Log.e("Freenet", "Failed to create temporary file for seednodes.");
            Log.e("Freenet", e.getMessage());
            return false;
        }

        try {
            Files.copy(seednodes, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.e("Freenet", "Failed to copy temporary file for seednodes.");
            Log.e("Freenet", e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        if (!isServiceStarted) {
            return;
        }
        isServiceStarted = false;

        Log.i("Freenet", "Stopping freenet");
        updateStatus(ServiceStatus.STOPPING, "Stopping node...");

        NodeStarter.stop_osgi(0);
        Log.i("Freenet", "NodeStarter.stop done");

        updateStatus(ServiceStatus.STOPPED, "");

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();

        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateStatus(ServiceStatus serviceStatus, String message) {
        Intent i = new Intent();
        i.setAction("STATUS");
        i.putExtra("STATUS_HUMAN_READABLE", message);
        i.putExtra("CODE", serviceStatus);
        sendBroadcast(i);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("STATUS", 0);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = pref.edit();
        editor.putString("STATUS", serviceStatus.toString());
    }

    private ServiceStatus getStatus() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("STATUS", 0);
        return ServiceStatus.valueOf(pref.getString("STATUS", ServiceStatus.STOPPED.toString()));
    }
}
