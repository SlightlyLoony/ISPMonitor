package com.dilatush.ispmonitor;

import com.dilatush.util.BitBuffer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dilatush.ispmonitor.DNSResultType.*;

/**
 * Queries a DNS server to test its availability by seeing if it can resolve a domain name.  In other words, queries for A records.  Note that this
 * class makes no attempt to decode the response; it merely tests to see if the DNS server responds at all.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class DNSTestQueryTask implements Task {

    private static final Logger LOGGER = Logger.getLogger( new Object(){}.getClass().getEnclosingClass().getCanonicalName());

    private static final int DNS_PORT = 53;
    private static final int DNS_ANSWER_MAX_LENGTH = 1500;

    private final DNSResultHandler handler;   // handler for the result of this query...
    private final String           dnsServer; // dotted-form IP address of the DNS server to query...
    private final String           domain;    // domain name to query...
    private final int              tries;     // the number of times a query should be tried before concluding a DNS server is unavailable...
    private final int              maxMs;     // how many milliseconds to wait for a response...


    /**
     * Creates a new {@link DNSTestQueryTask} instance, ready to query the specified DNS server for IP address corresponding to the specified domain name.
     * The query will be attempted no more than the specified number of tries, waiting no more than the specified maximum amount of time.
     *
     * @param _handler the handler for the result of this query
     * @param _dnsServer  the host name of the DNS server, which should be a dotted-form IP address (like 8.8.8.8)
     * @param _domain  the domain to query for (like "google.com")
     * @param _tries  the number of tries to make
     * @param _maxMs  the maximum number of milliseconds to wait for a response
     */
    /* package-private */ DNSTestQueryTask( final DNSResultHandler _handler, final String _dnsServer,
                                            final String _domain, final int _tries, final int _maxMs ) {
        handler   = _handler;
        dnsServer = _dnsServer;
        domain    = _domain;
        tries     = _tries;
        maxMs     = _maxMs;
    }


    /**
     * Execute the query configured in this instance.  The query will be tried at most the number of tries specified in this instance.
     */
    public void run() {

        // set up our default results...
        DNSResultType type          = ERROR;
        int           actualTries   = 0;
        long          actualTime    = 0;

        // keep a counter for our identifier...
        int id = 1;

        // our timeout value, which we'll double for each attempt...
        int timeout = maxMs / ((1 << tries) - 1);

        // store our starting time, so we can figure out how long this took...
        long start = System.currentTimeMillis();

        // make our query datagram (see RFC 1035, sections 4.1.1 and 4.1.2)...
        BitBuffer bb = new BitBuffer( 2000 );

        // first the header...
        bb.put( (short) id );          // ID field...
        bb.put( 0, 1 );                // QR field...
        bb.put( 0, 4 );                // OPCODE field...
        bb.put( 0, 1 );                // AA field...
        bb.put( 0, 1 );                // TC field...
        bb.put( 1, 1 );                // RD field...
        bb.put( 0, 1 );                // RA field...
        bb.put( 0, 3 );                // Z field...
        bb.put( 0, 4 );                // RCODE field...
        bb.put( (short) 1 );           // QDCOUNT field...
        bb.put( (short) 0 );           // ANCOUNT field...
        bb.put( (short) 0 );           // NSCOUNT field...
        bb.put( (short) 0 );           // ARCOUNT field...

        // then the question section...
        String[] parts = domain.split( "\\." );
        for( String part : parts ) {
            byte[] labelBytes = part.getBytes( StandardCharsets.US_ASCII );
            bb.put( (byte) labelBytes.length );       // the label length...
            for( byte labelByte : labelBytes ) {
                bb.put( labelByte );                  // a label character...
            }
        }
        bb.put( (byte) 0 );                          // the label terminator...
        bb.put( (short) 1 );                          // QTYPE field (A record)...
        bb.put( (short) 1 );                          // QCLASS field (Internet)...
        ByteBuffer queryBuffer = bb.getByteBuffer();  // get a byte buffer with our datagram's bytes...
        byte[] queryBytes = queryBuffer.array();      // get the bytes in our buffer...
        queryBytes = Arrays.copyOf( queryBytes, bb.position() >>> 3 );

        try( DatagramSocket socket = new DatagramSocket() ) {

            // get our server address into an Internet address...
            InetAddress serverAddress = InetAddress.getByName( dnsServer );

            // loop until we either get a response, or we give up...
            do {

                try {

                    // update the id field in our packet bytes...
                    queryBytes[1] = (byte) id;

                    // make our query packet...
                    DatagramPacket query = new DatagramPacket( queryBytes, queryBytes.length, serverAddress, DNS_PORT );

                    // send our query...
                    socket.setSoTimeout( timeout );
                    socket.send( query );

                    // wait for the response...
                    DatagramPacket response = new DatagramPacket( new byte[DNS_ANSWER_MAX_LENGTH], DNS_ANSWER_MAX_LENGTH );
                    socket.receive( response );

                    // if we got a response, we're done...
                    actualTime = System.currentTimeMillis() - start;
                    actualTries = response.getData()[1] & 0xFF;
                    type = COMPLETED;   // we got it...
                    break;
                }

                // if we timeout, we might want to try again, so set up for that...
                catch( SocketTimeoutException _e ) {
                    id++;                // go to the next id...
                    timeout += timeout;  // double our timeout value...
                    type = TIMEOUT;      // we'll assume a timeout now; this could be overridden upon either success or an error...
                }

            } while( id <= tries );
        }
        catch( IOException _e ) {
            type = ERROR;
            LOGGER.log( Level.SEVERE, "Unexpected exception while querying DNS", _e );
        }

        // send our result event...
        ISPMonitor.postEvent( new Event( EventType.DNSResult, new DNSResult( handler, type, actualTime, actualTries, dnsServer ) ) );
    }
}
