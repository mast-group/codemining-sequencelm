package codemining.lm.ngram.smoothing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import codemining.lm.ILanguageModel;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.util.SettingsLoader;

public class LaplaceSmoother extends AbstractNGramLM {

	private static final long serialVersionUID = -5288476873430715713L;

	private static final double COUNT_INCREMENT = (int) SettingsLoader
			.getNumericSetting("countIncrement", 1);

	public LaplaceSmoother(final AbstractNGramLM original) {
		super(original);
	}

	@Override
	public void addFromSentence(final List<String> sentence,
			final boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	protected void addNgram(final NGram<String> ngram, final boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void addSentences(final Collection<List<String>> sentenceSet,
			final boolean addNewVocabulary) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void cutoffRare(final int threshold) {
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

		return (ngramCount + COUNT_INCREMENT)
				/ (productionCount + COUNT_INCREMENT
						* trie.getRoot().prods.size());

	}

	@Override
	public void removeNgram(final NGram<String> ngram) {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void trainIncrementalModel(final Collection<File> files)
			throws IOException {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

	@Override
	public void trainModel(final Collection<File> files) throws IOException {
		throw new UnsupportedOperationException(
				"LaplaceSmoother is an immutable Language Model");
	}

}