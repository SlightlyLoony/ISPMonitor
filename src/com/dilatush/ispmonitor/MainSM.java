package com.dilatush.ispmonitor;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.EventType.*;
import static com.dilatush.ispmonitor.MainState.*;
import static com.dilatush.ispmonitor.SystemAvailability.*;

/**
 * Implements the state machine that is the heart of ISPMonitor.
 *
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MainSM implements StateMachine<MainState> {

    private static final Logger LOGGER                 = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());
    private static final int    STARTUP_SETTLING_TICKS = ISPMonitor.secondsToTicks( 10 );

    private final SystemAvailabilityState priISPDNS1;
    private final SystemAvailabilityState priISPDNS2;
    private final SystemAvailabilityState secISPDNS1;
    private final SystemAvailabilityState secISPDNS2;
    private final SystemAvailabilityState priISP;
    private final SystemAvailabilityState secISP;

    private MainState state;
    private int       startupSettlingTicks;


    public MainSM() {

        // set our startup state...
        state      = STOPPED;
        priISPDNS1 = new SystemAvailabilityState( UNKNOWN );
        priISPDNS2 = new SystemAvailabilityState( UNKNOWN );
        secISPDNS1 = new SystemAvailabilityState( UNKNOWN );
        secISPDNS2 = new SystemAvailabilityState( UNKNOWN );
        priISP     = new SystemAvailabilityState( UNKNOWN, 6, 0 );
        secISP     = new SystemAvailabilityState( UNKNOWN, 6, 0 );
    }


    /**
     * The main state machine processing method.
     *
     * @param _event the event to process
     */
    public void handleEvent( final Event _event ) {

        LOGGER.log( _event.type == Heartbeat ? Level.FINEST : Level.FINER, "MainSM handling " + _event );

        switch( _event.type ) {

            case Heartbeat:                handleHeartbeat();                                                  break;
            case PrimaryISPDNS1State:      handlePrimaryISPDNS1State(   (SystemAvailability) _event.payload ); break;
            case PrimaryISPDNS2State:      handlePrimaryISPDNS2State(   (SystemAvailability) _event.payload ); break;
            case SecondaryISPDNS1State:    handleSecondaryISPDNS1State( (SystemAvailability) _event.payload ); break;
            case SecondaryISPDNS2State:    handleSecondaryISPDNS2State( (SystemAvailability) _event.payload ); break;
            case PrimaryISPRawState:       handlePrimaryISPRawState(    (SystemAvailability) _event.payload ); break;
            case SecondaryISPRawState:     handleSecondaryISPRawState(  (SystemAvailability) _event.payload ); break;
            case RouterISP:                handleRouterISP(             (ISPUsed)            _event.payload ); break;
            default:
                LOGGER.warning( "Unknown event type (" + _event.type + ") received by state machine; ignoring" );
        }
    }


    @Override
    public MainState getState() {
        return state;
    }


    private void handleHeartbeat() {

        // if we're stopped, see if it's time to be running (known ISP state for STARTUP_SETTLING_SECONDS)...
        if( state == STOPPED ) {

            // if we know the state of our ISPs...
            if( (priISP.getState() != UNKNOWN) && (secISP.getState() != UNKNOWN) ) {

                // if we've had known ISP state's for our settling period...
                if( ++startupSettlingTicks >= STARTUP_SETTLING_TICKS ) {

                    // change our state to running...
                    state = RUNNING;
                    LOGGER.info( "Main state machine is now RUNNING" );
                }
            }

            // clear our settling period counter...
            else {
                startupSettlingTicks = 0;
            }
        }
    }


    private void handleRouterISP( final ISPUsed _ispUsed ) {

    }


    private void handlePrimaryISPRawState( final SystemAvailability _availability ) {

        // post an event if our state changed...
        SystemAvailability current = priISP.getState();
        priISP.update( _availability );
        if( current != priISP.getState() ) {
            postEvent( new Event( PrimaryISPAvailabilityChanged, priISP.getState() ) );
            LOGGER.info( "Primary ISP state changed to " + priISP.getState() );
        }
    }


    private void handleSecondaryISPRawState( final SystemAvailability _availability ) {

        // post an event if our state changed...
        SystemAvailability current = secISP.getState();
        secISP.update( _availability );
        if( current != secISP.getState() ) {
            postEvent( new Event( SecondaryISPAvailabilityChanged, secISP.getState() ) );
            LOGGER.info( "Secondary ISP state changed to " + secISP.getState() );
        }
    }


    private void handlePrimaryISPDNS1State( final SystemAvailability _availability ) {
        priISPDNS1.update( _availability );
        determinePriDNSState();
    }


    private void handlePrimaryISPDNS2State( final SystemAvailability _availability ) {
        priISPDNS2.update( _availability );
        determinePriDNSState();
    }


    private void determinePriDNSState() {
        if( (priISPDNS1.getState() == UP) || (priISPDNS2.getState() == UP) )
            postEvent( new Event( PrimaryISPRawState, UP ) );
        else
            postEvent( new Event( PrimaryISPRawState, DOWN ) );
    }


    private void handleSecondaryISPDNS1State( final SystemAvailability _availability ) {
        secISPDNS1.update( _availability );
        determineSecDNSState();
    }


    private void handleSecondaryISPDNS2State( final SystemAvailability _availability ) {
        secISPDNS2.update( _availability );
        determineSecDNSState();
    }


    private void determineSecDNSState() {
        if( (secISPDNS1.getState() == UP) || (secISPDNS2.getState() == UP) )
            postEvent( new Event( SecondaryISPRawState, UP ) );
        else
            postEvent( new Event( SecondaryISPRawState, DOWN ) );
    }
}
