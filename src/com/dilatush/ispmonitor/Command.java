package com.dilatush.ispmonitor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple POJO to contain information about a command that may be executed the command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Command {

    public final String name;
    public final String command;
    public final String expectedResponse;
    public final long   timeoutMS;


    /**
     * Creates a new instance of {@link Command} according to the data in the specified configuration object.
     *
     * @param _config the configuration for this instance
     */
    /* package-private */ Command( final JSONObject _config ) {

        name             = _config.getString( "name"             );
        command          = _config.getString( "command"          );
        expectedResponse = _config.getString( "expectedResponse" );
        timeoutMS        = _config.getLong(   "timeoutMS"        );
    }


    /**
     * Returns a map of {@link Command} instances, keyed by their name, as configured under the specified commands tag (as an array) in the specified
     * configuration.
     *
     * @param _config the configuration containing a commands array
     * @param _commandsTag the tag of the command array
     * @return a map of {@link Command} instances, keyed by their name
     */
    /* package-private */ static Map<String,Command> getCommands( final JSONObject _config, final String _commandsTag ) {
        Map<String,Command> commands = new HashMap<>();
        if( _config.has( _commandsTag ) ) {
            JSONArray commandArray = _config.getJSONArray( _commandsTag );
            for( int i = 0; i < commandArray.length(); i++ ) {
                JSONObject commandObj = commandArray.getJSONObject( i );
                commands.put( commandObj.getString( "name" ), new Command( commandObj ) );
            }
        }
        return commands;
    }
}
