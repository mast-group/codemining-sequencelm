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

	public ImmutableNGramLM(AbstractNGramLM originalDict) {
		super(originalDict);
	}

	@Override
	public void addFromSentence(List<String> sentence, boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

	@Override
	protected void addNgramToDict(NGram<String> ngram, boolean addNewVoc) {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

	@Override
	public void addSentences(Collection<List<String>> sentenceSet,
			boolean addNewVocabulary) {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.ed.inf.ngram.AbstractNGramDictionary#cutoffRare(int)
	 */
	@Override
	public void cutoffRare(int threshold) {
		throw new UnsupportedOperationException(
				"Cannot perform cutoff to ImmutableNGramDictionary");

	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return this;
	}

	@Override
	public double getProbabilityFor(NGram<String> ngram) {
		return getMLProbabilityFor(ngram, false);
	}

	@Override
	public void trainIncrementalModel(Collection<File> fromAllFilesInFolder)
			throws IOException {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

	@Override
	public void trainModel(Collection<File> fromAllFilesInFolder)
			throws IOException {
		throw new UnsupportedOperationException(
				"Cannot add sentence to ImmutableNGramDictionary");

	}

}
