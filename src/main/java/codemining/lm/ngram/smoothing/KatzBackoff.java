package codemining.lm.ngram.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.lm.ILanguageModel;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.lm.ngram.Trie.TrieNode;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultiset;

/**
 * An implementation of Katz backoff & smoothing scheme.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class KatzBackoff extends AbstractNGramLM {

	private static class NodeOrder {
		TrieNode<Long> node;
		int order;
	}

	public static final long NO_DISCOUNT_THRESHOLD = 10;

	private static final long serialVersionUID = 8858981913051295954L;

	// A cache for each ngram order
	SortedMap<Integer, Map<Long, Double>> katzCounts;

	// Contains the counts of counts
	private final Map<Integer, Map<Long, Long>> countOfCounts = new TreeMap<Integer, Map<Long, Long>>();

	private static final Logger LOGGER = Logger.getLogger(KatzBackoff.class
			.getName());

	public KatzBackoff(final AbstractNGramLM original) {
		super(original);
		computeKatzCountsOfCounts();
		katzCounts = Maps.newTreeMap();
		for (int i = 1; i <= getN(); i++) {
			computeKatzCounts(i);
		}

	}

	@Override
	public void addFromSentence(final List<String> sentence,
			final boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}

	@Override
	protected void addNgram(final NGram<String> ngram, final boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}

	@Override
	public void addSentences(final Collection<List<String>> sentenceSet,
			final boolean addNewVocabulary) {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}

	private double computeGamma(final NGram<String> ngram) {
		// First get all ngrams with the same prefix. Exclude UNK.
		final NGram<String> prefix = ngram.getPrefix();
		final double nominator = getResidualProbability(prefix);

		if (nominator == 0) {
			// No probability is remaining
			return Math.pow(10, -10);
		}

		final double denominator = getResidualProbability(prefix.getSuffix());

		if (denominator == 0) {
			return 1;
		}
		return nominator / denominator;
	}

	/**
	 * @param order
	 * @return
	 */
	private void computeKatzCounts(final int order) {
		final Map<Long, Long> countOfCountsForOrder = countOfCounts.get(order);

		final TreeMap<Long, Double> ngramKatzCount = Maps.newTreeMap();

		final double discount = (NO_DISCOUNT_THRESHOLD + 1.)
				* countOfCountsForOrder.get(NO_DISCOUNT_THRESHOLD + 1)
				/ ((double) countOfCountsForOrder.get(1L));
		checkArgument(discount > 0 && discount < 1,
				"Discount must be betwee 0 and 1, but is " + discount);

		for (long originalCount = 0; originalCount <= NO_DISCOUNT_THRESHOLD; originalCount++) {
			/*
			 * checkArgument(countOfCountsForOrder.containsKey(originalCount +
			 * 1)); final double N_c_1 = countOfCountsForOrder.get(originalCount
			 * + 1);
			 * 
			 * final double N_c; if (originalCount > 0) {
			 * checkArgument(countOfCountsForOrder.containsKey(originalCount));
			 * N_c = countOfCountsForOrder.get(originalCount); } else { long sum
			 * = 0; for (long i = 1; i <= NO_DISCOUNT_THRESHOLD; i++) { sum +=
			 * countOfCountsForOrder.get(i); } N_c = sum; }
			 * 
			 * /*final double nominatorPt1 = (originalCount + 1.) * N_c_1 / N_c;
			 * 
			 * final double katzCount = (nominatorPt1 - originalCount *
			 * discount) / (1. - discount);
			 */
			// absolute discounting scheme, because the other is not working
			// unless we have a very good estimate
			final double katzCount;
			if (originalCount == 0) {
				katzCount = discount;
			} else {
				katzCount = originalCount - discount * originalCount

				* 2 / (NO_DISCOUNT_THRESHOLD * (NO_DISCOUNT_THRESHOLD + 1));
			}
			ngramKatzCount.put(originalCount, katzCount);

			checkArgument(
					(originalCount - 1 < katzCount && katzCount <= originalCount + 1),
					"smoothed katz count is between original and (original-1), unless it's zero. Original "
							+ originalCount + " but katz " + katzCount);

		}

		katzCounts.put(order, ImmutableSortedMap.copyOf(ngramKatzCount));
	}

	/**
	 * Compute the counts of counts. i.e. n_i for each ngram order.
	 * 
	 * @param order
	 */
	private void computeKatzCountsOfCounts() {

		for (int i = 1; i <= getN(); i++) {
			final Map<Long, Long> ngramOrderCounts = Maps.newTreeMap();
			countOfCounts.put(i, ngramOrderCounts);
			for (long j = 1; j <= NO_DISCOUNT_THRESHOLD + 1; j++) {
				ngramOrderCounts.put(j, 0L);
			}
		}

		final ArrayDeque<NodeOrder> toCount = new ArrayDeque<NodeOrder>();
		final ArrayDeque<NodeOrder> unkToCount = new ArrayDeque<NodeOrder>();

		for (final Entry<Long, TrieNode<Long>> entry : trie.getRoot().prods
				.entrySet()) {
			final NodeOrder cnt = new NodeOrder();
			cnt.order = 1;
			cnt.node = entry.getValue();
			if (entry.getKey().equals(trie.getUnkSymbolId())) {
				unkToCount.push(cnt);
			} else {
				toCount.push(cnt);
			}

		}

		while (!toCount.isEmpty()) {
			final NodeOrder current = toCount.pop();

			// Count Update
			final Map<Long, Long> countsForOrder = countOfCounts
					.get(current.order);
			Long currentCount = countsForOrder.get(current.node.count);
			if (current.node.count > NO_DISCOUNT_THRESHOLD + 1) {
				continue; // We don't care, it's too large.
			}

			if (currentCount == null) {
				currentCount = 1L;
			} else {
				currentCount += 1L;
			}
			countsForOrder.put(current.node.count, currentCount);

			// Push children
			for (final Entry<Long, TrieNode<Long>> entry : current.node.prods
					.entrySet()) {
				final NodeOrder cnt = new NodeOrder();
				cnt.order = current.order + 1;
				cnt.node = entry.getValue();
				toCount.push(cnt);
			}

		}

		checkArgument(unkToCount.size() == 1);
		final NodeOrder current = unkToCount.pop();
		for (int i = 1; i <= getN(); i++) {
			countOfCounts.get(i).put(1L, current.node.count);
		}

	}

	@Override
	public void cutoffRare(final int threshold) {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return this;
	}

	public double getKatzCount(final long originalCount, final int order) {
		if (originalCount > NO_DISCOUNT_THRESHOLD) {
			return originalCount;
		}

		final Map<Long, Double> ngramKatzCount = checkNotNull(katzCounts
				.get(order));
		final double katzCount;

		katzCount = ngramKatzCount.get(originalCount);

		return katzCount;

	}

	@Override
	public double getProbabilityFor(final NGram<String> ngram) {
		final long thisNgramCount = trie.getCount(ngram, false, true);

		final long productionCount = trie.getCount(ngram.getPrefix(), false,
				false);

		if (thisNgramCount > 0 || ngram.size() == 1) {
			// Discount MLE
			final double discountedNgramCount = getKatzCount(thisNgramCount,
					ngram.size());
			return discountedNgramCount / (productionCount);

		} else if (productionCount > 0) {
			// backoff
			try {
				final double gamma = computeGamma(ngram);
				return gamma * getProbabilityFor(ngram.getSuffix());
			} catch (final IllegalArgumentException e) {
				// back off to normal model since we failed at doing something
				// (probably computing gamma)
				LOGGER.warning("Failed to compute gamma, using 1 instead: "
						+ ExceptionUtils.getFullStackTrace(e));
				return getProbabilityFor(ngram.getSuffix());
			}
		} else {
			return getProbabilityFor(ngram.getSuffix());
		}
	}

	/**
	 * @param prefix
	 */
	private double getResidualProbability(final NGram<String> prefix) {

		final TrieNode<Long> prefixU = trie.getNGramNodeForInput(prefix, true);

		// now for all these ngrams get their counts and sum their katz
		final TreeMultiset<Long> counts = TreeMultiset.create();
		final Long unkSymbolId = trie.getUnkSymbolId();

		for (final Entry<Long, TrieNode<Long>> child : prefixU.prods.entrySet()) {
			if (child.getKey().equals(unkSymbolId)) {
				continue;
			}
			counts.add(child.getValue().count);
		}

		// get the Katz counts and sum them up
		double katzCountSum = 0;
		for (final com.google.common.collect.Multiset.Entry<Long> entry : counts
				.entrySet()) {
			katzCountSum += getKatzCount(entry.getElement(), prefix.size() + 1)
					* (entry.getCount());
		}

		final double residual = 1. - katzCountSum
				/ (prefixU.count - prefixU.terminateHere);

		// There are cases where no probability mass is left
		checkArgument(residual >= 0);
		checkArgument(residual <= 1);

		return residual;
	}

	@Override
	public void removeNgram(final NGram<String> ngram) {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}

	@Override
	public void trainIncrementalModel(final Collection<File> files)
			throws IOException {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}

	@Override
	public void trainModel(final Collection<File> files) throws IOException {
		throw new UnsupportedOperationException(
				"KatzSmoother is an immutable Language Model");
	}
}