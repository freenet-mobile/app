package org.freenetproject.mobile.services.node;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.tanukisoftware.wrapper.WrapperManager;

import java.security.Security;

import freenet.node.NodeStarter;

/**
 * Small wrapper around NodeStarter and WrapperManager to start and stop the node. Also is responsible
 * for installing the bundled BouncyCastle security provider.
 */
public class Runner {
    private static Runner instance = null;
    private Runner() { }
    private Boolean isRunning = false;

    public static Runner getInstance() {
        if (instance == null) {
            Security.removeProvider("BC");
            Security.insertProviderAt(new BouncyCastleProvider(), 1);

            instance = new Runner();
        }
        return instance;
    }

    /**
     * Starts the node through NodeStarter unless it's already started.
     *
     * @param args Arguments to pass to the NodeStarter
     * @return -1 if the node is running or in transition
     *          -2 if the node is already running
     *          0 if the node could be started
     */
    public synchronized int start(String[] args) {
        if (!isStopped())  {
           return -1;
        }

        try {
            NodeStarter.main(args);
            isRunning = true;
        } catch (IllegalStateException e) {
            return -2;
        }

        return 0;
    }

    /**
     * Stops the node through WrapperManager unless it's already stopped.
     *
     * @return -1 if the node is stopped or in transition
     *          -2 if there's an error stopping the node
     *          0 if the node could be stopped
     */
    public synchronized int stop() {
        if (!isStarted()) {
            return -1;
        }

        try {
            WrapperManager.stopImmediate(0);
        } catch (NullPointerException e){
            // Node was already stopped
            isRunning = false;
        } catch (Exception e) {
            isRunning = false;
            return -2;
        }

        return 0;
    }

    public Boolean isStarted() {
        return isRunning;
    }

    public Boolean isStopped() {
        return !isStarted();
    }
}
