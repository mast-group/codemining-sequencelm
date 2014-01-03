/**
 * 
 */
package codemining.lm.ngram.cache;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

/**
 * A weighted cache that also holds a symbolic representation.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class SymbolicWeightCache<T> extends WeightCache<T> {

	/**
	 * An object representing the decay of this element.
	 * 
	 */
	public static class DecayFactor {
		final int decayCount;

		final boolean hasOneMinusAlpha;

		public DecayFactor(int alphaCount, boolean firstElement) {
			decayCount = alphaCount;
			hasOneMinusAlpha = !firstElement;
		}

		public double getDerivativeAt(final double alpha) {
			if (decayCount == 0 && hasOneMinusAlpha) {
				return -1;
			} else if (decayCount == 0 && !hasOneMinusAlpha) {
				return 0;
			} else if (!hasOneMinusAlpha) {
				return decayCount * Math.pow(alpha, decayCount - 1);
			} else {
				checkArgument(decayCount > 0);
				return (1. - alpha) * decayCount
						* Math.pow(alpha, decayCount - 1.)
						- Math.pow(alpha, decayCount);
			}
		}

		public double getForAlpha(final double alpha) {
			double accumulator = 1;
			if (hasOneMinusAlpha) {
				accumulator = 1. - alpha;
			}
			accumulator *= Math.pow(alpha, decayCount);
			return accumulator;
		}

		DecayFactor getNext() {
			return new DecayFactor(decayCount + 1, !hasOneMinusAlpha);
		}
	}

	private final Map<T, List<DecayFactor>> symbolicCache = new HashMap<T, List<DecayFactor>>();

	public SymbolicWeightCache(final double decayExponent, final T firstElement) {
		super(decayExponent, firstElement);
		final DecayFactor lst = new DecayFactor(0, true);
		final List<DecayFactor> decayList = Lists.newArrayList();
		decayList.add(lst);
		symbolicCache.put(firstElement, decayList);
	}

	public List<DecayFactor> getDecayFactorFor(final T element) {
		if (symbolicCache.containsKey(element)) {
			return symbolicCache.get(element);
		} else {
			return null; // A 0 equivalent
		}
	}

	@Override
	public void pushElement(final T element) {
		super.pushElement(element);

		for (final Entry<T, List<DecayFactor>> ent : symbolicCache.entrySet()) {
			final List<DecayFactor> lst = ent.getValue();
			final List<DecayFactor> toRemove = Lists.newArrayList();
			for (int i = 0; i < lst.size(); i++) {
				if (lst.get(i).decayCount < 100) {
					lst.set(i, lst.get(i).getNext());
				} else {
					toRemove.add(lst.get(i));
				}
			}

			for (final DecayFactor rm : toRemove) {
				lst.remove(rm);
			}
		}

		if (symbolicCache.containsKey(element)) {
			symbolicCache.get(element).add(new DecayFactor(0, false));
		} else {
			final List<DecayFactor> decayList = Lists.newArrayList();
			decayList.add(new DecayFactor(0, false));
			symbolicCache.put(element, decayList);
		}
	}
}
