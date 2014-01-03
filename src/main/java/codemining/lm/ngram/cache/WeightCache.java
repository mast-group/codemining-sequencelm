/**
 * 
 */
package codemining.lm.ngram.cache;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.math.RandomUtils;

/**
 * A cache that weights its elements T.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class WeightCache<T> implements ICache<T> {
	private final Map<T, Double> cache = new HashMap<T, Double>();

	/**
	 * The decay parameter of the cache.
	 */
	private final double decay;

	public static final double MIN_HOLDING_THRESHOLD = 1E-11;

	/**
	 * Constructor.
	 * 
	 * @param decayExponent
	 *            the decay exponent
	 * @param firstElement
	 */
	public WeightCache(final double decayExponent, final T firstElement) {
		checkArgument(decayExponent < 1. && decayExponent > 0,
				"Decay exponent must be in (0,1) but is " + decayExponent);
		decay = decayExponent;
		cache.put(firstElement, 1.);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.lm.ngram.cache.ICache#getProbabilityFor(T)
	 */
	@Override
	public double getProbabilityFor(final T element) {
		if (cache.containsKey(element)) {
			return cache.get(element);
		} else {
			return 0;
		}
	}

	@Override
	public T getRandomElement() {
		final double randomPick = RandomUtils.nextDouble();
		double sum = 0;
		for (final Entry<T, Double> element : cache.entrySet()) {
			sum += element.getValue();
			if (sum >= randomPick) {
				return element.getKey();
			}
		}

		checkArgument(false,
				"Should not reach this point. When adding probabilities, they should sum to 1.");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.lm.ngram.cache.ICache#pushElement(T)
	 */
	@Override
	public void pushElement(final T element) {
		final ArrayDeque<T> toRemove = new ArrayDeque<T>();

		final Map<T, Double> updates = new TreeMap<T, Double>();

		for (final Entry<T, Double> entry : cache.entrySet()) {
			final double newVal = entry.getValue().doubleValue() * decay;
			if (newVal > MIN_HOLDING_THRESHOLD) {
				updates.put(entry.getKey(), newVal);
			} else {
				toRemove.push(entry.getKey());
			}
		}

		for (final Entry<T, Double> ent : updates.entrySet()) {
			cache.put(ent.getKey(), ent.getValue());
		}

		while (!toRemove.isEmpty()) {
			final T ent = toRemove.pop();
			cache.remove(ent);
		}

		if (cache.containsKey(element)) {
			cache.put(element, cache.get(element) + 1. - decay);
		} else {
			cache.put(element, 1. - decay);
		}
	}
}
