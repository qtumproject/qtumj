package org.bitcoinj.examples;

import org.bitcoinj.core.*;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.DefaultCoinSelector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Example class that filters inputs from specified addresses
 * This is used in the QRC20 example to prevent spending OP_SENDER inputs
 */
public class ExcludedCoinSelector implements CoinSelector {
    protected CoinSelector delegate;
    protected final HashSet<Address> excludedAddresses = new HashSet<>();
    protected NetworkParameters params;

    public ExcludedCoinSelector(NetworkParameters params, Collection<Address> excludedAddresses) {
        init(params, DefaultCoinSelector.get(), excludedAddresses);
    }

    public ExcludedCoinSelector(NetworkParameters params, CoinSelector delegate, Collection<Address> excludedAddresses) {
        init(params, delegate, excludedAddresses);
    }

    private void init(NetworkParameters params, CoinSelector delegate, Collection<Address> excludedAddresses) {
        if (delegate == null) {
            throw new NullPointerException();
        }
        this.delegate = delegate;
        if (excludedAddresses == null) {
            throw new NullPointerException();
        }
        this.excludedAddresses.addAll(excludedAddresses);
        if (params == null) {
            throw new NullPointerException();
        }
        Address[] a = new Address[]{};
        a = excludedAddresses.toArray(a);
        for (int i = 0; i < a.length; i++) {
            System.out.println("ExcludedCoinSelector: " + a[i].toString());
        }
        this.params = params;
    }

    @Override
    public CoinSelection select(Coin target, List<TransactionOutput> candidates) {
        Iterator<TransactionOutput> iter = candidates.iterator();
        while (iter.hasNext()) {
            TransactionOutput output = iter.next();
            try {
                Address addr = output.getScriptPubKey().getToAddress(params);
                if (excludedAddresses.contains(addr)) {
                    iter.remove();
                    System.out.println("Removing output " + addr.toString());
                } else {
                    System.out.println("Allowing output " + addr.toString());
                }
            } catch (ScriptException e) {
                // Not a script we care about
            }
        }
        return delegate.select(target, candidates);
    }
}
