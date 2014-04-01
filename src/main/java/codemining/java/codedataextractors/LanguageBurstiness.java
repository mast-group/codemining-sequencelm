/**
 * 
 */
package codemining.java.codedataextractors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ITokenGeneratingLanguageModel;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.google.common.collect.Maps;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class LanguageBurstiness {

	private static class Occurences {
		int frequency = 0;
		double totalAvgLength = 0;
	}

	private static class WordPosition {
		int occurences = 0;
		int lastPosition = 0;
		int totalLengthBetween = 0;
	}

	protected static final Logger LOGGER = Logger
			.getLogger(LanguageBurstiness.class.getName());

	public static void main(final String[] args) throws ClassNotFoundException,
			SerializationException, FileNotFoundException, Exception {
		if (args.length < 2) {
			System.err
					.println("Usage [empirical|generated] dataFolder|model numOfFileToGen");
			return;
		}

		final LanguageBurstiness lb = new LanguageBurstiness();

		if (args[0].equals("empirical")) {
			final JavaTokenizer tok = new JavaTokenizer();
			lb.getEmpirical(tok, args[1]);
		} else if (args[0].equals("generated")) {
			String dir = args[1];
			int nFiles = Integer.parseInt(args[2]);

			lb.getGenerated(dir, nFiles);

		}

		lb.printOccurences();
	}

	final Map<Integer, Occurences> data = Maps.newTreeMap();

	/**
	 * Add the burstiness counts to the global data.
	 * 
	 * @param codeTokens
	 */
	void addFileCounts(final List<FullToken> codeTokens,
			final String identifierType) {
		final Map<String, WordPosition> wordData = extractWordData(codeTokens,
				identifierType);

		for (final Entry<String, WordPosition> ent : wordData.entrySet()) {
			final int timesRepeated = ent.getValue().occurences;
			if (!data.containsKey(timesRepeated)) {
				data.put(timesRepeated, new Occurences());
			}
			final Occurences oc = data.get(timesRepeated);
			oc.frequency++;
			final WordPosition pos = ent.getValue();
			oc.totalAvgLength += ((double) pos.totalLengthBetween)
					/ ((double) pos.occurences);
		}
	}

	/**
	 * Extract word position data from a list of tokens.
	 * 
	 * @param codeTokens
	 * @return
	 */
	private Map<String, WordPosition> extractWordData(
			final List<FullToken> codeTokens, final String identifierType) {
		final Map<String, WordPosition> wordData = new TreeMap<String, WordPosition>();

		for (int i = 0; i < codeTokens.size(); i++) {
			final FullToken token = codeTokens.get(i);
			if (!token.tokenType.equals(identifierType)) {
				continue;
			}
			if (!wordData.containsKey(token.token)) {
				final WordPosition p = new WordPosition();
				p.occurences = 1;
				p.lastPosition = i;
				wordData.put(token.token, p);
			} else {
				final WordPosition p = wordData.get(token.token);
				p.occurences++;
				p.totalLengthBetween += i - p.lastPosition;
				p.lastPosition = i;
			}
		}
		return wordData;
	}

	/**
	 * @param lb
	 * @param tok
	 * @param directory
	 */
	private void getEmpirical(final ITokenizer tok, final String directory) {
		final Collection<File> testFiles = FileUtils.listFiles(new File(
				directory), tok.getFileFilter(), DirectoryFileFilter.DIRECTORY);

		for (final File f : testFiles) {
			try {
				final List<FullToken> toks = tok.getTokenListFromCode(FileUtils
						.readFileToString(f).toCharArray());
				addFileCounts(toks, tok.getIdentifierType());
			} catch (IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}
	}

	/**
	 * @param lb
	 * @param dir
	 * @param nFiles
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SerializationException
	 */
	private void getGenerated(String dir, int nFiles)
			throws FileNotFoundException, ClassNotFoundException,
			SerializationException {
		final ITokenGeneratingLanguageModel<FullToken> lm = ((ITokenGeneratingLanguageModel<FullToken>) Serializer
				.getSerializer().deserializeFrom(dir));

		for (int i = 0; i < nFiles; i++) {
			final List<FullToken> codeToks = lm.generateSentence();
			addFileCounts(codeToks, lm.getTokenizer().getIdentifierType());
		}
	}

	void printOccurences() {
		for (final Entry<Integer, Occurences> ent : data.entrySet()) {
			System.out.println(ent.getKey() + ","
					+ ent.getValue().totalAvgLength / ent.getValue().frequency
					+ "," + ent.getValue().frequency);
		}
	}
}
