/*
 * Copyright 2013 Google Inc.
 * Copyright 2019 Andreas Schildbach
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

import org.bitcoinj.core.*;

import java.math.BigInteger;

/**
 * Network parameters used by the bitcoinj unit tests (and potentially your own). This lets you solve a block using
 * {@link Block#solve()} by setting difficulty to the easiest possible.
 */
public class UnitTestParams extends AbstractQtumNetParams {
    public static final int UNITNET_MAJORITY_WINDOW = 8;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 6;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 4;

    public UnitTestParams() {
        super();
        id = ID_UNITTESTNET;
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
        maxTarget = new BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
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
        genesisBlock.setTime(Utils.currentTimeSeconds());
        genesisBlock.setDifficultyTarget(Utils.encodeCompactBits(maxTarget));
        genesisBlock.solve();

        spendableCoinbaseDepth = 5;
        reducedBlockTimeSpendableCoinbaseDepth = 5;
        subsidyDecreaseBlockCount = 100;
        dnsSeeds = null;
        addrSeeds = null;
        bip32HeaderP2PKHpub = 0x043587cf; // The 4 byte header that serializes in base58 to "tpub".
        bip32HeaderP2PKHpriv = 0x04358394; // The 4 byte header that serializes in base58 to "tprv"
        bip32HeaderP2WPKHpub = 0x045f1cf6; // The 4 byte header that serializes in base58 to "vpub".
        bip32HeaderP2WPKHpriv = 0x045f18bc; // The 4 byte header that serializes in base58 to "vprv"

        majorityEnforceBlockUpgrade = 3;
        majorityRejectBlockOutdated = 4;
        majorityWindow = 7;
    }

    private static UnitTestParams instance;
    public static synchronized UnitTestParams get() {
        if (instance == null) {
            instance = new UnitTestParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return "unittest";
    }
}
