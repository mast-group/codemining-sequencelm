/**
 * 
 */
package codemining.lm.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.languagetools.ITokenizer;
import codemining.util.parallel.ParallelThreadPool;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset.Entry;

/**
 * A utility class allowing to build in parallel a token vocabulary from a given
 * corpus.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class TokenVocabularyBuilder {

	private static class VocabularyExtractorRunnable implements Runnable {
		final File codeFile;
		final ConcurrentHashMultiset<String> vocabularySet;
		final ITokenizer tokenizer;

		public VocabularyExtractorRunnable(final File file,
				final ConcurrentHashMultiset<String> vocabulary,
				final ITokenizer tokenizerModule) {
			codeFile = file;
			vocabularySet = vocabulary;
			tokenizer = tokenizerModule;
		}

		@Override
		public void run() {
			LOGGER.finer("Reading file " + codeFile.getAbsolutePath());
			try {
				vocabularySet.addAll(tokenizer.tokenListFromCode(codeFile));
			} catch (final IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(TokenVocabularyBuilder.class.getName());;

	/**
	 * Build a set of words in the vocabulary from a collection of files.
	 * 
	 * @param files
	 * @return
	 */
	public static Set<String> buildVocabulary(final Collection<File> files,
			final ITokenizer tokenizer, final int threshold) {
		final ConcurrentHashMultiset<String> vocabulary = ConcurrentHashMultiset
				.create();

		// add everything
		final ParallelThreadPool threadPool = new ParallelThreadPool();

		for (final File fi : files) {
			threadPool.pushTask(new VocabularyExtractorRunnable(fi, vocabulary,
					tokenizer));
		}

		threadPool.waitForTermination();

		// Remove rare
		pruneElementsFromMultiset(threshold, vocabulary);

		LOGGER.info("Vocabulary built, with " + vocabulary.elementSet().size()
				+ " words");

		return vocabulary.elementSet();
	}

	/**
	 * @param threshold
	 * @param vocabulary
	 */
	public static void pruneElementsFromMultiset(final int threshold,
			final ConcurrentHashMultiset<String> vocabulary) {
		final ArrayDeque<Entry<String>> toBeRemoved = new ArrayDeque<Entry<String>>();

		for (final Entry<String> ent : vocabulary.entrySet()) {
			if (ent.getCount() <= threshold) {
				toBeRemoved.add(ent);
			}
		}

		for (final Entry<String> ent : toBeRemoved) {
			vocabulary.remove(ent.getElement(), ent.getCount());
		}
	}

	private TokenVocabularyBuilder() {
	}
}
