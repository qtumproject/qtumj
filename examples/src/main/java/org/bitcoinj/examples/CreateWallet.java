package org.bitcoinj.examples;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script.ScriptType;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

public class CreateWallet {
	
	public static final NetworkParameters TESTNET = TestNet3Params.get();

	public static void main(String[] args) throws Exception {
		
		String mnemonicWords = "orchard clay room pencil stock token student vicious ginger order lift delay";
		DeterministicSeed seed = new DeterministicSeed(mnemonicWords, null, "", Utils.currentTimeSeconds());
		
		Wallet wallet = Wallet.fromSeed(TESTNET, seed, ScriptType.P2PKH);
		System.out.println(wallet.currentReceiveAddress());
		
		ECKey key = DumpedPrivateKey.fromBase58(TESTNET, "cPGDBUuUwoJNjGuWVGRYHnitntwUbzVMx4FzgG4VAVGrHEwwmLKY").getKey();
		Wallet wallet2 = Wallet.createBasic(TESTNET);
		wallet2.importKey(key);
		System.out.println(wallet2.isAddressMine(LegacyAddress.fromBase58(TESTNET, "qMzxdmq3Ueu22TfMmVk9pAcpmrUVjE7nze")));
	}

}
