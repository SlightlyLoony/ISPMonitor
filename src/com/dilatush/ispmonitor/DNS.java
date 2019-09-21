package com.dilatush.ispmonitor;

import org.json.JSONObject;

import static com.dilatush.ispmonitor.SystemAvailability.*;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class DNS {

    public final String ip;
    public final int    timeoutMS;
    public final ISP    isp;

    private int                heartbeatsUntilTest;
    private SystemAvailability availability;


    /* package-private */ DNS( final ISP _isp, final JSONObject _config ) {

        // the basics...
        isp       = _isp;
        ip        = _config.getString( "ip" );
        timeoutMS = _config.getInt( "timeoutMS" );

        // initially we set the heartbeats until test to 1, so the first heartbeat will kick off a test...
        heartbeatsUntilTest = 1;

        // and the DNS server's availability is unknown at first...
        availability = UNKNOWN;
    }


    /* package-private */ void heartbeat() {

        // see if it's time to kick off a test...
        if( (heartbeatsUntilTest > 0) && (--heartbeatsUntilTest <= 0) ) {

            // pick a random domain to test...
            String domain = isp.edgeRouter.testDomains[ isp.edgeRouter.random.nextInt( isp.edgeRouter.testDomains.length )];

            // kick off the DNS test query...
            ISPMonitor.executeTask( new DNSTestQueryTask( this::handleDNSResponse, ip, domain, isp.edgeRouter.maxDNSTestTries, timeoutMS ) );
        }
    }


    private void handleDNSResponse( final DNSResult _dnsResult ) {

        // figure out what the new availability is...
        SystemAvailability current = UNKNOWN;
        switch( _dnsResult.type ) {
            case COMPLETED: current = UP;   break;
            case TIMEOUT:   current = DOWN; break;
        }

        // if our availability has changed, update the local availability and let the ISP know there was a change...
        if( current != availability ) {
            availability = current;
            isp.dnsAvailabilityChanged();
        }

        // figure out when to kick off the next DNS test query...
        double minSecs = 1.0d * isp.edgeRouter.minDNSTestIntervalSeconds;
        double maxSecs = 1.0d * isp.edgeRouter.maxDNSTestIntervalSeconds;
        double nextSecs = minSecs + isp.edgeRouter.random.nextDouble() * (maxSecs - minSecs);
        heartbeatsUntilTest = ISPMonitor.secondsToTicks( nextSecs );
    }


    public SystemAvailability getAvailability() {
        return availability;
    }
}
