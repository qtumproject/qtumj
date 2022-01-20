package org.bitcoinj.params;

import java.math.BigInteger;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractQtumNetParams extends NetworkParameters {

    public static final String QTUM_SCHEME = "qtum";
    public static final int REWARD_HALVING_INTERVAL = 985500;

    public static final int TARGET_TIMESPAN_V2 = 4000;
    public static final int INTERVAL_V2 = TARGET_TIMESPAN_V2 / TARGET_SPACING;

    protected boolean powNoRetargeting;
    protected boolean posNoRetargeting;
    protected int targetTimespanV2;
    protected int reducedBlocktimeTimespan;
    protected int targetSpacing;
    protected int reducedBlocktimeTargetSpacing;
    protected int intervalV2;
    protected int stakeTimestampMask;
    protected int reducedBlocktimeStakeTimestampMask;
    protected BigInteger posMaxTarget;
    protected BigInteger qip9POSMaxTarget;
    protected BigInteger reducedBlockTimePosMaxTarget;

    protected long qip9Height;
    protected long reduceBlocktimeHeight;

    private static final Logger log = LoggerFactory.getLogger(AbstractQtumNetParams.class);

    public AbstractQtumNetParams() {
        super();
    }

    /**
     * Checks if we are at a reward halving point.
     * @param height The height of the previous stored block
     * @return If this is a reward halving point
     */
    public final boolean isRewardHalvingPoint(final int height) {
        return ((height + 1) % REWARD_HALVING_INTERVAL) == 0;
    }

    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
            throws VerificationException, BlockStoreException {
        BigInteger newTarget = getNextWorkRequired(storedPrev, nextBlock, blockStore);

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(newTarget);

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    /** @deprecated use {@link TransactionOutput#getMinNonDustValue()} */
    @Override
    @Deprecated
    public Coin getMinNonDustOutput() {
        return Transaction.MIN_NONDUST_OUTPUT;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public String getUriScheme() {
        return QTUM_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }

    @Override
    public BitcoinSerializer getSerializer(boolean parseRetain) {
        return new BitcoinSerializer(this, parseRetain);
    }

    @Override
    public int getProtocolVersionNum(ProtocolVersion version) {
        return version.getQtumProtocolVersion();
    }

    public int getTargetTimespanV2() {
        return targetTimespanV2;
    }

    public int getTimespan(int height) {
        if (height < qip9Height) {
            return targetTimespan;
        } else if (height < reduceBlocktimeHeight) {
            return targetTimespanV2;
        } else {
            return reducedBlocktimeTimespan;
        }
    }

    @Override
    public int getInterval(int height) {
        int targetTimespan = getTimespan(height);
        int targetSpacing = getTargetSpacing(height);
        return targetTimespan / targetSpacing;
    }

    @Override
    public int getSpendableCoinbaseDepth(int height) {
        if (height < reduceBlocktimeHeight) {
            return spendableCoinbaseDepth;
        } else {
            return reducedBlockTimeSpendableCoinbaseDepth;
        }
    }

    public int getStakeTimestampMask(int height) {
        if (height < reduceBlocktimeHeight) {
            return stakeTimestampMask;
        } else {
            return reducedBlocktimeStakeTimestampMask;
        }
    }

    private BigInteger getLimit(int height, boolean isProofOfStake) {
        if (isProofOfStake) {
            if (height < qip9Height) {
                return posMaxTarget;
            } else if (height < reduceBlocktimeHeight) {
                return qip9POSMaxTarget;
            } else {
                return reducedBlockTimePosMaxTarget;
            }
        } else {
            return maxTarget;
        }
    }

    private int getTargetSpacing(int height) {
        if (height < reduceBlocktimeHeight) {
            return targetSpacing;
        } else {
            return reducedBlocktimeTargetSpacing;
        }
    }

    private BigInteger getNextWorkRequired(StoredBlock last, Block nextBlock, BlockStore blockStore) throws BlockStoreException {
        final boolean isPOS = nextBlock.isPOS();
        BigInteger maxTarget = getLimit(last.getHeight() + 1, isPOS);

        StoredBlock prev = getLastSameTypeBlock(last, blockStore, isPOS);
        if (prev.getHeight() == 0) { // last block was first block
            return maxTarget;
        }

        StoredBlock prevPrev = getLastSameTypeBlock(prev.getPrev(blockStore), blockStore, isPOS);
        if (prevPrev.getHeight() == 0) { // last block was second block
            return maxTarget;
        }

        BigInteger newTarget = Utils.decodeCompactBits(prev.getHeader().getDifficultyTarget());
        if (isPOS && posNoRetargeting) {
            return newTarget;
        }
        if (!isPOS && powNoRetargeting) {
            return newTarget;
        }

        long actualSpacing = prev.getHeader().getTimeSeconds() - prevPrev.getHeader().getTimeSeconds();

        int targetSpacing = getTargetSpacing(last.getHeight() + 1);
        int interval = getInterval(last.getHeight() + 1);
        if (last.getHeight() + 1 < qip9Height) {
            if (actualSpacing < 0) {
                actualSpacing = targetSpacing;
            }
            if (actualSpacing > (long)targetSpacing * 10) {
                actualSpacing = (long)targetSpacing * 10;
            }
            newTarget = newTarget.multiply(BigInteger.valueOf((interval - 1) * (long)targetSpacing + (actualSpacing << 1)));
            newTarget = newTarget.divide(BigInteger.valueOf((interval + 1) * (long)targetSpacing));
        } else {
            if (actualSpacing < 0) {
                actualSpacing = targetSpacing;
            }
            if (actualSpacing > (long)targetSpacing * 20) {
                actualSpacing = (long)targetSpacing * 20;
            }
            int stakeTimestampMask = getStakeTimestampMask(last.getHeight() + 1);
            newTarget = mulExp(newTarget, 2 * (actualSpacing - targetSpacing) / (stakeTimestampMask + 1), (interval + 1) * (long)targetSpacing / (stakeTimestampMask + 1));
        }

        if (newTarget.compareTo(BigInteger.ZERO) <= 0 || newTarget.compareTo(maxTarget) > 0) {
            log.info("Difficulty hit limit: {}", newTarget.toString(16));
            newTarget = maxTarget;
        }
        return newTarget;

    }

    private StoredBlock getLastSameTypeBlock(StoredBlock last, BlockStore blockStore, boolean isProofOfStake) throws BlockStoreException {
        StoredBlock prev = last;
        do {
            if (prev.getHeader().isPOS() == isProofOfStake) {
                return prev;
            }
            last = prev;
            prev = last.getPrev(blockStore);
        } while (prev != null);
        return last;
    }

    // calculates a * exp(p/q) where |p/q| is small
    private BigInteger mulExp(BigInteger a, long p, long q) {
        BigInteger result = a;
        long n = 0;
        while (a.compareTo(BigInteger.ZERO) != 0) {
            n++;
            a = a.multiply(BigInteger.valueOf(p)).divide(BigInteger.valueOf(q * n));
            result = result.add(a);
        }
        return result;
    }

}
