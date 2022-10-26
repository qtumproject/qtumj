/*
 * Copyright 2013 Google Inc.
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

package org.bitcoinj.params;

import org.bitcoinj.core.Utils;
import java.math.BigInteger;
import java.net.URI;
import java.util.Date;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.net.discovery.HttpDiscovery;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends AbstractQtumNetParams {
    public static final int TESTNET_MAJORITY_WINDOW = 100;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51;
    private static final long GENESIS_TIME = 1296688602;
    private static final long GENESIS_NONCE = 414098458;
    private static final Sha256Hash GENESIS_HASH = Sha256Hash.wrap("000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943");

    public TestNet3Params() {
        super();
        id = ID_TESTNET;
        packetMagic = 0x0d221506;
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        targetTimespanV2 = TARGET_TIMESPAN_V2;
        reducedBlocktimeTimespan = 1000;
        intervalV2 = INTERVAL_V2;
        stakeTimestampMask = 15;
        reducedBlocktimeStakeTimestampMask = 3;
        targetSpacing = 128;
        reducedBlocktimeTargetSpacing = 32;
        powNoRetargeting = true;
        posNoRetargeting = false;
        maxTarget = Utils.decodeCompactBits(0x1f00ffffL);
        posMaxTarget = Utils.decodeCompactBits(0x1f00ffffL);
        qip9POSMaxTarget = Utils.decodeCompactBits(0x1a1fffffL);
        reducedBlockTimePosMaxTarget = Utils.decodeCompactBits(0x1a3fffffL);
        qip9Height = 446320;
        reduceBlocktimeHeight = 806600;
        port = 13888;
        addressHeader = 120;
        p2shHeader = 110;
        dumpedPrivateKeyHeader = 239;
        segwitAddressHrp = "tq";
        genesisBlock.setTime(1504695029L);
        genesisBlock.setDifficultyTarget(0x1f00ffffL);
        genesisBlock.setNonce(7349697);

        spendableCoinbaseDepth = 500;
        reducedBlockTimeSpendableCoinbaseDepth = 2000;
        subsidyDecreaseBlockCount = 985500;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("0000e803ee215c0684ca0d2f9220594d3f828617972aad66feb2ba51f5e14222"));
        alertSigningKey = Utils.HEX.decode("04302390343f91cc401d56d68b123028bf52e5fca1939df127f63c6467cdf9c8e2c14b61104cf817d0b780da337893ecc4aaff1309e536162dabbdb45200ca2b0a");
        /*

        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(Block.STANDARD_MAX_DIFFICULTY_TARGET);

        port = 18333;
        packetMagic = 0x0b110907;
        dumpedPrivateKeyHeader = 239;
        addressHeader = 111;
        p2shHeader = 196;
        segwitAddressHrp = "tb";
        spendableCoinbaseDepth = 100;
        bip32HeaderP2PKHpub = 0x043587cf; // The 4 byte header that serializes in base58 to "tpub".
        bip32HeaderP2PKHpriv = 0x04358394; // The 4 byte header that serializes in base58 to "tprv"
        bip32HeaderP2WPKHpub = 0x045f1cf6; // The 4 byte header that serializes in base58 to "vpub".
        bip32HeaderP2WPKHpriv = 0x045f18bc; // The 4 byte header that serializes in base58 to "vprv"

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;
         */

        dnsSeeds = new String[] {
                "qtum4.dynu.net",
        };
        // These are in big-endian format, which is what the SeedPeers code expects.
        // Updated Dec. 17th 2019
        addrSeeds = new int[] { 0x23c5eb1c, 0x68c605dc, 0x23c5840a, 0x23c2485f, };
        bip32HeaderP2PKHpub = 0x043587cf; // The 4 byte header that serializes in base58 to "tpub".
        bip32HeaderP2PKHpriv = 0x04358394; // The 4 byte header that serializes in base58 to "tprv"
        bip32HeaderP2WPKHpub = 0x045f1cf6; // The 4 byte header that serializes in base58 to "vpub".
        bip32HeaderP2WPKHpriv = 0x045f18bc; // The 4 byte header that serializes in base58 to "vprv"
        /* // TODO: QTUM?
        httpSeeds = new HttpDiscovery.Details[] {
                // Andreas Schildbach
                new HttpDiscovery.Details(
                        ECKey.fromPublicOnly(Utils.HEX.decode(
                                "0238746c59d46d5408bf8b1d0af5740fe1a6e1703fcb56b2953f0b965c740d256f")),
                        URI.create("http://testnet.httpseed.bitcoin.schildbach.de/peers")
                )
        };
        addrSeeds = null;
         */

    }

    private static TestNet3Params instance;
    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    @Override
    public Block getGenesisBlock() {
        synchronized (GENESIS_HASH) {
            if (genesisBlock == null) {
                genesisBlock = Block.createGenesis(this);
                genesisBlock.setDifficultyTarget(Block.STANDARD_MAX_DIFFICULTY_TARGET);
                genesisBlock.setTime(GENESIS_TIME);
                genesisBlock.setNonce(GENESIS_NONCE);
                checkState(genesisBlock.getHash().equals(GENESIS_HASH), "Invalid genesis hash");
            }
        }
        return genesisBlock;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }
}
