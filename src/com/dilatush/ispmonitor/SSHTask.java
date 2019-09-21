package com.dilatush.ispmonitor;

import com.dilatush.util.SSHExecutor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.SSHResultType.*;
import static com.dilatush.util.General.isNotNull;
import static com.dilatush.util.Strings.stripTrailingNewlines;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SSHTask implements Task {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private final SSHResultHandler handler;
    private final RemoteHost       host;
    private final Command          command;


    public SSHTask( final SSHResultHandler _handler, final RemoteHost _host, final Command _command ) {
        handler = _handler;
        host = _host;
        command = _command;
    }


    @Override
    public void run() {

        SSHResultType resultType;
        int           exitCode = -1;
        String        output = null;

        try {
            SSHExecutor executor = new SSHExecutor( host.getHostname(), command.command );
            if( isNotNull( host.getUser() ) )
                executor.setUser( host.getUser() );
            if( isNotNull( host.getIdentityFile() ) )
                executor.addIdentityFilePath( host.getIdentityFile() );
            long start = System.currentTimeMillis();
            LOGGER.finer( "SSHExecutor about to run \"" + executor + "\"" );
            executor.start();
            if( executor.waitFor( command.timeoutMS, TimeUnit.MILLISECONDS ) ) {

                // if we get here, the job completed normally...
                output = stripTrailingNewlines( executor.getRemoteOutput() );
                exitCode = executor.getExitCode();
                LOGGER.finer( "Exit code: " + exitCode + "; output: " + output );
                resultType = COMPLETED;
            }
            else
                resultType = TIMEOUT;
            LOGGER.finer( "SSH time: " + (System.currentTimeMillis() - start) + "ms" );
        }
        catch( IOException | InterruptedException _e ) {
            resultType = ERROR;
        }

        // send an event with our results...
        Event event = new Event( EventType.SSHResult, new SSHResult( handler, command, resultType, exitCode, output ) );
        ISPMonitor.postEvent( event );
    }
}
