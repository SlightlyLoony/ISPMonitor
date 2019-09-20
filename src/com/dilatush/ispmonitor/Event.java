package com.dilatush.ispmonitor;

import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.General.isNull;

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

        if( isNull( _type ) )
            throw new IllegalArgumentException( "No event type specified" );

        type = _type;
        payload = _payload;

        // check to see that our payload is of the right type...
        if( isNull( payload ) && isNotNull( type.payloadClass ) )
            throw new IllegalArgumentException( "Expected " + type.payloadClass.getSimpleName() + " event payload, but got null" );
        if( isNotNull( payload ) ) {
            if( isNull( type.payloadClass ) )
                throw new IllegalArgumentException( "Expected null event payload, but got " + payload.getClass().getSimpleName() );
            if( !type.payloadClass.isAssignableFrom( payload.getClass() ) )
                throw new IllegalArgumentException( "Expected " + type.payloadClass.getSimpleName()
                        + " event payload, but got " + payload.getClass().getSimpleName() );
        }
    }


    public String toString() {
        if( isNull( payload ) )
            return type.toString();
        assert payload != null;
        return type.toString() + " " + payload.toString();
    }
}
