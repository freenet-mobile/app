package org.freenetproject.mobilenode;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Security;

import freenet.node.NodeStarter;
import freenet.support.Logger;

public class FreenetService extends Service {
    private boolean running = false;
    private File FREENET_INI_PATH;
    private File DATA_DIR;
    private File LOG_DIR;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateStatus("Starting up...");
        doStart();
        return Service.START_NOT_STICKY;

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        FREENET_INI_PATH = getFileStreamPath("freenet.ini");
        DATA_DIR = getDir("data", MODE_PRIVATE);
        LOG_DIR = getDir("logs", MODE_PRIVATE);

        updateStatus("Setting up BouncyCastle...");
        this.setupBundledBCProvider();
        updateStatus("Setting up Seednodes...");
        this.createSeednodesFile();
        updateStatus("Done setup");
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

        File f = new File(DATA_DIR + "/seednodes.fref");
        if (f.exists()) {
            return true;
        }

        try {
            if (!f.createNewFile()) {
                Logger.error(this, "Failed to call createNewFile.");
                return false;
            }
        } catch (IOException e) {
            Logger.error(this, "Failed to create temporary file for seednodes.");
            Logger.error(this, e.getMessage());
            return false;
        }

        try {
            Files.copy(seednodes, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Logger.error(this, "Failed to copy temporary file for seednodes.");
            Logger.error(this, e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        doStop();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        Log.e("Freenet", "=== onBind ===");
        return null;
    }

    private synchronized void doStart() {
        Log.i("Freenet", "=== doStart === " + running);
        if (!running) {
            if (!FREENET_INI_PATH.exists()) {
                try {
                    final FileOutputStream ini = openFileOutput("freenet.ini", MODE_PRIVATE);
                    final PrintWriter out = new PrintWriter(new OutputStreamWriter(ini));
                    final String data = DATA_DIR.getAbsolutePath();
                    final String log = LOG_DIR.getAbsolutePath();
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized(FreenetService.this) {
                        running = true;
                    }

                    updateStatus("Starting node...");
                    NodeStarter.start_osgi(new String[] { FREENET_INI_PATH.getAbsolutePath() });
                    Log.i("Freenet", "==> NodeStarter.start thread returned");
                    updateStatus("Running");
                }
            });
            t.start();
        }
    }

    private synchronized void doStop() {
        Log.i("Freenet", "=== doStop === " + running);
        if (running) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized(FreenetService.this) {
                        running = false;
                    }
                    updateStatus("Stopping node...");
                    NodeStarter.stop_osgi(0);
                    Log.i("Freenet", "==> NodeStarter.stop thread returned");
                    updateStatus("");
                }
            });
            t.start();
        }
    }

    private void updateStatus(String message) {
        Intent i = new Intent();
        i.setAction("STATUS");
        i.putExtra("STATUS_HUMAN_READABLE", message);
        sendBroadcast(i);
    }
}
