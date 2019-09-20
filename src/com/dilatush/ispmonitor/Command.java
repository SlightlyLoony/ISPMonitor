package com.dilatush.ispmonitor;

import org.json.JSONObject;

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
    public Command( final JSONObject _config ) {

        name             = _config.getString( "name"             );
        command          = _config.getString( "command"          );
        expectedResponse = _config.getString( "expectedResponse" );
        timeoutMS        = _config.getLong(   "timeoutMS"        );
    }
}
