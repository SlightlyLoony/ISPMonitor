package com.dilatush.ispmonitor;

/**
 * Simple POJO to contain an event type and optional payload.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Event {

    /* package-private */ final EventType type;
    /* package-private */ final Object    payload;


    /* package-private */ Event( final EventType _type ) {
        type = _type;
        payload = null;
    }

    /* package-private */ Event( final EventType _type, final Object _payload ) {
        type = _type;
        payload = _payload;
    }
}
