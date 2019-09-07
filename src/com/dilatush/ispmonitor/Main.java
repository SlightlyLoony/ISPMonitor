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
        Config ispMonConfig = Config.fromJSONFile( config );
        long monitorIntervalSeconds = ispMonConfig.optLongDotted( "monitorInterval", 60 );
        long monitorInterval = 1000 * monitorIntervalSeconds;
        LOGGER.log( Level.INFO, "ISP Monitor is starting, publishing updates at " + monitorIntervalSeconds + " second intervals" );

        // get our ISP information...
        ISPInfo primary = new ISPInfo(
                ispMonConfig.getStringDotted( "primary.name"   ),
                ispMonConfig.getStringDotted( "primary.dns1"   ),
                ispMonConfig.getStringDotted( "primary.dns2"   ) );
        ISPInfo secondary = new ISPInfo(
                ispMonConfig.getStringDotted( "secondary.name" ),
                ispMonConfig.getStringDotted( "secondary.dns1" ),
                ispMonConfig.getStringDotted( "secondary.dns2" ) );

        // get the DNSDigger config...
        String[] domains       = ispMonConfig.getStringDotted( "DNSDigger.domains"          ).split( "," );
        long statistics_period = ispMonConfig.getLongDotted(   "DNSDigger.statisticsPeriod" );
        int tries              = ispMonConfig.getIntDotted(    "DNSDigger.tries"            );

        // test code...
        ISPTester isp2 = new ISPTester( secondary, domains, statistics_period, tries );
        for( int i = 0; i < 10; i++ ) {

            Thread.sleep( 2000 );
            boolean dug = isp2.test();
        }
        double availability = isp2.availability();

        // start up our post office...
        PostOffice po = new PostOffice( config );
        mailbox = po.createMailbox( "monitor" );

        // get the current setting of the default route in the router...
        String isp = new Executor( "ssh houserouter \"system script run get_isp\"" ).run();

        isp.hashCode();
    }
}
