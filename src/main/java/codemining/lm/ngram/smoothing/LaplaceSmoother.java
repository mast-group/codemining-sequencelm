package codemining.lm.ngram.smoothing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import codemining.lm.ILanguageModel;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;

public class LaplaceSmoother extends AbstractNGramLM {

	private static final long serialVersionUID = -5288476873430715713L;

	private final double countIncrement = 1;

	public LaplaceSmoother(AbstractNGramLM original) {
		super(original);
	}

	@Override
	public void addFromSentence(List<String> sentence, boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	protected void addNgramToDict(NGram<String> ngram, boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void addSentences(Collection<List<String>> sentenceSet,
			boolean addNewVocabulary) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void cutoffRare(int threshold) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return this;
	}

	@Override
	public double getProbabilityFor(final NGram<String> ngram) {
		final long ngramCount = trie.getCount(ngram, false, true);

		final long productionCount = trie.getCount(ngram.getPrefix(), false,
				false);

		return (ngramCount + countIncrement)
				/ (productionCount + countIncrement
						* trie.getRoot().prods.size());

	}

	@Override
	public void trainIncrementalModel(final Collection<File> files)
			throws IOException {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void trainModel(Collection<File> files) throws IOException {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

}