/**
 * 
 */
package codemining.lm.ngram;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.JavaSerialization;
import codemining.util.serialization.KryoSerialization;

import com.google.common.collect.Lists;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class NGramLMTest {

	@Test
	public void testSimpleNgrams1() {
		final List<String> sent = Lists.newArrayList();
		sent.add("h");
		sent.add("a");
		sent.add("e");
		sent.add("h");
		sent.add("a");
		sent.add("b");
		sent.add("i");
		sent.add("l");
		sent.add("p");
		sent.add("h");
		sent.add("p");
		sent.add("a");
		sent.add("b");
		sent.add("h");

		AbstractNGramLM dict = new NGramLM(2, null);
		assertEquals(dict.getN(), 2);
		dict.addFromSentence(sent, true);

		final List<String> ngramL = Lists.newArrayList();
		ngramL.add("h");
		ngramL.add("a");
		final NGram<String> ngram = new NGram<String>(ngramL);
		assertEquals(dict.getMLProbabilityFor(ngram, false), 2. / 3., .001);
		assertEquals(dict.getMLProbabilityFor(ngram, true), 2. / 3., .001);

		for (int i = 0; i < 10; i++) {
			dict.addFromSentence(sent, true);
			assertEquals(dict.getMLProbabilityFor(ngram, false), 2. / 3., .001);
			assertEquals(dict.getMLProbabilityFor(ngram, true), 2. / 3., .001);
		}

		dict = new NGramLM(3, null);
		dict.addFromSentence(sent, true);
		final List<String> ngram4L = Lists.newArrayList();
		;
		ngram4L.add("h");
		ngram4L.add("a");
		ngram4L.add("e");
		final NGram<String> ngram4 = new NGram<String>(ngram4L);
		for (int i = 0; i < 10; i++) {
			dict.addFromSentence(sent, true);
			assertEquals(dict.getMLProbabilityFor(ngram4, false), .5, .001);
		}

	}

	@Test
	public void testSimpleNgrams2() throws SerializationException {
		final List<String> sent = Lists.newArrayList();
		sent.add("h");
		sent.add("a");
		sent.add("e");
		sent.add("h");
		sent.add("a");
		sent.add("m");

		final List<String> sent2 = Lists.newArrayList();
		sent2.add("t");
		sent2.add("a");
		sent2.add("m");
		sent2.add("h");
		sent2.add("a");
		sent2.add("l");

		final List<String> sent3 = Lists.newArrayList();
		sent3.add("t");
		sent3.add("a");
		sent3.add("h");
		sent3.add("h");
		sent3.add("a");

		final NGramLM dict = new NGramLM(3, null);
		dict.addFromSentence(sent, true);
		dict.addFromSentence(sent2, true);
		dict.addFromSentence(sent3, true);

		final List<String> ngramL = Lists.newArrayList();

		ngramL.add("h");
		ngramL.add("a");
		final NGram<String> ngram = new NGram<String>(ngramL);
		assertEquals(dict.getMLProbabilityFor(ngram, false), 5. / 6., .001);

		final List<String> ngram2L = Lists.newArrayList();
		ngram2L.add("h");
		ngram2L.add("h");
		final NGram<String> ngram2 = new NGram<String>(ngram2L);
		assertEquals(dict.getMLProbabilityFor(ngram2, false), 1. / 6., .001);

		final List<String> ngram3L = Lists.newArrayList();
		ngram3L.add("h");
		ngram3L.add("a");
		ngram3L.add("e");
		final NGram<String> ngram3 = new NGram<String>(ngram3L);
		assertEquals(dict.getMLProbabilityFor(ngram3, false), 1. / 3., .001);

		final List<String> ngram4L = Lists.newArrayList();
		ngram4L.add("t");
		ngram4L.add("a");
		ngram4L.add("m");
		final NGram<String> ngram4 = new NGram<String>(ngram4L);
		assertEquals(dict.getMLProbabilityFor(ngram4, false), 1. / 2., .001);

		// Test serialization
		final byte[] serialized = (new JavaSerialization()).serialize(dict);

		final AbstractNGramLM dictD = (AbstractNGramLM) (new JavaSerialization())
				.deserializeFrom(serialized);

		assertEquals(dictD.getMLProbabilityFor(ngram, false), 5. / 6., .001);

		assertEquals(dictD.getMLProbabilityFor(ngram2, false), 1. / 6., .001);

		assertEquals(dictD.getMLProbabilityFor(ngram3, false), 1. / 3., .001);

		assertEquals(dictD.getMLProbabilityFor(ngram4, false), 1. / 2., .001);

		final byte[] serialized2 = (new KryoSerialization()).serialize(dictD);
		final AbstractNGramLM dictE = (AbstractNGramLM) (new KryoSerialization())
				.deserializeFrom(serialized2);

		assertEquals(dictE.getMLProbabilityFor(ngram, false), 5. / 6., .001);

		assertEquals(dictE.getMLProbabilityFor(ngram2, false), 1. / 6., .001);

		assertEquals(dictE.getMLProbabilityFor(ngram3, false), 1. / 3., .001);

		assertEquals(dictE.getMLProbabilityFor(ngram4, false), 1. / 2., .001);

	}

	@Test
	public void testSimpleNgrams3() {
		final List<String> sent = Lists.newArrayList();
		sent.add("h");
		sent.add("a");
		sent.add("e");
		sent.add("h");
		sent.add("a");
		sent.add("b");
		sent.add("i");
		sent.add("l");
		sent.add("p");
		sent.add("h");
		sent.add("p");
		sent.add("a");
		sent.add("b");
		sent.add("h");

		final AbstractNGramLM dict = new NGramLM(3, null);
		dict.addFromSentence(sent, true);

		List<String> ngramL = Lists.newArrayList();
		ngramL.add("b");
		ngramL.add("a");
		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramL), false), 0,
				.001);

		ngramL = Lists.newArrayList();
		ngramL.add("l");
		ngramL.add("a");
		ngramL.add("e");
		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramL), false), 0,
				.001);

	}

	@Test
	public void testSimpleNgrams4() {
		final List<String> sent = Lists.newArrayList();
		sent.add("h");
		sent.add("a");
		sent.add("h");
		sent.add("e");
		sent.add("h");
		sent.add("a");
		sent.add("h");
		sent.add("a");
		sent.add("h");
		sent.add("i");
		sent.add("h");
		sent.add("i");
		sent.add("h");
		sent.add("o");

		final AbstractNGramLM dict = new NGramLM(2, null);
		dict.addFromSentence(sent, true);

		final List<String> ngramHA = Lists.newArrayList();
		ngramHA.add("h");
		ngramHA.add("a");

		final List<String> ngramHE = Lists.newArrayList();
		ngramHE.add("h");
		ngramHE.add("e");

		final List<String> ngramHQ = Lists.newArrayList();
		ngramHQ.add("h");
		ngramHQ.add("q");

		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramHA), false),
				3. / 7., .001);

		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramHE), false),
				1. / 7., .001);

		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramHQ), false), 0,
				.001);

		dict.cutoffRare(1);

		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramHA), false),
				3. / 5., .001);

		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramHE), false), 0,
				.001);

		assertEquals(
				dict.getMLProbabilityFor(new NGram<String>(ngramHQ), false), 0,
				.001);

	}

}
