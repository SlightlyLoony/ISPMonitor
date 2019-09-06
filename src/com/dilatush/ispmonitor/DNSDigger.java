package com.dilatush.ispmonitor;


import com.dilatush.util.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSDigger {

    private static final Logger     LOGGER     = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName() );

    private static final long       STATISTICS_PERIOD = 1000 * 3600 * 24;  // period (in milliseconds) to keep statistics for...
    private static final int        TRIES              = 3;                 // number of times to try digging before we conclude it's not available...

    private final String            address;
    private final String[]          domains;
    private final Random            random;
    private final List<StateRecord> records = new ArrayList<>();

    private long retries = 0;    // total number of retries this instance has made...


    public DNSDigger( final String _address, final String[] _domains ) {
        address      = _address;
        random       = new Random( System.currentTimeMillis() + address.hashCode() );
        domains      = _domains;
    }


    public boolean test() {

        int tries = 0;
        State state = State.DOWN;
        do {

            // execute dig to test current availability of the DNS server at the address in this instance...
            if( 0 == dig() )
                state = State.UP;

            tries++;

        } while( (state == State.DOWN) && (tries < TRIES) );

        // update our retry counter...
        retries += (tries - 1);

        // update our stats...
        if( (records.size() == 0) ||
            (records.get( records.size() - 1 ).state != state) )
            records.add( new StateRecord( state, System.currentTimeMillis() ) );
        prune();

        return state == State.UP;
    }


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


    private void prune() {

        long threshold = System.currentTimeMillis() - STATISTICS_PERIOD;
        while( (records.size() > 1) && (records.get( 0 ).timestamp < threshold) ) {
            records.remove( 0 );
        }
    }


    private enum State {UP, DOWN}

    private static class StateRecord {
        private final State state;
        private final long timestamp;


        private StateRecord( final State _state, final long _timestamp ) {
            state = _state;
            timestamp = _timestamp;
        }
    }
}
