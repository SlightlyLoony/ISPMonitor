package com.dilatush.ispmonitor;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class Digger {

    private final ISPInfo primary;
    private final ISPInfo secondary;

    public ISPUsed ispInUse = ISPUsed.UNKNOWN;


    public Digger( final ISPInfo _primary, final ISPInfo _secondary ) {
        primary = _primary;
        secondary = _secondary;
    }
}
