/**
 * 
 */
package codemining.lm.ngram.cache;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.RandomUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ILanguageModel;
import codemining.lm.ITokenGeneratingLanguageModel;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.lm.ngram.cache.SymbolicWeightCache.DecayFactor;
import codemining.util.SettingsLoader;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.math.DoubleMath;

/**
 * A cache n-gram model that caches only on identifiers.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class IdentifierOnlyCachedNGramLM implements
		ITokenGeneratingLanguageModel<FullToken> {

	static class IdentifierOnlyCachedNGramLMDataExtractor extends
			IdentifierOnlyCachedNGramLM {

		private static final long serialVersionUID = 3047560629014010487L;

		final Multiset<ParameterOptimizer.LPair> elements = HashMultiset
				.create();

		public IdentifierOnlyCachedNGramLMDataExtractor(
				final AbstractNGramLM baseNgram,
				final AbstractNGramLM typeNgram, final String identifierType) {
			super(baseNgram, typeNgram, identifierType, .5, .5); // These params
																	// are not
																	// really
																	// used...
		}

		@Override
		protected ICache<String> createCache(final String firstEntry) {
			return new SymbolicWeightCache<String>(.5, firstEntry);
		}

		final Multiset<ParameterOptimizer.LPair> getDataParameters(
				final Collection<File> files) {
			for (final File f : files) {
				try {
					this.getAbsoluteEntropy(f);
				} catch (final IOException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
				if (elements.entrySet().size() > CALIBRATION_SIZE_THRESHOLD) {
					break;
				}
			}
			return elements;
		}

		@Override
		public double getProbabilityFor(final NGram<String> ngram,
				final ICache<String> cache, final double probOfIdentifier) {
			final double ngramProb = baseNgram.getProbabilityFor(ngram);
			final String lastWord = ngram.get(ngram.size() - 1);
			final SymbolicWeightCache<String> symbolicCache = (SymbolicWeightCache<String>) cache;

			final List<DecayFactor> cacheProb = symbolicCache
					.getDecayFactorFor(lastWord);

			final ParameterOptimizer.LPair ngramPair = new ParameterOptimizer.LPair();
			ngramPair.ngramProb = ngramProb;
			if (cacheProb != null) {
				ngramPair.cacheProb = Lists.newArrayList(cacheProb);
			} else {
				ngramPair.cacheProb = Lists.newArrayList();
			}
			ngramPair.importance = probOfIdentifier;
			checkArgument(ngramProb > 0 && ngramProb <= 1,
					"N-gram probablity should be between 0,1 but is "
							+ ngramProb);
			elements.add(ngramPair);

			return .5;
		}
	}

	protected static final Logger LOGGER = Logger
			.getLogger(IdentifierOnlyCachedNGramLM.class.getName());

	public static final int CALIBRATION_SIZE_THRESHOLD = (int) SettingsLoader
			.getNumericSetting("calibrationThreshold", 5 * 10E6);

	private static final long serialVersionUID = -3795883012836136927L;

	private final double cacheWeight;

	private final double cacheDecayConst;

	private final Pattern fileExtention = Pattern.compile("\\.[a-z]+$");

	final AbstractNGramLM baseNgram;

	final AbstractNGramLM typeNgram;

	final String identiferNameType;

	public static final int MAX_GENERATED_SENTENCE_LENGTH = 10000;

	/**
	 * A constructor that optimizes the parameters.
	 * 
	 * @param baseNgram
	 * @param typeNgram
	 * @param identifierType
	 * @param cacheLambda
	 * @param cacheDecay
	 */
	public IdentifierOnlyCachedNGramLM(final AbstractNGramLM baseNgram,
			final AbstractNGramLM typeNgram, final String identifierType,
			final Collection<File> parameterCalibrationFiles) {
		this.baseNgram = baseNgram;
		this.typeNgram = typeNgram;
		identiferNameType = identifierType;

		final IdentifierOnlyCachedNGramLMDataExtractor ex = new IdentifierOnlyCachedNGramLMDataExtractor(
				baseNgram, typeNgram, identifierType);

		final ParameterOptimizer opt = new ParameterOptimizer(
				ex.getDataParameters(parameterCalibrationFiles));

		opt.optimizeParameters();
		cacheWeight = opt.currentLambda;
		cacheDecayConst = opt.decay;
	}

	/**
	 * @param baseNgram
	 */
	public IdentifierOnlyCachedNGramLM(final AbstractNGramLM baseNgram,
			final AbstractNGramLM typeNgram, final String identifierType,
			final double cacheLambda, final double cacheDecay) {
		this.baseNgram = baseNgram;
		this.typeNgram = typeNgram;
		identiferNameType = identifierType;
		cacheWeight = cacheLambda;
		cacheDecayConst = cacheDecay;
	}

	private NGram<String> constructIdentNgram(final NGram<String> tokTypeNgram) {
		final List<String> modifiedGram = Lists.newArrayList();
		for (final String token : tokTypeNgram) {
			modifiedGram.add(token);
		}
		modifiedGram.set(modifiedGram.size() - 1, identiferNameType);

		return new NGram<String>(modifiedGram);
	}

	/**
	 * @param className
	 * @return
	 */
	protected ICache<String> createCache(final String className) {
		return new WeightCache<String>(cacheDecayConst, className);
	}

	@Override
	public List<FullToken> generateSentence() {
		final List<FullToken> production = Lists.newArrayList();
		final List<String> tokenProductions = Lists.newArrayList();
		final List<String> tokTypeProductions = Lists.newArrayList();

		FullToken nextToken = new FullToken(ITokenizer.SENTENCE_START,
				ITokenizer.SENTENCE_START);
		final FullToken lastToken = new FullToken(ITokenizer.SENTENCE_END,
				ITokenizer.SENTENCE_END);
		ICache<String> cache = null;

		int iteration = 0;

		while (!nextToken.equals(lastToken)
				&& iteration < MAX_GENERATED_SENTENCE_LENGTH) {
			production.add(nextToken);
			tokenProductions.add(nextToken.token);
			tokTypeProductions.add(nextToken.tokenType);
			iteration++;

			if (nextToken.tokenType.equals(identiferNameType)) {
				if (cache == null) {
					cache = createCache(nextToken.token);
				} else {
					cache.pushElement(nextToken.token);
				}
			}

			// First decide what type of token to generate
			final NGram<String> tokTypeNgram = NGram.constructNgramAt(
					tokTypeProductions.size() - 1, tokTypeProductions,
					typeNgram.getN() - 1);
			final String tokenType = typeNgram.pickRandom(tokTypeNgram);

			if (tokenType.equals(identiferNameType)
					&& RandomUtils.nextDouble() < cacheWeight && cache != null) {
				nextToken = new FullToken(cache.getRandomElement(),
						identiferNameType);

			} else {
				final NGram<String> prefix = NGram.constructNgramAt(
						tokenProductions.size() - 1, tokenProductions,
						baseNgram.getN() - 1);
				final String token = baseNgram.pickRandom(prefix);
				nextToken = baseNgram.getTokenizer().getTokenFromString(token);

				if (token.equals(AbstractNGramLM.UNK_SYMBOL)) {
					nextToken = new FullToken(cache.getRandomElement(),
							identiferNameType);
				}
			}

		}
		production.add(nextToken);
		tokenProductions.add(nextToken.token);
		tokTypeProductions.add(nextToken.tokenType);

		return production;
	}

	@Override
	public double getAbsoluteEntropy(final File file) throws IOException {
		return getAbsoluteEntropy(FileUtils.readFileToString(file),
				getCurrentFilename(file));
	}

	@Override
	public double getAbsoluteEntropy(final String fileContent) {
		return getAbsoluteEntropy(fileContent, "");
	}

	public double getAbsoluteEntropy(final String fileContent,
			final String className) {
		final char[] code = fileContent.toCharArray();
		if (code.length == 0) {
			return 0;
		}
		final List<FullToken> tokens = ImmutableList.copyOf(baseNgram
				.getTokenizer().getTokenListFromCode(code));
		if (tokens.isEmpty()) {
			return 0;
		}
		return getLogProbOfSentence(tokens, className);
	}

	/**
	 * @param file
	 */
	private String getCurrentFilename(final File file) {
		final Matcher m = fileExtention.matcher(file.getName());
		final int size;
		if (m.find()) {
			size = m.group().length();
		} else {
			size = 0;
		}
		return file.getName().substring(0, file.getName().length() - size);
	}

	@Override
	public double getExtrinsticEntropy(final File file) throws IOException {
		return getExtrinsticEntropy(FileUtils.readFileToString(file),
				getCurrentFilename(file));
	}

	@Override
	public double getExtrinsticEntropy(final String fileContent) {
		return getExtrinsticEntropy(fileContent, "");
	}

	public double getExtrinsticEntropy(final String fileContent,
			final String className) {
		final char[] code = fileContent.toCharArray();
		if (code.length == 0) {
			return 0;
		}
		final List<FullToken> tokens = ImmutableList.copyOf(baseNgram
				.getTokenizer().getTokenListFromCode(code));
		if (tokens.isEmpty()) {
			return 0;
		}
		return getLogProbOfSentence(tokens, className) / (tokens.size() - 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.lm.ILanguageModel#getImmutableVersion()
	 */
	@Override
	public ILanguageModel getImmutableVersion() {
		return this;
	}

	private double getLogProbOfSentence(final List<FullToken> sentence,
			final String className) {
		final ICache<String> cache = createCache(className);
		double logProb = 0;

		final List<String> tokenValues = Lists.newArrayList();
		final List<String> tokenTypes = Lists.newArrayList();
		for (int i = 0; i < sentence.size(); i++) {
			tokenValues.add(sentence.get(i).token);
			tokenTypes.add(sentence.get(i).tokenType);
		}

		for (int i = 0; i < sentence.size(); ++i) {
			final NGram<String> tokTypeNgram = NGram.constructNgramAt(i,
					tokenTypes, typeNgram.getN());
			final NGram<String> valueNgram = NGram.constructNgramAt(i,
					tokenValues, baseNgram.getN());

			final double probOfIdentifier = typeNgram
					.getProbabilityFor(constructIdentNgram(tokTypeNgram));

			if (valueNgram.size() > 1) {
				final double prob = probOfIdentifier
						* getProbabilityFor(valueNgram, cache, probOfIdentifier)
						+ (1. - probOfIdentifier)
						* baseNgram.getProbabilityFor(valueNgram);
				checkArgument(prob > 0);
				checkArgument(!Double.isInfinite(prob));
				logProb += DoubleMath.log2(prob);
			}

			if (tokTypeNgram.get(tokTypeNgram.size() - 1).equals(
					identiferNameType)) {
				cache.pushElement(valueNgram.get(valueNgram.size() - 1));
			}
		}
		return logProb;
	}

	public double getProbabilityFor(final NGram<String> ngram,
			final ICache<String> cache, final double probOfIdentifier) {
		final double ngramProb = baseNgram.getProbabilityFor(ngram);
		final String lastWord = ngram.get(ngram.size() - 1);
		final double cacheProb = cache.getProbabilityFor(lastWord);

		return (1 - cacheWeight) * ngramProb + cacheWeight * cacheProb;
	}

	@Override
	public ITokenizer getTokenizer() {
		return baseNgram.getTokenizer();
	}

	@Override
	public AbstractFileFilter modelledFilesFilter() {
		return baseNgram.modelledFilesFilter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * codemining.lm.ILanguageModel#trainIncrementalModel(java.util.Collection)
	 */
	@Override
	public void trainIncrementalModel(final Collection<File> files)
			throws IOException {
		throw new UnsupportedOperationException(
				"CachedNGramLM is an immutable Language Model");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.lm.ILanguageModel#trainModel(java.util.Collection)
	 */
	@Override
	public void trainModel(final Collection<File> files) throws IOException {
		throw new UnsupportedOperationException(
				"CachedNGramLM is an immutable Language Model");
	}
}
