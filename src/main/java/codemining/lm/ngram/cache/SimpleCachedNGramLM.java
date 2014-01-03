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
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ILanguageModel;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.lm.ngram.cache.ParameterOptimizer.LPair;
import codemining.lm.ngram.cache.SymbolicWeightCache.DecayFactor;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.math.DoubleMath;

/**
 * A cached ngram model with a single cache and no parameter tuning.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class SimpleCachedNGramLM implements ILanguageModel {

	/**
	 * Extract the values of the cache and n-gram probablity from the model. The
	 * values can then be passed into the ParameterOptimizer.
	 * 
	 * This class extends its superclass. This is very odd, but it is a good
	 * hack to reuse the same methods...
	 * 
	 */
	class SimpleCacheValueExtractor extends SimpleCachedNGramLM {

		private static final long serialVersionUID = 1L;

		final Multiset<ParameterOptimizer.LPair> elems = HashMultiset.create();

		SimpleCacheValueExtractor(final Collection<File> tuneFiles)
				throws IOException {
			super(SimpleCachedNGramLM.this.baseNgram, 0.5, 0.5);
			for (final File f : tuneFiles) {
				this.getAbsoluteEntropy(f);
			}
		}

		@Override
		protected ICache<String> createCache(final String firstEntry) {
			return new SymbolicWeightCache<String>(.5, firstEntry);
		}

		@Override
		public double getProbabilityFor(final NGram<String> ngram,
				final ICache<String> cache) {
			final double ngramProb = baseNgram.getProbabilityFor(ngram);
			final String lastWord = ngram.get(ngram.size() - 1);
			final SymbolicWeightCache<String> symbolicCache = (SymbolicWeightCache<String>) cache;
			final List<DecayFactor> cacheProb = symbolicCache
					.getDecayFactorFor(lastWord);

			final LPair ngramPair = new LPair();
			ngramPair.ngramProb = ngramProb;
			if (cacheProb != null) {
				ngramPair.cacheProb = Lists.newArrayList(cacheProb);
			} else {
				ngramPair.cacheProb = Lists.newArrayList();
			}
			checkArgument(ngramProb > 0 && ngramProb <= 1,
					"N-gram probablity should be between 0,1 but is "
							+ ngramProb);
			elems.add(ngramPair);

			symbolicCache.pushElement(lastWord);
			return .5; // Should not be used, but we have to return it...
		}

	}

	private static final long serialVersionUID = 8666715787641598671L;

	private final double cacheWeight;

	private final double cacheDecayConst;

	private final Pattern fileExtention = Pattern.compile("\\.[a-z]+$");

	final AbstractNGramLM baseNgram;

	protected static final Logger LOGGER = Logger
			.getLogger(SimpleCachedNGramLM.class.getName());

	public static void main(String[] args) throws ClassNotFoundException,
			IOException, SerializationException {
		if (args.length != 3) {
			System.err
					.println("Usage <fullNGram> <paramTuningFolder> <output>");
			return;
		}
		final AbstractNGramLM fullNGram = ((AbstractNGramLM) Serializer
				.getSerializer().deserializeFrom(args[0]));

		final Collection<File> testFiles = FileUtils.listFiles(
				new File(args[1]), fullNGram.modelledFilesFilter(),
				DirectoryFileFilter.DIRECTORY);

		final SimpleCachedNGramLM cachedLM = new SimpleCachedNGramLM(fullNGram,
				testFiles);
		Serializer.getSerializer().serialize(cachedLM, args[2]);
	}

	public SimpleCachedNGramLM(final AbstractNGramLM baseNgram,
			final Collection<File> parameterCalibrationFiles)
			throws IOException {
		this.baseNgram = baseNgram;

		final SimpleCacheValueExtractor opt = new SimpleCacheValueExtractor(
				parameterCalibrationFiles);
		final ParameterOptimizer pOpt = new ParameterOptimizer(opt.elems);
		pOpt.optimizeParameters();
		cacheWeight = pOpt.currentLambda;
		cacheDecayConst = pOpt.decay;

	}

	/**
	 * @param baseNgram
	 */
	public SimpleCachedNGramLM(final AbstractNGramLM baseNgram,
			final double cacheLambda, final double cacheDecay) {
		this.baseNgram = baseNgram;
		cacheWeight = cacheLambda;
		cacheDecayConst = cacheDecay;
	}

	/**
	 * @param firstEntry
	 * @return
	 */
	protected ICache<String> createCache(final String firstEntry) {
		return new WeightCache<String>(cacheDecayConst, firstEntry);
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
		if (code.length == 0)
			return 0;
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
		if (code.length == 0)
			return 0;
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

		final List<String> stringToks = Lists.newArrayList();
		for (int i = 0; i < sentence.size(); i++) {
			stringToks.add(sentence.get(i).token);
		}

		for (int i = 0; i < sentence.size(); ++i) {
			final NGram<String> ngram = NGram.constructNgramAt(i, stringToks,
					baseNgram.getN());
			if (ngram.size() > 1) {
				final double prob = getProbabilityFor(ngram, cache);

				checkArgument(prob > 0);
				checkArgument(!Double.isInfinite(prob));
				logProb += DoubleMath.log2(prob);
			}
		}
		return logProb;
	}

	public double getProbabilityFor(final NGram<String> ngram,
			final ICache<String> cache) {
		final double ngramProb = baseNgram.getProbabilityFor(ngram);
		final String lastWord = ngram.get(ngram.size() - 1);
		final double cacheProb = cache.getProbabilityFor(lastWord);
		cache.pushElement(lastWord);
		return (1 - cacheWeight) * ngramProb + cacheWeight * cacheProb;
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
	public void trainIncrementalModel(Collection<File> files)
			throws IOException {
		throw new UnsupportedOperationException(
				"CachedNGramLM cannot is an immutable Language Model");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.lm.ILanguageModel#trainModel(java.util.Collection)
	 */
	@Override
	public void trainModel(Collection<File> files) throws IOException {
		throw new UnsupportedOperationException(
				"CachedNGramLM cannot is an immutable Language Model");
	}

}
