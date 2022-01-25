package org.bitcoinj.examples;

import com.google.common.util.concurrent.MoreExecutors;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.LevelDBFullPrunedBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.SendRequest;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class QRC20TransferOpSender {

    public static String toHex(Address address) {
        return Utils.HEX.encode(address.getHash());
    }

    public static void main(String[] args) throws Exception {
        NetworkParameters regTestParams = RegTestParams.get();
        NetworkParameters testNetParams = TestNet3Params.get();
        NetworkParameters mainNetParams = MainNetParams.get();
        try {
            NetworkParameters params;
            String folderSuffix = "";
            switch (args[0].toLowerCase()) {
                case "mainnet":
                    params = mainNetParams;
                    folderSuffix = ".mainnet";
                    break;
                case "testnet":
                    params = testNetParams;
                    folderSuffix = ".testnet";
                    break;
                case "regtest":
                    params = regTestParams;
                    folderSuffix = ".regtest";
                    break;
                default:
                    throw new IllegalArgumentException("expected network type as first argument (mainnet/testnet/regtest)");
            }

            String contractAddress = args[1];

            Wallet wallet = Wallet.createBasic(params);
            List<ECKey> keys = new ArrayList<>();
            List<Address> opSenderAddresses = new ArrayList<>();
            Address changeAddress = null;

            for (int i = 2; i < args.length; i++) {
                // Decode the private key from Satoshis Base58 variant. If 51 characters long then it's from Bitcoins
                // dumpprivkey command and includes a version byte and checksum, or if 52 characters long then it has
                // compressed pub key. Otherwise assume it's a raw key.
                ECKey key;
                if (args[i].length() == 51 || args[i].length() == 52) {
                    DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, args[i]);
                    key = dumpedPrivateKey.getKey();
                } else {
                    BigInteger privKey = Base58.decodeToBigInteger(args[i]);
                    key = ECKey.fromPrivate(privKey);
                }
                String hexAddress = Utils.HEX.encode(LegacyAddress.fromKey(params, key).getHash());
                System.out.println("Address from private key (" + i + ") is: " + LegacyAddress.fromKey(params, key) + " / 0x" + hexAddress);

                keys.add(key);
                wallet.importKey(key);
                if (i == 2) {
                    changeAddress = LegacyAddress.fromKey(params, key);
                } else {
                    opSenderAddresses.add(LegacyAddress.fromKey(params, key));
                }
            }

            ECKey fromKey = keys.get(1);
            String toAddress = args[4];

            ExcludedCoinSelector excludeOpSenderInputsCoinSelector = new ExcludedCoinSelector(params, opSenderAddresses);

            String fileName = "leveldbstore" + folderSuffix;
            System.out.println("Creating file leveldbstore " + fileName);

            LevelDBFullPrunedBlockStore blockStore = new LevelDBFullPrunedBlockStore(params, fileName, 1000, 100 * 1024 * 10241, 10 * 1024 * 1024, 100000, true, Integer.MAX_VALUE);
            // final MemoryBlockStore blockStore = new MemoryBlockStore(params);

            BlockChain chain = new BlockChain(params, wallet, blockStore);

            final PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addWallet(wallet);
            peerGroup.addAddress(new PeerAddress(params, InetAddress.getLocalHost()));
            peerGroup.startAsync();
            System.out.println("Downloading blockchain");
            peerGroup.downloadBlockChain();

            String from = toHex(LegacyAddress.fromKey(params, fromKey));
            String to = toHex(LegacyAddress.fromBase58(params, toAddress));

            ContractAddress qrc20TokenContract = ContractAddress.fromString(contractAddress);
            long gasLimit = 55000L;
            long gasPrice = 40L;

            byte[] transfer = Utils.HEX.decode(
                    "a9059cbb" +
                            "000000000000000000000000" + to +
                            "0000000000000000000000000000000000000000000000000000000005f5e100"
            );
            // creates OP_SENDER QRC20 Transfer, exclude fromKey parameter to do a normal OP_CALL
            SendRequest req = SendRequest.callContract(params, fromKey, qrc20TokenContract, transfer, gasLimit, gasPrice);
            // exclude OP_SENDER accounts as inputs
            req.coinSelector = excludeOpSenderInputsCoinSelector;
            req.changeAddress = changeAddress;

            System.out.println("Sending 1 QRC20 token(" + contractAddress + ") from " + from + " to " + to + " with " + changeAddress.toString() + " paying gas");
            for (int i = 5; i > 0; i--) {
                System.out.println("Sleeping " + i + " seconds, CTRL-C to cancel");
                Thread.sleep(1000);
            }

            final CountDownLatch tx2Completed = new CountDownLatch(1);
            Wallet.SendResult result2 = null;
            for (int i = 0; i < 7; i++) {
                try {
                    result2 = wallet.sendCoins(req);
                    break;
                } catch (InsufficientMoneyException e) {
                    // change output isn't in a block yet
                    System.out.println("Can not send coins: " + e.getLocalizedMessage());
                    System.out.println("Waiting for block to be mined for change output");
                    Thread.sleep(10 * 1000);
                    peerGroup.downloadBlockChain();
                }
            }

            if (result2 == null) {
                System.out.println("Giving up waiting for spendable outputs");
                peerGroup.stopAsync();
                blockStore.close();
                Thread.sleep(10 * 1000);
                System.exit(0);
            }

            Sha256Hash resultTxId = result2.tx.getTxId();
            result2.broadcastComplete.addListener(() -> {
                System.out.println("transaction sent: " + resultTxId);
                tx2Completed.countDown();
            }, MoreExecutors.directExecutor());

            System.out.println("Waiting for broadcast transaction");
            boolean successfullyBroadcast2nd = tx2Completed.await(30, TimeUnit.SECONDS);
            if (!successfullyBroadcast2nd) {
                System.out.println("Failed to broadcast transaction");
                peerGroup.stopAsync();
                blockStore.close();
                Thread.sleep(10 * 1000);
                System.exit(1);
            }

            System.out.println("Successfully broadcast transaction");

            peerGroup.stopAsync();
            System.out.println("Finished, closing blockchain.");
            blockStore.close();
            Thread.sleep(10 * 1000);
            System.exit(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("[mainnet|testnet|regtest] qrc20ContractAddress opSpenderPrivateKey fromPrivateKey toBase58Address");
        }
    }

}
