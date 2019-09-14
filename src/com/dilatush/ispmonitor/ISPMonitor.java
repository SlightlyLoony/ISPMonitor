package com.dilatush.ispmonitor;

import com.dilatush.mop.Mailbox;
import com.dilatush.mop.PostOffice;
import com.dilatush.util.Config;

import java.io.File;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNotNull;

/**
 * Implements a monitor of our Internet connection.  Accepts the a single argument on the command line for the ISP monitor configuration file, whose
 * default name is "isp_monitor_config.json".  This program is normally run from a jar, via a systemd service.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ISPMonitor {

    private static final Logger LOGGER                     = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());
    private static final int    ROUTER_CONTROL_NOT_WORKING = 1;
    private static final int    MAX_QUEUED_TASKS           = 500;

    private static Mailbox mailbox;
    private static LinkedBlockingQueue<Task> tasks;
    private static StateMachine stateMachine;


    public static void main( String[] _args ) throws InterruptedException {

        // determine the configuration file...
        String config = "isp_monitor_config.json";   // the default...
        if( isNotNull( (Object) _args ) && (_args.length > 0) ) config = _args[0];
        if( !new File( config ).exists() ) {
            System.out.println( "ISP Monitor configuration file \"" + config + "\" does not exist!" );
            return;
        }

        // get our configuration file...
        Config ispMonConfig = Config.fromJSONFile( config );
        long monitorIntervalSeconds = ispMonConfig.optLongDotted( "monitorInterval", 60 );
        long monitorInterval = 1000 * monitorIntervalSeconds;
        LOGGER.log( Level.INFO, "ISP Monitor is starting, publishing updates at " + monitorIntervalSeconds + " second intervals" );

        // get our router...
        Router router = new Router( ispMonConfig.getStringDotted( "router" ) );

        // start up our post office...
        PostOffice po = new PostOffice( config );
        mailbox = po.createMailbox( "monitor" );

        // start up our timer...
        Timer timer = new Timer( "Timer", true );

        // set up our task queue...
        tasks = new LinkedBlockingQueue<>( MAX_QUEUED_TASKS );

        // set up and start our state machine...
        stateMachine = new StateMachine();

        // start up our permanently scheduled tasks...
        DNSTester dnsTester = new DNSTester( timer, ispMonConfig );

        // get the current setting of the default route in the router...
        ISPUsed isp = router.getCurrentISP();

        // if it's unknown, we have a bad problem and it's time to leave...
        if( isp == ISPUsed.UNKNOWN ) {
            LOGGER.severe( "Cannot read the current state of the router; aborting" );
            System.exit( ROUTER_CONTROL_NOT_WORKING );
        }

        LOGGER.info( "Router is currently configured to " + isp + " ISP" );

        try {
            //noinspection InfiniteLoopStatement
            while( true ) {
                Task task = tasks.take();
                task.run();
            }
        }
        catch( InterruptedException _exc ) {
            LOGGER.severe( "ISPMonitor aborted by InterruptedException" );
        }
    }


    /* package-private */ static void executeTask( final Task _task ) {
        if( !tasks.offer( _task ) )
            throw new IllegalStateException( "ISPMonitor task queue is full" );
    }


    /* package-private */ static void postEvent( final Event _event ) {
        stateMachine.postEvent( _event );
    }
}
