package org.freenetproject.mobile.services.node;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.tanukisoftware.wrapper.WrapperManager;

import java.security.Security;

import freenet.node.NodeStarter;
import freenet.node.SemiOrderedShutdownHook;

/**
 * Small wrapper around NodeStarter and WrapperManager to start and stop the node. Also is responsible
 * for installing the bundled BouncyCastle security provider.
 */
public class Runner {
    private static Runner instance = null;
    private Runner() { }
    private Boolean isRunning = false;
    private static Boolean DEBUG = false;
    private static Integer DEBUG_START_DELAY = 1000; // ms
    private static Integer DEBUG_STOP_DELAY = 1000;

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
            if (DEBUG) {
                Thread.sleep(DEBUG_START_DELAY);
            } else {
                NodeStarter.start_osgi(args);
            }
            isRunning = true;
        } catch (IllegalStateException | InterruptedException e) {
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
            // Already stopped
            return 0;
        }

        try {
            if (DEBUG) {
                Thread.sleep(DEBUG_STOP_DELAY);
            } else {
                NodeStarter.stop_osgi(0);
                WrapperManager.stop(0);
            }
        } catch (NullPointerException e){
            // Node was already stopped
        } catch (Exception e) {
            return -2;
        } finally {
            isRunning = false;
        }

        return 0;
    }

    /**
     *
     * @return
     */
    public synchronized int pause() {
        return NodeStarter.pause();
    }

    /**
     *
     * @return
     */
    public synchronized int resume() {
        return NodeStarter.resume();
    }

    public Boolean isStarted() {
        return isRunning;
    }

    public Boolean isStopped() {
        return !isStarted();
    }
}
