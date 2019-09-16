package com.dilatush.ispmonitor;

import com.dilatush.mop.Actor;
import com.dilatush.mop.Message;
import com.dilatush.util.Config;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Implements a test for connectivity of monitored MOP applications to the Central Post Office.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class POTester {

    private static final Logger LOGGER           = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());
    private static final long   TEST_INTERVAL_MS = 5000;
    private static final long   TIMEOUT_MS       = 500;

    private static final POTesterActor actor = new POTesterActor();

    private final Set<String> postOffices;
    private TimeoutEvent timeout;


    public POTester( final Config _config ) {

        // get the names of post offices we need to test from our configuration...
        JSONObject monitoredPostOffices = _config.getJSONObject( "monitoredPostOffices" );
        postOffices = monitoredPostOffices.keySet();

        // schedule our CPO queries...
        ISPMonitor.getTimer().scheduleAtFixedRate( new RunTests(), TEST_INTERVAL_MS, TEST_INTERVAL_MS );
    }


    private class RunTests extends TimerTask {

        /**
         * Queue the query task...
         */
        @Override
        public void run() {

            // start the tests...
            actor.tester = POTester.this;
            ISPMonitor.executeTask( () -> actor.mailbox.send( actor.mailbox.createDirectMessage( "central.po", "manage.connected", false ) ) );

            // start a timeout event going...
            timeout = new TimeoutEvent( new Event( EventType.CPOQueryTimeout), TIMEOUT_MS );
        }
    }


    private static class POTesterActor extends Actor {

        private volatile POTester tester;

        /* package-private */ POTesterActor() {
            super( ISPMonitor.getPostOffice(), "POTester" );
            registerFQDirectMessageHandler( this::connectedHandler, "central.po", "manage", "connected" );
        }


        /* package-private */ void connectedHandler( final Message _message ) {

            // make sure we beat the timeout...
            if( tester.timeout.complete() ) {

                // extract our information...
                String connectedPOs = _message.getString( "postOffices" );

                LOGGER.info( "Post Offices: " + connectedPOs );

                // analyze the connected post offices we just received, versus the ones we're monitoring...
                Set<String> connectedPostOffices = new HashSet<>( Arrays.asList( connectedPOs.split( "," ) ) );
                for( String monitoredPO : tester.postOffices ) {

                    boolean isConnected = connectedPostOffices.contains( monitoredPO );
                    ISPMonitor.postEvent( new Event( isConnected ? EventType.POConnected : EventType.PODisconnected, monitoredPO ) );
                }
            }
        }
    }
}
