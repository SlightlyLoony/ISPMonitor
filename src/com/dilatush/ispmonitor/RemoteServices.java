package com.dilatush.ispmonitor;

import com.dilatush.util.Config;
import com.dilatush.util.SSHExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.RemoteServices.ServiceAction.*;
import static com.dilatush.ispmonitor.RemoteServices.ServiceActionResult.*;
import static com.dilatush.ispmonitor.RemoteServices.ServiceActionResult.FAILURE;
import static com.dilatush.util.General.isNull;
import static com.dilatush.util.Strings.stripTrailingNewlines;

/**
 * Provides methods for controlling services running on remote hosts using the specified configuration.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class RemoteServices {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private static final long   TIMEOUT_MS = 3000;

    private final Map<String, ServiceInfo> services;


    /**
     * Creates a new instance of {@link RemoteServices} using the specified configuration.  Note that the configuration is specified in a possibly
     * counterintuitive format: the key is the MOP post office name used by the service, <i>not</i> the systemd service name.
     *
     * @param _config the configuration specifying the remote services
     */
    public RemoteServices( final Config _config ) {
        services = new HashMap<>();
        Set<String> keys = _config.getJSONObject( "monitoredPostOffices" ).keySet();
        for( String key : keys ) {
            services.put( key, new ServiceInfo(
                 key,
                _config.getStringDotted( "monitoredPostOffices." + key + ".serviceName" ),
                _config.getStringDotted( "monitoredPostOffices." + key + ".stop"        ),
                _config.getStringDotted( "monitoredPostOffices." + key + ".start"       ),
                _config.getStringDotted( "monitoredPostOffices." + key + ".restart"     ),
                _config.getStringDotted( "monitoredPostOffices." + key + ".hostname"    )
            ) );
        }
    }


    public void stop( final String _poName ) {
        ISPMonitor.executeTask( new Task() {

            @Override
            public void run() {

                ServiceActionResult result;
                ServiceInfo info = services.get( _poName );
                if( isNull( info ) )
                    throw new IllegalArgumentException( "Unknown post office name: " + _poName );

                try {
                    SSHExecutor executor = new SSHExecutor( info.hostname, info.stop );
                    long start = System.currentTimeMillis();
                    executor.start();
                    if( executor.waitFor( TIMEOUT_MS, TimeUnit.MILLISECONDS ) ) {

                        // if we get here, the job completed normally - gather info with the results...
                        boolean success = "inactive".equals( stripTrailingNewlines( executor.getRemoteOutput() ) );
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
                Event event = new Event( EventType.RemoteService, new ServiceActionInfo( info.serviceName, STOP, result ) );
                LOGGER.finer( "RemoteServices.stop " + event );
                ISPMonitor.postEvent( event );
            }
        } );
    }


    public static class ServiceActionInfo {
        public final String              serviceName;
        public final ServiceAction       action;
        public final ServiceActionResult result;


        public ServiceActionInfo( final String _serviceName, final ServiceAction _action, final ServiceActionResult _result ) {
            serviceName = _serviceName;
            action = _action;
            result = _result;
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
        private final String postOfficeName;
        private final String serviceName;
        private final String stop;
        private final String start;
        private final String restart;
        private final String hostname;


        /**
         * Creates a new instance of this class with the specified information.
         *
         * @param _postOfficeName the MOP post office name for the service
         * @param _serviceName the systemd service name for the service
         * @param _stop command to stop the service
         * @param _start command to start the service
         * @param _restart command to restart the service
         * @param _hostname hostname of the server hosting the service
         */
        private ServiceInfo( final String _postOfficeName, final String _serviceName, final String _stop,
                             final String _start, final String _restart, final String _hostname ) {
            postOfficeName = _postOfficeName;
            serviceName = _serviceName;
            stop = _stop;
            start = _start;
            restart = _restart;
            hostname = _hostname;
        }
    }
}
