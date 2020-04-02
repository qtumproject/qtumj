package org.bitcoinj.core;

import static com.google.common.base.Preconditions.checkNotNull;

public class ContractAddress {
    
    /**
     * An address is a RIPEMD160 hash of SHA256 hash of an OutPoint, therefore is always 160 bits or 20 bytes.
     */
    public static final int LENGTH = 20;
    private final byte[] bytes;

    private ContractAddress(byte[] hash160) {
        this.bytes = checkNotNull(hash160);
        if (hash160.length != LENGTH) {
            throw new AddressFormatException.InvalidDataLength(
                    "Contract addresses are 20 byte (160 bit) hashes, but got: " + hash160.length);
        }
    }
    
    public static ContractAddress fromString(String addr) {
        checkNotNull(addr);
        return new ContractAddress(Utils.HEX.decode(addr));
    }
    
    public static ContractAddress fromBytes(byte[] addr) {
        return new ContractAddress(addr);
    }
    
    public byte[] getBytes() {
        return bytes;
    }
}
