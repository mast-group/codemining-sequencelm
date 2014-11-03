/**
 *
 */
package codemining.lm.ngram;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import codemining.lm.ngram.Trie.TrieNode;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * An trie of K, with an alphabet of longs
 *
 * @author Miltos Allamanis<m.allamanis@ed.ac.uk>
 *
 */
@DefaultSerializer(JavaSerializer.class)
public class LongTrie<K> implements Serializable {

	private final Trie<Long> baseTrie = new Trie<Long>(null);

	private static final long serialVersionUID = -7194495381473625925L;

	/**
	 * An alphabet containing a mapping of Keys to Strings.
	 */
	private final BiMap<K, Long> alphabet;

	private long nextId;

	private final K unkSymbol;

	public LongTrie(final K unk) {
		nextId = Long.MIN_VALUE;
		alphabet = HashBiMap.create();
		baseTrie.unkSymbolId = nextId;
		unkSymbol = unk;
		alphabet.put(unk, baseTrie.unkSymbolId);
		nextId++;
	}

	/**
	 * Add an n-gram to the trie. If parts of the ngram do not exist in the
	 * dictionary, introduce them
	 *
	 * @param ngram
	 */
	public void add(final NGram<K> ngram, final boolean introduceVoc) {
		final List<Long> keys = getSymbolIds(ngram, introduceVoc);
		if (!introduceVoc) {
			// replace with unks
			for (int i = 0; i < keys.size(); i++) {
				if (keys.get(i) == null) {
					keys.set(i, baseTrie.getUnkSymbolId());
				}
			}
		}
		baseTrie.add(keys);
	}

	private synchronized long addSymbolId(final K element) {
		alphabet.put(element, nextId);
		nextId++;
		return nextId - 1;
	}

	/**
	 * Create symbols for all the given tokens. Useful when building the
	 * vocabulary first.
	 *
	 * @param words
	 *            a collection of words
	 */
	public void buildVocabularySymbols(final Set<K> words) {
		for (final K elem : words) {
			addSymbolId(elem);
		}
	}

	/**
	 * Return N_{1+}(ngram,*)
	 *
	 * @param ngram
	 * @param useUNKs
	 * @return
	 */
	public long countDistinctStartingWith(final NGram<K> ngram,
			final boolean useUNKs) {
		return baseTrie.countDistinctStartingWith(getSymbolIds(ngram, false),
				useUNKs);
	}

	public void cutoffRare(final int threshold) {
		baseTrie.cutoffRare(threshold);

		// Now scan everything and remove unwanted symbols from vocabulary.
		final Set<Long> usedSymbols = Sets.newTreeSet();
		final ArrayDeque<TrieNode<Long>> stack = new ArrayDeque<TrieNode<Long>>();
		stack.push(baseTrie.getRoot());

		while (!stack.isEmpty()) {
			final TrieNode<Long> node = stack.pop();
			usedSymbols.addAll(node.prods.keySet());
			for (final TrieNode<Long> childNode : node.prods.values()) {
				stack.push(childNode);
			}
		}

		final List<Long> difference = Lists.newArrayList(Sets.difference(
				alphabet.values(), usedSymbols));
		for (final long keyToRemove : difference) {
			if (keyToRemove == getUnkSymbolId()) {
				continue;
			}
			checkNotNull(alphabet.inverse().remove(keyToRemove));
		}
	}

	/**
	 * Returns the count of the n-gram in the dictionary. If a token does not
	 * exist in the dictionary then it is replaced with UNK. If UNKs do not
	 * exist at the current point then 0 is returned.
	 *
	 * @param ngram
	 * @return
	 */
	public long getCount(final NGram<K> ngram, final boolean useUNKs,
			final boolean useTerminals) {
		return baseTrie.getCount(getSymbolIds(ngram, false), useUNKs,
				useTerminals);
	}

	public TrieNode<Long> getNGramNodeForInput(final NGram<K> ngram,
			final boolean useUNKs) {
		return baseTrie
				.getTrieNodeForInput(getSymbolIds(ngram, false), useUNKs);
	}

	public TrieNode<Long> getNGramNodeForInput(final NGram<K> ngram,
			final boolean useUNKs, final TrieNode<Long> fromNode) {
		return baseTrie.getTrieNodeForInput(getSymbolIds(ngram, false),
				useUNKs, fromNode);
	}

	/**
	 * Return all the possible productions from a specific prefix.
	 *
	 * @param prefix
	 * @return
	 */
	public Map<K, Long> getPossibleProductionsWithCounts(final NGram<K> prefix) {
		final TrieNode<Long> node = baseTrie.getTrieNodeForInput(
				getSymbolIds(prefix, false), false);

		final Map<K, Long> productions = new TreeMap<K, Long>();

		if (node == null) {
			return productions;
		}

		for (final Entry<Long, TrieNode<Long>> prodEntry : node.prods
				.entrySet()) {
			final K key = alphabet.inverse().get(prodEntry.getKey());
			final long count = prodEntry.getValue().count;
			if (key != null) {
				productions.put(key, count);
			} else {
				productions.put(unkSymbol, count);
			}
		}

		return productions;
	}

	public TrieNode<Long> getRoot() {
		return baseTrie.getRoot();
	}

	public Set<K> getRootSymbols() {
		final Set<K> rootProductions = Sets.newHashSet();
		final TrieNode<Long> rootNode = baseTrie.getRoot();
		for (final long symbolId : rootNode.prods.keySet()) {
			rootProductions.add(getSymbolFromKey(symbolId));
		}

		return rootProductions;
	}

	/**
	 * Return the symbol from the key.
	 *
	 * @param key
	 * @return
	 */
	public K getSymbolFromKey(final Long key) {
		if (key.equals(baseTrie.unkSymbolId)) {
			return unkSymbol;
		}
		return alphabet.inverse().get(key);
	}

	/**
	 * Helper function to create symbol IDs from list.
	 *
	 * @param objectList
	 * @param createIfNotFoundK
	 * @return
	 */
	public List<Long> getSymbolIds(final Iterable<K> objectList,
			final boolean createIfNotFound) {
		final List<Long> symbols = Lists.newArrayList();
		for (final K element : objectList) {
			final Long key = alphabet.get(element);
			if (key == null && createIfNotFound) {
				symbols.add(addSymbolId(element));
			} else if (key == null) {
				symbols.add(null);
			} else {
				symbols.add(key);
			}
		}
		return symbols;
	}

	public Long getUnkSymbolId() {
		return baseTrie.getUnkSymbolId();
	}

	public Set<K> getVocabulary() {
		return alphabet.keySet();
	}

	public boolean isUNK(final K token) {
		return !alphabet.containsKey(token);
	}

	/**
	 * Remove an n-gram from the trie. The n-gram must exist.
	 *
	 * @param ngram
	 */
	public void remove(final NGram<K> ngram) {
		final List<Long> keys = getSymbolIds(ngram, false);

		// replace nulls with unks
		for (int i = 0; i < keys.size(); i++) {
			if (keys.get(i) == null) {
				keys.set(i, baseTrie.getUnkSymbolId());
			}
		}

		baseTrie.remove(keys);
	}

	/**
	 * Substitute all the tokens in the current ngram with UNK when they do not
	 * exist in the dictionary.
	 *
	 * @param ngram
	 */
	public NGram<K> substituteWordsToUNK(final NGram<K> ngram) {
		final List<K> ngramCopy = Lists.newArrayList();
		for (final K gram : ngram) {
			final Long key = alphabet.get(gram);
			if (key == null) {
				ngramCopy.add(alphabet.inverse().get(baseTrie.unkSymbolId));
			} else {
				ngramCopy.add(gram);
			}
		}
		return new NGram<K>(ngramCopy);
	}

	/**
	 * Return c_(ngram,*)
	 *
	 * @param ngram
	 * @param useUNKs
	 * @return
	 */
	public long sumStartingWith(final NGram<K> ngram, final boolean useUNKs) {
		return baseTrie.sumStartingWith(getSymbolIds(ngram, false), useUNKs);
	}

	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (final Entry<Long, TrieNode<Long>> ngramEntry : baseTrie.getRoot().prods
				.entrySet()) {
			final Long ngram = ngramEntry.getKey();
			final List<String> prods = Lists.newArrayList();
			toStringHelper(alphabet.inverse().get(ngram).toString(),
					ngramEntry.getValue(), prods);
			for (final String prod : prods) {
				buf.append(prod + System.lineSeparator());
			}
		}
		buf.append(']');
		return buf.toString();
	}

	/**
	 * Helper function to convert dictionary to string.
	 *
	 * @param currentString
	 * @param currentUnit
	 * @param productions
	 */
	private void toStringHelper(final String currentString,
			final TrieNode<Long> currentUnit, final List<String> productions) {
		if (currentUnit.prods.size() == 0) {
			productions.add(currentString + " count:" + currentUnit.count);
		} else {
			for (final Entry<Long, TrieNode<Long>> prodEntry : currentUnit.prods
					.entrySet()) {
				final Long prod = prodEntry.getKey();
				toStringHelper(
						currentString + ", " + alphabet.inverse().get(prod),
						prodEntry.getValue(), productions);
			}
		}
	}

}
