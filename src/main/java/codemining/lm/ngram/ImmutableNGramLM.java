/**
 * 
 */
package codemining.lm.ngram;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import codemining.lm.ILanguageModel;

/**
 * An immutable ngram language model.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class ImmutableNGramLM extends AbstractNGramLM {

	private static final long serialVersionUID = -1774233421443065352L;

	public ImmutableNGramLM(final AbstractNGramLM originalDict) {
		super(originalDict);
	}

	@Override
	public void addFromSentence(final List<String> sentence,
			final boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

	@Override
	protected void addNgram(final NGram<String> ngram, final boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"Cannot add ngram to ImmutableNGramDictionary");

	}

	@Override
	public void addSentences(final Collection<List<String>> sentenceSet,
			final boolean addNewVocabulary) {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.ed.inf.ngram.AbstractNGramDictionary#cutoffRare(int)
	 */
	@Override
	public void cutoffRare(final int threshold) {
		throw new UnsupportedOperationException(
				"Cannot perform cutoff to ImmutableNGramDictionary");

	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return this;
	}

	@Override
	public double getProbabilityFor(final NGram<String> ngram) {
		return getMLProbabilityFor(ngram, false);
	}

	@Override
	public void removeNgram(final NGram<String> ngram) {
		throw new UnsupportedOperationException(
				"Cannot remove ngram from ImmutableNGramDictionary");

	}

	@Override
	public void trainIncrementalModel(
			final Collection<File> fromAllFilesInFolder) throws IOException {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

	@Override
	public void trainModel(final Collection<File> fromAllFilesInFolder)
			throws IOException {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

}
