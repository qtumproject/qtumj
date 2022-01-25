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


public class QRC20 {

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

            Wallet wallet = Wallet.createBasic(params);
            List<ECKey> keys = new ArrayList<>();
            List<Address> opSenderAddresses = new ArrayList<>();
            Address changeAddress = null;

            for (int i = 1; i < args.length; i++) {
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
                if (i == 1) {
                    changeAddress = LegacyAddress.fromKey(params, key);
                } else {
                    opSenderAddresses.add(LegacyAddress.fromKey(params, key));
                }
            }

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

            String from = toHex(LegacyAddress.fromKey(params, keys.get(0)));

            /**
             * pragma solidity ^0.8.0;
             *
             * contract SafeMath {
             *
             *     constructor() {
             *     }
             *
             *     function safeAdd(uint256 _x, uint256 _y) pure internal returns (uint256) {
             *         uint256 z = _x + _y;
             *         assert(z >= _x);
             *         return z;
             *     }
             *
             *     function safeSub(uint256 _x, uint256 _y) pure internal returns (uint256) {
             *         assert(_x >= _y);
             *         return _x - _y;
             *     }
             *
             *     function safeMul(uint256 _x, uint256 _y) pure internal returns (uint256) {
             *         uint256 z = _x * _y;
             *         assert(_x == 0 || z / _x == _y);
             *         return z;
             *     }
             *
             * }
             *
             * contract QRC20Token is SafeMath {
             *     string public constant standard = 'Token 0.1';
             *     uint8 public constant decimals = 8; // it's recommended to set decimals to 8 in QTUM
             *
             *     // you need change the following three values
             *     string public constant name = 'QRC TEST';
             *     string public constant symbol = 'QTC';
             *     //Default assumes totalSupply can't be over max (2^256 - 1).
             *     //you need multiply 10^decimals by your real total supply.
             *     uint256 public totalSupply = 10**9 * 10**uint256(decimals);
             *
             *     mapping (address => uint256) public balanceOf;
             *     mapping (address => mapping (address => uint256)) public allowance;
             *
             *     event Transfer(address indexed _from, address indexed _to, uint256 _value);
             *     event Approval(address indexed _owner, address indexed _spender, uint256 _value);
             *
             *     constructor() {
             *         balanceOf[msg.sender] = totalSupply;
             *     }
             *
             *     // validates an address - currently only checks that it isn't null
             *     modifier validAddress(address _address) {
             *         require(_address != address(0x0));
             *         _;
             *     }
             *
             *     function transfer(address _to, uint256 _value)
             *     public
             *     validAddress(_to)
             *     returns (bool success)
             *     {
             *         balanceOf[msg.sender] = safeSub(balanceOf[msg.sender], _value);
             *         balanceOf[_to] = safeAdd(balanceOf[_to], _value);
             *         emit Transfer(msg.sender, _to, _value);
             *         return true;
             *     }
             *
             *     function transferFrom(address _from, address _to, uint256 _value)
             *     public
             *     validAddress(_from)
             *     validAddress(_to)
             *     returns (bool success)
             *     {
             *         allowance[_from][msg.sender] = safeSub(allowance[_from][msg.sender], _value);
             *         balanceOf[_from] = safeSub(balanceOf[_from], _value);
             *         balanceOf[_to] = safeAdd(balanceOf[_to], _value);
             *         emit Transfer(_from, _to, _value);
             *         return true;
             *     }
             *
             *     function approve(address _spender, uint256 _value)
             *     public
             *     validAddress(_spender)
             *     returns (bool success)
             *     {
             *         // To change the approve amount you first have to reduce the addresses`
             *         //  allowance to zero by calling `approve(_spender, 0)` if it is not
             *         //  already 0 to mitigate the race condition described here:
             *         //  https://github.com/ethereum/EIPs/issues/20#issuecomment-263524729
             *         require(_value == 0 || allowance[msg.sender][_spender] == 0);
             *         allowance[msg.sender][_spender] = _value;
             *         emit Approval(msg.sender, _spender, _value);
             *         return true;
             *     }
             *
             *     // disable pay QTUM to this contract
             *     receive() external payable {
             *         revert();
             *     }
             * }
             */
            byte[] QRC20TokenByteCode = Utils.HEX.decode("60806040526100106008600a610141565b61001e90633b9aca00610154565b60005534801561002d57600080fd5b50600080543382526001602052604090912055610173565b634e487b7160e01b600052601160045260246000fd5b600181815b8085111561009657816000190482111561007c5761007c610045565b8085161561008957918102915b93841c9390800290610060565b509250929050565b6000826100ad5750600161013b565b816100ba5750600061013b565b81600181146100d057600281146100da576100f6565b600191505061013b565b60ff8411156100eb576100eb610045565b50506001821b61013b565b5060208310610133831016604e8410600b8410161715610119575081810a61013b565b610123838361005b565b806000190482111561013757610137610045565b0290505b92915050565b600061014d838361009e565b9392505050565b600081600019048311821515161561016e5761016e610045565b500290565b6106e0806101826000396000f3fe6080604052600436106100855760003560e01c806306fdde0314610094578063095ea7b3146100de57806318160ddd1461010e57806323b872dd14610132578063313ce567146101525780635a3b7e421461017957806370a08231146101ae57806395d89b41146101db578063a9059cbb1461020a578063dd62ed3e1461022a57600080fd5b3661008f57600080fd5b600080fd5b3480156100a057600080fd5b506100c860405180604001604052806008815260200167145490c8151154d560c21b81525081565b6040516100d5919061050a565b60405180910390f35b3480156100ea57600080fd5b506100fe6100f936600461057b565b610262565b60405190151581526020016100d5565b34801561011a57600080fd5b5061012460005481565b6040519081526020016100d5565b34801561013e57600080fd5b506100fe61014d3660046105a5565b610315565b34801561015e57600080fd5b50610167600881565b60405160ff90911681526020016100d5565b34801561018557600080fd5b506100c860405180604001604052806009815260200168546f6b656e20302e3160b81b81525081565b3480156101ba57600080fd5b506101246101c93660046105e1565b60016020526000908152604090205481565b3480156101e757600080fd5b506100c86040518060400160405280600381526020016251544360e81b81525081565b34801561021657600080fd5b506100fe61022536600461057b565b61042d565b34801561023657600080fd5b506101246102453660046105fc565b600260209081526000928352604080842090915290825290205481565b6000826001600160a01b03811661027857600080fd5b8215806102a657503360009081526002602090815260408083206001600160a01b0388168452909152902054155b6102af57600080fd5b3360008181526002602090815260408083206001600160a01b03891680855290835292819020879055518681529192917f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92591015b60405180910390a35060019392505050565b6000836001600160a01b03811661032b57600080fd5b836001600160a01b03811661033f57600080fd5b6001600160a01b038616600090815260026020908152604080832033845290915290205461036d90856104c8565b6001600160a01b0387166000818152600260209081526040808320338452825280832094909455918152600190915220546103a890856104c8565b6001600160a01b0380881660009081526001602052604080822093909355908716815220546103d790856104eb565b6001600160a01b03808716600081815260016020526040908190209390935591519088169060008051602061068b833981519152906104199088815260200190565b60405180910390a350600195945050505050565b6000826001600160a01b03811661044357600080fd5b3360009081526001602052604090205461045d90846104c8565b33600090815260016020526040808220929092556001600160a01b0386168152205461048990846104eb565b6001600160a01b03851660008181526001602052604090819020929092559051339060008051602061068b833981519152906103039087815260200190565b6000818310156104da576104da61062f565b6104e4828461065b565b9392505050565b6000806104f88385610672565b9050838110156104e4576104e461062f565b600060208083528351808285015260005b818110156105375785810183015185820160400152820161051b565b81811115610549576000604083870101525b50601f01601f1916929092016040019392505050565b80356001600160a01b038116811461057657600080fd5b919050565b6000806040838503121561058e57600080fd5b6105978361055f565b946020939093013593505050565b6000806000606084860312156105ba57600080fd5b6105c38461055f565b92506105d16020850161055f565b9150604084013590509250925092565b6000602082840312156105f357600080fd5b6104e48261055f565b6000806040838503121561060f57600080fd5b6106188361055f565b91506106266020840161055f565b90509250929050565b634e487b7160e01b600052600160045260246000fd5b634e487b7160e01b600052601160045260246000fd5b60008282101561066d5761066d610645565b500390565b6000821982111561068557610685610645565b50019056feddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3efa26469706673582212204f0b7715a86df24f99b5c5614a27135476c273daed593651ad8a700c8be258cd64736f6c634300080b0033");
            SendRequest req = null;
            if (keys.size() == 1) {
                System.out.println("Deploying contract with OP_CREATE");
                req = SendRequest.createContract(params, QRC20TokenByteCode);
            } else {
                System.out.println("Deploying contract with OP_SENDER OP_CREATE from " + from);
                req = SendRequest.createContract(params, keys.get(0), QRC20TokenByteCode, 2500000L, 40L);
            }

            // exclude OP_SENDER accounts as inputs
            req.coinSelector = excludeOpSenderInputsCoinSelector;
            req.changeAddress = changeAddress;

            final CountDownLatch txCompleted = new CountDownLatch(1);
            Wallet.SendResult result = wallet.sendCoins(req);
            final List<ContractAddress> deployedContracts = new ArrayList<>();
            result.broadcastComplete.addListener(() -> {
                System.out.println("1st transaction sent: " + result.tx.getTxId());
                List<ContractAddress> contracts = result.tx.getOpCreateContractAddresses();
                deployedContracts.addAll(contracts);
                for (ContractAddress contractAddress : contracts) {
                    System.out.println("Deployed contract at " + Utils.HEX.encode(contractAddress.getBytes()));
                }
                txCompleted.countDown();
            }, MoreExecutors.directExecutor());

            System.out.println("Waiting for transaction to be broadcast");
            boolean successfullyBroadcast = txCompleted.await(30, TimeUnit.SECONDS);
            if (!successfullyBroadcast) {
                System.out.println("Failed to broadcast transaction");
                blockStore.close();
                Thread.sleep(10 * 1000);
                System.exit(1);
            }

            ContractAddress qrc20TokenContract = deployedContracts.get(0);

            String contract = "tokens(" + Utils.HEX.encode(qrc20TokenContract.getBytes()) + ")";
            long gasLimit = 55000L;
            long gasPrice = 40L;

            for (int transferCount = 0; transferCount < 5; transferCount++) {
                // transfer
                // 0xa9059cbb
                // 0000000000000000000000001b42cb6d7c1c7187d0b4bba702377caa26f083a2 // address
                // 000000000000000000000000000000000000000000000003bd913e6c1df40000 // 6.9*10^19
                byte[] transfer = Utils.HEX.decode(
                        "a9059cbb" +
                                "000000000000000000000000" + from +
                                "0000000000000000000000000000000000000000000000000000000005f5e100"
                );
                SendRequest req2 = null;
                if (keys.size() == 1) {
                    System.out.println("Sending " + contract + " from " + from + " => " + from + " with OP_CALL");
                    req2 = SendRequest.callContract(params, qrc20TokenContract, transfer, gasLimit, gasPrice);
                } else {
                    String last = toHex(LegacyAddress.fromKey(params, keys.get(keys.size() - 1)));
                    for (int i = 1; i < keys.size() - 1; i++) {
                        // for (int i = 1; i < 2; i++) {
                        if (i > 10) {
                            throw new RuntimeException("Only 10 keys supported in this example");
                        }

                        // transfer from key(0) => key(i) => key(last)
                        String to = toHex(LegacyAddress.fromKey(params, keys.get(i)));
                        transfer = Utils.HEX.decode(
                                "a9059cbb" +
                                        "000000000000000000000000" + to +
                                        "0000000000000000000000000000000000000000000000000000000005f5e100"
                        );

                        System.out.println("Sending " + contract + " " + from + " => " + to + " with OP_SENDER OP_CALL");
                        if (req2 == null) {
                            req2 = SendRequest.callContract(params, keys.get(0), qrc20TokenContract, transfer, gasLimit, gasPrice);
                        } else {
                            SendRequest.callContract(req2, keys.get(0), qrc20TokenContract, transfer, gasLimit, gasPrice);
                        }

                        transfer = Utils.HEX.decode(
                                "a9059cbb" +
                                        "000000000000000000000000" + last +
                                        "0000000000000000000000000000000000000000000000000000000005f5e100"
                        );

                        System.out.println("Sending " + contract + " " + to + " => " + last + " with OP_SENDER OP_CALL");
                        SendRequest.callContract(req2, keys.get(i), qrc20TokenContract, transfer, gasLimit, gasPrice);
                    }
                }

                // exclude OP_SENDER accounts as inputs
                req2.coinSelector = excludeOpSenderInputsCoinSelector;
                req2.changeAddress = changeAddress;

                final CountDownLatch tx2Completed = new CountDownLatch(1);
                Wallet.SendResult result2 = null;
                for (int i = 0; i < 7; i++) {
                    try {
                        result2 = wallet.sendCoins(req2);
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

                Sha256Hash result2TxId = result2.tx.getTxId();
                int transactionIndex = transferCount + 1;
                result2.broadcastComplete.addListener(() -> {
                    System.out.println(transactionIndex + (transactionIndex == 2 ? "nd":(transactionIndex == 3 ? "rd":"th")) + " transaction sent: " + result2TxId);
                    tx2Completed.countDown();
                }, MoreExecutors.directExecutor());

                System.out.println("Waiting for broadcast transaction #" + transferCount+1);
                boolean successfullyBroadcast2nd = tx2Completed.await(30, TimeUnit.SECONDS);
                if (!successfullyBroadcast2nd) {
                    System.out.println("Failed to broadcast transaction #" + transferCount+1);
                    blockStore.close();
                    Thread.sleep(10 * 1000);
                    System.exit(1);
                }

                System.out.println("Successfully broadcast transaction #" + transferCount+1);
            }

            peerGroup.stopAsync();
            System.out.println("Finished, closing blockchain.");
            blockStore.close();
            Thread.sleep(10 * 1000);
            System.exit(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("First arg should be private key in Base58 format. Second argument should be address " +
                    "to send to.");
        }
    }

}
