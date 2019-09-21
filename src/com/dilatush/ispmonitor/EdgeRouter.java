package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.EventType.ISPChangeNeeded;
import static com.dilatush.ispmonitor.ISPChoice.*;
import static com.dilatush.ispmonitor.SSHResultType.COMPLETED;
import static com.dilatush.ispmonitor.SystemAvailability.DOWN;
import static com.dilatush.ispmonitor.SystemAvailability.UP;
import static com.dilatush.util.General.isNull;

/**
 * Provides methods for controlling a router via SSH.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class EdgeRouter {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    /* package-private */ final String       hostname;
    /* package-private */ final String       user;
    /* package-private */ final String       identityFile;
    /* package-private */ final int          minDNSTestIntervalSeconds;
    /* package-private */ final int          maxDNSTestIntervalSeconds;
    /* package-private */ final int          maxDNSTestTries;
    /* package-private */ final String[]     testDomains;
    /* package-private */ final Random       random;
    private final Map<String,Command>        commands;
    private final ISP                        primaryISP;
    private final ISP                        secondaryISP;

    private ISPChoice          ispInUse;
    private ISPChoice          ispShouldUse;
    private SystemAvailability availability;


    /**
     * Creates a new instance of {@link EdgeRouter} that is configured via the specified configuration data.
     *
     * @param _config the configuration data
     */
    /* package-private */ EdgeRouter( final Config _config ) {

        if( isNull( _config ) )
            throw new IllegalArgumentException( "Configuration not provided" );

        try {

            JSONObject routerConfig = _config.getJSONObject( "edgeRouter" );

            // the basics...
            hostname                  = routerConfig.getString( "hostname"                  );
            user                      = routerConfig.optString( "user", null                );
            identityFile              = routerConfig.optString( "identityFile", null        );
            testDomains               = routerConfig.getString( "testDomains"               ).split( "," );
            minDNSTestIntervalSeconds = routerConfig.getInt(    "minDNSTestIntervalSeconds" );
            maxDNSTestIntervalSeconds = routerConfig.getInt(    "maxDNSTestIntervalSeconds" );
            maxDNSTestTries           = routerConfig.getInt(    "maxDNSTestTries"           );

            // get any commands we might have...
            commands = Command.getCommands( routerConfig, "commands" );

            // get our ISP records...
            primaryISP   = new ISP( this, routerConfig.getJSONObject( "primaryISP"   ) );
            secondaryISP = new ISP( this, routerConfig.getJSONObject( "secondaryISP" ) );

            // set up our initial state...
            random       = new Random( System.currentTimeMillis() + hostname.hashCode() );
            ispInUse = UNKNOWN;
            availability = SystemAvailability.UNKNOWN;
        }
        catch( JSONException _je ) {
            throw new IllegalArgumentException( "Configuration malformed", _je );
        }
    }


    /* package-private */ void heartbeat() {

        // iterate over all the DNS instances, checking to see if it's time to test them again...
        for( DNS dns : primaryISP.dnss )   { dns.heartbeat(); }
        for( DNS dns : secondaryISP.dnss ) { dns.heartbeat(); }
    }


    /* package-private */ void ispAvailabilityChanged() {

        // figure out which ISP we should be using...
        if( primaryISP.getAvailability() == UP )
            ispShouldUse = PRIMARY;
        else if( secondaryISP.getAvailability() == UP )
            ispShouldUse = SECONDARY;
        else
            ispShouldUse = NONE;

        // if what we ARE using and what we SHOULD BE using are different, send an event to notify...
        if( ispInUse != ispShouldUse )
            ISPMonitor.postEvent( new Event( ISPChangeNeeded, this ) );
    }


    /**
     * Queries the router to get the current ISP that the router is using (as determined by the router's default route).  This command works by
     * querying the router via SSH; this job is queued and may not execute immediately.  Once the job completes, an {@link Event} of type
     * {@link EventType#SSHResult} is dispatched, with a payload of {@link SSHResult} that describes the result.  The event handler calls
     * {@link #handleGetCurrentISP(SSHResult)} to process the result.
     */
    /* package-private */ void getCurrentISP() {
        ISPMonitor.executeTask( new SSHTask( this::handleGetCurrentISP, hostname, user, identityFile, commands.get( "queryISP" ) ) );
    }


    private void handleGetCurrentISP( final SSHResult _sshResult ) {

        // if the SSH task completed, then we process the result...
        if( _sshResult.type == COMPLETED ) {

            // first, we know the router is up...
            availability = UP;

            // figure out our state by the output from the router script: PRIMARY, SECONDARY, or ERROR...
            if( "PRIMARY".equals( _sshResult.output ) )
                ispInUse = PRIMARY;
            else if( "SECONDARY".equals( _sshResult.output ) )
                ispInUse = SECONDARY;

            // if we get here, then something bad happened and we don't know which ISP the router is using - but the router itself is up...
            else
                ispInUse = UNKNOWN;
        }

        // otherwise we got an error or timeout, and we have no idea what ISP the router is using - and we assume the router is down...
        else {
            ispInUse = UNKNOWN;
            availability = DOWN;
        }
    }


    /**
     * Commands the router to set the current ISP that the router is using (as determined by the router's default route) to the primary ISP.  This
     * command works by commanding the router via SSH; this job is queued and may not execute immediately.  Once the job completes, an {@link Event}
     * of type {@link EventType#SSHResult} is dispatched, with a payload of {@link SSHResult} that describes the result.  The event handler calls
     * {@link #handleSetPrimaryISP(SSHResult)} to process the result.
     */
    /* package-private */ void setPrimaryISP() {
        ISPMonitor.executeTask( new SSHTask( this::handleSetPrimaryISP, hostname, user, identityFile, commands.get( "setPrimaryISP" ) ) );
    }


    private void handleSetPrimaryISP( final SSHResult _sshResult ) {

        // if the SSH task completed, then we process the result...
        if( _sshResult.type == COMPLETED ) {

            // first, we know the router is up...
            availability = UP;

            // figure out our state by the output from the router script: PRIMARY, SECONDARY, or ERROR...
            if( "SUCCESS".equals( _sshResult.output ) )
                ispInUse = PRIMARY;

            // if we get here, then something bad happened and we don't know which ISP the router is using - but the router itself is up...
            else
                ispInUse = UNKNOWN;
        }

        // otherwise we got an error or timeout, and we have no idea what ISP the router is using - and we assume the router is down...
        else {
            ispInUse = UNKNOWN;
            availability = DOWN;
        }
    }


    /**
     * Commands the router to set the current ISP that the router is using (as determined by the router's default route) to the secondary ISP.  This
     * command works by commanding the router via SSH; this job is queued and may not execute immediately.  Once the job completes, an {@link Event}
     * of type {@link EventType#SSHResult} is dispatched, with a payload of {@link SSHResult} that describes the result.  The event handler calls
     * {@link #handleSetSecondaryISP(SSHResult)} to process the result.
     */
    /* package-private */ void setSecondaryISP() {
        ISPMonitor.executeTask( new SSHTask( this::handleSetPrimaryISP, hostname, user, identityFile, commands.get( "setSecondaryISP" ) ) );
    }


    private void handleSetSecondaryISP( final SSHResult _sshResult ) throws IOException {

        // if the SSH task completed, then we process the result...
        if( _sshResult.type == COMPLETED ) {

            // first, we know the router is up...
            availability = UP;

            // figure out our state by the output from the router script: PRIMARY, SECONDARY, or ERROR...
            if( "SUCCESS".equals( _sshResult.output ) )
                ispInUse = SECONDARY;

                // if we get here, then something bad happened and we don't know which ISP the router is using - but the router itself is up...
            else
                ispInUse = UNKNOWN;
        }

        // otherwise we got an error or timeout, and we have no idea what ISP the router is using - and we assume the router is down...
        else {
            ispInUse = UNKNOWN;
            availability = DOWN;
        }
    }


    public ISPChoice getIspInUse() {
        return ispInUse;
    }


    public ISPChoice getIspShouldUse() {
        return ispShouldUse;
    }


    public SystemAvailability getAvailability() {
        return availability;
    }
}
