package com.dilatush.ispmonitor;

import com.dilatush.mop.Mailbox;
import com.dilatush.mop.Message;
import com.dilatush.util.Config;
import org.json.JSONException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.SystemAvailability.DOWN;
import static com.dilatush.ispmonitor.SystemAvailability.UP;

/**
 * Implements a test for connectivity of monitored MOP applications to the Central Post Office.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class POTester {

    private static final Logger LOGGER           = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final Set<String> postOffices;
    private final long        intervalMS;
    private final long        timeoutMS;
    private final Mailbox     mailbox;
    private final RemoteHosts hosts;


    /**
     * Creates a new instance of {@link POTester} with the specified configuration.
     *
     * @param _config the configuration data
     */
    public POTester( final Config _config, final RemoteHosts _hosts, final Mailbox _mailbox ) {

        // the basics...
        mailbox = _mailbox;
        hosts   = _hosts;

        try {

            // get our timing configuration...
            intervalMS = _config.getLongDotted( "poTests.intervalMS" );
            timeoutMS  = _config.getLongDotted( "poTests.timeoutMS"  );
        }
        catch( JSONException _je ) {
            throw new IllegalArgumentException( "Configuration malformed", _je );
        }

        // get the names of post offices we need to test from our configuration...
        postOffices = new HashSet<>();
        Set<RemoteService> services = hosts.getServicesUsingPostOffice();
        for( RemoteService service : services ) {
            postOffices.add( service.getPostOffice() );
        }

        // schedule our CPO queries...
        ISPMonitor.getTimer().scheduleAtFixedRate( new RunTests(), intervalMS, intervalMS );
    }


    private class RunTests extends TimerTask {

        /**
         * Queue the query task...
         */
        @Override
        public void run() {

            // start the tests...
            ISPMonitor.executeTask( () -> {

                // send the query to the central post office...
                mailbox.send( mailbox.createDirectMessage( "central.po", "manage.connected", false ) );

                try {

                    // wait for a response, for a limited time...
                    Message response = mailbox.poll( timeoutMS, TimeUnit.MILLISECONDS );

                    // make sure we got the right message...
                    if( "central.po".equals( response.from) && "manage.connected".equals( response.type) ) {

                        // get the connected post offices...
                        String connectedPOs = response.getString( "postOffices" );

                        LOGGER.fine( "Connected Post Offices: " + connectedPOs );

                        // analyze the connected post offices we just received, versus the ones we're monitoring...
                        Set<String> connectedPostOffices = new HashSet<>( Arrays.asList( connectedPOs.split( "," ) ) );
                        for( String monitoredPO : postOffices ) {

                            // send an event describing the result...
                            SystemAvailability sa = connectedPostOffices.contains( monitoredPO ) ? UP : DOWN;
                            ISPMonitor.postEvent( new Event( EventType.PostOfficeTest, new POTestResult( sa, monitoredPO ) ) );
                        }
                    }
                }
                catch( InterruptedException _ie ) {
                    LOGGER.warning( "Post office query task interrupted" );
                }
            } );
        }
    }
}
