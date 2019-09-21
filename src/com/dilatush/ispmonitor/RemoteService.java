package com.dilatush.ispmonitor;

import org.json.JSONObject;

import java.util.Map;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.SystemAvailability.*;

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

    private       SystemAvailability         state;     // the current state of this service...
    private       SystemAvailability         poState;   // the current state of the post office associated with this service (unknown if none)...


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
        commands = Command.getCommands( _config, "commands" );

        state = UNKNOWN;
        poState = UNKNOWN;
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


    /* package-private */ void updatePostOfficeAvailability( final SystemAvailability _poAvailability ) {

        // if our new state is different than the previous state...
        if( _poAvailability != poState ) {

            // update it...
            poState = _poAvailability;

            // let the host know...
            host.postOfficeStateChanged();
        }
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will stop that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#SSHResult} event is dispatched, with a payload of {@link SSHResult} that describes
     * the result.  The event handler calls {@link #handleStop(SSHResult)} to process the result.
     */
    /* package-private */ void stop() {
        ISPMonitor.executeTask( new SSHTask( this::handleStop, host.getHostname(), host.getUser(), host.getIdentityFile(), commands.get( "stop" ) ) );
    }


    private void handleStop( final SSHResult _result ) {
        analyzeSSHResult( _result, DOWN );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will start that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#SSHResult} event is dispatched, with a payload of {@link SSHResult} that describes
     * the result.  The event handler calls {@link #handleStart(SSHResult)} to process the result.
     */
    /* package-private */ void start() {
        ISPMonitor.executeTask( new SSHTask( this::handleStart, host.getHostname(), host.getUser(), host.getIdentityFile(), commands.get( "start" ) ) );
    }


    private void handleStart( final SSHResult _result ) {
        analyzeSSHResult( _result, UP );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will restart that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#SSHResult} event is dispatched, with a payload of {@link SSHResult} that describes
     * the result.  The event handler calls {@link #handleRestart(SSHResult)} to process the result.
     */
    /* package-private */ void restart() {
        ISPMonitor.executeTask( new SSHTask( this::handleRestart, host.getHostname(), host.getUser(), host.getIdentityFile(), commands.get( "restart" ) ) );
    }


    private void handleRestart( final SSHResult _result ) {
        analyzeSSHResult( _result, UP );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will check that service to see if it is active.  This job is
     * queued and may not execute immediately.  Upon completion, a {@link EventType#SSHResult} event is dispatched, with a payload of
     * {@link SSHResult} that describes the result.  The event handler calls {@link #handleCheck(SSHResult)} to process the result.
     */
    /* package-private */ void check() {
        ISPMonitor.executeTask( new SSHTask( this::handleCheck, host.getHostname(), host.getUser(), host.getIdentityFile(), commands.get( "check" ) ) );
    }


    private void handleCheck( final SSHResult _result ) {
        analyzeSSHResult( _result, UP );
    }


    private void analyzeSSHResult( final SSHResult _result, final SystemAvailability _expected ) {

        // if the command completed, then analyze the results...
        if( _result.type == SSHResultType.COMPLETED ) {

            // if we got the expected output, then our service has the expected availability...
            if( _result.command.expectedResponse.equals( _result.output ) )
                setState( _expected );

                // but if we got something unexpected, then our service has the unexpected availability...
            else
                setState( (_expected == UP) ? DOWN : UP );
        }

        // otherwise the command had a problem executing, and our state is unknown...
        else
            setState( UNKNOWN );
    }


    private void setState( final SystemAvailability _newState ) {

        // if the state isn't changing, then there's naught to do...
        if( _newState == state )
            return;

        state = _newState;
        host.serviceStateChanged();  // notify the host object that we've changed...
    }
}
