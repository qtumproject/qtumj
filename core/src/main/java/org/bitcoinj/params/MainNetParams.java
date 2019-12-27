/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
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
import org.bitcoinj.net.discovery.*;

import java.net.*;

import static com.google.common.base.Preconditions.*;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends AbstractQtumNetParams {
    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;

    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        targetTimespanV2 = TARGET_TIMESPAN_V2;
        intervalV2 = INTERVAL_V2;
        targetSpacing = 128;
        powNoRetargeting = true;
        posNoRetargeting = false;
        maxTarget = Utils.decodeCompactBits(0x1f00ffffL);
        posMaxTarget = Utils.decodeCompactBits(0x1d00ffffL);
        qip9POSMaxTarget = Utils.decodeCompactBits(0x1a1fffffL);
        qip9Height = 466600;
        dumpedPrivateKeyHeader = 128;
        addressHeader = 58;
        p2shHeader = 50;
        segwitAddressHrp = "qc";
        port = 3888;
        packetMagic = 0xf1cfa6d3L;
        bip32HeaderP2PKHpub = 0x0488b21e; // The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ade4; // The 4 byte header that serializes in base58 to "xprv"
        bip32HeaderP2WPKHpub = 0x04b24746; // The 4 byte header that serializes in base58 to "zpub".
        bip32HeaderP2WPKHpriv = 0x04b2430c; // The 4 byte header that serializes in base58 to "zprv"

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        genesisBlock.setDifficultyTarget(0x1f00ffffL);
        genesisBlock.setTime(1504695029L);
        genesisBlock.setNonce(8026361);

        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 985500;
        spendableCoinbaseDepth = 500;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("000075aef83cf2853580f8ae8ce6f8c3096cfa21d98334d6e3f95e5582ed986c"),
                genesisHash);

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
        checkpoints.put(5000, Sha256Hash.wrap("00006a5338e5647872bd91de1d291365e941e14dff1939b5f16d1804d1ce61cd"));
        checkpoints.put(45000, Sha256Hash.wrap("060c6af680f6975184c7a17059f2ff4970544fcfd4104e73744fe7ab7be14cfc"));
        checkpoints.put(90000, Sha256Hash.wrap("66fcf426b0aa6f2c9e3330cb2775e9e13c4a2b8ceedb50f8931ae0e12078ad50"));
        checkpoints.put(245000, Sha256Hash.wrap("ed79607feeadcedf5b94f1c43df684af5106e79b0989a008a88f9dc2221cc12a"));
        checkpoints.put(353000, Sha256Hash.wrap("d487896851fed42b07771f950fcc4469fbfa79211cfefed800f6d7806255e23f"));
        checkpoints.put(367795, Sha256Hash.wrap("1209326b73e38e44ec5dc210f51dc5d8c3494e9c698521032dd754747d4c1685"));
        checkpoints.put(445709, Sha256Hash.wrap("814e7d91aac6c577e4589b76918f44cf80020212159d39709fbad3f219725c9f"));
        checkpoints.put(498000, Sha256Hash.wrap("497f28fd4b1dadc9ff6dd2ac771483acfd16e4c4664eb45d0a6008dc33811418"));

        dnsSeeds = new String[] {
                "qtum3.dynu.net",
                "qtum5.dynu.net",
                "qtum6.dynu.net",
                "qtum7.dynu.net",
        };

        // These are in big-endian format, which is what the SeedPeers code expects.
        // Updated Dec. 17th 2019
        addrSeeds = new int[] { 0x23c2ad65, 0x23c2bbf3, 0x23bd5b9b, 0x23bd6f6a, 0x23bce2fc, 0x23baafe4, 0x2d4da090,
                0x2d4dc081, 0x23e07744, 0x23c5cd2c, 0x23c8c2d3, 0x68c6d71b, 0x23e65006, 0x23bbc435, 0x23c07c05,
                0x23c58aa3, 0x23c66c38, 0x23e17ca2, 0x23e1bc5d, 0x23e21fce, 0x23e6ac35, 0x23e2cba7, 0x23e21cb0,
                0x23c34b8c, 0x23e8adad, 0x23cde943, 0x23c3ee29, 0x23e279e9, 0x23c8a37d, 0x23c036a1, 0x23e90014,
                0x23c3ce77, 0x23c090c4, 0x23cdcf1c, 0x82d3dc20, 0x23c8a76e, 0x23c6004c, 0x68c7c28e, 0x23bd7b58,
                0x23c8ffd4, 0x23c89f44, 0x23e78cdd, 0x23bcd6fa, 0x23c888bd, 0x23bd4249, 0x23e07b92, 0x23bd5f1f,
                0x23c88954, 0x23bc1a55, 0x23e69a9a, 0x23c88235, 0x3b1174f5, 0x72ce0b74, 0x7628f025, 0x2f59ffd8,
                0x2f4b83b0, 0xaf732506, 0x7d86569a, 0x70a87b3f, 0x2f5e8c55, 0x784f8dde, 0x23c80410, 0xdffffac2,
                0x70d89a52, 0x2f345f02, 0x738b23a7, 0x77435921, 0x01f9b67d, 0x4da814d1, 0x36faef1a, 0xb4402dc5,
                0xb4402dc5, 0x79a8f5b1, 0x2772d780, 0xdcc22bec, 0x5f5ac3d4, 0x0e8b5f44, 0x56c309ed, 0x23bebfb6,
                0xdaeec757, 0x0d7c814e, 0x0de41807, 0x77c2df2b, 0x23e5243c, 0x7988ea7e, 0xb76defc8, 0x7d891c16,
                0x3140215a, 0x72d9d827, 0x3140bd77, 0x72d86ba8, 0x79e36710, 0xb46ba82e, 0xb46c3913, 0x72dcc744,
                0x72dafc78, 0x72d86b03, 0x72da67af, 0x72da6688, 0x79e409d1, 0x314021b0, 0x753e6b13, 0x3140bb83,
                0xdde1d96e, 0x7550dac2, 0x314021f1, 0x79e02c15, 0x72d86bde, 0xb7d171ad, 0x79e02ccc, 0x72dcc712,
                0x72d8906b, 0x7550da0a, 0x72d9d8a2, 0xb46ba808, 0x79e02ca8, 0x72db0738, 0x79e02c82, 0x3155def9,
                0x75507bbf, 0x79e409b4, 0xb46c39b7, 0x7551b876, 0xb7d17354, 0x314076e4, 0xb7d17315, 0x72d9d8ca,
                0x3149ae6c, 0xdde1d9f2, 0xb46b91c3, 0xb46b91f3, 0xb46ab2af, 0xb46b66b3, 0x79e02c0a, 0x72dcc766,
                0xb46c3987, 0xb46ab291, 0x3140bd80, 0x75507b41, 0xb46ab20c, 0x79e02caa, 0x72dafc42, 0xb46ab2d5,
                0x3140bd4f, 0x3155de9c, 0xb467a820, 0x3140bb60, 0x79e4094d, 0x72d9d8ba, 0x3149ec6e, 0x31402131,
                0x3148a5c1, 0xb46ab240, 0x79e02c6c, 0xb46a84a4, 0x749400f8, 0xb46b09e5, 0xdde1d97c, 0xb7d1731b,
                0xb7d1711e, 0x3149ae32, 0x3140bd21, 0xb7d17347, 0x72dcc7dd, 0xb46b9150, 0xb7d17287, 0xb7d17398,
                0xb7d1739d, 0x72d89055, 0xb46c398b, 0x3155de5f, 0x7551b84e, 0x3140bd82, 0x3149ae89, 0xb46c39ab,
                0x79e02c13, 0xb46abf5b, 0x72d89073, 0x3140766e, 0x3149aecc, 0x3155de27, 0x7551b827, 0x72d86b69,
                0xb46c393f, 0x72dac21a, 0xb46b0918, 0x31407624, 0x753e6bee, 0xb7d172d3, 0x31400a6d, 0x3ad07840,
                0x31407650, 0x72dafcd5, 0x72d9d861, 0x79e02c2e, 0x3140760e, 0x72da64b1, 0xb467a858, 0x79e02cc0,
                0x7551b8fc, 0x31402165, 0x72dafcda, 0x72d86b18, 0xb46b0935, 0xb7d17299, 0x753e6b1f, 0x72d86ba2,
                0x7551b855, 0x3149ec17, 0xb7d171a1, 0x79eef514, 0xdf5de0e2, 0x72d86b0c, 0x314021f4, 0x3ad0785e,
                0x75507bba, 0x7551b8b1, 0xb7d17190, 0x75507baf, 0x79e02c95, 0x7551fe9e, 0x79e02c1d, 0xb7d17158,
                0xb46b5610, 0x72da94f1, 0x3148e0f5, 0xb7d17303, 0x3155dee4, 0xdde1d942, 0x3155de4d, 0xb7d1703c,
                0x7551b879, 0xb46ba8ab, 0xb46b91b6, 0xb7d172b4, 0x79e02c39, 0x72db075a, 0xb7d1705d, 0x72dafcaf,
                0xb46ba83b, 0xb7d17264, 0x31402183, 0x72da65ef, 0x72dafcef, 0x3ad0782d, 0xb46ab26c, 0x72d86b59,
                0x72dafcf6, 0x3149aec2, 0x72dac25b, 0xb46b914e, 0xb46ab2be, 0xb46ba805, 0x79e02cea, 0xb7d17151,
                0xb4750add, 0xb7d173bd, 0x72d86bd0, 0x79e02cb9, 0x3149aec5, 0x72dcc743, 0x3149ae10, 0x79e02c8c,
                0x3148a507, 0xb46abf15, 0x72da666a, 0x753e6b23, 0xb46b66f2, 0x72dafcc0, 0x72da94f0, 0x3140763a,
                0x72dafcb2, 0xb7d1717d, 0x3148a567, 0x753e6b3c, 0xb7f8f31c, 0xb7d17172, 0x72d86b33, 0x7551b80b,
                0x7551b8e1, 0x3140bd41, 0xb46b6671, 0x314021c1, 0xb7d17037, 0x72db0735, 0xb46abfe5, 0xb46a855a,
        };
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
