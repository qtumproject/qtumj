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

    /**
     * Computes the address a contract will be deployed at with an OP_CREATE TransactionOutPoint and vout index
     * @param transactionOutPointHash TransactionOutPoint.getHash()
     * @param voutIndex Index of the vout in the signed transaction
     * @return the address a contract will be deployed at
     */
    public static ContractAddress fromOutPointHash(Sha256Hash transactionOutPointHash, long voutIndex) {
        byte[] hashBytes = transactionOutPointHash.getReversedBytes();
        // 4 bytes, little endian
        byte[] hashBytesWithVoutIndex = new byte[hashBytes.length + 4];
        // a transaction won't ever have enough vouts to reach two bytes, so we can ignore the other 3 bytes
        hashBytesWithVoutIndex[hashBytesWithVoutIndex.length - 4] = Long.valueOf(voutIndex).byteValue();
        System.arraycopy(hashBytes, 0, hashBytesWithVoutIndex, 0, hashBytes.length);
        return new ContractAddress(Utils.sha256hash160(hashBytesWithVoutIndex));
    }
    
    public byte[] getBytes() {
        return bytes;
    }
}
