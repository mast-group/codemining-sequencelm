package codemining.lm.ngram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;

public class NGramTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private List<String> getSample() {
		final List<String> sample = Lists.newArrayList();
		sample.add("a");
		sample.add("b");
		sample.add("c");
		sample.add("d");
		sample.add("e");
		sample.add("f");
		sample.add("g");
		sample.add("h");
		sample.add("i");

		return sample;
	}

	@Test
	public void testConstructNGram() {
		final List<String> sentence = getSample();
		final NGram<String> n1 = new NGram<String>(sentence, 3, 6);
		final NGram<String> n2 = NGram.constructNgramAt(5, sentence, 3);
		assertEquals(n1, n2);

		assertEquals(NGram.constructNgramAt(0, sentence, 3).size(), 1);
		assertEquals(NGram.constructNgramAt(0, sentence, 5).size(), 1);

		final NGram<String> n3 = new NGram<String>(sentence, 0, 3);
		final NGram<String> n4 = NGram.constructNgramAt(2, sentence, 3);
		assertEquals(n3, n4);
		assertFalse(n3.equals(n1));
		assertFalse(n3.equals(n2));

		final NGram<String> n5 = new NGram<String>(n3, 1, 3);
		assertEquals(n5.size(), 2);
		assertEquals(n5.get(0), "b");
		assertEquals(n5.get(1), "c");
	}

	@Test
	public void testEquals() {
		final List<String> sentence = getSample();
		final NGram<String> abc1 = new NGram<String>(sentence, 0, 3);
		final NGram<String> abc2 = new NGram<String>(sentence, 0, 3);
		final NGram<String> ab = new NGram<String>(sentence, 0, 2);
		final NGram<String> bcd = new NGram<String>(sentence, 1, 4);

		assertEquals(abc1, abc2);
		assertTrue(abc1.equals(abc2));

		assertFalse(abc1.equals(ab));
		assertFalse(abc1.equals(bcd));
		assertFalse(abc1.equals(sentence));

	}

	@Test
	public void testGet() {
		final List<String> sentence = getSample();
		final NGram<String> def = new NGram<String>(sentence, 3, 6);
		assertEquals(def.get(0), "d");
		assertEquals(def.get(1), "e");
		assertEquals(def.get(2), "f");

		exception.expect(IllegalArgumentException.class);
		def.get(3);

	}

	@Test
	public void testGetPrefix() {
		final List<String> sentence = getSample();
		final NGram<String> abc = new NGram<String>(sentence, 0, 3);
		assertEquals(abc.size(), 3);

		assertEquals(abc.get(0), "a");
		assertEquals(abc.get(1), "b");
		assertEquals(abc.get(2), "c");

		final NGram<String> ab = abc.getPrefix();
		assertEquals(ab.size(), 2);

		assertEquals(ab.get(0), "a");
		assertEquals(ab.get(1), "b");

		final NGram<String> a = ab.getPrefix();
		assertEquals(a.size(), 1);
		assertEquals(a.get(0), "a");

		final NGram<String> empty = a.getPrefix();
		assertEquals(empty.size(), 0);
		exception.expect(IllegalArgumentException.class);
		empty.get(0);
	}

	@Test
	public void testGetSuffix() {
		final List<String> sentence = getSample();
		final NGram<String> abc = new NGram<String>(sentence, 0, 3);
		assertEquals(abc.size(), 3);

		assertEquals(abc.get(0), "a");
		assertEquals(abc.get(1), "b");
		assertEquals(abc.get(2), "c");

		final NGram<String> bc = abc.getSuffix();
		assertEquals(bc.size(), 2);

		assertEquals(bc.get(0), "b");
		assertEquals(bc.get(1), "c");

		final NGram<String> c = bc.getSuffix();
		assertEquals(c.size(), 1);
		assertEquals(c.get(0), "c");

		final NGram<String> empty = c.getSuffix();
		assertEquals(empty.size(), 0);
		exception.expect(IllegalArgumentException.class);
		empty.get(0);

	}

	@Test
	public void testIterable() {
		final List<String> sentence = getSample();
		final NGram<String> ngram = new NGram<String>(sentence, 4, 8);
		int count = 0;
		final String[] chars = { "e", "f", "g", "h" };
		for (String gram : ngram) {
			assertEquals(gram, chars[count]);
			count++;
		}
		assertEquals(count, ngram.size());
	}

	@Test
	public void testSize() {
		final List<String> sentence = getSample();
		final NGram<String> abc = new NGram<String>(sentence, 0, 3);
		assertEquals(abc.size(), 3);

		final NGram<String> defg = new NGram<String>(sentence, 3, 7);
		assertEquals(defg.size(), 4);

		assertEquals(defg.getPrefix().size(), 3);
	}

}
