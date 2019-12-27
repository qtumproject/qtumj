package org.bitcoinj.core;

public class SendCmpctMessage extends Message {

    public SendCmpctMessage(NetworkParameters params, byte[] payload) {
        super(params, payload, 0);
    }

    @Override
    protected void parse() throws ProtocolException {
        // TODO: not implemented yet
        length = 9;
    }

}
