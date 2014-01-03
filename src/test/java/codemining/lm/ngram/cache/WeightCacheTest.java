package codemining.lm.ngram.cache;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeightCacheTest {

	@Test
	public void test() {
		final ICache<String> c1 = new WeightCache<String>(.5, "first!");
		assertEquals(c1.getProbabilityFor("first!"), 1, 10E-10);
		assertEquals(c1.getProbabilityFor("second"), 0, 10E-10);

		c1.pushElement("second:(");
		assertEquals(c1.getProbabilityFor("first!"), .5, 10E-10);
		assertEquals(c1.getProbabilityFor("second:("), .5, 10E-10);

		c1.pushElement("third");
		assertEquals(c1.getProbabilityFor("first!"), .25, 10E-10);
		assertEquals(c1.getProbabilityFor("second:("), .25, 10E-10);
		assertEquals(c1.getProbabilityFor("third"), .5, 10E-10);

		c1.pushElement("first!");
		assertEquals(c1.getProbabilityFor("first!"), .625, 10E-10);
		assertEquals(c1.getProbabilityFor("second:("), .125, 10E-10);
		assertEquals(c1.getProbabilityFor("third"), .25, 10E-10);
		assertEquals(c1.getProbabilityFor("blah"), 0, 10E-10);
	}

}
