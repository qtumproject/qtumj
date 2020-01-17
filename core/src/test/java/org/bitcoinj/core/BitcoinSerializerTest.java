/*
 * Copyright 2011 Noa Resare
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import org.bitcoinj.params.MainNetParams;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class BitcoinSerializerTest {
    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final byte[] ADDRESS_MESSAGE_BYTES = HEX.decode("f1cfa6d36164647200000000000000001f000000" +
            "f0b6ff65012b21145e010000000000000000000000000000000000ffff0a0000010f30");

    private static final byte[] TRANSACTION_MESSAGE_BYTES = HEX.withSeparator(" ", 2).decode(
            "f1 cf a6 d3 74 78 00 00  00 00 00 00 00 00 00 00" +
            "e1 00 00 00 0e ee af 44  02 00 00 00 01 5e ee 80" +
            "7b a1 33 39 4f 13 f1 de  21 96 90 c4 2e 30 b0 24" +
            "8d c6 bf 60 00 af 9c 8c  81 e7 b3 61 6f 00 00 00" +
            "00 6a 47 30 44 02 20 1d  dd 52 2a d9 85 26 27 1c" +
            "31 10 63 a2 b8 7a c3 bf  a0 5b d7 87 5b f2 21 10" +
            "59 87 6d 8e f9 ae fb 02  20 23 d1 6a 76 63 c2 8f" +
            "b7 40 ab 35 dc 48 6b ff  5e 2f e4 57 3b e9 90 80" +
            "f0 9b f3 fd 5d d4 9f b6  d4 01 21 03 bc 3b a7 d6" +
            "bb 4f 73 f9 b3 25 77 0b  e8 5e d1 47 4b a2 03 1b" +
            "23 b7 ac 2c d2 25 cb 71  e9 ca b1 5d fe ff ff ff" +
            "02 49 b1 97 4b 00 00 00  00 19 76 a9 14 27 a5 44" +
            "d8 ef 3d 2b 61 d5 fb 1d  b8 f3 b1 91 be a3 f7 10" +
            "b9 88 ac 20 f3 7d eb 00  00 00 00 19 76 a9 14 ee" +
            "bd 5a 96 3f ff fa 31 fb  91 cc 2a 50 3f 92 2f cd" +
            "70 e2 c1 88 ac cd f5 07  00");

    @Test
    public void testAddr() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();
        AddressMessage addressMessage = (AddressMessage) serializer.deserialize(ByteBuffer.wrap(ADDRESS_MESSAGE_BYTES));
        assertEquals(1, addressMessage.getAddresses().size());
        PeerAddress peerAddress = addressMessage.getAddresses().get(0);
        assertEquals(3888, peerAddress.getPort());
        assertEquals("10.0.0.1", peerAddress.getAddr().getHostAddress());
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ADDRESS_MESSAGE_BYTES.length);
        serializer.serialize(addressMessage, bos);

        assertEquals(31, addressMessage.getMessageSize());
        addressMessage.addAddress(new PeerAddress(MAINNET, InetAddress.getLocalHost()));
        assertEquals(61, addressMessage.getMessageSize());
        addressMessage.removeAddress(0);
        assertEquals(31, addressMessage.getMessageSize());

        //this wont be true due to dynamic timestamps.
        //assertTrue(LazyParseByteCacheTest.arrayContains(bos.toByteArray(), addrMessage));
    }

    @Test
    public void testCachedParsing() throws Exception {
        MessageSerializer serializer = MAINNET.getSerializer(true);

        // first try writing to a fields to ensure uncaching and children are not affected
        Transaction transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());

        transaction.setLockTime(1);
        // parent should have been uncached
        assertFalse(transaction.isCached());
        // child should remain cached.
        assertTrue(transaction.getInputs().get(0).isCached());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertFalse(Arrays.equals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray()));

        // now try writing to a child to ensure uncaching is propagated up to parent but not to siblings
        transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());

        transaction.getInputs().get(0).setSequenceNumber(1);
        // parent should have been uncached
        assertFalse(transaction.isCached());
        // so should child
        assertFalse(transaction.getInputs().get(0).isCached());

        bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertFalse(Arrays.equals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray()));

        // deserialize/reserialize to check for equals.
        transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());
        bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertArrayEquals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray());

        // deserialize/reserialize to check for equals.  Set a field to it's existing value to trigger uncache
        transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());

        transaction.getInputs().get(0).setSequenceNumber(transaction.getInputs().get(0).getSequenceNumber());

        bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertArrayEquals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray());
    }

    /**
     * Get 1 header of the block number 1 (the first one is 0) in the chain
     */
    @Test
    public void testHeaders1() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();

        byte[] headersMessageBytes = HEX.decode("f1cfa6d3686561646572730000000000b700000085bedbd8"
                + "01000000206c98ed82555ef9e3d63483d921fa6c09c3f8e68caef8803585f23cf8ae7500007ced"
                + "ea072eb141887b85795cff161c8be553d5d26ed5abef70f35830a4d5412ef92db259ffff001fd4"
                + "fc0000e965ffd002cd6ad0e2dc402b8044de833e06b23127ea8c3d80aec9141077149556e81f17"
                + "1bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b4210000000000000000000000"
                + "000000000000000000000000000000000000000000ffffffff0000");
        HeadersMessage headersMessage = (HeadersMessage) serializer.deserialize(ByteBuffer.wrap(headersMessageBytes));

        // The first block after the genesis
        // https://qtum.info/block/1
        Block block = headersMessage.getBlockHeaders().get(0);
        assertEquals("0000d5dab5e76310ae640e9bcfa270c2eb23a1e5948bdf01fc7ed1f157110ab7", block.getHashAsString());
        assertNotNull(block.transactions);
        assertEquals("2e41d5a43058f370efabd56ed2d553e58b1c16ff5c79857b8841b12e07eaed7c", Utils.HEX.encode(block.getMerkleRoot().getBytes()));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        serializer.serialize(headersMessage, byteArrayOutputStream);
        byte[] serializedBytes = byteArrayOutputStream.toByteArray();
        assertArrayEquals(headersMessageBytes, serializedBytes);
    }

    /**
     * Get 6 headers of blocks 1-6 in the chain
     */
    @Test
    public void testHeaders2() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();

        byte[] headersMessageBytes = HEX.decode("f1cfa6d36865616465727300000000004504000010120d87"
                + "06000000206c98ed82555ef9e3d63483d921fa6c09c3f8e68caef8803585f23cf8ae7500007ced"
                + "ea072eb141887b85795cff161c8be553d5d26ed5abef70f35830a4d5412ef92db259ffff001fd4"
                + "fc0000e965ffd002cd6ad0e2dc402b8044de833e06b23127ea8c3d80aec9141077149556e81f17"
                + "1bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b4210000000000000000000000"
                + "000000000000000000000000000000000000000000ffffffff0000000000206c98ed82555ef9e3"
                + "d63483d921fa6c09c3f8e68caef8803585f23cf8ae7500007cedea072eb141887b85795cff161c"
                + "8be553d5d26ed5abef70f35830a4d5412ef92db259ffff001fd4fc0000e965ffd002cd6ad0e2dc"
                + "402b8044de833e06b23127ea8c3d80aec9141077149556e81f171bcc55a6ff8345e692c0f86e5b"
                + "48e01b996cadc001622fb5e363b421000000000000000000000000000000000000000000000000"
                + "0000000000000000ffffffff000000000020ea015d1ebbd5f32b49fcb0ef86c76220304585d27a"
                + "b9ab1175cf7d843a3d0000e43cf2a312e95e803099a317fb895b19acca2e138ab453042962321e"
                + "5a8ef652fa2db259ffff001f68420000e965ffd002cd6ad0e2dc402b8044de833e06b23127ea8c"
                + "3d80aec9141077149556e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363"
                + "b4210000000000000000000000000000000000000000000000000000000000000000ffffffff00"
                + "0000000020b51d19bbe4c715858ab343f36748ae596afd31e9e1e82bd3f35b3a12bf320000e83c"
                + "62cb5c42ced51b26496843335b8cafee1acfa90c741d25cf48954f0bea3ffb2db259ffff001fa5"
                + "340000e965ffd002cd6ad0e2dc402b8044de833e06b23127ea8c3d80aec9141077149556e81f17"
                + "1bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b4210000000000000000000000"
                + "000000000000000000000000000000000000000000ffffffff000000000020ee6e85862fa1966a"
                + "d5885924a9cfba021b2e7e60d66c88d0de519f7cb52a0000f3c5a3c5f3397ade021f67169f13fc"
                + "a867fb5c5cacdefb70cf58cc0c7691f86cfb2db259ffff001fc2970000e965ffd002cd6ad0e2dc"
                + "402b8044de833e06b23127ea8c3d80aec9141077149556e81f171bcc55a6ff8345e692c0f86e5b"
                + "48e01b996cadc001622fb5e363b421000000000000000000000000000000000000000000000000"
                + "0000000000000000ffffffff0000000000203c1dd1f669a115110da995bef586f76c33bfb7e93b"
                + "d643cf058ae0da63fd00000aa041480a6b45c3e53f0c99b11c4c0b8ba03e0ef98f3aee429c2509"
                + "d40d17a0fb2db259ffff001f1b500000e965ffd002cd6ad0e2dc402b8044de833e06b23127ea8c"
                + "3d80aec9141077149556e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363"
                + "b4210000000000000000000000000000000000000000000000000000000000000000ffffffff0000");
        HeadersMessage headersMessage = (HeadersMessage) serializer.deserialize(ByteBuffer.wrap(headersMessageBytes));

        assertEquals(6, headersMessage.getBlockHeaders().size());

        // index 0 block is the number 1 block in the block chain
        // https://qtum.info/block/1
        Block zeroBlock = headersMessage.getBlockHeaders().get(0);
        assertEquals("0000d5dab5e76310ae640e9bcfa270c2eb23a1e5948bdf01fc7ed1f157110ab7",
                zeroBlock.getHashAsString());
        assertEquals(64724L, zeroBlock.getNonce());

        // index 3 block is the number 4 block in the block chain
        // https://qtum.info/block/4
        Block thirdBlock = headersMessage.getBlockHeaders().get(3);
        assertEquals("00002ab57c9f51ded0886cd6607e2e1b02bacfa9245988d56a96a12f86856eee",
                thirdBlock.getHashAsString());
        assertEquals(13477L, thirdBlock.getNonce());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        serializer.serialize(headersMessage, byteArrayOutputStream);
        byte[] serializedBytes = byteArrayOutputStream.toByteArray();
        assertArrayEquals(headersMessageBytes, serializedBytes);
    }

    @Test(expected = BufferUnderflowException.class)
    public void testBitcoinPacketHeaderTooShort() {
        new BitcoinSerializer.BitcoinPacketHeader(ByteBuffer.wrap(new byte[] { 0 }));
    }

    @Test(expected = ProtocolException.class)
    public void testBitcoinPacketHeaderTooLong() {
        // Message with a Message size which is 1 too big, in little endian format.
        byte[] wrongMessageLength = HEX.decode("000000000000000000000000010000020000000000");
        new BitcoinSerializer.BitcoinPacketHeader(ByteBuffer.wrap(wrongMessageLength));
    }

    @Test(expected = BufferUnderflowException.class)
    public void testSeekPastMagicBytes() {
        // Fail in another way, there is data in the stream but no magic bytes.
        byte[] brokenMessage = HEX.decode("000000");
        MAINNET.getDefaultSerializer().seekPastMagicBytes(ByteBuffer.wrap(brokenMessage));
    }

    /**
     * Tests serialization of an unknown message.
     */
    @Test(expected = Error.class)
    public void testSerializeUnknownMessage() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();

        Message unknownMessage = new Message() {
            @Override
            protected void parse() throws ProtocolException {
            }
        };
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ADDRESS_MESSAGE_BYTES.length);
        serializer.serialize(unknownMessage, bos);
    }
}
