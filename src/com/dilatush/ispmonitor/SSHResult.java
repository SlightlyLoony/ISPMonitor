package com.dilatush.ispmonitor;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SSHResult {

    public final SSHResultHandler handler;
    public final Command          command;
    public final SSHResultType    type;
    public final int              exitCode;
    public final String           output;


    public SSHResult( final SSHResultHandler _handler, final Command _command, final SSHResultType _type, final int _exitCode, final String _output ) {
        handler  = _handler;
        command  = _command;
        type     = _type;
        exitCode = _exitCode;
        output   = _output;
    }


    public String toString() {
        return type + ", exit code: " + exitCode + "; output: " + output;
    }
}
