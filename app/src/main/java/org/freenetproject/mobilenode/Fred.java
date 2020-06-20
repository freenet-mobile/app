package org.freenetproject.mobilenode;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import freenet.node.NodeInitException;

public class Fred extends Service {
    public Fred() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
