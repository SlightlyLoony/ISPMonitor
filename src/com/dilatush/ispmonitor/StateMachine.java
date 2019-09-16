package com.dilatush.ispmonitor;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public interface StateMachine<E> {

    void handleEvent( final Event _event );
    E getState();

    default void postEvent( final Event _event ) {
        ISPMonitor.postEvent( _event );
    }
}
