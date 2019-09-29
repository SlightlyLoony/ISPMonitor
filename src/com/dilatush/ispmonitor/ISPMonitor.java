package com.dilatush.ispmonitor;

import com.dilatush.mop.Mailbox;
import com.dilatush.mop.Message;
import com.dilatush.mop.PostOffice;
import com.dilatush.util.Config;

import java.io.File;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.EventType.Start;
import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.Strings.isEmpty;

/**
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ISPMonitor {

    private static final Logger    LOGGER                     = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private static final int       MAX_QUEUED_TASKS           = 500;
    private static final int       TICKS_PER_SECOND           = 8;

    private static PostOffice                po;
    private static Mailbox                   mailbox;
    private static LinkedBlockingQueue<Task> tasks;
    private static StateMachine              mainStateMachine;
    private static EventQueue                eventQueue;
    private static Timer                     timer;


    public static void main( String[] _args ) throws InterruptedException {

        /*
            Implementation notes:

            Tasks scheduled to execute via the Timer, and event processing within the state machines, should all execute quickly.  That is, they
            should not block, and should not be computationally intensive.  Failing to do this will cause all sorts of nasty timing issues.

            When you have tasks that are going to take some appreciable time, or that block, the proper mechanism to use is the tasks queue.  You can
            add a task to this queue through the executeTask() method in this class.  The tasks on that queue are executed sequentially in a single
            thread so it's possible that such tasks will be delayed a few seconds or even more.

            If you have a task that may take appreciable time but MUST be executed quickly, then the proper way to handle that is with a separate
            thread.
         */

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

        // set up our task queue...
        tasks = new LinkedBlockingQueue<>( MAX_QUEUED_TASKS );

        // start up our timer...
        timer = new Timer( "Timer", true );

        // start up our post office...
        po = new PostOffice( config );
        mailbox = po.createMailbox( "monitor" );

        // wait until we've connected to the central post office...
        while( !po.isConnected() ) {
            Thread.sleep( 100 );
        }

        // set up and start our state machine...
        mainStateMachine = new MainSM( ispMonConfig );
        eventQueue = new EventQueue( mainStateMachine );
        mainStateMachine.postEvent( new Event( Start ) );

        // we just loop here forever, executing any tasks that get queued...
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


    /**
     * Converts the specified floating point seconds (which must be greater than zero) into an integral number of ticks.  Note that the result is
     * always rounded <i>up</i>, so the number of ticks returned will always represent a time that is greater than or equal to the specified number
     * of seconds.
     *
     * @param _seconds the number of seconds to convert to ticks
     * @return the smallest number of ticks that represents at least the specified number of seconds
     */
    /* package-private */ static int secondsToTicks( final double _seconds ) {

        // sanity checks...
        if( _seconds <= 0 )
            throw new IllegalArgumentException( "Specified seconds must be greater than zero: " + _seconds );
        if( _seconds > (double)(Integer.MAX_VALUE / TICKS_PER_SECOND) )
            throw new IllegalArgumentException( "Specified seconds must be less than 2^31: " + _seconds );

        return (int) Math.ceil( _seconds * TICKS_PER_SECOND );
    }


    /* package-private */ static void executeTask( final Task _task ) {
        if( !tasks.offer( _task ) )
            throw new IllegalStateException( "ISPMonitor task queue is full" );
    }


    /**
     * Sends an event message to the MOP events system.  The message has "isp.monitor" as its source; the rest of the attributes are as specified.
     *
     * @param _tag the tag for the event
     * @param _type the type for the event
     * @param _subject the subject of the event (for email and text)
     * @param _message the message about the event
     * @param _level the level of the event (0..9)
     */
    /* package-private */ static void sendMOPEvent( final String _tag, final String _type, final String _subject,
                                                    final String _message, final int _level ) {

        // sanity checks...
        if( isEmpty( _tag ) || isEmpty( _type ) || isEmpty( _subject ) || isEmpty( _message ) )
            throw new IllegalArgumentException( "String argument missing" );
        if( (_level < 0) || (_level > 9) )
            throw new IllegalArgumentException( "Level is out of range (0..9): " + _level );

        // build the event message...
        Message msg = mailbox.createDirectMessage( "events.post", "event.post", false );
        msg.put( "tag", _tag );
        msg.put( "timestamp", System.currentTimeMillis() );
        msg.putDotted( "event.source",  "isp.monitor" );
        msg.putDotted( "event.type",    _type         );
        msg.putDotted( "event.message", _message      );
        msg.putDotted( "event.level",   _level        );
        msg.putDotted( "event.subject", _subject      );

        // send it...
        mailbox.send( msg );

        LOGGER.finer( "Sent event: " + _tag + ", " + _subject );
    }


    /* package-private */ static void postEvent( final Event _event ) {
        eventQueue.postEvent( _event );
    }


    /* package-private */ static Mailbox getMailbox() {
        return mailbox;
    }


    /* package-private */ static PostOffice getPostOffice() {
        return po;
    }


    /* package-private */ static Timer getTimer() {
        return timer;
    }
}
