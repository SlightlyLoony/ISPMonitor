package com.dilatush.ispmonitor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.SystemAvailability.*;
import static com.dilatush.util.General.isNotNull;

/**
 * Contains information about a remote host, including the services it hosts and the SSH tunnel we might have to it.  Provides methods to start, stop,
 * or restart all configured services on the remote host.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class RemoteHost {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final String                     hostname;
    private final String                     user;                // null if same user as this process...
    private final String                     identityFile;        // null if same identity file path (to private key) as this process...
    private final Map<String, Command>       commands;            // key is the command's name...
    private final SSHTunnel                  tunnel;              // null if this remote host has no tunnel to it...
    private final Map<String, RemoteService> services;            // key is the service's systemd name...

    private SystemAvailability               desiredTunnelState;
    private SystemAvailability               actualTunnelState;


    /**
     * Creates a new instance of {@link RemoteHost} using the specified configuration, which is one {@link JSONObject} in the array under the top-
     * level key "remoteHosts".
     *
     * @param _config the JSONObject specifying a remote host
     */
    /* package-private */ RemoteHost( final JSONObject _config ) {

        // the basics...
        hostname           = _config.getString( "hostname"     );
        user               = _config.has( "user" ) ? _config.getString( "user" ) : null;
        identityFile       = _config.has( "identityFile" ) ? _config.getString( "identityFile" ) : null;
        desiredTunnelState = DOWN;
        actualTunnelState  = DOWN;

        // get our tunnel, if we have one...
        tunnel = SSHTunnel.getTunnelIfSpecified( this, _config );

        // get any commands we might have...
        commands = Command.getCommands( _config, "commands" );

        // get any services we might have...
        services = new HashMap<>();
        if( _config.has( "services" ) ) {
            JSONArray serviceArray = _config.getJSONArray( "services" );
            for( int i = 0; i < serviceArray.length(); i++ ) {
                JSONObject serviceObj = serviceArray.getJSONObject( i );
                services.put( serviceObj.getString( "name" ), new RemoteService( this, serviceObj ) );
            }
        }
    }


    /* package-private */ void heartbeat() {

        // if we have a tunnel, and the tunnel is supposed to be up, but it's not, then start it up...
        if( isNotNull( tunnel ) && (desiredTunnelState == UP) && (actualTunnelState != UP) ) {
            tunnel.start();
            if( tunnel.isUp() )
                actualTunnelState = UP;
        }
    }


    /* package-private */ void serviceStateChanged() {
        LOGGER.info( "Service state changed" );
    }


    /* package-private */ void postOfficeStateChanged() {
        LOGGER.info( "Post office connection state changed" );
    }


    /**
     * Returns the instance of {@link RemoteService} that represents the service on this host with the specified systemd name, or {@code null} if
     * there is no service by that name configured on this host.
     *
     * @param _serviceName the name of the systemd service to retrieve
     * @return the service
     */
    /* package-private */ RemoteService getService( final String _serviceName ) {
        return services.get( _serviceName );
    }


    /**
     * Return a collection of the services hosted on the server represented by this instance.
     *
     * @return a collection of the services hosted on the server represented by this instance
     */
    /* package-private */ Collection<RemoteService> getServices() {
        return services.values();
    }


    /* package-private */ String getHostname() {
        return hostname;
    }


    /* package-private */ String getUser() {
        return user;
    }


    /* package-private */ String getIdentityFile() {
        return identityFile;
    }


    /* package-private */ Map<String, Command> getCommands() {
        return commands;
    }


    /* package-private */ SSHTunnel getTunnel() {
        return tunnel;
    }


    public SystemAvailability getDesiredTunnelState() {
        return desiredTunnelState;
    }


    public void setDesiredTunnelState( final SystemAvailability _desiredTunnelState ) {
        desiredTunnelState = _desiredTunnelState;
    }


    public SystemAvailability getActualTunnelState() {
        return actualTunnelState;
    }
}
