/**
 * 
 */
package codemining.lm.hmm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ILanguageModel;
import codemining.lm.ITokenGeneratingLanguageModel;
import codemining.lm.util.TokenVocabularyBuilder;
import codemining.lm.util.VocabularyToInt;
import codemining.util.SettingsLoader;

import com.google.common.collect.Lists;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class HiddenMarkovModelLM implements
		ITokenGeneratingLanguageModel<FullToken> {

	private static final Logger LOGGER = Logger
			.getLogger(HiddenMarkovModelLM.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 4431685854743499177L;

	final ITokenizer tokenizer;
	VocabularyToInt vocabularyMapper;
	HMM hmm;

	public static final int CLEAN_VOCABULARY_THRESHOLD = (int) SettingsLoader
			.getNumericSetting("CleanVocabularyThreshold", 10);

	private static final int NUM_ITERATIONS = (int) SettingsLoader
			.getNumericSetting("iterations", 100);

	/**
	 * 
	 */
	public HiddenMarkovModelLM(final ITokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	@Override
	public List<FullToken> generateSentence() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getAbsoluteEntropy(final File file) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAbsoluteEntropy(final String fileContent) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getExtrinsticEntropy(final File file) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getExtrinsticEntropy(final String fileContent) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return this;
	}

	@Override
	public ITokenizer getTokenizer() {
		return tokenizer;
	}

	@Override
	public AbstractFileFilter modelledFilesFilter() {
		return tokenizer.getFileFilter();
	}

	@Override
	public void trainIncrementalModel(final Collection<File> files)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void trainModel(final Collection<File> files) throws IOException {
		final Set<String> vocabulary = TokenVocabularyBuilder.buildVocabulary(
				files, tokenizer, CLEAN_VOCABULARY_THRESHOLD);
		vocabularyMapper = new VocabularyToInt(tokenizer, vocabulary);
		LOGGER.info("Vocabulary Built. Reading files...");

		hmm = new HMM(100, vocabulary.size());

		final List<Integer> allTokens = Lists.newArrayList();
		for (final File file : files) {
			try {
				for (final int token : vocabularyMapper.fileToIntSequence(file)) {
					allTokens.add(token);
				}

			} catch (final Throwable e) {
				LOGGER.warning("Failed to add file " + file.getAbsolutePath()
						+ " " + ExceptionUtils.getFullStackTrace(e));
			}
		}
		LOGGER.info("Sequences stored. Stating " + NUM_ITERATIONS
				+ " iterations");

		final int[] allToks = new int[allTokens.size()];
		for (int i = 0; i < allToks.length; i++) {
			allToks[i] = allTokens.get(i);
		}
		hmm.train(allToks, NUM_ITERATIONS);

	}

}
