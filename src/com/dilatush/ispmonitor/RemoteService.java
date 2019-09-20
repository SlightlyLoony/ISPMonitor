package com.dilatush.ispmonitor;

import com.dilatush.util.SSHExecutor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.RemoteServiceAction.*;
import static com.dilatush.ispmonitor.RemoteServiceActionResult.*;
import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.stripTrailingNewlines;

/**
 * Contains information about a systemd service running on a remote host, as configured in the configuration file.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class RemoteService {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final RemoteHost                 host;
    private final String                     name;      // the systemd name of this service...
    private final String                     po;        // the MOP post office used by this service, or null if it doesn't use a post office at all...
    private final Map<String, Command>       commands;  // key is the command's name...


    /**
     * Creates a new instance of {@link RemoteService} using the specified configuration.  Note that the configuration is specified in a possibly
     * counterintuitive format: the key is the MOP post office name used by the service, <i>not</i> the systemd service name.
     *
     * @param _host the host for this service
     * @param _config the configuration specifying the remote services
     */
    /* package-private */ RemoteService( final RemoteHost _host, final JSONObject _config ) {

        // the basics...
        host = _host;
        name = _config.getString( "name" );

        // if this service has an MOP post office, get it...
        po = _config.has( "postOffice" ) ? _config.getString( "postOffice" ) : null;

        // get any commands we might have...
        commands = new HashMap<>();
        if( _config.has( "commands" ) ) {
            JSONArray commandArray = _config.getJSONArray( "commands" );
            for( int i = 0; i < commandArray.length(); i++ ) {
                JSONObject commandObj = commandArray.getJSONObject( i );
                commands.put( commandObj.getString( "name" ), new Command( commandObj ) );
            }
        }
    }


    /**
     * Return the MOP post office used by this service, or {@code null} if none.
     *
     * @return the MOP post office used by this service, or {@code null} if none
     */
    /* package-private */ String getPostOffice() {
        return po;
    }


    /* package-private */ RemoteHost getHost() {
        return host;
    }


    /* package-private */ String getName() {
        return name;
    }


    /* package-private */ String getPo() {
        return po;
    }


    /* package-private */ Map<String, Command> getCommands() {
        return commands;
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will stop that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#RemoteServiceRaw} is dispatched, with a payload of {@link ServiceActionInfo} that
     * describes the result.
     */
    /* package-private */ void stop() {
        createTask( commands.get( "stop" ), STOP );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will start that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#RemoteServiceRaw} is dispatched, with a payload of {@link ServiceActionInfo} that
     * describes the result.
     */
    /* package-private */ void start() {
        createTask( commands.get( "start" ), START );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will restart that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#RemoteServiceRaw} is dispatched, with a payload of {@link ServiceActionInfo} that
     * describes the result.
     */
    /* package-private */ void restart() {
        createTask( commands.get( "restart" ), RESTART );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will check that service to see if it is active.  This job is
     * queued and may not execute immediately.  Upon completion, a {@link EventType#RemoteServiceRaw} is dispatched, with a payload of
     * {@link ServiceActionInfo} that describes the result.
     */
    /* package-private */ void check() {
        createTask( commands.get( "check" ), CHECK );
    }


    /**
     * Creates and queues a task to run a command on a remote server via SSH.  The command and the event it generates are controlled by the specified
     * service information, command string, service action, and expected result.
     *
     * @param _command the command string
     * @param _action the action being taken
     */
    private void createTask( final Command _command, final RemoteServiceAction _action ) {

        if( isNull( _command ) )
            throw new IllegalArgumentException( "Command not specified" );

        ISPMonitor.executeTask( () -> {

            RemoteServiceActionResult result;

            try {
                SSHExecutor executor = new SSHExecutor( host.getHostname(), _command.command );
                if( isNotNull( host.getUser() ) )
                    executor.setUser( host.getUser() );
                if( isNotNull( host.getIdentityFile() ) )
                    executor.addIdentityFilePath( host.getIdentityFile() );
                long start = System.currentTimeMillis();
                LOGGER.finer( "SSHExecutor about to run \"" + executor + "\"" );
                executor.start();
                if( executor.waitFor( _command.timeoutMS, TimeUnit.MILLISECONDS ) ) {

                    // if we get here, the job completed normally - gather info with the results...
                    String output = stripTrailingNewlines( executor.getRemoteOutput() );
                    LOGGER.finer( "Output: " + output );
                    boolean success = _command.expectedResponse.equals( output );
                    result = success ? SUCCESS : FAILURE;
                }
                else
                    result = TIMEOUT;
                LOGGER.finer( "SSH time: " + (System.currentTimeMillis() - start) + "ms" );
            }
            catch( IOException | InterruptedException _e ) {
                result = ERROR;
            }

            // send an event with our results...
            Event event = new Event( EventType.RemoteServiceRaw,
                                     new ServiceActionInfo( host.getHostname(), name, _action, result ) );
            ISPMonitor.postEvent( event );
        } );
    }
}
