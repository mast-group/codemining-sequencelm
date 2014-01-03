/**
 * 
 */
package codemining.lm.sequencememoizer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.RandomUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ILanguageModel;
import codemining.lm.ITokenGeneratingLanguageModel;
import codemining.lm.util.TokenVocabularyBuilder;
import codemining.lm.util.VocabularyToInt;
import codemining.util.SettingsLoader;
import codemining.util.serialization.ISerializationStrategy;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;

import edu.columbia.stat.wood.pub.sequencememoizer.IntSequenceMemoizer;
import edu.columbia.stat.wood.pub.sequencememoizer.IntSequenceMemoizerParameters;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
@DefaultSerializer(JavaSerializer.class)
public class SequenceMemoizerLM implements
		ITokenGeneratingLanguageModel<FullToken> {

	private static final Logger LOGGER = Logger
			.getLogger(SequenceMemoizerLM.class.getName());

	public static final int CLEAN_VOCABULARY_THRESHOLD = (int) SettingsLoader
			.getNumericSetting("CleanVocabularyCountThreshold", 20);

	private IntSequenceMemoizer memoizer;

	private final ITokenizer tokenizer;

	private static final long serialVersionUID = 8676708745889523608L;

	private static final int NUM_ITERATIONS = (int) SettingsLoader
			.getNumericSetting("iterations", 100);

	private static final double SAMPLE_PERCENTAGE = SettingsLoader
			.getNumericSetting("sample", 1);

	VocabularyToInt vocabularyMapper;

	public SequenceMemoizerLM(final ITokenizer tokenizer) {
		this.tokenizer = tokenizer;

	}

	@Override
	public List<FullToken> generateSentence() {
		final List<FullToken> generated = Lists.newArrayList();

		final int[] start = new int[1];
		start[0] = vocabularyMapper.getAlphabet()
				.get(ITokenizer.SENTENCE_START);
		generated.add(new FullToken(ITokenizer.SENTENCE_START, ""));

		// Generate 10000 tokens anyway...
		final int[] sample = memoizer.generateSequence(start, 10000);

		final BiMap<Integer, String> reverse = vocabularyMapper.getAlphabet()
				.inverse();
		for (int i = 0; i < sample.length; i++) {
			final String token = reverse.get(sample[i]);
			generated.add(new FullToken(token, ""));

			if (token.equals(ITokenizer.SENTENCE_END)) {
				break;
			}
		}

		return generated;
	}

	@Override
	public double getAbsoluteEntropy(final File file) throws IOException {
		final int[] tokens = vocabularyMapper.fileToIntSequence(file);
		final double sequenceProbability = memoizer.sequenceProbability(
				new int[0], tokens);
		return sequenceProbability;
	}

	@Override
	public double getAbsoluteEntropy(final String fileContent) {
		final int[] tokens = vocabularyMapper.codeToIntSequence(fileContent);
		final double sequenceProbability = memoizer.sequenceProbability(
				new int[0], tokens);
		return sequenceProbability;
	}

	@Override
	public double getExtrinsticEntropy(final File file) throws IOException {
		final int[] tokens = vocabularyMapper.fileToIntSequence(file);
		final double sequenceProbability = memoizer.sequenceProbability(
				new int[0], tokens);
		return sequenceProbability / tokens.length;
	}

	@Override
	public double getExtrinsticEntropy(final String fileContent) {
		final int[] tokens = vocabularyMapper.codeToIntSequence(fileContent);
		final double sequenceProbability = memoizer.sequenceProbability(
				new int[0], tokens);
		return sequenceProbability / tokens.length;
	}

	@Override
	public ILanguageModel getImmutableVersion() {
		return this; // TODO
	}

	@Override
	public ITokenizer getTokenizer() {
		return tokenizer;
	}

	@Override
	public AbstractFileFilter modelledFilesFilter() {
		return tokenizer.getFileFilter();
	}

	public void serializeToDisk(final String filename)
			throws SerializationException {
		final ISerializationStrategy st = Serializer.getSerializer();
		st.serialize(this, filename);
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

		memoizer = new IntSequenceMemoizer(new IntSequenceMemoizerParameters(
				vocabulary.size()));

		for (final File file : files) {
			try {
				if (RandomUtils.nextDouble() > SAMPLE_PERCENTAGE) {
					continue;
				}
				memoizer.newSequence();
				final int[] tokenSq = vocabularyMapper.fileToIntSequence(file);
				memoizer.continueSequence(tokenSq);
			} catch (final Throwable e) {
				LOGGER.warning("Failed to add file " + file.getAbsolutePath()
						+ " " + ExceptionUtils.getFullStackTrace(e));
			}
		}
		LOGGER.info("Sequences stored. Stating " + NUM_ITERATIONS
				+ " iterations");
		memoizer.sample(NUM_ITERATIONS);

	}

}
