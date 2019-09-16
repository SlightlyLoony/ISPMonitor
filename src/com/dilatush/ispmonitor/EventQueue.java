package com.dilatush.ispmonitor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a simple FIFO queue that accepts new events asynchronously and dispatches them synchronously to a specified state machine in a separate
 * thread.  This mechanism guarantees that event processing will not have concurrency problems, at the price of being dependent on fast event
 * processing for responsive operation.  That is, any event that blocked or consumed significant compute resources would hold up all other event
 * processing.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EventQueue extends Thread {

    private static final Logger LOGGER                    = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());
    private static final int    DEFAULT_MAX_QUEUED_EVENTS = 100;

    private final LinkedBlockingQueue<Event> events;
    private final StateMachine               stateMachine;


    public EventQueue( final StateMachine _stateMachine, final int _limit ) {
        stateMachine = _stateMachine;
        events = new LinkedBlockingQueue<>( _limit );
        setName( "EventQueue" );
        setDaemon( true );
        start();
    }


    public EventQueue( final StateMachine _stateMachine ) {
        this( _stateMachine, DEFAULT_MAX_QUEUED_EVENTS );
    }


    /**
     * Run the state machine.  This call will not return unless there is an error.
     */
    public void run() {

        try {
            //noinspection InfiniteLoopStatement
            while( true ) {
                stateMachine.handleEvent( events.take() );
            }
        }
        catch( InterruptedException _e ) {
            LOGGER.log( Level.SEVERE, "Event queue interrupted", _e );
        }
    }


    /* package-private */ void postEvent( final Event _event ) {
        if( !events.offer( _event ) )
            throw new IllegalStateException( "Event queue is full" );
    }
}
