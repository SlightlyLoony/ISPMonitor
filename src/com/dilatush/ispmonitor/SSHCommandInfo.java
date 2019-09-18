package com.dilatush.ispmonitor;

import org.json.JSONObject;

/**
 * A simple POJO that contains information about SSH commands.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SSHCommandInfo {

    /* package-private */ final String command;
    /* package-private */ final long   timeoutMS;


    /**
     * Creates a new instance of {@link SSHCommandInfo} with the specified command and timeout.
     *
     * @param _command an SSH command string (like "~/scripts/start-service xxx"
     * @param _timeoutMS how long to wait for this command to complete, in milliseconds
     */
    public SSHCommandInfo( final String _command, final long _timeoutMS ) {
        command   = _command;
        timeoutMS = _timeoutMS;
    }


    /**
     * Creates a new instance of {@link SSHCommandInfo} from the command and timeout specified in the given {@link JSONObject}.
     *
     * @param _config a JSONObject containing the command and timeout for this instance
     * @throws org.json.JSONException on any problem reading the JSON configuration
     */
    /* package-private */ SSHCommandInfo( final JSONObject _config ) {
        command   = _config.getString( "command"   );
        timeoutMS = _config.getLong(   "timeoutMS" );
    }
}
