package com.dilatush.ispmonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of the state (up, down) of something over the specified statistics period.
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.</p>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Stats {

    private final List<StateRecord> records = new ArrayList<>();
    private final long              statistics_period;


    /**
     * Creates a new instance of this class with the given statistics period (in milliseconds).
     *
     * @param _statistics_period the statistics period for this instance, in milliseconds
     */
    public Stats( final long _statistics_period ) {

        statistics_period = _statistics_period;

        // add one record to our statistics, showing an assumed "UP" at instantiation time...
        records.add( new StateRecord( State.UP ) );
    }


    /**
     * Update the statistics records in this instance with the given state.
     *
     * @param _state the state that should represent the most recent state being tracked
     */
    public void update( final State _state ) {

        // update our stats, adding a record if the current result is different than the most recent record...
        if( records.get( records.size() - 1 ).state != _state )
            records.add( new StateRecord( _state ) );

        // now prune any records that are too old, always leaving the most recent one (even if it IS old)...
        long threshold = System.currentTimeMillis() - statistics_period;
        while( (records.size() > 1) && (records.get( 0 ).timestamp < threshold) ) {
            records.remove( 0 );
        }
    }


    /**
     * Returns the uptime tracked by this instance over the last statistics period as a number in the range [0..1], where 0 is never up and 1 is
     * always up.
     *
     * @return the uptime tracked by this instance over the last statistics period
     */
    public double uptime() {

        // tote up the up and down milliseconds...
        long up = 0;
        long dn = 0;

        // iterate over all the state periods we've recorded...
        StateRecord a;
        StateRecord b;
        for( int i = 0; i < records.size() - 1; i++ ) {

            // get the two relevant records...
            a = records.get( i );
            b = records.get( i + 1 );

            // update the appropriate accumulator...
            if( b.state == State.UP )
                up += (b.timestamp - a.timestamp);
            else
                dn += (b.timestamp - a.timestamp);
        }

        // add in the current period to the appropriate accumulator...
        b = records.get( records.size() - 1 );
        if( b.state == State.UP )
            up += (System.currentTimeMillis() - b.timestamp);
        else
            dn += (System.currentTimeMillis() - b.timestamp);

        // now we've got the data for our uptime ratio, just turn it into a double and leave...
        return (1.0D * up) / (up + dn);
    }
}
