package com.dilatush.ispmonitor;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Implements a simple, threadsafe timeout event sender.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TimeoutEvent extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final AtomicBoolean marker;
    private final Event         event;
    private final long          timeoutMS;


    /**
     * Creates a new instance of {@link TimeoutEvent} with the specified {@link Event} to be sent upon a timeout duration of the specified number of
     * milliseconds.  A timeout occurs if more than the specified duration passes after this instance is constructed without the {@link #complete()}
     * method on this instance being called.
     *
     * @param _event the event to be sent if a timeout occurs
     * @param _timeoutMS the duration of the timeout, in milliseconds
     */
    /* package-private */ TimeoutEvent( final Event _event, final long _timeoutMS ) {

        event     = _event;
        marker    = new AtomicBoolean( false );
        timeoutMS = _timeoutMS;

        ISPMonitor.getTimer().schedule( this, _timeoutMS );
    }


    /**
     * A call to this method indicates that the operation with a possible timeout has actually completed.  Returns {@code true} if this timeout
     * event has <i>not</i> been sent.  A {@code false} return can occur when the operation completes at essentially the same time as the timeout
     * expires.  In this case there's a race, and a {@code false} return means the timeout won.  Using this method's return results properly
     * guarantees that either a timeout or a successful completion will occur, and not both or neither.
     *
     * @return {@code true} if the timeout event has not been sent
     */
    public boolean complete() {
        cancel();
        boolean result = !marker.getAndSet( true );
        LOGGER.finest( "Timeout completion " + (result ? "successful" : "failed") );
        return result;
    }


    /**
     * Send the specified timeout event if the timeout duration has passed without the operation completing.
     */
    @Override
    public void run() {

        // if the marker was false before we set it to true, then we timed out before the operation asking for this completed...
        if( !marker.getAndSet( true ) ) {

            // post the timeout event...
            ISPMonitor.postEvent( event );

            LOGGER.finest( "Timed out after " + timeoutMS + "ms" );
        }
    }
}
