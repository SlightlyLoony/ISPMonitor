package com.dilatush.ispmonitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.SystemAvailability.*;

/**
 * Performs a simple TCP connectivity test to the configured service.  Note that the TCP connection is simply established and immediately terminated;
 * no data is sent or received.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class ConnectivityTestTask implements Task {

    private static final Logger LOGGER                 = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final String                        host;
    private final int                           port;
    private final int                           timeoutMS;
    private final String                        name;
    private final ConnectivityTestResultHandler handler;


    /* package-private */ ConnectivityTestTask( final ConnectivityTestResultHandler _handler,
                                                final String _host, final int _port, final int _timeoutMS, final String _name ) {

        handler   = _handler;
        host      = _host;
        port      = _port;
        timeoutMS = _timeoutMS;
        name      = _name;
    }


    /**
     * Runs this task.
     */
    @Override
    public void run() {

        SystemAvailability serviceState;

        try ( Socket socket = new Socket(); ) {

            // attempt to establish a connection...
            long start = System.currentTimeMillis();

            socket.connect( new InetSocketAddress( host, port ), timeoutMS );
            LOGGER.finest( "Time to connect to " + host + ":" + port + " was " + (System.currentTimeMillis() - start) + "ms" );

            // it's established, so tear it down...
            socket.close();

            // this service is UP...
            serviceState = UP;
        }

        // if we get a socket timeout exception, then we took too long to connect, so we declare this service DOWN...
        catch( SocketTimeoutException _ste ) {
            serviceState = DOWN;
        }

        // any other I/O exception means we've got some worser problem, and we have no idea whether the service is up or down...
        catch( IOException _e ) {
            serviceState = UNKNOWN;
        }


        // send an event reporting the results...
        ISPMonitor.postEvent( new Event( EventType.ConnectivityTest, new ConnectivityTestResult( handler, serviceState, name ) ) );
    }
}
