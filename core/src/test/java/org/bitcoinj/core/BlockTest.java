/*
 * Copyright 2011 Google Inc.
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

import com.google.common.io.ByteStreams;

import org.bitcoinj.core.AbstractBlockChain.NewBlockType;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.Wallet.BalanceType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.bitcoinj.core.Utils.HEX;

public class BlockTest {
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final NetworkParameters UNITTEST = UnitTestParams.get();
    private static final NetworkParameters MAINNET = MainNetParams.get();

    private byte[] block57172Bytes;
    private Block block57172;

    @Before
    public void setUp() throws Exception {
        new Context(TESTNET);
        // One with some of transactions in, so a good test of the merkle tree hashing.
        block57172Bytes = ByteStreams.toByteArray(BlockTest.class.getResourceAsStream("block_testnet57172.dat"));
        block57172 = TESTNET.getDefaultSerializer().makeBlock(block57172Bytes);
        assertEquals("a59fad44cfb109a6d49815d96dbe66ca5160573c9f06143fdf9e86a02cc020c5", block57172.getHashAsString());
    }

    @Test
    public void testWork() throws Exception {
        BigInteger work = TESTNET.getGenesisBlock().getWork();
        double log2Work = Math.log(work.longValue()) / Math.log(2);
        // This number is printed by Bitcoin Core at startup as the calculated value of chainWork on testnet:
        // UpdateTip: new best=000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943 height=0 version=0x00000001 log2_work=32.000022 tx=1 date='2011-02-02 23:16:42' ...
        assertEquals(16.000022, log2Work, 0.0000001);
    }

    @Test
    public void testBlockVerification() throws Exception {
        block57172.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
    }

    @Test
    public void testDate() throws Exception {
        assertEquals("2017-12-26T14:22:08Z", Utils.dateTimeFormat(block57172.getTime()));
    }

    @Ignore("fix after pos added")
    @Test
    public void testProofOfWork() throws Exception {
        // This params accepts any difficulty target.
        Block block = UNITTEST.getDefaultSerializer().makeBlock(block57172Bytes);
        block.setNonce(12346);
        try {
            block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
            fail();
        } catch (VerificationException e) {
            // Expected.
        }
        // Blocks contain their own difficulty target. The BlockChain verification mechanism is what stops real blocks
        // from containing artificially weak difficulties.
        block.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET);
        // Now it should pass.
        block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
        // Break the nonce again at the lower difficulty level so we can try solving for it.
        block.setNonce(1);
        try {
            block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
            fail();
        } catch (VerificationException e) {
            // Expected to fail as the nonce is no longer correct.
        }
        // Should find an acceptable nonce.
        block.solve();
        block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
    }

    @Test
    public void testBadTransactions() throws Exception {
        // Re-arrange so the coinbase transaction is not first.
        Transaction tx1 = block57172.transactions.get(0);
        Transaction tx2 = block57172.transactions.get(1);
        block57172.transactions.set(0, tx2);
        block57172.transactions.set(1, tx1);
        try {
            block57172.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
            fail();
        } catch (VerificationException e) {
            // We should get here.
        }
    }

    @Test
    public void testHeaderParse() throws Exception {
        Block header = block57172.cloneAsHeader();
        Block reparsed = TESTNET.getDefaultSerializer().makeBlock(header.bitcoinSerialize());
        assertEquals(reparsed, header);
    }

    @Test
    public void testBitcoinSerialization() throws Exception {
        // We have to be able to reserialize everything exactly as we found it for hashing to work. This test also
        // proves that transaction serialization works, along with all its subobjects like scripts and in/outpoints.
        //
        // NB: This tests the bitcoin serialization protocol.
        assertArrayEquals(block57172Bytes, block57172.bitcoinSerialize());
    }

    @Test
    public void testUpdateLength() {
        Block block = UNITTEST.getGenesisBlock().createNextBlockWithCoinbase(Block.BLOCK_VERSION_GENESIS, new ECKey().getPubKey(), Block.BLOCK_HEIGHT_GENESIS);
        assertEquals(block.bitcoinSerialize().length, block.length);
        final int origBlockLen = block.length;
        Transaction tx = new Transaction(UNITTEST);
        // this is broken until the transaction has > 1 input + output (which is required anyway...)
        //assertTrue(tx.length == tx.bitcoinSerialize().length && tx.length == 8);
        byte[] outputScript = new byte[10];
        Arrays.fill(outputScript, (byte) ScriptOpCodes.OP_FALSE);
        tx.addOutput(new TransactionOutput(UNITTEST, null, Coin.SATOSHI, outputScript));
        tx.addInput(new TransactionInput(UNITTEST, null, new byte[] {(byte) ScriptOpCodes.OP_FALSE},
                new TransactionOutPoint(UNITTEST, 0, Sha256Hash.of(new byte[] { 1 }))));
        int origTxLength = 8 + 2 + 8 + 1 + 10 + 40 + 1 + 1;
        assertEquals(tx.unsafeBitcoinSerialize().length, tx.length);
        assertEquals(origTxLength, tx.length);
        block.addTransaction(tx);
        assertEquals(block.unsafeBitcoinSerialize().length, block.length);
        assertEquals(origBlockLen + tx.length, block.length);
        block.getTransactions().get(1).getInputs().get(0).setScriptBytes(new byte[] {(byte) ScriptOpCodes.OP_FALSE, (byte) ScriptOpCodes.OP_FALSE});
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength + 1);
        block.getTransactions().get(1).getInputs().get(0).clearScriptBytes();
        assertEquals(block.length, block.unsafeBitcoinSerialize().length);
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength - 1);
        block.getTransactions().get(1).addInput(new TransactionInput(UNITTEST, null, new byte[] {(byte) ScriptOpCodes.OP_FALSE},
                new TransactionOutPoint(UNITTEST, 0, Sha256Hash.of(new byte[] { 1 }))));
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength + 41); // - 1 + 40 + 1 + 1
    }

    @Test
    public void testCoinbaseHeightTestnet() throws Exception {
        // Testnet block 21066 (hash 0000000004053156021d8e42459d284220a7f6e087bf78f30179c3703ca4eefa)
        // contains a coinbase transaction whose height is two bytes, which is
        // shorter than we see in most other cases.

        Block block = TESTNET.getDefaultSerializer().makeBlock(
            ByteStreams.toByteArray(getClass().getResourceAsStream("block_testnet21066.dat")));

        // Check block.
        assertEquals("b896032064c1ada669d2e1072a13797c1fe6230f93ca9c7983ef032ee26b693f", block.getHashAsString());
        block.verify(21066, EnumSet.of(Block.VerifyFlag.HEIGHT_IN_COINBASE));

        // Testnet block 32768 (hash 000000007590ba495b58338a5806c2b6f10af921a70dbd814e0da3c6957c0c03)
        // contains a coinbase transaction whose height is three bytes, but could
        // fit in two bytes. This test primarily ensures script encoding checks
        // are applied correctly.

        block = TESTNET.getDefaultSerializer().makeBlock(
            ByteStreams.toByteArray(getClass().getResourceAsStream("block_testnet32768.dat")));

        // Check block.
        assertEquals("b0fd31a2041d5a734163094a507cacdeffa8ef90ac56b13b19227487dc305db9", block.getHashAsString());
        block.verify(32768, EnumSet.of(Block.VerifyFlag.HEIGHT_IN_COINBASE));
    }

    @Ignore("not doable for now")
    @Test
    public void testReceiveCoinbaseTransaction() throws Exception {
        // Block 169482 (hash 0000000000000756935f1ee9d5987857b604046f846d3df56d024cdb5f368665)
        // contains coinbase transactions that are mining pool shares.
        // The private key MINERS_KEY is used to check transactions are received by a wallet correctly.

        // The address for this private key is 1GqtGtn4fctXuKxsVzRPSLmYWN1YioLi9y.
        final String MINING_PRIVATE_KEY = "5JDxPrBRghF1EvSBjDigywqfmAjpHPmTJxYtQTYJxJRHLLQA4mG";

        final long BLOCK_NONCE = 0L;
        final Coin BALANCE_AFTER_BLOCK = Coin.valueOf(22223642);
        Block block169482 = MAINNET.getDefaultSerializer().makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block169482.dat")));

        // Check block.
        assertNotNull(block169482);
        block169482.verify(169482, EnumSet.noneOf(Block.VerifyFlag.class));
        assertEquals(BLOCK_NONCE, block169482.getNonce());

        StoredBlock storedBlock = new StoredBlock(block169482, BigInteger.ONE, 169482); // Nonsense work - not used in test.

        // Create a wallet contain the miner's key that receives a spend from a coinbase.
        ECKey miningKey = DumpedPrivateKey.fromBase58(MAINNET, MINING_PRIVATE_KEY).getKey();
        assertNotNull(miningKey);
        Context context = new Context(MAINNET);
        Wallet wallet = Wallet.createDeterministic(context, Script.ScriptType.P2PKH);
        wallet.importKey(miningKey);

        // Initial balance should be zero by construction.
        assertEquals(Coin.ZERO, wallet.getBalance());

        // Give the wallet the first transaction in the block - this is the coinbase tx.
        List<Transaction> transactions = block169482.getTransactions();
        assertNotNull(transactions);
        wallet.receiveFromBlock(transactions.get(0), storedBlock, NewBlockType.BEST_CHAIN, 0);

        // Coinbase transaction should have been received successfully but be unavailable to spend (too young).
        assertEquals(BALANCE_AFTER_BLOCK, wallet.getBalance(BalanceType.ESTIMATED));
        assertEquals(Coin.ZERO, wallet.getBalance(BalanceType.AVAILABLE));
    }

    @Test
    public void testBlock5889_witnessCommitmentInCoinbase() throws Exception {
        Block block5889 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block5889.dat")));
        assertEquals(2, block5889.getTransactions().size());
        assertEquals("9a7ef62517cb8888c2b42d8095d5535a13351dbefe2c736d7968626888a25a3e",
                block5889.getMerkleRoot().toString());

        // This block has no witnesses.
        for (Transaction tx : block5889.getTransactions())
            assertFalse(tx.hasWitnesses());

        // Nevertheless, there is a witness commitment (but no witness reserved).
        Transaction coinbase = block5889.getTransactions().get(0);
        assertEquals("b878e67f6f1d8291f0c8a9a3111030157674cdb8a3f5d7ccc1ced96caf308d68", coinbase.getTxId().toString());
        assertEquals("b878e67f6f1d8291f0c8a9a3111030157674cdb8a3f5d7ccc1ced96caf308d68",
                coinbase.getWTxId().toString());
        Sha256Hash witnessCommitment = coinbase.findWitnessCommitment();
        assertEquals("598bdca4eab6001d5753ba33e9a50b1986649f4b477948b5f761decd6b742b4e", witnessCommitment.toString());
    }

    @Test
    public void testBlock481829_witnessTransactions() throws Exception {
        Block block481829 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block481829.dat")));
        assertEquals(3, block481829.getTransactions().size());
        assertEquals("d0d385376b2b25a95baa725fa53711febcae614ebc3d9f97cfe2054ef66cedf3",
                block481829.getMerkleRoot().toString());
        assertEquals("1a34a4fc68c0c2c0f1db362fd2d54032345950904cf0c0af9a6aabdc4e2da6ea",
                block481829.getWitnessRoot().toString());

        Transaction coinbase = block481829.getTransactions().get(0);
        assertEquals("a22dfa027fbcc0548bdf1fadcb039cc068ff0223e5cd1e1b9453bd09dd804743", coinbase.getTxId().toString());
        assertEquals("7eb24cc1a1a3ddd63ab0c042d971ee13887d878d71d05e4d6a0b5b712cdbd245",
                coinbase.getWTxId().toString());
        Sha256Hash witnessCommitment = coinbase.findWitnessCommitment();
        assertEquals("d304233cb1ac6d7dd5494eb6af70b85e66a570eb306dd1f95ea19e861c537378", witnessCommitment.toString());
        byte[] witnessReserved = coinbase.getInput(0).getWitness().getPush(0);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", HEX.encode(witnessReserved));
        block481829.checkWitnessRoot();
    }

    @Ignore("not applicable")
    @Test
    public void isBIPs() throws Exception {
        final Block genesis = MAINNET.getGenesisBlock();
        assertFalse(genesis.isBIP34());
        assertFalse(genesis.isBIP66());
        assertFalse(genesis.isBIP65());

        // 227835/00000000000001aa077d7aa84c532a4d69bdbff519609d1da0835261b7a74eb6: last version 1 block
        final Block block227835 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block227835.dat")));
        assertFalse(block227835.isBIP34());
        assertFalse(block227835.isBIP66());
        assertFalse(block227835.isBIP65());

        // 227836/00000000000000d0dfd4c9d588d325dce4f32c1b31b7c0064cba7025a9b9adcc: version 2 block
        final Block block227836 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block227836.dat")));
        assertTrue(block227836.isBIP34());
        assertFalse(block227836.isBIP66());
        assertFalse(block227836.isBIP65());

        // 363703/0000000000000000011b2a4cb91b63886ffe0d2263fd17ac5a9b902a219e0a14: version 3 block
        final Block block363703 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block363703.dat")));
        assertTrue(block363703.isBIP34());
        assertTrue(block363703.isBIP66());
        assertFalse(block363703.isBIP65());

        // 383616/00000000000000000aab6a2b34e979b09ca185584bd1aecf204f24d150ff55e9: version 4 block
        final Block block383616 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block383616.dat")));
        assertTrue(block383616.isBIP34());
        assertTrue(block383616.isBIP66());
        assertTrue(block383616.isBIP65());

        // 370661/00000000000000001416a613602d73bbe5c79170fd8f39d509896b829cf9021e: voted for BIP101
        final Block block370661 = MAINNET.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block370661.dat")));
        assertTrue(block370661.isBIP34());
        assertTrue(block370661.isBIP66());
        assertTrue(block370661.isBIP65());
    }

    @Test
    public void parseBlockWithHugeDeclaredTransactionsSize() throws Exception{
        Block block = new Block(UNITTEST, 1, Sha256Hash.ZERO_HASH, Sha256Hash.ZERO_HASH, 1, 1, 1, new ArrayList<Transaction>()) {
            @Override
            protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
                Utils.uint32ToByteStreamLE(getVersion(), stream);
                stream.write(getPrevBlockHash().getReversedBytes());
                stream.write(getMerkleRoot().getReversedBytes());
                Utils.uint32ToByteStreamLE(getTimeSeconds(), stream);
                Utils.uint32ToByteStreamLE(getDifficultyTarget(), stream);
                Utils.uint32ToByteStreamLE(getNonce(), stream);

                stream.write(new VarInt(Integer.MAX_VALUE).encode());
            }

            @Override
            public byte[] bitcoinSerialize() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    bitcoinSerializeToStream(baos);
                } catch (IOException e) {
                }
                return baos.toByteArray();
            }
        };
        byte[] serializedBlock = block.bitcoinSerialize();
        try {
            UNITTEST.getDefaultSerializer().makeBlock(serializedBlock, serializedBlock.length);
            fail("We expect ProtocolException with the fixed code and OutOfMemoryError with the buggy code, so this is weird");
        } catch (ProtocolException e) {
            //Expected, do nothing
        }
    }
}
