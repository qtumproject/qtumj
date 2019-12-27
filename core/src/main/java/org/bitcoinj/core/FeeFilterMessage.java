package org.bitcoinj.core;

public class FeeFilterMessage extends Message {

    public FeeFilterMessage(NetworkParameters params, byte[] payload) {
        super(params, payload, 0);
    }

    @Override
    protected void parse() throws ProtocolException {
        // TODO: not implemented yet
        length = 8;
    }

}
