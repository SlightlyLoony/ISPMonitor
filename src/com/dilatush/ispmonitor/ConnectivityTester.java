package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ConnectivityTester {

    private final List<Group> groups;
    private final List<Test>  tests;


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
        }
        catch( JSONException _e ) {
            throw new IllegalArgumentException( "Configuration malformed", _e );
        }
    }


    private static class Group {

        private final List<Test>  tests;
        private final int         intervalSeconds;
        private final int         level;


        private Group( final int _groupNum, final JSONObject _groupConfig, final List<Test> _tests ) {

            // the basics...
            tests  = new ArrayList<>();

            // get our attributes...
            intervalSeconds = _groupConfig.getInt( "intervalSeconds" );
            level           = _groupConfig.getInt( "level"           );

            // filter tests by our group number...
            for( Test test : _tests ) {
                if( _groupNum == test.group ) {
                    tests.add( test );
                }
            }
        }
    }


    private static class Test {

        private String host;
        private long   timeoutMS;
        private int    group;
        private String name;


        private Test( final JSONObject _testConfig ) {
            host      = _testConfig.getString( "host"      );
            timeoutMS = _testConfig.getLong(   "timeoutMS" );
            group     = _testConfig.getInt(    "group"     );
            name      = _testConfig.getString( "name"      );
        }
    }
}
