package org.freenetproject.mobile.proxy;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * From: https://resources.oreilly.com/examples/9781565923713/blob/master/SimpleProxyServer.java
 */
public class Simple implements Runnable {
    public void run(String remotehost, int remoteport, int localport) throws IOException {
        // Create a ServerSocket to listen for connections with
        ServerSocket ss = new ServerSocket(localport);

        // Create buffers for client-to-server and server-to-client communication.
        // We make one final so it can be used in an anonymous class below.
        // Note the assumptions about the volume of traffic in each direction...
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];

        // This is a server that never returns, so enter an infinite loop.
        while (true) {
            // Variables to hold the sockets to the client and to the server.
            Socket client = null, server = null;
            try {
                // Wait for a connection on the local port
                client = ss.accept();

                // Get client streams.  Make them final so they can
                // be used in the anonymous thread below.
                final InputStream from_client = client.getInputStream();
                final OutputStream to_client = client.getOutputStream();

                // Make a connection to the real server
                // If we cannot connect to the server, send an error to the
                // client, disconnect, then continue waiting for another connection.
                try {
                    server = new Socket(remotehost, remoteport);
                } catch (IOException e) {
                    client.close();
                    continue;
                }

                // Get server streams.
                final InputStream from_server = server.getInputStream();
                final OutputStream to_server = server.getOutputStream();

                // Make a thread to read the client's requests and pass them to the
                // server.  We have to use a separate thread because requests and
                // responses may be asynchronous.
                Thread t = new Thread() {
                    public void run() {
                        int bytes_read;
                        try {
                            while ((bytes_read = from_client.read(request)) != -1) {
                                to_server.write(request, 0, bytes_read);
                                to_server.flush();
                            }
                        } catch (IOException ignored) {
                        }

                        // the client closed the connection to us, so  close our
                        // connection to the server.  This will also cause the
                        // server-to-client loop in the main thread exit.
                        try {
                            to_server.close();
                        } catch (IOException e) {
                        }
                    }
                };

                // Start the client-to-server request thread running
                t.start();

                // Meanwhile, in the main thread, read the server's responses
                // and pass them back to the client.  This will be done in
                // parallel with the client-to-server request thread above.
                int bytes_read;
                try {
                    while ((bytes_read = from_server.read(reply)) != -1) {
                        to_client.write(reply, 0, bytes_read);
                        to_client.flush();
                    }
                } catch (IOException ignored) {
                }

                // The server closed its connection to us, so close our
                // connection to our client.  This will make the other thread exit.
                to_client.close();
            } catch (IOException e) {
                System.err.println(e);
            }
            // Close the sockets no matter what happens each time through the loop.
            finally {
                try {
                    if (server != null) server.close();
                    if (client != null) client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void run() {
        Log.d("Freenet", "Proxy thread started");
        try {
            run("127.0.0.1", 8888, 9999);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
