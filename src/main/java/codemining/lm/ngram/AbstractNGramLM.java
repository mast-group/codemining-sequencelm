package codemining.lm.ngram;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang.math.RandomUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ITokenGeneratingLanguageModel;
import codemining.lm.ngram.Trie.TrieNode;
import codemining.util.SettingsLoader;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultiset;
import com.google.common.math.DoubleMath;

/**
 * An abstract ngram language model.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public abstract class AbstractNGramLM implements
		ITokenGeneratingLanguageModel<FullToken> {

	private static final long serialVersionUID = -5876426022517622146L;

	public static final String UNK_SYMBOL = "UNK_SYMBOL";

	public static final boolean DEBUG_PROBS = SettingsLoader.getBooleanSetting(
			"debugProbs", false);

	private static final Logger LOGGER = Logger.getLogger(AbstractNGramLM.class
			.getName());

	/**
	 * @param ngram
	 * @param prob
	 * @return
	 */
	public static String getProbString(final NGram<String> ngram,
			final double prob) {
		final StringBuffer buf = new StringBuffer();
		buf.append("P(");
		buf.append(ngram.get(ngram.size() - 1));
		buf.append('|');
		buf.append(ngram.getPrefix());
		buf.append(")= ");
		buf.append(prob);
		return buf.toString();
	}

	public static AbstractNGramLM readFromSerialized(final String filename)
			throws SerializationException {
		final AbstractNGramLM ngrams = (AbstractNGramLM) Serializer
				.getSerializer().deserializeFrom(filename);
		return ngrams;
	}

	public static AbstractNGramLM readSerialized(final String filename)
			throws SerializationException {
		return (AbstractNGramLM) Serializer.getSerializer().deserializeFrom(
				filename);
	}

	/**
	 * The size of the ngram dictionary.
	 */
	private int nGramSize;

	protected ITokenizer tokenizer;

	protected LongTrie<String> trie;

	protected AbstractNGramLM() {

	}

	/**
	 * Return an immutable copy of this abstractNGramLM
	 * 
	 * @param original
	 */
	protected AbstractNGramLM(final AbstractNGramLM original) {
		nGramSize = original.nGramSize;
		trie = original.trie; // TODO: Need a deep (immutable) copy?
		tokenizer = original.getTokenizer();
	}

	public AbstractNGramLM(final int size, final ITokenizer tokenizerModule) {
		nGramSize = size;
		trie = new LongTrie<String>(UNK_SYMBOL);
		tokenizer = tokenizerModule;
	}

	public abstract void addFromSentence(final List<String> sentence,
			boolean addNewVoc);

	protected abstract void addNgram(final NGram<String> ngram,
			final boolean addNewVoc);

	public abstract void addSentences(
			final Collection<List<String>> sentenceSet,
			final boolean addNewVocabulary);

	public abstract void cutoffRare(final int threshold);

	/**
	 * Generate a random sentence from the model.
	 * 
	 * @return
	 */
	@Override
	public List<FullToken> generateSentence() {
		final List<String> productionStr = Lists.newArrayList();
		String nextToken = ITokenizer.SENTENCE_START;
		while (!nextToken.equals(ITokenizer.SENTENCE_END)) {
			productionStr.add(nextToken);

			// Generate next token
			final NGram<String> prefix = NGram.constructNgramAt(
					productionStr.size() - 1, productionStr, getN() - 1);
			nextToken = pickRandom(prefix);
		}
		productionStr.add(nextToken);

		final List<FullToken> production = Lists.newArrayList();
		for (final String token : productionStr) {
			production.add(tokenizer.getTokenFromString(token));
		}

		return production;
	}

	@Override
	public double getAbsoluteEntropy(final File file) throws IOException {
		return getAbsoluteEntropy(FileUtils.readFileToString(file));
	}

	@Override
	public double getAbsoluteEntropy(final String fileContent) {
		final char[] code = fileContent.toCharArray();
		if (code.length == 0) {
			return 0;
		}
		final List<String> tokens = ImmutableList.copyOf(getTokenizer()
				.tokenListFromCode(code));
		if (tokens.isEmpty()) {
			return 0;
		}
		final double sentenceProb = getLogProbOfSentence(tokens);

		return sentenceProb;
	}

	public Multiset<String> getAlternativeNamings(
			final Multiset<NGram<String>> ngrams, final String tokenToSubstitute) {
		final Multiset<String> namings = TreeMultiset.create();
		final LongTrie<String> globalTrie = getTrie();

		for (final Multiset.Entry<NGram<String>> ngramEntry : ngrams.entrySet()) {
			final NGram<String> ngram = ngramEntry.getElement();

			final Set<String> alternatives = checkNotNull(getAlternativesForNGram(
					globalTrie, ngram, tokenToSubstitute));
			namings.addAll(alternatives);
		}

		return namings;
	}

	/**
	 * Return name alternatives for ngram.
	 * 
	 * @param globalTrie
	 * @param ngram
	 * @return
	 */
	public Set<String> getAlternativesForNGram(
			final LongTrie<String> globalTrie, final NGram<String> ngram,
			final String tokenToSubstitute) {
		// First get the n-gram up to the wildcard
		NGram<String> prefix = ngram;
		while (!prefix.get(prefix.size() - 1).contains(tokenToSubstitute)) {
			prefix = prefix.getPrefix();
		}
		prefix = prefix.getPrefix(); // Remove the substitute token

		// Then get the node that has as children all possible names
		final TrieNode<Long> sNode = globalTrie.getNGramNodeForInput(prefix,
				false);
		if (sNode == null) {
			return Collections.emptySet();
		}

		// Then for each child construct one ngram replacing the wildcard
		// unless this is the "last" N
		final Set<String> renamings = Sets.newTreeSet();
		if (prefix.size() != getN() - 1) {

			final int prefixSize = prefix.size();
			final int ngramSize = ngram.size();
			final NGram<String> suffix = new NGram<String>(ngram,
					prefixSize + 1, ngramSize);

			for (final Map.Entry<Long, TrieNode<Long>> entry : sNode.prods
					.entrySet()) {
				final String token = globalTrie
						.getSymbolFromKey(entry.getKey());
				final NGram<String> replacedNgramSuffix = NGram
						.substituteTokenWith(suffix, tokenToSubstitute, token); // The
																				// current
																				// tokenToSubstitute,
																				// but
																				// also
																				// any
				// future appearances in the n-gram
				final TrieNode<Long> fNode = globalTrie.getNGramNodeForInput(
						replacedNgramSuffix, false, entry.getValue());
				if (fNode != null) {
					renamings.add(token);
				}
			}
		} else {
			for (final Long key : sNode.prods.keySet()) {
				final String token = globalTrie.getSymbolFromKey(key);
				renamings.add(token);
			}
		}
		return renamings;
	}

	@Override
	public double getExtrinsticEntropy(final File file) throws IOException {
		return getExtrinsticEntropy(FileUtils.readFileToString(file));
	}

	@Override
	public double getExtrinsticEntropy(final String fileContent) {
		final char[] code = fileContent.toCharArray();
		if (code.length == 0) {
			return 0;
		}
		final List<String> tokens = ImmutableList.copyOf(getTokenizer()
				.tokenListFromCode(code));
		if (tokens.isEmpty()) {
			return 0;
		}
		final double sentenceProb = getLogProbOfSentence(tokens);

		return sentenceProb / (tokens.size() - 1.);
	}

	public ArrayList<Double> getLogProbDistOfSentence(final String fileContent) {
		final ArrayList<Double> logProbDist = new ArrayList<Double>();

		final char[] code = fileContent.toCharArray();
		if (code.length == 0) {
			return logProbDist;
		}
		final List<String> tokens = ImmutableList.copyOf(getTokenizer()
				.tokenListFromCode(code));
		if (tokens.isEmpty()) {
			return logProbDist;
		}

		for (int i = 0; i < tokens.size(); ++i) {
			final NGram<String> ngram = NGram.constructNgramAt(i, tokens,
					nGramSize);
			if (ngram.size() > 1) {
				final double prob = getProbabilityFor(ngram);
				if (AbstractNGramLM.DEBUG_PROBS) {
					LOGGER.info(AbstractNGramLM.getProbString(
							trie.substituteWordsToUNK(ngram), prob));
				}
				checkArgument(prob > 0);
				checkArgument(!Double.isInfinite(prob));
				logProbDist.add(DoubleMath.log2(prob));
			}
		}
		return logProbDist;
	}

	public double getLogProbOfSentence(final List<String> sentence) {
		double logProb = 0;
		for (int i = 0; i < sentence.size(); ++i) {
			final NGram<String> ngram = NGram.constructNgramAt(i, sentence,
					nGramSize);
			if (ngram.size() > 1) {
				final double prob = getProbabilityFor(ngram);
				if (AbstractNGramLM.DEBUG_PROBS) {
					LOGGER.info(AbstractNGramLM.getProbString(
							trie.substituteWordsToUNK(ngram), prob));
				}
				checkArgument(prob > 0);
				checkArgument(!Double.isInfinite(prob));
				logProb += DoubleMath.log2(prob);
			}
		}
		return logProb;
	}

	/**
	 * Return the frequency that an ngram appears in the dictionary.
	 * 
	 * @param ngram
	 *            the ngram
	 * @return a double representing the ML frequency in the training corpus
	 */
	public double getMLProbabilityFor(final NGram<String> ngram,
			final boolean useUNKs) {
		final long ngramCount = trie.getCount(ngram, useUNKs, true);
		final long productionCount = trie.getCount(ngram.getPrefix(), useUNKs,
				false);
		if (productionCount == 0) {
			return 0;
		}

		checkArgument(ngramCount <= productionCount);
		return ((double) ngramCount) / ((double) (productionCount));
	}

	/**
	 * 
	 * @return the size of the n-grams in the dictionary.
	 */
	public final int getN() {
		return nGramSize;
	}

	/**
	 * Return the (possibly smoothed) probability of an n-gram for this language
	 * model.
	 * 
	 * @param ngram
	 * @return
	 */
	public abstract double getProbabilityFor(final NGram<String> ngram);

	@Override
	public ITokenizer getTokenizer() {
		return tokenizer;
	}

	public final LongTrie<String> getTrie() {
		return trie;
	}

	public final Long getUNKSymbolId() {
		return trie.getUnkSymbolId();
	}

	@Override
	public AbstractFileFilter modelledFilesFilter() {
		return getTokenizer().getFileFilter();
	}

	/**
	 * Pick a random given the prefix.
	 * 
	 * @param prefix
	 *            the prefix
	 * @return
	 */
	public String pickRandom(final NGram<String> prefix) {
		final Map<String, Long> productions = trie
				.getPossibleProductionsWithCounts(prefix);
		if (productions.size() == 0) {
			return pickRandom(prefix.getSuffix());
		}
		double sum = 0;
		for (final Entry<String, Long> entry : productions.entrySet()) {
			sum += entry.getValue();
		}

		final double randomPoint = RandomUtils.nextDouble() * sum;
		long currentSum = 0;
		for (final Entry<String, Long> entry : productions.entrySet()) {
			currentSum += entry.getValue();
			if (currentSum >= randomPoint) {
				return entry.getKey();
			}
		}

		throw new IllegalStateException(
				"Should never reach this point. Picking random production failed.");
	}

	/**
	 * Remove the n-gram from the LM.
	 */
	public abstract void removeNgram(final NGram<String> ngram);

	public void serializeToDisk(final String filename)
			throws SerializationException {
		Serializer.getSerializer().serialize(this, filename);
	}

	@Override
	public String toString() {
		return trie.toString();
	}

}