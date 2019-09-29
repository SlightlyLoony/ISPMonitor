package com.dilatush.ispmonitor;

import com.dilatush.mop.Mailbox;
import com.dilatush.util.Config;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.EventType.Heartbeat;
import static com.dilatush.ispmonitor.MainState.INITIAL;
import static com.dilatush.ispmonitor.SystemAvailability.*;
import static com.dilatush.util.General.isNotNull;

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
    private static final TimerTask HEARTBEAT_TIMER_TASK   = new TimerTask() { public void run() { ISPMonitor.postEvent( HEARTBEAT_EVENT ); } };

    private final Config                              config;
    private final Timer                               timer;
    private final Mailbox                             mailbox;

    private MainState          state;
    private EdgeRouter         edgeRouter;
    private RemoteHosts        hosts;
    private POTester           poTester;
    private ConnectivityTester connectivityTester;


    public MainSM( final Config _config ) {

        config = _config;

        // just for convenience, get a reference to the timer...
        timer = ISPMonitor.getTimer();

        // set our startup state...
        state      = INITIAL;
        mailbox    = ISPMonitor.getPostOffice().createMailbox( "test" );  // get our special testing mailbox...
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
            case SSHResult:                handleSSHResult(             (SSHResult)              _event.payload );                       break;
            case DNSResult:                handleDNSResult(             (DNSResult)              _event.payload );                       break;
            case RouterISP:                handleRouterISP(             (ISPChoice)              _event.payload );                       break;
            case PostOfficeTest:           handlePostOfficeTest(        (POTestResult)           _event.payload );                       break;
            case ConnectivityTest:         handleConnectivityTest(      (ConnectivityTestResult) _event.payload );                       break;

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

        // start up our connectivity tester...
        connectivityTester = new ConnectivityTester( config );

        // get our edge router and query its state...
        edgeRouter = new EdgeRouter( config );
        edgeRouter.getCurrentISP();

        // get our remote hosts...
        hosts = new RemoteHosts( config );

        // start up our post office tester...
        POTester poTester = new POTester( config, hosts, mailbox );

        // start our heartbeat...
        ISPMonitor.getTimer().scheduleAtFixedRate( HEARTBEAT_TIMER_TASK, HEARTBEAT_MS, HEARTBEAT_MS );

        /////// test code //////////
        hosts.getHost( "paradise.dilatush.com" ).setDesiredTunnelState( UP );
        ////////////////////////////
    }


    private void handleConnectivityTest( final ConnectivityTestResult _result ) {
        _result.handler.handle( _result );
    }


    private void handlePostOfficeTest( final POTestResult _poTestResult ) {
        hosts.getServiceUsingPostOffice( _poTestResult.postOffice ).updatePostOfficeAvailability( _poTestResult.availability );
    }


    private void handleSSHResult( final SSHResult _sshResult ) {
        _sshResult.handler.handle( _sshResult );
        hashCode();
    }


    private void handleDNSResult( final DNSResult _dnsResult ) {
        _dnsResult.handler.handle( _dnsResult );
        hashCode();
    }


    private void handleHeartbeat() {

        if( isNotNull( edgeRouter) )
            edgeRouter.heartbeat();
        if( isNotNull( hosts ) )
            hosts.heartbeat();
    }


    private void handleRouterISP( final ISPChoice _ispChoice ) {

    }


    /* package-private */ void executeTask( final Task _task ) {
        ISPMonitor.executeTask( _task );
    }
}
