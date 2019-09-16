package com.dilatush.ispmonitor;

import com.dilatush.util.Config;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.EventType.*;
import static com.dilatush.ispmonitor.SystemAvailability.DOWN;
import static com.dilatush.ispmonitor.SystemAvailability.UP;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSTester {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final int        minIntervalSeconds;
    private final int        maxIntervalSeconds;
    private final int        tries;
    private final String[]   testDomains;
    private final String     priDNS1;
    private final String     priDNS2;
    private final int        priMaxMs;
    private final String     secDNS1;
    private final String     secDNS2;
    private final int        secMaxMs;
    private final Timer      timer;
    private final Random     random;


    public DNSTester( final Timer _timer, final Config _config ) {

        // stuff away our timer and executor...
        timer = _timer;

        // get our configuration...
        minIntervalSeconds      = _config.getIntDotted(    "dnsTests.minTestIntervalSeconds"  );
        maxIntervalSeconds      = _config.getIntDotted(    "dnsTests.maxTestIntervalSeconds"  );
        tries                   = _config.getIntDotted(    "dnsTests.tries"                   );
        testDomains             = _config.getStringDotted( "dnsTests.domains"                 ).split( "," );
        priDNS1                 = _config.getStringDotted( "dnsTests.primary.dns1"            );
        priDNS2                 = _config.getStringDotted( "dnsTests.primary.dns2"            );
        priMaxMs                = _config.getIntDotted(    "dnsTests.primary.maxMs"           );
        secDNS1                 = _config.getStringDotted( "dnsTests.secondary.dns1"          );
        secDNS2                 = _config.getStringDotted( "dnsTests.secondary.dns2"          );
        secMaxMs                = _config.getIntDotted(    "dnsTests.secondary.maxMs"         );

        // set up our source of randomness...
        random = new Random( System.currentTimeMillis() + Arrays.hashCode( testDomains ) );

        // schedule our first test to happen right away...
        timer.schedule( new RunTests(), 1 );
    }


    private class RunTests extends TimerTask {

        /**
         * Initiate tests of all four DNS servers.
         */
        @Override
        public void run() {

            // start the tests...
            ISPMonitor.executeTask( new DNSTest( priDNS1, priMaxMs, testDomains[random.nextInt( testDomains.length )], PrimaryISPDNS1State   ) );
            ISPMonitor.executeTask( new DNSTest( priDNS2, priMaxMs, testDomains[random.nextInt( testDomains.length )], PrimaryISPDNS2State   ) );
            ISPMonitor.executeTask( new DNSTest( secDNS1, secMaxMs, testDomains[random.nextInt( testDomains.length )], SecondaryISPDNS1State ) );
            ISPMonitor.executeTask( new DNSTest( secDNS2, secMaxMs, testDomains[random.nextInt( testDomains.length )], SecondaryISPDNS2State ) );

            // schedule the next one...
            long delay = 1000 * minIntervalSeconds + random.nextInt( 1000 * (maxIntervalSeconds - minIntervalSeconds ) );
            timer.schedule( new RunTests(), delay );
        }
    }


    private class DNSTest implements Task {

        private final String address;
        private final String domain;
        private final int maxMs;
        private final EventType type;


        private DNSTest( final String _dnsServerAddress, final int _maxMs, final String _domain, final EventType _type ) {
            address = _dnsServerAddress;
            domain  = _domain;
            maxMs   = _maxMs;
            type    = _type;
        }


        @Override
        public void run() {

            // query the DNS server...
            DNSTestQuery query = new DNSTestQuery( address, domain, tries, maxMs );
            query.query();

            // report the results to the state machine...
            ISPMonitor.postEvent( new Event( type, query.isUp() ? UP : DOWN ) );

            if( query.isUp() )
                LOGGER.info( "Queried DNS server " + address + " for " + domain + "; it is UP, responded in " + query.getActualTime()
                        + "ms on try " + query.getActualTries() );
            else
                LOGGER.info( "Queried DNS server " + address + " for " + domain + "; it is DOWN, didn't respond within " + maxMs + "ms" );
        }
    }
}
