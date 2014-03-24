package codemining.lm.ngram;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Lists;

/**
 * A trie of lists of T with an UNK symbol. This implementation is thread-safe
 * but only eventually consistent after edits (add/remove)
 * 
 * @author Miltiadis Allamanis <m.allamanis@ed.ac.uk>
 * 
 * @param <T>
 *            the type of the trie nodes (where the branching happens).
 */
public class Trie<T extends Comparable<T>> implements Serializable {

	/**
	 * Struct class, representing the node of a trie.
	 */
	public static class TrieNode<T> implements Serializable {

		private static final long serialVersionUID = -3590197616044935851L;

		/**
		 * The children of this trie node
		 */
		public final SortedMap<T, TrieNode<T>> prods = new TreeMap<T, TrieNode<T>>();
		public long count;
		public long terminateHere;
	}

	private static final long serialVersionUID = 1365912094350571019L;

	/**
	 * A lock to allow editing of this trie.
	 */
	private final Lock editLock = new ReentrantLock();

	/**
	 * The dictionary is a trie of NGramUnits.
	 */
	private final TrieNode<T> root = new TrieNode<T>();

	protected T unkSymbolId;

	public Trie(final T unk) {
		unkSymbolId = unk;
	}

	/**
	 * 
	 * @param elementSequence
	 */
	public final void add(final List<T> elementSequence) {
		editLock.lock();
		try {
			root.count++;
			TrieNode<T> currentUnit = root;

			for (final T tokId : elementSequence) {
				if (!currentUnit.prods.containsKey(tokId)) {
					currentUnit.prods.put(tokId, new TrieNode<T>());
				}
				final TrieNode<T> next = currentUnit.prods.get(tokId);
				next.count++;

				currentUnit = next;
			}
			currentUnit.terminateHere++;
		} finally {
			editLock.unlock();
		}
	}

	private final void checkCount(final TrieNode<T> node) {
		if (node.count >= 0) {
			return;
		}
		node.count = 0;
		throw new IllegalStateException("Removed a non-existent sequence.");
	}

	/**
	 * Return N_{1+}(units,*), that is the number of possible productions.
	 * 
	 * @param ngram
	 * @param useUNKs
	 * @return
	 */
	public final long countDistinctStartingWith(final List<T> prefix,
			final boolean useUNKs) {
		checkArgument(prefix.size() > 0);
		final TrieNode<T> current = getTrieNodeForInput(prefix, useUNKs);

		if (current == null) {
			return 0;
		}

		if (!useUNKs && current.prods.containsKey(getUnkSymbolId())) {
			return current.prods.size() - 1;
		} else {
			return current.prods.size();
		}
	}

	/**
	 * Remove elements with low count.
	 * 
	 * @param threshold
	 */
	public final synchronized void cutoffRare(final int threshold) {
		cutoffRare(root, threshold);
	}

	/**
	 * Internal cutoff method to recursively remove symbols.
	 * 
	 * @param node
	 * @param threshold
	 */
	private final void cutoffRare(final TrieNode<T> node, final int threshold) {
		final List<T> toBeRemoved = Lists.newArrayList();

		// Create or retrieve the UNK
		final TrieNode<T> unkUnit;
		if (node.prods.containsKey(getUnkSymbolId())) {
			unkUnit = node.prods.get(getUnkSymbolId());
		} else {
			unkUnit = new TrieNode<T>();
		}

		// For every production that is below the threshold, merge and
		// recursively cut
		for (final Entry<T, TrieNode<T>> prodEntry : node.prods.entrySet()) {
			final T production = prodEntry.getKey();
			final TrieNode<T> currentPos = prodEntry.getValue();
			if (currentPos.count <= threshold
					&& (!production.equals(getUnkSymbolId()))) {
				toBeRemoved.add(production);
				mergeTrieNodes(currentPos, unkUnit);
			} else {
				cutoffRare(currentPos, threshold);
			}
		}

		if (unkUnit.count > 0) {
			node.prods.put(getUnkSymbolId(), unkUnit);
			cutoffRare(unkUnit, threshold);
		}

		// Remove all, from current node.
		for (final T production : toBeRemoved) {
			node.prods.remove(production);
		}

	}

	/**
	 * @param useUNKs
	 * @param useTerminals
	 * @param current
	 * @return
	 */
	public final long getCount(final List<T> ngramSymbols,
			final boolean useUNKs, final boolean useTerminals) {
		final TrieNode<T> current = getTrieNodeForInput(ngramSymbols, useUNKs);
		if (current == null) {
			return 0;
		}

		final long unkDiscountCount;
		if (!useUNKs) {
			final TrieNode<T> unkUnit = current.prods.get(getUnkSymbolId());

			if (unkUnit != null) {
				unkDiscountCount = unkUnit.count;
			} else {
				unkDiscountCount = 0;
			}
		} else {
			unkDiscountCount = 0;
		}

		final long totalCount;
		if (useTerminals) {
			totalCount = current.count - unkDiscountCount;
		} else {
			totalCount = current.count - current.terminateHere
					- unkDiscountCount;
		}

		checkArgument(totalCount >= 0);
		return totalCount;
	}

	public final TrieNode<T> getRoot() {
		return root;
	}

	/**
	 * 
	 * @param ngramSymbols
	 * @param useUNKs
	 * @return
	 */
	public final TrieNode<T> getTrieNodeForInput(final List<T> ngramSymbols,
			final boolean useUNKs) {
		return getTrieNodeForInput(ngramSymbols, useUNKs, root);
	}

	/**
	 * @param ngramSymbols
	 * @param useUNKs
	 * @param fromNode
	 * @return
	 */
	public TrieNode<T> getTrieNodeForInput(final List<T> ngramSymbols,
			final boolean useUNKs, final TrieNode<T> startNode) {
		TrieNode<T> fromNode = startNode;
		for (final T symbol : ngramSymbols) {
			if (symbol != null && fromNode.prods.containsKey(symbol)) {
				fromNode = fromNode.prods.get(symbol);
			} else if (fromNode.prods.containsKey(getUnkSymbolId()) && useUNKs) {
				fromNode = fromNode.prods.get(getUnkSymbolId());
			} else {
				fromNode = null;
				break;
			}
		}
		return fromNode;
	}

	public final T getUnkSymbolId() {
		return unkSymbolId;
	}

	/**
	 * Merge two trieNodes units recursively.
	 * 
	 * @param from
	 * @param to
	 */
	private final void mergeTrieNodes(final TrieNode<T> from,
			final TrieNode<T> to) {
		checkNotNull(to).count += checkNotNull(from).count;
		to.terminateHere += from.terminateHere;

		for (final Entry<T, TrieNode<T>> fromChild : from.prods.entrySet()) {
			if (to.prods.containsKey(fromChild.getKey())) {
				mergeTrieNodes(fromChild.getValue(),
						to.prods.get(fromChild.getKey()));
			} else {
				to.prods.put(fromChild.getKey(), fromChild.getValue());
			}
		}
	}

	/**
	 * 
	 * @param elementSequence
	 */
	public final void remove(final List<T> elementSequence) {
		editLock.lock();
		try {
			root.count--;
			checkCount(root);
			TrieNode<T> currentUnit = root;

			for (final T tokId : elementSequence) {
				if (!currentUnit.prods.containsKey(tokId)) {
					currentUnit.prods.put(tokId, new TrieNode<T>());
				}
				final TrieNode<T> next = currentUnit.prods.get(tokId);
				next.count--;
				checkCount(next);
				currentUnit = next;
			}
			currentUnit.terminateHere--;
		} finally {
			editLock.unlock();
		}
	}

	/**
	 * Return c_(ngram,*)
	 * 
	 * @param ngram
	 * @param useUNKs
	 * @return
	 */
	public final long sumStartingWith(final List<T> prefix,
			final boolean useUNKs) {
		checkArgument(prefix.size() > 0);
		final TrieNode<T> unit = getTrieNodeForInput(prefix, useUNKs);

		if (unit == null) {
			return 0;
		}

		final long count = unit.count - unit.terminateHere;

		return count;
	}
}