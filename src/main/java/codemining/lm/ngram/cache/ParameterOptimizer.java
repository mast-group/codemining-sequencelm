/**
 * 
 */
package codemining.lm.ngram.cache;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import cc.mallet.optimize.GradientAscent;
import cc.mallet.optimize.Optimizable;
import codemining.lm.ngram.cache.SymbolicWeightCache.DecayFactor;

import com.google.common.base.Objects;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.math.DoubleMath;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
class ParameterOptimizer implements Optimizable.ByGradientValue {

	public static class LPair {
		double ngramProb;
		List<DecayFactor> cacheProb;
		double importance = 1; // This is used when only some tokens use this
								// cache

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof LPair))
				return false;
			final LPair other = (LPair) obj;
			if (Double.compare(other.ngramProb, ngramProb) != 0)
				return false;
			if (Double.compare(importance, other.importance) != 0)
				return false;
			if (other.cacheProb.equals(cacheProb))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(ngramProb, cacheProb, importance);
		}

	}

	final Multiset<LPair> elems;

	protected static final Logger LOGGER = Logger
			.getLogger(ParameterOptimizer.class.getName());

	double currentLambda = .5;

	double decay = .5;

	public ParameterOptimizer(Multiset<LPair> data) {
		elems = data;
	}

	/**
	 * @param decayValue
	 * @param lamdbaValue
	 * @return
	 */
	private double computeValue(final double decayValue, double lamdbaValue) {

		double penalty = 0;
		if (lamdbaValue < 0) {
			lamdbaValue = 10E-10;
			penalty = lamdbaValue * lamdbaValue;
		} else if (lamdbaValue > 1) {
			lamdbaValue = 1 - 10E-10;
			penalty = (lamdbaValue - 1) * (lamdbaValue - 1);
		}

		double sum = 0;
		for (final Entry<LPair> p : elems.entrySet()) {
			double decaySums = 0;
			for (final DecayFactor f : p.getElement().cacheProb) {
				decaySums += f.getForAlpha(decayValue);
			}

			checkArgument(p.getElement().ngramProb > 0
					&& p.getElement().ngramProb <= 1.);
			double baseProb = lamdbaValue * decaySums + (1. - lamdbaValue)
					* p.getElement().ngramProb;
			sum += p.getCount()
					* DoubleMath.log2(p.getElement().importance * baseProb
							+ (1. - p.getElement().importance)
							* p.getElement().ngramProb);
		}
		final double value = sum / elems.size() - penalty;
		checkArgument(!Double.isInfinite(value) && !Double.isNaN(value),
				"Value Should not be NaN or Inf but is " + value + " with sum="
						+ sum);

		return value;
	}

	@Override
	public int getNumParameters() {
		return 1;
	}

	@Override
	public double getParameter(int i) {
		return currentLambda;
	}

	@Override
	public void getParameters(double[] buffer) {
		buffer[0] = currentLambda;
	}

	@Override
	public double getValue() {
		return computeValue(decay, currentLambda);
	}

	@Override
	public void getValueGradient(final double[] gradient) {

		double lambdaValue = currentLambda;
		double penalty = 0;
		if (lambdaValue < 0) {
			lambdaValue = 10E-10;
			penalty = 2 * lambdaValue;
		} else if (lambdaValue > 1) {
			lambdaValue = 1;
			penalty = 2 * (lambdaValue - 1);
		}
		double lambdaSum = 0;
		for (final Entry<LPair> p : elems.entrySet()) {
			double decayValueSum = 0;
			for (final DecayFactor f : p.getElement().cacheProb) {
				decayValueSum += f.getForAlpha(decay);
			}

			checkArgument(decayValueSum >= 0 && decayValueSum <= 1,
					"Decay must be between 0 and 1 but is " + decayValueSum);

			final double denominator = Math.log(2)
					* (p.getElement().importance
							* (lambdaValue * decayValueSum + (1. - lambdaValue)
									* p.getElement().ngramProb) + (1 - p
							.getElement().importance)
							* p.getElement().ngramProb);

			lambdaSum += p.getCount() * p.getElement().importance
					* (decayValueSum - p.getElement().ngramProb) / denominator;
		}

		gradient[0] = lambdaSum / elems.size() - penalty;

		checkArgument(
				!Double.isInfinite(gradient[0]) && !Double.isNaN(gradient[0]),
				"gradient(lambda) should not be NaN or Inf but is "
						+ gradient[0]);

		// Do gradient check
		/*
		 * final double turbulance = 10E-6; final double lambdaGrad =
		 * (computeValue(decay, currentLambda + turbulance) -
		 * computeValue(decay, currentLambda)) / turbulance; checkArgument(
		 * Math.abs((lambdaGrad - gradient[0]) / gradient[0]) < 10E-4,
		 * "Relative diffrence of gradients is larger than threshold. " +
		 * (lambdaGrad - gradient[0]) / gradient[0] + " to be exact");
		 */

		System.err.println(Arrays.toString(gradient));
	}

	/**
	 * @param simpleCachedNGramLM
	 * 
	 */
	protected void optimizeParameters() {
		double bestLikelihood = Double.NEGATIVE_INFINITY;
		double bestD = .8;
		double bestL = .3;
		final double[] dMx = { .15, .2, .25, .3, .35, .4, .45, .5, .55, .6,
				.65, .7, .75, .8, .85, .9, .95, .98, .99 };

		for (final double decayValue : dMx) {
			decay = decayValue;
			currentLambda = .5;
			System.err.println("Start at d=" + decayValue + " l="
					+ currentLambda);
			final GradientAscent optimizer = new GradientAscent(this);
			optimizer.setMaxStepSize(.01);
			optimizer.setInitialStepSize(.01);
			boolean converged = false;
			try {
				converged = optimizer.optimize();
			} catch (IllegalArgumentException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
			if (bestLikelihood < getValue() && converged) {
				LOGGER.info("With l=" + currentLambda + " d=" + decayValue
						+ " the value is " + getValue());
				checkArgument(Double.compare(decayValue, decay) == 0,
						"Decay value has been changed!");
				bestD = decayValue;
				bestL = getParameter(0);
				bestLikelihood = getValue();
			}
		}
		decay = bestD;
		currentLambda = bestL;

		LOGGER.info("Optimized parameters for d=" + decay + " l="
				+ currentLambda + " val=" + bestLikelihood);
	}

	@Override
	public void setParameter(int i, double value) {
		if (i == 0) {
			currentLambda = value;
		} else if (i == 1) {
			decay = value;
		}
	}

	@Override
	public void setParameters(double[] values) {
		currentLambda = values[0];
	}

}
