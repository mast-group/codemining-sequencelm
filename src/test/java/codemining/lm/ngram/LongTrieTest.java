/**
 * 
 */
package codemining.lm.ngram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class LongTrieTest {

	/**
	 * Test method for
	 * {@link codemining.lm.ngram.LongTrie#add(codemining.lm.ngram.NGram, boolean)}
	 * and
	 * {@link codemining.lm.ngram.LongTrie#remove(codemining.lm.ngram.NGram)}
	 */
	@Test
	public void testAddRemoveNGramOfKBoolean() {
		final LongTrie<String> testTrie = new LongTrie<String>("UNK");
		final List<String> snt = Lists.newArrayList();
		snt.add("a");
		snt.add("b");
		snt.add("c");
		snt.add("d");
		snt.add("e");
		snt.add("f");
		snt.add("g");
		snt.add("h");
		snt.add("i");
		snt.add("a");

		final NGram<String> ng = new NGram<String>(snt, 0, 3);
		testTrie.add(ng, true);

		assertEquals(testTrie.sumStartingWith(ng.getPrefix(), false), 1);
		assertEquals(testTrie.sumStartingWith(ng.getPrefix(), true), 1);
		assertEquals(
				testTrie.sumStartingWith(ng.getPrefix().getPrefix(), false), 1);
		assertEquals(
				testTrie.sumStartingWith(ng.getPrefix().getPrefix(), true), 1);

		final NGram<String> ng2 = new NGram<String>(snt, 4, 7);
		testTrie.add(ng2, false);

		assertEquals(testTrie.sumStartingWith(ng2.getPrefix(), false), 0);
		assertEquals(testTrie.sumStartingWith(ng2.getPrefix(), true), 1);
		assertEquals(
				testTrie.sumStartingWith(ng2.getPrefix().getPrefix(), false), 0);
		assertEquals(
				testTrie.sumStartingWith(ng2.getPrefix().getPrefix(), true), 1);

		// Now remove
		testTrie.remove(ng2);
		assertEquals(testTrie.sumStartingWith(ng.getPrefix(), false), 1);
		assertEquals(testTrie.sumStartingWith(ng.getPrefix(), true), 1);
		assertEquals(
				testTrie.sumStartingWith(ng.getPrefix().getPrefix(), false), 1);
		assertEquals(
				testTrie.sumStartingWith(ng.getPrefix().getPrefix(), true), 1);

		testTrie.remove(ng);
		assertEquals(testTrie.sumStartingWith(ng.getPrefix(), false), 0);
		assertEquals(testTrie.sumStartingWith(ng.getPrefix(), true), 0);
		assertEquals(
				testTrie.sumStartingWith(ng.getPrefix().getPrefix(), false), 0);
		assertEquals(
				testTrie.sumStartingWith(ng.getPrefix().getPrefix(), true), 0);
	}

	/**
	 * Test method for
	 * {@link codemining.lm.ngram.LongTrie#buildVocabularySymbols(java.util.Set)}
	 * .
	 */
	@Test
	public void testBuildVocabularySymbols() {
		final LongTrie<String> testTrie = new LongTrie<String>("UNK");

		final Set<String> words = Sets.newTreeSet();
		words.add("a");
		words.add("b");
		words.add("c");
		words.add("d");
		words.add("e");
		words.add("f");
		words.add("g");
		words.add("h");

		testTrie.buildVocabularySymbols(words);

		assertEquals(testTrie.getVocabulary().size(), words.size() + 1);
		assertTrue(testTrie.isUNK("l"));
		assertFalse(testTrie.isUNK("a"));
		assertFalse(testTrie.isUNK("c"));

	}

	/**
	 * Test method for
	 * {@link codemining.lm.ngram.LongTrie#getSymbolIds(java.lang.Iterable, boolean)}
	 * .
	 */
	@Test
	public void testGetSymbolIds() {
		final LongTrie<String> testTrie = new LongTrie<String>("UNK");

		final List<String> words = Lists.newArrayList();
		words.add("a");
		words.add("b");
		words.add("c");
		words.add("d");
		words.add("e");
		words.add("f");
		words.add("g");
		words.add("g");

		final List<Long> syms = testTrie.getSymbolIds(words, false);
		assertEquals(syms.size(), 8);
		for (final Long symbol : syms) {
			assertEquals(symbol, null);
		}

		final List<Long> symbols = testTrie.getSymbolIds(words, true);
		assertEquals(symbols.size(), words.size());

		final Set<Long> symbolSet = Sets.newTreeSet();
		symbolSet.addAll(symbols);
		assertEquals(symbolSet.size(), words.size() - 1);

		final LongTrie<String> testTrie2 = new LongTrie<String>("UNK");
		testTrie2.buildVocabularySymbols(new TreeSet<String>(words));

		assertEquals(testTrie.getSymbolIds(words, false),
				testTrie2.getSymbolIds(words, false));

		final List<String> unks = Lists.newArrayList();
		unks.add("hello");
		List<Long> keys = testTrie2.getSymbolIds(unks, false);
		assertEquals(keys.size(), 1);
		assertEquals(keys.get(0), null);

		keys = testTrie2.getSymbolIds(unks, true);
		assertEquals(keys.size(), 1);
		assertFalse(keys.get(0) == null);

	}

}
