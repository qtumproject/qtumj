package org.bitcoinj.core;

public class WitnessTransactionIdTransactionRelay extends EmptyMessage {
    public WitnessTransactionIdTransactionRelay() {
    }

    // this is needed by the BitcoinSerializer
    public WitnessTransactionIdTransactionRelay(NetworkParameters params, byte[] payload) {
    }
}
