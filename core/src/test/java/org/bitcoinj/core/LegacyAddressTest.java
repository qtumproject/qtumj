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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.Networks;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.script.Script.ScriptType;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class LegacyAddressTest {
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final NetworkParameters MAINNET = MainNetParams.get();

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(LegacyAddress.class)
                .withPrefabValues(NetworkParameters.class, MAINNET, TESTNET)
                .suppress(Warning.NULL_FIELDS)
                .suppress(Warning.TRANSIENT_FIELDS)
                .usingGetClass()
                .verify();
    }

    @Test
    public void testJavaSerialization() throws Exception {
        LegacyAddress testAddress = LegacyAddress.fromBase58(TESTNET, "qRAGAemcGKAjaTJbWBUnB8Hycg4qh4g85Y");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testAddress);
        LegacyAddress testAddressCopy = (LegacyAddress) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()))
                .readObject();
        assertEquals(testAddress, testAddressCopy);

        LegacyAddress mainAddress = LegacyAddress.fromBase58(MAINNET, "QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainAddress);
        LegacyAddress mainAddressCopy = (LegacyAddress) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()))
                .readObject();
        assertEquals(mainAddress, mainAddressCopy);
    }

    @Test
    public void stringification() {
        // Test a testnet address.
        LegacyAddress a = LegacyAddress.fromPubKeyHash(TESTNET, HEX.decode("53619ad2a51d5a7139b6056f5c44955cbdc2a575"));
        assertEquals("qRAGAemcGKAjaTJbWBUnB8Hycg4qh4g85Y", a.toString());
        assertEquals(ScriptType.P2PKH, a.getOutputScriptType());

        LegacyAddress b = LegacyAddress.fromPubKeyHash(MAINNET, HEX.decode("cc5e9334b41fb5d557cff0eaacf252bc4cae1950"));
        assertEquals("QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE", b.toString());
        assertEquals(ScriptType.P2PKH, b.getOutputScriptType());
    }

    @Test
    public void decoding() {
        LegacyAddress a = LegacyAddress.fromBase58(TESTNET, "qRAGAemcGKAjaTJbWBUnB8Hycg4qh4g85Y");
        assertEquals("53619ad2a51d5a7139b6056f5c44955cbdc2a575", Utils.HEX.encode(a.getHash()));

        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");
        assertEquals("cc5e9334b41fb5d557cff0eaacf252bc4cae1950", Utils.HEX.encode(b.getHash()));
    }

    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            LegacyAddress.fromBase58(TESTNET, "this is not a valid address!");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            LegacyAddress.fromBase58(TESTNET, "");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            LegacyAddress.fromBase58(TESTNET, "QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            // Success.
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() {
        NetworkParameters params = LegacyAddress.getParametersFromAddress("QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");
        assertEquals(MAINNET.getId(), params.getId());
        params = LegacyAddress.getParametersFromAddress("qRAGAemcGKAjaTJbWBUnB8Hycg4qh4g85Y");
        assertEquals(TESTNET.getId(), params.getId());
    }

    @Test
    public void getAltNetwork() {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        NetworkParameters params = LegacyAddress.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = LegacyAddress.getParametersFromAddress("QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");
        assertEquals(MAINNET.getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            LegacyAddress.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) { }
    }

    @Test
    public void p2shAddress() {
        // Test that we can construct P2SH addresses
        LegacyAddress mainNetP2SHAddress = LegacyAddress.fromBase58(MainNetParams.get(), "MUWEsZbNQiMXqxHAifTVCm7hK69vLHU42a");
        assertEquals(mainNetP2SHAddress.getVersion(), MAINNET.p2shHeader);
        assertEquals(ScriptType.P2SH, mainNetP2SHAddress.getOutputScriptType());
        LegacyAddress testNetP2SHAddress = LegacyAddress.fromBase58(TestNet3Params.get(), "mNqb1uX2qQMnjgWXFRyrWj4q9us1K4upFj");
        assertEquals(testNetP2SHAddress.getVersion(), TESTNET.p2shHeader);
        assertEquals(ScriptType.P2SH, testNetP2SHAddress.getOutputScriptType());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = LegacyAddress.getParametersFromAddress("MUWEsZbNQiMXqxHAifTVCm7hK69vLHU42a");
        assertEquals(MAINNET.getId(), mainNetParams.getId());
        NetworkParameters testNetParams = LegacyAddress.getParametersFromAddress("mNqb1uX2qQMnjgWXFRyrWj4q9us1K4upFj");
        assertEquals(TESTNET.getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("e204f2a2c2a50972a3527bd187147ca49979da3f");
        LegacyAddress a = LegacyAddress.fromScriptHash(MAINNET, hex);
        assertEquals("MUWEsZbNQiMXqxHAifTVCm7hK69vLHU42a", a.toString());
        LegacyAddress b = LegacyAddress.fromScriptHash(TESTNET, HEX.decode("490c00ac413d5fbb39c8fa46064a016abfc5cadb"));
        assertEquals("mNqb1uX2qQMnjgWXFRyrWj4q9us1K4upFj", b.toString());
        LegacyAddress c = LegacyAddress.fromScriptHash(MAINNET,
                ScriptPattern.extractHashFromP2SH(ScriptBuilder.createP2SHOutputScript(hex)));
        assertEquals("MUWEsZbNQiMXqxHAifTVCm7hK69vLHU42a", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKey key1 = DumpedPrivateKey.fromBase58(MAINNET, "Kwypcvc619wYZ7yPB53znrBTNNyc91jX5j5DRSBpjDktb9UHCaKM").getKey();
        key1 = ECKey.fromPrivate(key1.getPrivKeyBytes());
        ECKey key2 = DumpedPrivateKey.fromBase58(MAINNET, "KzczwzukDAjaXJ7avmia7BmTGEpFN5fvpRUU1RHnuXvtBcchAHUE").getKey();
        key2 = ECKey.fromPrivate(key2.getPrivKeyBytes());
        ECKey key3 = DumpedPrivateKey.fromBase58(MAINNET, "Ky3bJEZ3J1nqn9VydTaWrVTthgQf9JCWVBRNbxTDnVhm2o4e2KAU").getKey();
        key3 = ECKey.fromPrivate(key3.getPrivKeyBytes());

        List<ECKey> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys);
        LegacyAddress address = LegacyAddress.fromScriptHash(MAINNET,
                ScriptPattern.extractHashFromP2SH(p2shScript));
        assertEquals("MUWEsZbNQiMXqxHAifTVCm7hK69vLHU42a", address.toString());
    }

    @Test
    public void cloning() throws Exception {
        LegacyAddress a = LegacyAddress.fromPubKeyHash(TESTNET, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        LegacyAddress b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() {
        String base58 = "QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE";
        assertEquals(base58, LegacyAddress.fromBase58(null, base58).toBase58());
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "Qeqn1rv8WmZMBPiQEV8y2n7pciTDim25mY");
        LegacyAddress b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonLessThan() {
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "Qeqn1rv8WmZMBPiQEV8y2n7pciTDim25mY");
        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");

        int result = a.compareTo(b);
        assertTrue(result < 0);
    }

    @Test
    public void comparisonGreaterThan() {
        LegacyAddress a = LegacyAddress.fromBase58(MAINNET, "QfEbFcvEE6B3scHAb7UaARaWLKxYXCSxNE");
        LegacyAddress b = LegacyAddress.fromBase58(MAINNET, "Qeqn1rv8WmZMBPiQEV8y2n7pciTDim25mY");

        int result = a.compareTo(b);
        assertTrue(result > 0);
    }

    @Test
    public void comparisonBytesVsString() throws Exception {
        BufferedReader dataSetReader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("LegacyAddressTestDataset.txt")));
        String line;
        while ((line = dataSetReader.readLine()) != null) {
            String addr[] = line.split(",");
            LegacyAddress first = LegacyAddress.fromBase58(MAINNET, addr[0]);
            LegacyAddress second = LegacyAddress.fromBase58(MAINNET, addr[1]);
            assertTrue(first.compareTo(second) < 0);
            assertTrue(first.toString().compareTo(second.toString()) < 0);
        }
    }
}
