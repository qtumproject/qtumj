/*
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

package org.bitcoinj.crypto;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.BIP38PrivateKey.BadPassphraseException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class BIP38PrivateKeyTest {
    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final NetworkParameters TESTNET = TestNet3Params.get();

    @Test
    public void bip38testvector_noCompression_noEcMultiply_test1() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PRP4FDk4TJm7fViXdbc6U4ZH3QvwuHajFHc8fCj3JrFxkXgMynGHHJm5u");
        ECKey key = encryptedKey.decrypt("TestingOneTwoThree");
        assertEquals("5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_noCompression_noEcMultiply_test2() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PRSfw6tpX9276Fi2xGVqU7JXxbBtTQnKqDRJSGTZQd9jQHJyjv7JNVsqP");
        ECKey key = encryptedKey.decrypt("Satoshi");
        assertEquals("5HtasZ6ofTHP6HCwTqTkLDuLQisYPah7aUnSKfC7h4hMUVw2gi5", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_noCompression_noEcMultiply_test3() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PRLqnxUetjM4UUfkoWhhy4oLJwtivgrYcADnLyHznnXbLy9V4FtWkRKMp");
        StringBuilder passphrase = new StringBuilder();
        passphrase.appendCodePoint(0x03d2); // GREEK UPSILON WITH HOOK
        passphrase.appendCodePoint(0x0301); // COMBINING ACUTE ACCENT
        passphrase.appendCodePoint(0x0000); // NULL
        passphrase.appendCodePoint(0x010400); // DESERET CAPITAL LETTER LONG I
        passphrase.appendCodePoint(0x01f4a9); // PILE OF POO
        ECKey key = encryptedKey.decrypt(passphrase.toString());
        assertEquals("5Jajm8eQ22H3pGWLEVCXyvND8dQZhiQhoLJNKjYXk9roUFTMSZ4", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_compression_noEcMultiply_test1() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PYUYP8xxyeB1mgXyWzktgSNYvhky9meidrwrwr9AJhUUvdCkiZEaLqZd8");
        ECKey key = encryptedKey.decrypt("TestingOneTwoThree");
        assertEquals("L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_compression_noEcMultiply_test2() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PYRGeevePcNUT9DZPhkbEHcTkLjcQ2WXFFppseU5GuLFQ9CpTo2w9hL9M");
        ECKey key = encryptedKey.decrypt("Satoshi");
        assertEquals("KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_ecMultiply_noCompression_noLotAndSequence_test1() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PfLGsH7QGsX1JwD27uphyTXZ6BcGj9WeCXMAemwuTTYNetkgU6DvXMdDH");
        ECKey key = encryptedKey.decrypt("TestingOneTwoThree");
        assertEquals("5JPTjf1AdZaCugxm4jj1frqTzyyS4HG77AW64EnfsbLSG23LMqV", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_ecMultiply_noCompression_noLotAndSequence_test2() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PfQdB6AHNGt2vUncFiGu6Cis81u7wnF8W7W9zVafVC9qPyyCz5qxnBQRn");
        ECKey key = encryptedKey.decrypt("Satoshi");
        assertEquals("5JaXa7ci6DJ3RRioDqCKvXD8b1dCuuapueMtrLxxZFTmNZDKazC", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_ecMultiply_noCompression_lotAndSequence_test1() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PgH4ewAXih5RMFirBZM1GM4DpY8oWA3PcyM1tVgLaoUpoxhGcuu3S1urz");
        ECKey key = encryptedKey.decrypt("MOLON LABE");
        assertEquals("5Kf24de8728mYwjkBF28eceTB4mzUMkib4Ba7Qmgdh11XZRxYJx", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_ecMultiply_noCompression_lotAndSequence_test2() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PgLkr65WqNMxhhqXHUmZ4Dm5KTAHE8aCUVDNr8mETmMfmcX4ZnWMxQQD6");
        ECKey key = encryptedKey.decrypt("ΜΟΛΩΝ ΛΑΒΕ");
        assertEquals("5KDJ5n4MBJT8om63JuWvekKmViT311LPwj7S5W4dAP7T64T3co1", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bitcoinpaperwallet_testnet() throws Exception {
        // values taken from bitcoinpaperwallet.com
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(TESTNET,
                "6PRMCrGJibW5UirDXpji9KTVQkZaMugfZSY1aprCEBFDAB8EZ6rSDQRc9Z");
        ECKey key = encryptedKey.decrypt("password");
        assertEquals("93MLfjbY6ugAsLeQfFY6zodDa8izgm1XAwA9cpMbUTwLkDitopg", key.getPrivateKeyEncoded(TESTNET)
                .toString());
    }

    @Test
    public void bitaddress_testnet() throws Exception {
        // values taken from bitaddress.org
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(TESTNET,
                "6PfXFP3sQ2DaZoaq9aqfs3twinbvJFkZX6LsthRNyhrNtNy9G1KDvdPW1D");
        ECKey key = encryptedKey.decrypt("password");
        assertEquals("92BVV3drZwXZzq8Hzz58ZzjjtDjbudgyp8vt4hL2Y9FmktDKUjh", key.getPrivateKeyEncoded(TESTNET)
                .toString());
    }

    @Test(expected = BadPassphraseException.class)
    public void badPassphrase() throws Exception {
        BIP38PrivateKey encryptedKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg");
        encryptedKey.decrypt("BAD");
    }

    @Test(expected = AddressFormatException.InvalidDataLength.class)
    public void fromBase58_invalidLength() {
        String base58 = Base58.encodeChecked(1, new byte[16]);
        BIP38PrivateKey.fromBase58(null, base58);
    }

    @Test
    public void testJavaSerialization() throws Exception {
        BIP38PrivateKey testKey = BIP38PrivateKey.fromBase58(TESTNET,
                "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testKey);
        BIP38PrivateKey testKeyCopy = (BIP38PrivateKey) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(testKey, testKeyCopy);

        BIP38PrivateKey mainKey = BIP38PrivateKey.fromBase58(MAINNET,
                "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainKey);
        BIP38PrivateKey mainKeyCopy = (BIP38PrivateKey) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(mainKey, mainKeyCopy);
    }

    @Test
    public void cloning() throws Exception {
        BIP38PrivateKey a = BIP38PrivateKey.fromBase58(TESTNET, "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb");
        // TODO: Consider overriding clone() in BIP38PrivateKey to narrow the type
        BIP38PrivateKey b = (BIP38PrivateKey) a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() throws Exception {
        String base58 = "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb";
        assertEquals(base58, BIP38PrivateKey.fromBase58(MAINNET, base58).toBase58());
    }
}
