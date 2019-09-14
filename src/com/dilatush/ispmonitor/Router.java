package com.dilatush.ispmonitor;

import com.dilatush.util.Executor;

/**
 * Provides methods for controlling a MikroTik router at the specified hostname, via SSH.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Router {

    private final String host;


    public Router( final String _host ) {
        host = _host;
    }


    public ISPUsed getCurrentISP() {

        String sshStr = "ssh " + host + " \"system script run get_isp\"";
        Executor executor = new Executor( sshStr );
        String result = strip( executor.run() );
        if( "PRIMARY".equals( result ) )
            return ISPUsed.PRIMARY;
        else if( "SECONDARY".equals( result ) )
            return ISPUsed.SECONDARY;
        else
            return ISPUsed.UNKNOWN;
    }


    public boolean setPrimaryISP() {

        String sshStr = "ssh " + host + " \"system script run set_primary\"";
        Executor executor = new Executor( sshStr );
        String result = strip( executor.run() );
        return "SUCCESS".equals( result );
    }


    public boolean setSecondaryISP() {

        String sshStr = "ssh " + host + " \"system script run set_secondary\"";
        Executor executor = new Executor( sshStr );
        String result = strip( executor.run() );
        return "SUCCESS".equals( result );
    }


    private String strip( final String _str ) {
        if( '\n' == _str.charAt( _str.length() - 1 ) )
            return _str.substring( 0, _str.length() - 1 );
        return _str;
    }
}
