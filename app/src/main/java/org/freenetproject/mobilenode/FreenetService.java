package org.freenetproject.mobilenode;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import freenet.node.NodeStarter;

public class FreenetService extends Service {
    private boolean running = false;
    private File FREENET_INI_PATH;
    private File DATA_DIR;
    private File LOG_DIR;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doStart();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FREENET_INI_PATH = getFileStreamPath("freenet.ini");
        DATA_DIR = getDir("data", MODE_PRIVATE);
        LOG_DIR = getDir("logs", MODE_PRIVATE);
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
                    NodeStarter.start_osgi(new String[] { FREENET_INI_PATH.getAbsolutePath() });
                    Log.i("Freenet", "==> NodeStarter.start thread returned");
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
                    NodeStarter.stop_osgi(0);
                    Log.i("Freenet", "==> NodeStarter.stop thread returned");
                }
            });
            t.start();
        }
    }
}
