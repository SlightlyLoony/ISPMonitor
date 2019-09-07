package com.dilatush.ispmonitor;


import com.dilatush.util.Executor;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Uses the standard Unix command "dig" to test whether a given DNS server is functioning and accessible.  The only result of this operation is a
 * "yes" or "no" regarding the DNS server's functioning and accessibility - the actual results are not returned or saved.  A domain randomly selected
 * from amongst the given domains is tested; these domains should be popular, likely long-lived domain names (such as google.com, wikipedia.org,
 * etc.).  See {@link #test()} for more details.
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSDigger {

    private static final Logger     LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private final String            address;
    private final String[]          domains;
    private final Random            random;
    private final int               tries;
    private final Stats             stats;

    private long retries = 0;    // total number of retries this instance has made since instantiation...


    /**
     * Creates a new instance of this class with the given attributes.
     *
     * @param _address dotted-form IP address of the DNS server to test
     * @param _domains popular, stable domain names to test the DNS server with (by attempting to resolve them to IP addresses)
     * @param _statistics_period the period of time, in milliseconds, to keep availability statistics for
     * @param _tries the number of times to try "digging" a DNS server before concluding it's down
     */
    public DNSDigger( final String _address, final String[] _domains, final long _statistics_period, final int _tries ) {
        address           = _address;
        random            = new Random( System.currentTimeMillis() + address.hashCode() );
        domains           = _domains;
        stats             = new Stats( _statistics_period );
        tries             = _tries;
    }


    /**
     * Test the DNS server configured in this instance, returning <code>true</code> if the DNS server responded within the configured number of tries,
     * or <code>false</code> if it did not.
     *
     * @return <code>true</code> if this instance's DNS server is accessible and functioning
     */
    public boolean test() {

        int triesSoFar = 0;
        State state = State.DOWN;  // we assume down until proven otherwise...
        do {

            // execute dig to test current availability of the DNS server at the address in this instance...
            // exit code of zero means all worked; anything else means some sort of problem (and we don't care what kind)...
            if( 0 == dig() )
                state = State.UP;

            triesSoFar++;

        } while( (state == State.DOWN) && (triesSoFar < tries) );  // keep trying until we see the DNS server up, or we've tried all that we should...

        // update our retry counter...
        retries += (triesSoFar - 1);

        // update our stats...
        stats.update( state );

        // log and return our results...
        LOGGER.fine( "DNS server at " + address + " is " + state );
        return (state == State.UP);
    }


    /**
     * Returns the availability of this DNS server over the last statistics period, where 0 is never available and 1 is always available.
     *
     * @return the availability of this DNS server over the last statistics period
     */
    public double availability() {
        return stats.uptime();
    }


    /**
     * Execute "dig" against our configured DNS server, with a randomly-selected domain name, and return the exit code.
     *
     * @return the exit code from "dig" after attempting to resolve a domain name
     */
    private int dig() {

        // get a random domain to test...
        String domain = domains[random.nextInt( domains.length )];

        // construct the string we're going to execute...
        String execute = "dig +tries=1 +short +time=1 @" + address + " " + domain;
        LOGGER.finer( "Executing: " + execute );

        // create our executor...
        Executor executor = new Executor( execute );

        // run the executor and return the exit code...
        executor.run();
        return executor.getExitCode();
    }


    /**
     * Returns the dotted-form IP address of the DNS server this instance tests.
     *
     * @return the dotted-form IP address of the DNS server this instance tests
     */
    public String getAddress() {
        return address;
    }


    /**
     * Returns the total number (since instantiation) of retries this instance has made when "digging" the configured DNS server.  Note that this
     * count does <i>not</i> include the initial try, but just the <i>re</i>tries.
     *
     * @return the total number (since instantiation) of retries this instance has made
     */
    public long getRetries() {
        return retries;
    }


}
