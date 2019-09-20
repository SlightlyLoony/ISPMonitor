package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNotNull;

/**
 * Creates a collection of remote hosts (and the services and tunnels they host) from a configuration file.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class RemoteHosts {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final Map<String, RemoteHost>    hosts;         // key is hostname...
    private final Map<String, RemoteService> servicesByPO;  // key is MOP post office name, ...


    /**
     * Creates a new instance of {@link RemoteHosts} using the top-level "remoteHosts" array in the specified configuration.
     *
     * @param _config the configuration specifying the remote services
     * @throws IllegalArgumentException on a malformed configuration
     */
    /* package-private */ RemoteHosts( final Config _config ) {
        try {

            // get our hosts...
            hosts = new HashMap<>();
            JSONArray hostObjects = _config.getJSONArray( "remoteHosts" );
            for( int i = 0; i < hostObjects.length(); i++ ) {
                JSONObject host = hostObjects.getJSONObject( i );
                hosts.put( host.getString( "hostname" ), new RemoteHost( host ) );
            }
        }
        catch( JSONException _je ) {
            throw new IllegalArgumentException( "Configuration malformed", _je );
        }

        // build our map of post offices to services...
        servicesByPO = new HashMap<>();
        for( RemoteHost host : hosts.values() ) {
            for( RemoteService service : host.getServices() ) {
                if( isNotNull( service.getPostOffice() ) ) {
                    servicesByPO.put( service.getPostOffice(), service );
                }
            }
        }
    }


    /**
     * Returns the service that uses the specified MOP post office, or {@code null} if none.
     *
     * @param _postOffice the MOP post office whose service is desired
     * @return the service that uses the specified MOP post office
     */
    /* package-private */ RemoteService getServiceUsingPostOffice( final String _postOffice ) {
        return servicesByPO.get( _postOffice );
    }
}
