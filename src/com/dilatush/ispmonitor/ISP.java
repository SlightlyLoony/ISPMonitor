package com.dilatush.ispmonitor;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.dilatush.ispmonitor.EventType.*;
import static com.dilatush.ispmonitor.SystemAvailability.*;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class ISP {

    public final EdgeRouter edgeRouter;
    public final String     name;
    public final DNS[]      dnss;

    private SystemAvailability availability;


    /* package-private */ ISP( final EdgeRouter _edgeRouter, final JSONObject _config ) {

        // the basics...
        edgeRouter = _edgeRouter;
        name       = _config.getString( "name" );

        // get any DNS servers we have to test...
        JSONArray dnsConfig = _config.getJSONArray( "dns" );
        dnss = new DNS[dnsConfig.length()];
        for( int i = 0; i < dnsConfig.length(); i++ ) {
            dnss[i] = new DNS( this, dnsConfig.getJSONObject( i ) );
        }

        // we don't know the ISP's availability when we start up...
        availability = UNKNOWN;
    }


    /* package-private */ void dnsAvailabilityChanged() {

        // if any of our DNS servers are up, the ISP is up...
        SystemAvailability sa = DOWN;
        for( DNS dns : dnss ) {
            if( dns.getAvailability() == UP ) {
                sa = UP;
                break;
            }
        }

        // if our availability has changed, notify the router and send an event...
        if( sa != availability ) {
            availability = sa;
            edgeRouter.ispAvailabilityChanged();
            ISPMonitor.postEvent( new Event( ISPAvailabilityChanged, this ) );
        }
    }


    /* package-private */ SystemAvailability getAvailability() {
        return availability;
    }
}
