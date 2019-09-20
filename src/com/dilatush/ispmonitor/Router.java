package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import com.dilatush.util.SSHExecutor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.ISPUsed.*;
import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.*;

/**
 * Provides methods for controlling a router via SSH.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Router {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final String                     hostname;
    private final String                     user;
    private final String                     identityFile;
    private final Map<String,SSHCommandInfo> commands;


    /**
     * Creates a new instance of {@link Router} that is configured via the specified configuration data.
     *
     * @param _config the configuration data
     */
    /* package-private */ Router( final Config _config ) {

        if( isNull( _config ) )
            throw new IllegalArgumentException( "Configuration not provided" );

        try {
            JSONObject routerConfig = _config.getJSONObject( "router" );
            hostname = routerConfig.getString( "hostname" );
            user = routerConfig.optString( "user", "" );
            identityFile = routerConfig.optString( "identityFile", "" );
            commands = new HashMap<>();
            JSONObject cmds = routerConfig.getJSONObject( "commands" );
            Iterator<String> keys = cmds.keys();
            while( keys.hasNext() ) {
                String key = keys.next();
                commands.put( key, new SSHCommandInfo( cmds.getJSONObject( key ) ) );
            }
        }
        catch( JSONException _je ) {
            throw new IllegalArgumentException( "Configuration malformed", _je );
        }
    }


    /**
     * Queries the router to get the current ISP that the router is using (as determined by the router's default route).  This command works by
     * querying the router via SSH; this job is queued and may not execute immediately.  Once the job completes, an {@link Event} of type
     * {@link EventType#RouterISP} is dispatched, with a payload of {@link ISPUsed}.  If any error occurs, then the payload equals
     * {@link ISPUsed#UNKNOWN}; otherwise it is {@link ISPUsed#PRIMARY} or {@link ISPUsed#SECONDARY} according to which ISP the router is using.
     */
    /* package-private */ void getCurrentISP() {
        createTask( "queryISP", new Event( EventType.RouterISP, UNKNOWN ), this::handleGetCurrentISP );
    }


    /**
     * Commands the router to set the current ISP that the router is using (as determined by the router's default route) to the primary ISP.  This
     * command works by commanding the router via SSH; this job is queued and may not execute immediately.  Once the job completes, an {@link Event}
     * of type {@link EventType#RouterSet} is dispatched, with a payload of {@link Boolean}, which is {@code true} if the operation was successful.
     * If any error occurs, then the payload equals {@code false}.
     */
    /* package-private */ void setPrimaryISP() {
        createTask( "setPrimaryISP", new Event( EventType.RouterSet, UNKNOWN ), this::handleSetPrimaryISP );
    }


    /**
     * Commands the router to set the current ISP that the router is using (as determined by the router's default route) to the secondary ISP.  This
     * command works by commanding the router via SSH; this job is queued and may not execute immediately.  Once the job completes, an {@link Event}
     * of type {@link EventType#RouterSet} is dispatched, with a payload of {@link Boolean}, which is {@code true} if the operation was successful.
     * If any error occurs, then the payload equals {@code false}.
     */
    /* package-private */ void setSecondaryISP() {
        createTask( "setSecondaryISP", new Event( EventType.RouterSet, UNKNOWN ), this::handleSetSecondaryISP );
    }


    /**
     * Creates and queues a task to execute the specified command on the configured router, using the configured user and identity.  The specified
     * error event is sent if there is any error in this process.  The specified task processor analyzes the result of the command, and sends the
     * appropriate event after doing so.
     *
     * @param _command the command to execute on the router (via SSH)
     * @param _errorEvent the event to send if there is any error in this process
     * @param _taskProcessor the task processor that will analyze the output of the command and send the appropriate event
     * @throws IllegalStateException if the given command is not configured
     */
    private void createTask( final String _command, final Event _errorEvent, final TaskProcessor _taskProcessor ) {

        ISPMonitor.executeTask( () -> {

            Event event = _errorEvent;
            SSHCommandInfo info = commands.get( _command );

            // if we got information about the command, we're good to go...
            if( isNotNull( info ) ) {

                try {

                    SSHExecutor executor = new SSHExecutor( hostname, info.command );

                    // if we had a configured user and/or identity file, add them to our executor...
                    if( isNonEmpty( user         ) ) executor.setUser( user );
                    if( isNonEmpty( identityFile ) ) executor.addIdentityFilePath( identityFile );

                    long start = System.currentTimeMillis();
                    LOGGER.finer( "SSHExecutor about to run \"" + executor + "\"" );
                    executor.start();

                    if( executor.waitFor( info.timeoutMS, TimeUnit.MILLISECONDS ) ) {

                        // if we get here, the job completed normally - gather info with the results...
                        event = _taskProcessor.process( executor );

                    }
                    LOGGER.finer( "SSH time: " + (System.currentTimeMillis() - start) + "ms" );
                }
                catch( IOException | InterruptedException _e ) {
                    // naught to do, as we'll just send the error event...
                }
            }

            // if the dummy tried to run a command that isn't configured...
            else
                throw new IllegalStateException( "Specified command is not configured: " + _command );

            // send an event with our results...
            ISPMonitor.postEvent( event );
        } );
    }


    private Event handleGetCurrentISP( final SSHExecutor _executor ) throws IOException {

        // router script returns PRIMARY, SECONDARY, or ERROR...
        ISPUsed routerState = UNKNOWN;
        String output = stripTrailingNewlines( _executor.getRemoteOutput() );
        if( "PRIMARY".equals( output ) )
            routerState = PRIMARY;
        else if( "SECONDARY".equals( output ) )
            routerState = SECONDARY;
        return new Event( EventType.RouterSet, routerState );
    }


    private Event handleSetPrimaryISP( final SSHExecutor _executor ) throws IOException {

        // router script returns SUCCESS or ERROR...
        String output = stripTrailingNewlines( _executor.getRemoteOutput() );
        return new Event( EventType.RouterSet, "SUCCESS".equals( output ) ? PRIMARY : UNKNOWN );
    }


    private Event handleSetSecondaryISP( final SSHExecutor _executor ) throws IOException {

        // router script returns SUCCESS or ERROR...
        String output = stripTrailingNewlines( _executor.getRemoteOutput() );
        return new Event( EventType.RouterSet, "SUCCESS".equals( output ) ? SECONDARY : UNKNOWN );
    }


    // functional interface like Consumer<SSHExecutor> with the addition of throwing an IOException...
    private interface TaskProcessor {
        Event process( final SSHExecutor _executor ) throws IOException;
    }
}
