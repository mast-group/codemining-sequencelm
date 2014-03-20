package codemining.lm.ngram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TrieTest {

	final List<String> testList1 = Lists.newArrayList();
	final List<String> testList2 = Lists.newArrayList();
	final List<String> testList3 = Lists.newArrayList();
	final List<String> testList4 = Lists.newArrayList();

	@Before
	public void setUp() {
		testList1.add("tok1");
		testList1.add("tok2");
		testList1.add("tok3");

		testList2.add("tok1");
		testList2.add("tok2");
		testList2.add("tok3");

		testList3.add("tok1");
		testList3.add("tok2");
		testList3.add("tok4");

		testList4.add("tok5");
		testList4.add("tok6");
		testList4.add("tok7");
	}

	@Test
	public void testCountDistinct() {
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);
		testTrie.add(testList1);
		testTrie.add(testList3);
		testTrie.add(testList4);

		// Count distinct next elements in trie
		final List<String> testPrefix = Lists.newArrayList();
		testPrefix.add("tok1");
		assertEquals(testTrie.countDistinctStartingWith(testPrefix, true), 1);
		assertEquals(testTrie.countDistinctStartingWith(testPrefix, false), 1);

		testPrefix.add("tok2");
		assertEquals(testTrie.countDistinctStartingWith(testPrefix, true), 2);
		assertEquals(testTrie.countDistinctStartingWith(testPrefix, false), 2);

		testPrefix.add("tok3");
		assertEquals(testTrie.countDistinctStartingWith(testPrefix, true), 0);
		assertEquals(testTrie.countDistinctStartingWith(testPrefix, false), 0);
	}

	@Test
	public void testCutoff() {
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);
		testTrie.add(testList1);
		testTrie.add(testList3);
		testTrie.add(testList4);

		testTrie.cutoffRare(1);

		final List<String> testPrefix = Lists.newArrayList();
		testPrefix.add("tok1");
		assertEquals(testTrie.getCount(testPrefix, true, true), 3);
		assertEquals(testTrie.getCount(testPrefix, false, true), 3);
		assertEquals(testTrie.getCount(testPrefix, true, false), 3);
		assertEquals(testTrie.getCount(testPrefix, false, false), 3);

		testPrefix.add("tok2");
		assertEquals(testTrie.getCount(testPrefix, true, true), 3);
		assertEquals(testTrie.getCount(testPrefix, false, true), 2);
		assertEquals(testTrie.getCount(testPrefix, true, false), 3);
		assertEquals(testTrie.getCount(testPrefix, false, false), 2);

		testPrefix.add("tok3");
		assertEquals(testTrie.getCount(testPrefix, true, true), 2);
		assertEquals(testTrie.getCount(testPrefix, false, true), 2);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);

		testPrefix.remove(2);
		testPrefix.add("tok4");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 0);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);

		assertEquals(testTrie.getCount(testList4, false, true), 0);
		assertEquals(testTrie.getCount(testList4, true, true), 1);
	}

	@Test
	public void testGetCount1() {
		// Add the same ngram many times and test that it still works
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);
		assertEquals(testTrie.getCount(testList1, true, true), 1);
		assertEquals(testTrie.getCount(testList1, false, true), 1);

		testTrie.add(testList1);
		assertEquals(testTrie.getCount(testList1, true, true), 2);
		assertEquals(testTrie.getCount(testList1, false, true), 2);

		testTrie.add(testList2);
		assertEquals(testTrie.getCount(testList1, true, true), 3);
		assertEquals(testTrie.getCount(testList1, false, true), 3);

		assertEquals(testTrie.getCount(testList2, true, true), 3);
		assertEquals(testTrie.getCount(testList2, false, true), 3);

		testTrie.remove(testList2);
		assertEquals(testTrie.getCount(testList1, true, true), 2);
		assertEquals(testTrie.getCount(testList1, false, true), 2);

		testTrie.remove(testList1);
		assertEquals(testTrie.getCount(testList1, true, true), 1);
		assertEquals(testTrie.getCount(testList1, false, true), 1);

		testTrie.remove(testList1);
		assertEquals(testTrie.getCount(testList1, true, true), 0);
		assertEquals(testTrie.getCount(testList1, false, true), 0);
	}

	@Test
	public void testGetCount2() {
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);

		// Test if prefix counting works
		List<String> testPrefix = Lists.newArrayList();
		testPrefix.add("tok1");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 1);
		assertEquals(testTrie.getCount(testPrefix, false, false), 1);

		testPrefix.add("tok2");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 1);
		assertEquals(testTrie.getCount(testPrefix, false, false), 1);

		testPrefix.add("tok3");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);

		// Now add another irrelevant and see that everything still holds
		testPrefix = Lists.newArrayList();
		testTrie.add(testList4);
		testPrefix.add("tok1");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 1);
		assertEquals(testTrie.getCount(testPrefix, false, false), 1);

		testPrefix.add("tok2");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 1);
		assertEquals(testTrie.getCount(testPrefix, false, false), 1);

		testPrefix.add("tok3");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);

		// Now remove and everything should be zero
		testTrie.remove(testList1);
		assertEquals(testTrie.getCount(testPrefix, true, true), 0);
		assertEquals(testTrie.getCount(testPrefix, false, true), 0);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);
	}

	@Test
	public void testGetCount3() {
		// Test multiple paths
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);
		testTrie.add(testList1);
		testTrie.add(testList3);

		// Test if prefix counting works
		final List<String> testPrefix = Lists.newArrayList();
		testPrefix.add("tok1");
		assertEquals(testTrie.getCount(testPrefix, true, true), 3);
		assertEquals(testTrie.getCount(testPrefix, false, true), 3);
		assertEquals(testTrie.getCount(testPrefix, true, false), 3);
		assertEquals(testTrie.getCount(testPrefix, false, false), 3);

		testPrefix.add("tok2");
		assertEquals(testTrie.getCount(testPrefix, true, true), 3);
		assertEquals(testTrie.getCount(testPrefix, false, true), 3);
		assertEquals(testTrie.getCount(testPrefix, true, false), 3);
		assertEquals(testTrie.getCount(testPrefix, false, false), 3);

		testPrefix.add("tok3");
		assertEquals(testTrie.getCount(testPrefix, true, true), 2);
		assertEquals(testTrie.getCount(testPrefix, false, true), 2);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);

		testPrefix.remove(2);
		testPrefix.add("tok4");
		assertEquals(testTrie.getCount(testPrefix, true, true), 1);
		assertEquals(testTrie.getCount(testPrefix, false, true), 1);
		assertEquals(testTrie.getCount(testPrefix, true, false), 0);
		assertEquals(testTrie.getCount(testPrefix, false, false), 0);
	}

	@Test(expected = IllegalStateException.class)
	public void testRemoveException() {
		// Add the same ngram many times and test that it still works
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);
		testTrie.add(testList1);
		testTrie.add(testList2);

		testTrie.remove(testList2);

		testTrie.remove(testList1);

		testTrie.remove(testList1);
		assertTrue(true);
		testTrie.remove(testList1);
		fail("Should never reach this point. Previous statement should through exception");
	}

	@Test
	public void testSumStarting() {
		final Trie<String> testTrie = new Trie<String>("UNK");
		testTrie.add(testList1);
		testTrie.add(testList1);
		testTrie.add(testList3);
		testTrie.add(testList4);

		List<String> testPrefix = Lists.newArrayList();
		testPrefix.add("tok1");
		assertEquals(testTrie.sumStartingWith(testPrefix, true), 3);
		assertEquals(testTrie.sumStartingWith(testPrefix, false), 3);

		testPrefix.add("tok2");
		assertEquals(testTrie.sumStartingWith(testPrefix, true), 3);
		assertEquals(testTrie.sumStartingWith(testPrefix, false), 3);

		testPrefix.add("tok3");
		assertEquals(testTrie.sumStartingWith(testPrefix, true), 0);
		assertEquals(testTrie.sumStartingWith(testPrefix, false), 0);

		// Use an irrelevant tag
		testPrefix.remove(2);
		testPrefix.set(1, "tokL");
		assertEquals(testTrie.sumStartingWith(testPrefix, true), 0);
		assertEquals(testTrie.sumStartingWith(testPrefix, false), 0);

		testPrefix = Lists.newArrayList();
		testPrefix.add("tok5");
		assertEquals(testTrie.sumStartingWith(testPrefix, true), 1);
		assertEquals(testTrie.sumStartingWith(testPrefix, false), 1);

		// Cut-off
		testTrie.cutoffRare(1);
		assertEquals(testTrie.sumStartingWith(testPrefix, true), 1);
		assertEquals(testTrie.sumStartingWith(testPrefix, false), 0);
	}

}
