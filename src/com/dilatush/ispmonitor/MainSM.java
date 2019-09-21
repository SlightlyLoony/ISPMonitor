package com.dilatush.ispmonitor;

import com.dilatush.util.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.EventType.*;
import static com.dilatush.ispmonitor.MainState.INITIAL;
import static com.dilatush.ispmonitor.SystemAvailability.*;

/**
 * Implements the state machine that is the heart of ISPMonitor.  Events are dispatched from a single thread (in an instance of {@link EventQueue}),
 * so all state changes and inspections occur in the context of that thread &mdash; meaning there should be no concurrency issues in the state
 * machine.
 *
 * <p>Instances of this class are mutable and <i>not</i> threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class MainSM implements StateMachine<MainState> {

    private static final Logger    LOGGER                 = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());
    private static final Event     HEARTBEAT_EVENT        = new Event( Heartbeat );
    private static final long      HEARTBEAT_MS           = 1000 / 8;
    private static final TimerTask HEARTBEAT_TASK         = new TimerTask() { public void run() { ISPMonitor.postEvent( HEARTBEAT_EVENT ); } };

    private final SystemAvailabilityState             priISPDNS1;
    private final SystemAvailabilityState             priISPDNS2;
    private final SystemAvailabilityState             secISPDNS1;
    private final SystemAvailabilityState             secISPDNS2;
    private final SystemAvailabilityState             priISP;
    private final SystemAvailabilityState             secISP;
    private final Config                              config;
    private final Timer                               timer;
    private final Map<String,SystemAvailabilityState> servicesState;

    private MainState       state;
    private Router          router;
    private RemoteHosts     hosts;


    public MainSM( final Config _config ) {

        config = _config;

        // just for convenience, get a reference to the timer...
        timer = ISPMonitor.getTimer();

        // set our startup state...
        state      = INITIAL;

        priISPDNS1    = new SystemAvailabilityState( UNKNOWN );
        priISPDNS2    = new SystemAvailabilityState( UNKNOWN );
        secISPDNS1    = new SystemAvailabilityState( UNKNOWN );
        secISPDNS2    = new SystemAvailabilityState( UNKNOWN );
        priISP        = new SystemAvailabilityState( UNKNOWN, 6, 0 );
        secISP        = new SystemAvailabilityState( UNKNOWN, 6, 0 );
        servicesState = new HashMap<>();
    }


    /**
     * The main state machine event processing method.  Note that this method <i>must</i> be called <i>only</i> from {@link EventQueue#run()};
     * otherwise there may be concurrency issues.
     *
     * @param _event the event to process
     */
    public void handleEvent( final Event _event ) {

        LOGGER.log( _event.type == Heartbeat ? Level.FINEST : Level.FINER, "MainSM handling " + _event );

        switch( _event.type ) {

            case Heartbeat:                handleHeartbeat();                                                                            break;
            case Start:                    handleStart();                                                                                break;
            case SSHResult:                handleSSHResult(             (SSHResult)          _event.payload );                           break;
            case PrimaryISPDNS1State:      handlePrimaryISPDNS1State(   (SystemAvailability) _event.payload );                           break;
            case PrimaryISPDNS2State:      handlePrimaryISPDNS2State(   (SystemAvailability) _event.payload );                           break;
            case SecondaryISPDNS1State:    handleSecondaryISPDNS1State( (SystemAvailability) _event.payload );                           break;
            case SecondaryISPDNS2State:    handleSecondaryISPDNS2State( (SystemAvailability) _event.payload );                           break;
            case PrimaryISPRawState:       handlePrimaryISPRawState(    (SystemAvailability) _event.payload );                           break;
            case SecondaryISPRawState:     handleSecondaryISPRawState(  (SystemAvailability) _event.payload );                           break;
            case RouterISP:                handleRouterISP(             (ISPUsed)            _event.payload );                           break;
            default:
                LOGGER.warning( "Unknown event type (" + _event.type + ") received by state machine; ignoring" );
        }
    }


    @Override
    public MainState getState() {
        return state;
    }


    /**
     * Handles a {@link EventType#Start} {@link Event}, which should only occur if the state machine is in initial state.
     */
    private void handleStart() {

        // sanity check...
        if( state != INITIAL )
            throw new IllegalStateException( "Start event occurred while in " + state + " state, instead of INITIAL state" );

        // get our router and query its state...
        router = new Router( config );
        router.getCurrentISP();

        // start up our permanently scheduled tasks...
        DNSTester dnsTester = new DNSTester( timer, config );                     // tests whether DNS servers are up for primary and secondary ISPs...
//        timer.scheduleAtFixedRate( HEARTBEAT_TASK, HEARTBEAT_MS, HEARTBEAT_MS );  // fixed-rate heartbeat event...
//        POTester poTester = new POTester( config );

        // get our remote hosts...
        hosts = new RemoteHosts( config );

        // test code //
        hosts.getServiceUsingPostOffice( "paradise" ).check();
        ///////////////

//        // set up our services state map to all in unknown state, with an up delay of zero and a down delay of 2, launch a check...
//        Collection<ServiceInfo> serviceInfos = hosts.getServices();
//        for( ServiceInfo serviceInfo : serviceInfos ) {
//            servicesState.put( serviceInfo.host.hostname + ":" + serviceInfo.serviceName, new SystemAvailabilityState( UNKNOWN, 0, 2 ) );
//            hosts.check( serviceInfo );
//        }
    }


    private void handleSSHResult( final SSHResult _sshResult ) {
        _sshResult.handler.handle( _sshResult );
        hashCode();
    }


    private void handleHeartbeat() {
        LOGGER.info( "WWW: " + servicesState.get( "paradise.dilatush.com:paradisewww" ).getState() );
    }


    private void handleRouterISP( final ISPUsed _ispUsed ) {

    }


    private void handlePrimaryISPRawState( final SystemAvailability _availability ) {

        // post an event if our state changed...
        SystemAvailability current = priISP.getState();
        priISP.update( _availability );
        if( current != priISP.getState() ) {
            LOGGER.info( "Primary ISP state changed to " + priISP.getState() );
            postEvent( new Event( PrimaryISPAvailabilityChanged, priISP.getState() ) );
        }
    }


    private void handleSecondaryISPRawState( final SystemAvailability _availability ) {

        // post an event if our state changed...
        SystemAvailability current = secISP.getState();
        secISP.update( _availability );
        if( current != secISP.getState() ) {
            LOGGER.info( "Secondary ISP state changed to " + secISP.getState() );
            postEvent( new Event( SecondaryISPAvailabilityChanged, secISP.getState() ) );
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
