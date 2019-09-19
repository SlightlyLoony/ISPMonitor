package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import com.dilatush.util.SSHExecutor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.stripTrailingNewlines;

/**
 * Provides methods for controlling services running on remote hosts using the specified configuration.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
/* package-private */ class RemoteServices {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final Map<String, RemoteHostInfo> hostsByName;     // key is "<systemd service name>"...
    private final Map<String, ServiceInfo>    servicesByPO;    // key is "<MOP post office name>"...
    private final Map<String, ServiceInfo>    servicesByName;  // key is "<host>:<service"...


    /**
     * Creates a new instance of {@link RemoteServices} using the specified configuration.  Note that the configuration is specified in a possibly
     * counterintuitive format: the key is the MOP post office name used by the service, <i>not</i> the systemd service name.
     *
     * @param _config the configuration specifying the remote services
     */
    /* package-private */ RemoteServices( final Config _config ) {
        try {

            // get our hosts...
            hostsByName = new HashMap<>();
            JSONArray hosts = _config.getJSONArray( "remoteHosts" );
            for( int i = 0; i < hosts.length(); i++ ) {
                JSONObject host = hosts.getJSONObject( i );
                RemoteHostInfo hostInfo = new RemoteHostInfo(
                        host.getString( "hostname" ),
                        host.has( "user" ) ? host.getString( "user" ) : null,
                        host.has( "identityFile" ) ? host.getString( "identityFile" ) : null,
                        new CommandInfo( host.getJSONObject( "stopAll" ) ),
                        new CommandInfo( host.getJSONObject( "startAll" ) ),
                        new CommandInfo( host.getJSONObject( "restartAll" ) )
                );
                hostsByName.put( hostInfo.hostname, hostInfo );
            }

            // get our services...
            servicesByName = new HashMap<>();
            servicesByPO   = new HashMap<>();
            JSONArray services = _config.getJSONArray( "services" );
            for( int i = 0; i < services.length(); i++ ) {
                JSONObject service = services.getJSONObject( i );
                ServiceInfo serviceInfo = new ServiceInfo(
                        service.getString( "postOffice" ),
                        service.getString( "name" ),
                        hostsByName.get( service.getString( "hostname" ) ),
                        new CommandInfo( service.getJSONObject( "stop" ) ),
                        new CommandInfo( service.getJSONObject( "start" ) ),
                        new CommandInfo( service.getJSONObject( "restart" ) )
                );
                servicesByName.put( serviceInfo.host.hostname + ":" + serviceInfo.serviceName, serviceInfo );
                servicesByPO.put( serviceInfo.postOfficeName, serviceInfo );
            }
        }
        catch( JSONException _je ) {
            throw new IllegalArgumentException( "Configuration malformed", _je );
        }
    }


    /**
     * Retrieve service information for the service that uses the specified MOP post office.
     *
     * @param _poName the MOP post office name
     * @return the service information for the service that uses the specified MOP post office
     */
    /* package-private */ ServiceInfo byPostOffice( final String _poName ) {
        return servicesByPO.get( _poName );
    }


    /**
     * Retrieve service information for the service running on the specified hostname and systemd service name.
     *
     * @param _hostName the hostname of the server the service is running on
     * @param _serviceName the name of the systemd service
     * @return the service information for the service running on the specified hostname and systemd service name
     */
    /* package-private */ ServiceInfo byHostAndServiceName( final String _hostName, final String _serviceName ) {
        return servicesByName.get( _hostName + ":" + _serviceName );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will stop that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#RemoteService} is dispatched, with a payload of {@link ServiceActionInfo} that
     * describes the result.
     *
     * @param _serviceInfo information about the service to be stopped
     */
    /* package-private */ void stop( final ServiceInfo _serviceInfo ) {
        createTask( _serviceInfo, _serviceInfo.stop, ServiceAction.STOP, "inactive" );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will start that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#RemoteService} is dispatched, with a payload of {@link ServiceActionInfo} that
     * describes the result.
     *
     * @param _serviceInfo information about the service to be started
     */
    /* package-private */ void start( final ServiceInfo _serviceInfo ) {
        createTask( _serviceInfo, _serviceInfo.start, ServiceAction.START, "active" );
    }


    /**
     * Runs a command on the server (via SSH) hosting the specified systemd service that will restart that service.  This job is queued and may not
     * execute immediately.  Upon completion, a {@link EventType#RemoteService} is dispatched, with a payload of {@link ServiceActionInfo} that
     * describes the result.
     *
     * @param _serviceInfo information about the service to be restarted
     */
    /* package-private */ void restart( final ServiceInfo _serviceInfo ) {
        createTask( _serviceInfo, _serviceInfo.restart, ServiceAction.RESTART, "active" );
    }

    // TODO: add methods to start, stop, and restart ALL configured services on a remote server...


    /**
     * Creates and queues a task to run a command on a remote server via SSH.  The command and the event it generates are controlled by the specified
     * service information, command string, service action, and expected result.
     *
     * @param _serviceInfo information about the service the command is concerning
     * @param _command the command string
     * @param _action the action being taken
     * @param _expected the expected return value
     */
    private void createTask( final ServiceInfo _serviceInfo, final CommandInfo _command, final ServiceAction _action, final String _expected ) {

        if( isNull( _serviceInfo ) )
            throw new IllegalArgumentException( "Service information not specified" );

        ISPMonitor.executeTask( () -> {

            ServiceActionResult result;

            try {
                SSHExecutor executor = new SSHExecutor( _serviceInfo.host.hostname, _command.command );
                if( isNotNull( _serviceInfo.host.user ) )
                    executor.setUser( _serviceInfo.host.user );
                if( isNotNull( _serviceInfo.host.identityFile ) )
                    executor.addIdentityFilePath( _serviceInfo.host.identityFile );
                long start = System.currentTimeMillis();
                executor.start();
                if( executor.waitFor( _command.timeoutMS, TimeUnit.MILLISECONDS ) ) {

                    // if we get here, the job completed normally - gather info with the results...
                    String output = stripTrailingNewlines( executor.getRemoteOutput() );
                    LOGGER.finer( "Output: " + output );
                    boolean success = _expected.equals( output );
                    result = success ? ServiceActionResult.SUCCESS : ServiceActionResult.FAILURE;
                }
                else
                    result = ServiceActionResult.TIMEOUT;
                LOGGER.finer( "SSH time: " + (System.currentTimeMillis() - start) + "ms" );
            }
            catch( IOException | InterruptedException _e ) {
                result = ServiceActionResult.ERROR;
            }

            // send an event with our results...
            Event event = new Event( EventType.RemoteService,
                                     new ServiceActionInfo( _serviceInfo.host.hostname, _serviceInfo.serviceName, _action, result ) );
            ISPMonitor.postEvent( event );
        } );
    }


    /* package-private */ static class ServiceActionInfo {
        /* package-private */ final String              hostName;
        /* package-private */ final String              serviceName;
        /* package-private */ final ServiceAction       action;
        /* package-private */ final ServiceActionResult result;


        private ServiceActionInfo( final String _hostName, final String _serviceName,
                                  final ServiceAction _action, final ServiceActionResult _result ) {
            hostName    = _hostName;
            serviceName = _serviceName;
            action      = _action;
            result      = _result;
        }

        public String toString() {
            return serviceName + " " + action + ": " + result;
        }
    }


    public enum ServiceActionResult { SUCCESS, FAILURE, TIMEOUT, ERROR }
    public enum ServiceAction { STOP, START, RESTART }


    /**
     * Simple POJO to hold information about a service.
     */
    private static class ServiceInfo {
        private final String         postOfficeName;
        private final String         serviceName;
        private final RemoteHostInfo host;
        private final CommandInfo    stop;
        private final CommandInfo    start;
        private final CommandInfo    restart;


        private ServiceInfo( final String _postOfficeName, final String _serviceName, final RemoteHostInfo _host,
                            final CommandInfo _stop, final CommandInfo _start, final CommandInfo _restart ) {
            postOfficeName = _postOfficeName;
            serviceName = _serviceName;
            host = _host;
            stop = _stop;
            start = _start;
            restart = _restart;
        }
    }


    private static class RemoteHostInfo {
        private final String      hostname;
        private final String      user;
        private final String      identityFile;
        private final CommandInfo startAll;
        private final CommandInfo stopAll;
        private final CommandInfo restartAll;


        private RemoteHostInfo( final String _hostname, final String _user, final String _identityFile,
                               final CommandInfo _startAll, final CommandInfo _stopAll, final CommandInfo _restartAll ) {
            hostname = _hostname;
            user = _user;
            identityFile = _identityFile;
            startAll = _startAll;
            stopAll = _stopAll;
            restartAll = _restartAll;
        }
    }


    private static class CommandInfo {
        private final String command;
        private final long   timeoutMS;


        private CommandInfo( final JSONObject _command ) {
            command   = _command.getString( "command" );
            timeoutMS = _command.getLong( "timeoutMS" );
        }


        private CommandInfo( final String _command, final long _timeoutMS ) {
            command   = _command;
            timeoutMS = _timeoutMS;
        }
    }
}
