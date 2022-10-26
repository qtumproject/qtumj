/*
 * Copyright 2013 Google Inc.
 * Copyright 2018 Andreas Schildbach
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

import java.math.BigInteger;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.KeccakHash;
import org.bitcoinj.core.Sha256Hash;

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters for the regression test mode of bitcoind in which all blocks are trivially solvable.
 */
public class RegTestParams extends AbstractQtumNetParams {
    private static final BigInteger MAX_TARGET = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    private static final long GENESIS_TIME = 1504695029;
    private static final long GENESIS_NONCE = 17;
    private static final Sha256Hash GENESIS_HASH = Sha256Hash.wrap("665ed5b402ac0b44efc37d8926332994363e8a7278b7ee9a58fb972efadae943");

    public RegTestParams() {
        super();
        getGenesisBlock();
        id = ID_REGTEST;
        packetMagic = 0xfdddc6e1L;
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
        posNoRetargeting = true;
        maxTarget = MAX_TARGET;
        posMaxTarget = MAX_TARGET;
        qip9POSMaxTarget = MAX_TARGET;
        reducedBlockTimePosMaxTarget = MAX_TARGET;
        qip9Height = 0;
        reduceBlocktimeHeight = 0;
        port = 23888;
        addressHeader = 120;
        p2shHeader = 110;
        dumpedPrivateKeyHeader = 239;
        segwitAddressHrp = "qcrt";
        genesisBlock.setTime(1504695029);
        genesisBlock.setDifficultyTarget(0x207fffffL);
        genesisBlock.setNonce(17);

        spendableCoinbaseDepth = 500;
        reducedBlockTimeSpendableCoinbaseDepth = 2000;
        subsidyDecreaseBlockCount = 150;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("665ed5b402ac0b44efc37d8926332994363e8a7278b7ee9a58fb972efadae943"));
        dnsSeeds = null;
        addrSeeds = null;
        /*
        
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(Block.EASIEST_DIFFICULTY_TARGET);
        // Difficulty adjustments are disabled for regtest.
        // By setting the block interval for difficulty adjustments to Integer.MAX_VALUE we make sure difficulty never
        // changes.
        interval = Integer.MAX_VALUE;
        subsidyDecreaseBlockCount = 150;

        port = 18444;
        packetMagic = 0xfabfb5daL;
        dumpedPrivateKeyHeader = 239;
        addressHeader = 111;
        p2shHeader = 196;
        segwitAddressHrp = "bcrt";
        spendableCoinbaseDepth = 100;
         */
        bip32HeaderP2PKHpub = 0x043587cf; // The 4 byte header that serializes in base58 to "tpub".
        bip32HeaderP2PKHpriv = 0x04358394; // The 4 byte header that serializes in base58 to "tprv"
        bip32HeaderP2WPKHpub = 0x045f1cf6; // The 4 byte header that serializes in base58 to "vpub".
        bip32HeaderP2WPKHpriv = 0x045f18bc; // The 4 byte header that serializes in base58 to "vprv"

        // Difficulty adjustments are disabled for regtest.
        // By setting the block interval for difficulty adjustments to Integer.MAX_VALUE we make sure difficulty never
        // changes.

        majorityEnforceBlockUpgrade = MainNetParams.MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MainNetParams.MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MainNetParams.MAINNET_MAJORITY_WINDOW;

        dnsSeeds = null;
        addrSeeds = null;
    }

    @Override
    public boolean allowEmptyPeerChain() {
        return true;
    }

    private static RegTestParams instance;
    public static synchronized RegTestParams get() {
        if (instance == null) {
            instance = new RegTestParams();
        }
        return instance;
    }

    @Override
    public Block getGenesisBlock() {
        synchronized (GENESIS_HASH) {
            if (genesisBlock == null) {
                genesisBlock = Block.createGenesis(this);
                genesisBlock.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET);
                genesisBlock.setTime(GENESIS_TIME);
                genesisBlock.setNonce(GENESIS_NONCE);
                genesisBlock.setHashStateRoot(Sha256Hash.wrap("e965ffd002cd6ad0e2dc402b8044de833e06b23127ea8c3d80aec91410771495"));
                genesisBlock.setHashUtxoRoot(KeccakHash.of(new byte[] {(byte) 0x80})); // RLP("") is 0x80
                genesisBlock.setStakePrevTxid(Sha256Hash.ZERO_HASH);
                genesisBlock.setStakeOutputIndex(0xffffffffL);
                checkState(genesisBlock.getHash().equals(GENESIS_HASH), "Invalid genesis hash: " + genesisBlock.getHash());
            }
        }
        return genesisBlock;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_REGTEST;
    }
}
