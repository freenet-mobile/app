package org.freenetproject.mobile.services.node;

import android.util.Log;

import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Installer {
    private String path = null;
    private static Installer instance = null;
    private Installer() { }

    private final String FREENET_INI = "freenet.ini";
    private final String SEEDNODES = "seednodes.fref";

    public static Installer getInstance() {
        if (instance == null) {
            instance = new Installer();
        }
        return instance;
    }

    /**
     * Install the node with default configuration and seed nodes both from resources. Some configuration
     * properties are dynamically calculated at runtime on the first startup (mostly related to directory paths).
     *
     * @param path Path to installation location.
     * @param seeds Input stream of seeds file.
     * @param config Input stream of default configuration.
     *
     * @throws FileNotFoundException
     */
    public void install(String path, InputStream seeds, InputStream config) throws FileNotFoundException {
        File f = new File(path);
        if (!f.exists() || !f.canWrite()) {
            throw new FileNotFoundException("Invalid installation directory: " + path);
        }

        this.path = path;

        // Install bundled configurations
        createConfiguration(seeds, getSeednodesPath());
        createConfiguration(config, getFreenetIniPath());

        // Update freenet.ini with dynamic configuration
        final FileOutputStream ini = new FileOutputStream(getFreenetIniPath(), true);
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(ini));

        out.println("node.install.persistentTempDir=" + path + "/persistent-temp");
        out.println("node.install.cfgDir=" + path);
        out.println("node.masterKeyFile=" + path + "/master.keys");
        out.println("node.install.storeDir=" + path + "/pathstore");
        out.println("node.install.userDir=" + path);
        out.println("node.install.pluginStoresDir=" + path + "/plugin-path");
        out.println("node.install.tempDir=" + path + "/temp");
        out.println("node.install.nodeDir=" + path);
        out.println("node.install.pluginDir=" + path + "/plugins");
        out.println("node.install.runDir=" + path);
        out.println("node.downloadsDir=" + path + "/downloads");

        out.println("logger.dirname=" + path + "/logs");
        out.println("End");
        out.close();
    }

    /**
     * Creates the seednodes.fref file in order for the node to bootstrap into the network.
     *
     * @return Boolean true if the seednodes file exists or was created successfully, false otherwise.
     */
    private boolean createConfiguration(InputStream configuration, String filename) {
        File f = new File(filename);
        if (f.exists()) {
            return true;
        }

        try {
            if (!f.createNewFile()) {
                Log.e("Feeenet", "Failed to call createNewFile.");
                return false;
            }
        } catch (IOException e) {
            Log.e("Freenet", "Failed to create temporary file.");
            Log.e("Freenet", e.getMessage());
            return false;
        }

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            Log.e("Freenet", "Failed to create temporary file.");
            Log.e("Freenet", e.getMessage());
            return false;
        }

        try {
            IOUtils.copy(configuration, outputStream);
        } catch (IOException e) {
            Log.e("Freenet", "Failed to copy temporary file.");
            Log.e("Freenet", e.getMessage());
            return false;
        }

        return true;
    }

    public boolean isInstalled() {
        File f = new File(getFreenetIniPath());
        return f.exists();
    }

    // TODO: Implement uninstall
    public void uninstall() {
        Log.d("Freenet", "Unimplemented uninstall method.");
    }

    public String getPath() {
        return path;
    }

    public String getSeednodesPath() {
        return path + "/" + SEEDNODES;
    }

    public String getFreenetIniPath() {
        return path + "/" + FREENET_INI;
    }
}
