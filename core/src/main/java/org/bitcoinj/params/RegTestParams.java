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

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters for the regression test mode of bitcoind in which all blocks are trivially solvable.
 */
public class RegTestParams extends AbstractQtumNetParams {
    private static final BigInteger MAX_TARGET = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    public RegTestParams() {
        super();
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
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_REGTEST;
    }
}
