/**
 * 
 */
package codemining.lm.ngram.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.lm.ngram.NGramLM;

/**
 * An n-gram lm that is represented as a Dirichlet posterior.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class DirichletNgramLM extends NGramLM {

	private static final long serialVersionUID = -5849880849111464974L;

	private final AbstractNGramLM priorLm;
	final double dirichletAlpha;

	public DirichletNgramLM(final AbstractNGramLM prior,
			final double dirichletAlpha) {
		super(prior.getN(), prior.getTokenizer());
		priorLm = prior;
		this.dirichletAlpha = dirichletAlpha;
	}

	@Override
	public double getProbabilityFor(final NGram<String> ngram) {
		final double priorProbability = priorLm.getProbabilityFor(ngram);
		final long ngramCount = trie.getCount(ngram, true, true);
		final long productionCount = trie.getCount(ngram.getPrefix(), true,
				false);

		checkArgument(ngramCount <= productionCount);
		return ((double) ngramCount + dirichletAlpha * priorProbability)
				/ ((double) (productionCount + dirichletAlpha));
	}

}
