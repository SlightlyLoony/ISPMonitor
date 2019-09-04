package com.dilatush.ispmonitor;

import com.dilatush.mop.Mailbox;
import com.dilatush.mop.PostOffice;
import com.dilatush.util.Config;
import com.dilatush.util.Executor;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.util.General.isNotNull;

/**
 * Implements a monitor of our Internet connection.  Accepts the a single argument on the command line for the ISP monitor configuration file, whose
 * default name is "isp_monitor_config.json".  This program is normally run from a jar, via a systemd service.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());
    private static Mailbox mailbox;


    public static void main( String[] _args ) throws InterruptedException {

        // determine the configuration file...
        String config = "isp_monitor_config.json";   // the default...
        if( isNotNull( (Object) _args ) && (_args.length > 0) ) config = _args[0];
        if( !new File( config ).exists() ) {
            System.out.println( "ISP Monitor configuration file \"" + config + "\" does not exist!" );
            return;
        }

        // get our configuration file...
        Config ntpConfig = Config.fromJSONFile( config );
        long monitorIntervalSeconds = ntpConfig.optLongDotted( "monitorInterval", 60 );
        long monitorInterval = 1000 * monitorIntervalSeconds;
        LOGGER.log( Level.INFO, "ISP Monitor is starting, publishing updates at " + monitorIntervalSeconds + " second intervals" );

        // start up our post office...
        PostOffice po = new PostOffice( config );
        mailbox = po.createMailbox( "monitor" );

        // get the current setting of the default route in the router...
        String isp = new Executor( "ssh houserouter \"system script run get_isp\"" ).run();

        isp.hashCode();
    }
}
// xfinity
// 75.75.75.75
// 75.75.76.76

// verizon
// 198.224.164.135
// 198.224.160.135