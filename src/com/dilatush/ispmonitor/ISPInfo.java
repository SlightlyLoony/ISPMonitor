package com.dilatush.ispmonitor;

/**
 * A simple POJO to hold information about an Internet Service Provider (ISP).  Instances of this class are immutable and threadsafe.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class ISPInfo {

    public final String name;
    public final String dns1;
    public final String dns2;


    public ISPInfo( final String _name, final String _dns1, final String _dns2 ) {
        name = _name;
        dns1 = _dns1;
        dns2 = _dns2;
    }
}
