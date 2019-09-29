package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.SystemAvailability.*;

/**
 * Implements periodic connectivity testing to configured TCP services.  Changes in connectivity to those services are reported both as an event in
 * the {@link ISPMonitor} program and as a MOP event.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConnectivityTester {

    private static final Logger LOGGER                 = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final List<Group> groups;
    private final List<Test>  tests;


    /**
     * Creates a new instance of {@link ConnectivityTester} from the specified configuration.
     *
     * @param _config the configuration to build an instance of {@code ConnectivityTester} from
     */
    /* package-private */ ConnectivityTester( final Config _config ) {

        try {
            // the basics...
            groups = new ArrayList<>();
            tests  = new ArrayList<>();
            JSONObject connectivityTestsConfig = _config.getJSONObject( "connectivityTests" );

            // get our tests from configuration...
            JSONArray testsConfig = connectivityTestsConfig.getJSONArray( "tests" );
            for( int i = 0; i < testsConfig.length(); i++ ) {
                tests.add( new Test( testsConfig.getJSONObject( i ) ) );
            }

            // get our groups from configuration...
            JSONArray groupsConfig = connectivityTestsConfig.getJSONArray( "groups" );
            for( int i = 0; i < groupsConfig.length(); i++ ) {
                groups.add( new Group( i, groupsConfig.getJSONObject( i ), tests ) );
            }

            // fill in the group instance in the tests...
            for( Test test : tests ) {
                test.groupInstance = groups.get( test.group );
            }
        }
        catch( JSONException _e ) {
            throw new IllegalArgumentException( "Configuration malformed", _e );
        }

        // everything is configured, so now kick off the initial tests for all the tests we have configured...
        for( Test test : tests ) {
            test.executeTest();
        }
    }


    private static class Group {

        private final List<Test>   tests;
        private final int          intervalSeconds;
        private final int          level;
        private final String       name;
        private final boolean      internalNetwork;

        private SystemAvailability availability;


        private Group( final int _groupNum, final JSONObject _groupConfig, final List<Test> _tests ) {

            // the basics...
            tests  = new ArrayList<>();
            availability = UNKNOWN;

            // get our attributes...
            intervalSeconds = _groupConfig.getInt(     "intervalSeconds" );
            level           = _groupConfig.getInt(     "level"           );
            name            = _groupConfig.getString(  "name"            );
            internalNetwork = _groupConfig.getBoolean( "internalNetwork" );

            // filter tests by our group number...
            for( Test test : _tests ) {
                if( _groupNum == test.group ) {
                    tests.add( test );
                }
            }
        }


        private void handleConnectivityChange() {

            // see if the entire group is available...
            SystemAvailability sa = UP;  // assume it's up until proven otherwise...
            for( Test test : tests ) {
                if( test.availability != UP )
                    sa = test.availability;
            }

            // if we've changed group availability, time to tell the world...
            if( sa != availability ) {
                LOGGER.finer( "Connectivity for " + name + " changed from " + availability + " to " + sa );

                // but if we're going from unknown to up, skip this because that's just a startup thing...
                if( !((availability == UNKNOWN) && (sa == UP)) ) {
                    availability = sa;
                    String msg = "Connectivity to " + name + " just went " + availability + ".";
                    String type = "connectivity." + availability.toString().toLowerCase();
                    String sub = "Connectivity to " + name + " just went " + availability + ".";
                    ISPMonitor.sendMOPEvent( "connectivity", type, sub, msg, level );
                }
            }
        }
    }


    /* package-private */  static class Test {

        /* package-private */ final String host;
        /* package-private */ final int    port;
        /* package-private */ final int    timeoutMS;
        /* package-private */ final int    group;
        /* package-private */ final String name;

        private SystemAvailability availability;
        private Group              groupInstance;


        private Test( final JSONObject _testConfig ) {

            availability = UNKNOWN;
            host         = _testConfig.getString( "host"      );
            port         = _testConfig.getInt(    "port"      );
            timeoutMS    = _testConfig.getInt(    "timeoutMS" );
            group        = _testConfig.getInt(    "group"     );
            name         = _testConfig.getString( "name"      );
        }


        private void executeTest() {
            ISPMonitor.executeTask( new ConnectivityTestTask( this::handleResult, host, port, timeoutMS, name ) );
        }


        private void handleResult( final ConnectivityTestResult _result ) {

            // handle a change in availability...
            if( _result.availability != availability ) {
                availability = _result.availability;
                groupInstance.handleConnectivityChange();
            }

            // schedule the next test...
            ISPMonitor.getTimer().schedule( new TimerTask() {
                @Override
                public void run() {
                    executeTest();
                }
            }, groupInstance.intervalSeconds * 1000 );
        }
    }
}
