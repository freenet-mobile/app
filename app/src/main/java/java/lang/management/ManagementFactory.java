package java.lang.management;

/**
 * Overwriting ManagementFactory to be able to run on 1492 on android where these
 * libraries are missing and probably are not going to be added anything soon.
 */
public class ManagementFactory {
    public static ThreadMXBean getThreadMXBean() {
        return new ThreadMXBean();
    }
}
