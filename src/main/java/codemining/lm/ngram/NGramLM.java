/**
 * 
 */
package codemining.lm.ngram;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.languagetools.ITokenizer;
import codemining.lm.ILanguageModel;
import codemining.lm.util.TokenVocabularyBuilder;
import codemining.util.SettingsLoader;
import codemining.util.parallel.ParallelThreadPool;

import com.google.common.collect.ImmutableList;

/**
 * A language model using ngrams.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class NGramLM extends AbstractNGramLM {

	private static class CutoffRunnable implements Runnable {

		final AbstractNGramLM dict;

		public CutoffRunnable(final AbstractNGramLM ngramModel) {
			dict = ngramModel;
		}

		@Override
		public void run() {
			dict.cutoffRare(CLEAN_THRESHOLD);
		}

	}

	/**
	 * Extract ngrams from a specific file.
	 * 
	 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
	 * 
	 */
	private static class NGramExtractorRunnable implements Runnable {

		final File codeFile;

		final AbstractNGramLM dict;

		final ITokenizer tokenizer;

		final boolean addNewVoc;

		public NGramExtractorRunnable(final File file,
				final AbstractNGramLM ngramModel,
				final ITokenizer tokenizerModule,
				final boolean addNewToksToVocabulary) {
			codeFile = file;
			dict = ngramModel;
			tokenizer = tokenizerModule;
			addNewVoc = addNewToksToVocabulary;
		}

		@Override
		public void run() {
			LOGGER.finer("Reading file " + codeFile.getAbsolutePath());
			try {
				final char[] code = FileUtils.readFileToString(codeFile)
						.toCharArray();
				final List<String> tokens = ImmutableList.copyOf(tokenizer
						.tokenListFromCode(code));
				dict.addFromSentence(tokens, addNewVoc);
			} catch (final IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}
	}

	private static final Logger LOGGER = Logger.getLogger(NGramLM.class
			.getName());

	private static final long serialVersionUID = 2765488075402402353L;

	public static final int PRUNE_RATE = (int) SettingsLoader
			.getNumericSetting("PruneRate", 5000);

	public static final int CLEAN_THRESHOLD = (int) SettingsLoader
			.getNumericSetting("CleanCountThreshold", 1);

	private boolean neverCleanedUp = true;

	/**
	 * Constructor.
	 * 
	 * @param size
	 *            the max-size of the n-grams. The n.
	 */
	public NGramLM(final int size, final ITokenizer tokenizerModule) {
		super(size, tokenizerModule);
	}

	/**
	 * Given a sentence (i.e. a list of strings) add all appropriate ngrams.
	 * 
	 * @param sentence
	 *            an (ordered) list of tokens belonging to a sentence.
	 */
	@Override
	public void addFromSentence(final List<String> sentence,
			final boolean addNewToks) {
		for (int i = 0; i < sentence.size(); i++) {
			final NGram<String> ngram = NGram.constructNgramAt(i, sentence,
					getN());
			if (ngram.size() > 1) {
				addNgram(ngram, addNewToks);
			}
		}

		for (int i = sentence.size() - getN() + 1; i < sentence.size(); i++) {
			final NGram<String> ngram = NGram.constructNgramAt(
					sentence.size() - 1, sentence, sentence.size() - i);
			addNgram(ngram, addNewToks);
		}
	}

	/**
	 * Given an ngram (a list of strings with size <= n) add it to the trie and
	 * update the counts of counts.
	 * 
	 * @param ngram
	 */
	@Override
	public void addNgram(final NGram<String> ngram, final boolean addNewVoc) {
		checkArgument(ngram.size() > 0 && ngram.size() <= getN(),
				"Adding a n-gram of size %s but we have a %s-gram",
				ngram.size(), getN());
		trie.add(ngram, addNewVoc);
	}

	/**
	 * Add a set of sentences to the dictionary.
	 * 
	 * @param sentenceSet
	 */
	@Override
	public void addSentences(final Collection<List<String>> sentenceSet,
			final boolean addNewVocabulary) {
		for (final List<String> sent : sentenceSet) {
			addFromSentence(sent, addNewVocabulary);
		}
	}

	/**
	 * Cut-off rare ngrams by removing rare tokens.
	 * 
	 * @param threshold
	 */
	@Override
	public void cutoffRare(final int threshold) {
		trie.cutoffRare(threshold);
	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return new ImmutableNGramLM(this);
	}

	@Override
	public double getProbabilityFor(final NGram<String> ngram) {
		return getMLProbabilityFor(ngram, false);
	}

	@Override
	public void removeNgram(final NGram<String> ngram) {
		checkArgument(ngram.size() > 0 && ngram.size() <= getN(),
				"Removing a n-gram of size %s but we have a %s-gram",
				ngram.size(), getN());
		trie.remove(ngram);
	}

	@Override
	public void trainIncrementalModel(final Collection<File> files)
			throws IOException {
		trainModel(files);

	}

	@Override
	public void trainModel(final Collection<File> files) throws IOException {
		LOGGER.finer("Building vocabulary...");
		trie.buildVocabularySymbols(TokenVocabularyBuilder.buildVocabulary(
				files, getTokenizer(), CLEAN_THRESHOLD));

		LOGGER.finer("Vocabulary Built. Counting n-grams");
		trainModel(files, false, false);
	}

	/**
	 * @param files
	 * @param performCleanups
	 */
	private void trainModel(final Collection<File> files,
			final boolean performCleanups, final boolean addNewToksToVocabulary) {
		final ParallelThreadPool threadPool = new ParallelThreadPool();

		int count = 0;
		for (final File fi : files) {
			threadPool.pushTask(new NGramExtractorRunnable(fi, this,
					getTokenizer(), addNewToksToVocabulary));
			count++;
			if (count % PRUNE_RATE == PRUNE_RATE - 1 && performCleanups) {
				threadPool.pushTask(new CutoffRunnable(this));
				neverCleanedUp = false;
			}
		}

		threadPool.waitForTermination();

		if (neverCleanedUp && performCleanups) {
			cutoffRare(CLEAN_THRESHOLD);
		}
	}
}
