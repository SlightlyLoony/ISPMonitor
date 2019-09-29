package com.dilatush.ispmonitor;

import com.dilatush.util.SSHExecutor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 * Implements a managed, self-tested SSH tunnel, configured through a configuration file.  Typically there is an instance of this class associated
 * with each instance of {@link RemoteHost}.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SSHTunnel {

    private static final java.util.logging.Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final int SERVER_ALIVE_INTERVAL_SECONDS = 5;
    private final int SERVER_ALIVE_COUNT            = 2;

    public  final int           testTXPort;
    public  final int           testRXPort;
    private final List<Forward> forwards;
    private final RemoteHost    host;

    private SSHExecutor         sshExecutor;


    /**
     * Creates a new instance of {@link SSHTunnel}, configured through the specified configuration.
     *
     * @param _host the remote host that this tunnel is associated with
     * @param _tunnelConfig the configuration for the tunnel (part of the ISPMonitor's remote host configuration)
     */
    public SSHTunnel( final RemoteHost _host, final JSONObject _tunnelConfig ) {

        // the basics...
        host = _host;
        forwards = new ArrayList<>();

        // first extract the test connection details...
        JSONObject testConnection = _tunnelConfig.getJSONObject( "testConnection" );
        testRXPort       = testConnection.getInt( "localRxPort" );
        testTXPort       = testConnection.getInt( "localTxPort" );
        int remoteRXPort = testConnection.getInt( "remoteRxPort" );

        // configure our two testing forwards...
        forwards.add( new Forward( true,  testTXPort,   remoteRXPort, "localhost" ) );
        forwards.add( new Forward( false, remoteRXPort, testRXPort,   "localhost" ) );

        // configure the local forwards...
        JSONArray locals = _tunnelConfig.getJSONArray( "localForwards" );
        for( int i = 0; i < locals.length(); i++ ) {
            JSONObject local = locals.getJSONObject( i );
            forwards.add( new Forward( true, local.getInt( "localPort" ), local.getInt( "remotePort" ), local.getString( "remoteHost" ) ) );
        }

        // configure the remote forwards...
        JSONArray remotes = _tunnelConfig.getJSONArray( "remoteForwards" );
        for( int i = 0; i < remotes.length(); i++ ) {
            JSONObject remote = remotes.getJSONObject( i );
            forwards.add( new Forward( false, remote.getInt( "localPort" ), remote.getInt( "remotePort" ), remote.getString( "localHost" ) ) );
        }
    }


    public void start() {

        try {
            sshExecutor = getExecutor();
            LOGGER.finer( "SSH Tunnel about to start: " + sshExecutor );
            sshExecutor.start();
        }
        catch( IOException _e ) {
            LOGGER.log( Level.WARNING, "SSH tunnel cannot connect", _e );
        }
    }


    public boolean isUp() {
        return isNotNull( sshExecutor ) && sshExecutor.isAlive();
    }


    /**
     * Returns an instance of {@link SSHExecutor} configured with the port forwards configured in this instance.
     *
     * @return an instance of {@code SSHExecutor} configured with the port forwards configured in this instance
     */
    private SSHExecutor getExecutor() {

        // get a basic forwarding executor...
        SSHExecutor executor = new SSHExecutor( host.getHostname(), SERVER_ALIVE_INTERVAL_SECONDS, SERVER_ALIVE_COUNT );

        // if we have a specified user or identity file, provision them...
        if( !isEmpty( host.getUser() ) )
            executor.setUser( host.getUser() );
        if( !isEmpty( host.getIdentityFile() ) )
            executor.addIdentityFilePath( host.getIdentityFile() );

        // now add all our forwards...
        for( Forward forward : forwards ) {
            if( forward.isLocal )
                executor.addLocalPortForwarding( forward.localPort, forward.host, forward.remotePort );
            else
                executor.addRemotePortForwarding( forward.remotePort, forward.host, forward.localPort );
        }

        return executor;
    }


    /**
     * If the specified remote host configuration contains the specification for a tunnel, returns a new instance of {@link SSHTunnel}.  Otherwise
     * return {@code null}.
     *
     * @param _remoteHostConfig the remote host configuration that may (or may not!) contain a tunnel configuration.
     * @return a new instance of {@code SSHTunnel}, or {@code null} if no tunnel is specified
     */
    public static SSHTunnel getTunnelIfSpecified( final RemoteHost _host, final JSONObject _remoteHostConfig ) {

        return _remoteHostConfig.has( "tunnel" ) ? new SSHTunnel( _host, _remoteHostConfig.getJSONObject( "tunnel" ) ) : null;
    }


    private static class Forward {
        private boolean isLocal;
        private int     localPort;
        private int     remotePort;
        private String  host;


        public Forward( final boolean _isLocal, final int _localPort, final int _remotePort, final String _host ) {
            isLocal = _isLocal;
            localPort = _localPort;
            remotePort = _remotePort;
            host = _host;
        }
    }
}
