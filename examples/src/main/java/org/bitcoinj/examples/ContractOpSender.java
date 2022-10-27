package org.bitcoinj.examples;

import com.google.common.util.concurrent.MoreExecutors;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.store.LevelDBBlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.SendRequest;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ContractOpSender {

    public static byte[] getContractAddress(Sha256Hash hash, long voutIndex) {
        byte[] hashBytes = hash.getReversedBytes();
        byte[] hashBytesWithVoutIndex = new byte[hashBytes.length + 4];
        System.arraycopy(hashBytes, 0, hashBytesWithVoutIndex, 0, hashBytes.length);
        hashBytesWithVoutIndex[hashBytesWithVoutIndex.length - 4] = Long.valueOf(voutIndex).byteValue();
        return Utils.sha256hash160(hashBytesWithVoutIndex);
    }

    public static void main(String[] args) throws Exception {
        NetworkParameters params = TestNet3Params.get();
        try {
            // Import the private key to a fresh wallet.
            Wallet wallet = Wallet.createDeterministic(params, Script.ScriptType.P2PKH);
            ArrayList<ECKey> keys = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
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
                System.out.println("Address from private key (" + i + ") is: " + LegacyAddress.fromKey(params, key) + " - " + key.getPublicKeyAsHex());

                keys.add(key);
                wallet.importKey(key);
            }

            File f = new File("leveldbstore");
            f.delete();

            Context context = new Context(params);
            LevelDBBlockStore blockStore = new LevelDBBlockStore(context, f);

            // final MemoryBlockStore blockStore = new MemoryBlockStore(params);
            BlockChain chain = new BlockChain(params, wallet, blockStore);

            System.out.println("Creating peer group");
            final PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addWallet(wallet);
            peerGroup.addAddress(new PeerAddress(params, InetAddress.getLocalHost()));
            System.out.println("Start async");
            peerGroup.startAsync();
            System.out.println("Downloading blockchain");
            peerGroup.downloadBlockChain();

            byte[] code = Utils.HEX.decode("608060405234801561001057600080fd5b5061021c806100206000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c80633de4eb171461004657806343ae80d3146100645780638588b2c51461008f575b600080fd5b61004e6100b0565b60405161005b919061017c565b60405180910390f35b6100776100723660046101b7565b6100f6565b6040516001600160a01b03909116815260200161005b565b6100a261009d3660046101b7565b610116565b60405190815260200161005b565b6100b861015d565b604080516102008101918290529060009060109082845b81546001600160a01b031681526001909101906020018083116100cf575050505050905090565b6000816010811061010657600080fd5b01546001600160a01b0316905081565b6000600f82111561012657600080fd5b336000836010811061013a5761013a6101d0565b0180546001600160a01b0319166001600160a01b03929092169190911790555090565b6040518061020001604052806010906020820280368337509192915050565b6102008101818360005b60108110156101ae5781516001600160a01b0316835260209283019290910190600101610186565b50505092915050565b6000602082840312156101c957600080fd5b5035919050565b634e487b7160e01b600052603260045260246000fdfea264697066735822122030627c28006c8c423df956d43c0dfe9d3942dc066cfba338ceedb7aea227c2d264736f6c63430008090033");
            SendRequest req = SendRequest.createContract(params, code);

            Wallet.SendResult result = wallet.sendCoins(req);
            result.broadcastComplete.addListener(() -> {
                System.out.println("sent tx: " + result.tx.getTxId());
                for (long i = 0; i < result.tx.getOutputs().size(); i++) {
                    TransactionOutput output = result.tx.getOutput(i);
                    if (ScriptPattern.isOpCreate(output.getScriptPubKey())) {
                        TransactionOutPoint outPoint = output.getOutPointFor();
                        System.out.println("output: " + i + " => " + Utils.HEX.encode(getContractAddress(outPoint.getHash(), i)));
                    }
                }
            }, MoreExecutors.directExecutor());
            System.exit(0);
            Thread.sleep(5000);

            byte[] code2 = Utils.HEX.decode("8588b2c50000000000000000000000000000000000000000000000000000000000000000");
            ContractAddress addr = ContractAddress.fromString("4896733ab72546e526464f45b92a693f98511e2f");
            SendRequest req2 = null;
            if (keys.size() == 1) {
                System.out.println("OP_CALL");
                req2 = SendRequest.callContract(params, addr, code2, 100000L, 400L);
            } else {
                for (int i = 1; i < keys.size(); i++) {
                    if (i > 10) {
                        throw new RuntimeException("Only 10 keys supported in this example");
                    }
                    code2 = Utils.HEX.decode("8588b2c5000000000000000000000000000000000000000000000000000000000000000" + (i-1));
                    System.out.println("OP_SENDER OP_CALL " + i + "/" + keys.size());
                    if (req2 == null) {
                        req2 = SendRequest.callContract(params, keys.get(i), addr, code2, 100000L, 400L);
                    } else {
                        SendRequest.callContract(req2, keys.get(i), addr, code2, 100000L, 400L);
                    }
                }
            }

            Wallet.SendResult result2 = wallet.sendCoins(req2);
            result2.broadcastComplete.addListener(() -> {
                System.out.println("sent 2nd tx: " + result2.tx.getTxId());
            }, MoreExecutors.directExecutor());

            Thread.sleep(5000);
            peerGroup.stopAsync();
            System.exit(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("First arg should be private key in Base58 format. Second argument should be address " +
                    "to send to.");
        }
    }

}
