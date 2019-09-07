package com.dilatush.ispmonitor;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Uses the standard Unix command "dig" to test whether the configured Internet Service Provider (ISP) is up or down.  The ISP is considered up if
 * either of two DNS servers belonging to the ISP are accessible and functional.  One DNS server is chosen at random to be tested, and if it is up,
 * then the ISP is considered up.  If the first DNS server is down, then the second DNS server is also tested, and if it is up then the ISP is
 * considered up.  However, if <i>both</i> DNS servers are down, then the ISP is considered down.
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ISPTester {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final DNSDigger         dns1;
    private final DNSDigger         dns2;
    private final Random            random;
    private final Stats             stats;
    private final ISPInfo           isp;


    /**
     * Creates a new instance of this class with the given attributes.
     *
     * @param _isp information about the ISP to be tested by this instance
     * @param _domains popular, stable domain names to test the DNS servers with (by attempting to resolve them to IP addresses)
     * @param _statistics_period the period of time, in milliseconds, to keep availability statistics for
     * @param _tries the number of times to try "digging" a DNS server before concluding it's down
     */
    public ISPTester( final ISPInfo _isp, final String[] _domains, final long _statistics_period, final int _tries  ) {

        isp    = _isp;
        stats  = new Stats( _statistics_period );
        random = new Random( System.currentTimeMillis() + _isp.name.hashCode() );
        dns1   = new DNSDigger( _isp.dns1, _domains, _statistics_period, _tries );
        dns2   = new DNSDigger( _isp.dns2, _domains, _statistics_period, _tries );
    }


    /**
     * Test the ISP configured in this instance, returning <code>true</code> if at least one of the DNS servers associated with this ISP is up,
     * or <code>false</code> if it did not.
     *
     * @return <code>true</code> if this instance's ISP is accessible and functioning
     */
    public boolean test() {

        // choose a DNS server to start with...
        boolean dns1First = random.nextBoolean();

        // test the first DNS server...
        boolean ispUp;
        if( dns1First )
            ispUp = dns1.test();
        else
            ispUp = dns2.test();

        // if the first DNS server is down, then we need to test the second DNS server...
        if( !ispUp ) {

            // test the second DNS server...
            if( dns1First )
                ispUp = dns2.test();
            else
                ispUp = dns1.test();
        }

        // track our statistics...
        stats.update( ispUp ? State.UP : State.DOWN );

        // log and return our result...
        LOGGER.fine( "ISP " + isp.name + " is " + (ispUp ? "UP" : "DOWN") );
        return ispUp;
    }


    /**
     * Returns the availability of this ISP over the last statistics period, where 0 is never available and 1 is always available.
     *
     * @return the availability of this ISP over the last statistics period
     */
    public double availability() {
        return stats.uptime();
    }


    public DNSDigger getDNS1() {
        return dns1;
    }


    public DNSDigger getDNS2() {
        return dns2;
    }
}
