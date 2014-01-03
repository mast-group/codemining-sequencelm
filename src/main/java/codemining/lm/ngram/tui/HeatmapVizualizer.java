/**
 * 
 */
package codemining.lm.ngram.tui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.java.codeutils.JavaTokenizer;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class HeatmapVizualizer {

	private static final Logger LOGGER = Logger
			.getLogger(HeatmapVizualizer.class.getName());

	private static void addSlack(final String substring, final StringBuffer buf) {
		for (char c : substring.toCharArray()) {
			if (c == ' ') {
				buf.append("&nbsp;");
			} else if (c == '\n') {
				buf.append("<br/>\n");
			} else if (c == '\t') {
				buf.append("&nbsp;&nbsp;&nbsp;");
			} else {
				buf.append(c);
			}
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SerializationException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			SerializationException {
		if (args.length != 3) {
			System.err.println("Usage <fileDir> <lmModel> <tokenizerClass>");
			return;
		}

		final AbstractNGramLM lm = AbstractNGramLM.readFromSerialized(args[1]);
		for (File f : FileUtils.listFiles(new File(args[0]),
				lm.modelledFilesFilter(), DirectoryFileFilter.DIRECTORY)) {
			try {
				writeHTMLwithXent(lm, f);
			} catch (Exception e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}

	}

	/**
	 * @param lm
	 * @param codeFile
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected static void writeHTMLwithXent(final AbstractNGramLM lm,
			final File codeFile) throws IOException, InstantiationException,
			IllegalAccessException {
		final String code = FileUtils.readFileToString(codeFile);
		final File outFile = new File(codeFile.getName() + ".html");

		final StringBuffer buf = new StringBuffer();

		final ITokenizer jTokenizer = new JavaTokenizer(); // TODO: get from
																// main
		final SortedMap<Integer, FullToken> toks = jTokenizer
				.fullTokenListWithPos(code.toCharArray());

		final ITokenizer tokenizer = lm.getTokenizer();
		final List<String> tokenList = tokenizer.tokenListFromCode(code
				.toCharArray());

		int i = 0;
		int prevPos = 0;
		buf.append("<html><body style='font-family:monospace; background-color:rgb(200,200,255)'>");
		for (Entry<Integer, FullToken> entry : toks.entrySet()) {
			if (i == 0 || entry.getKey() == Integer.MAX_VALUE) {
				i++;
				continue;
			}
			final NGram<String> ngram = NGram.constructNgramAt(i, tokenList,
					lm.getN());
			if (ngram.size() > 1) {
				final double prob = lm.getProbabilityFor(ngram);
				String extraStyle = "";

				if (lm.getTrie().isUNK(tokenList.get(i))) {
					extraStyle = "text-decoration:underline; ";
				}

				addSlack(code.substring(prevPos, entry.getKey()), buf);
				long hue = 0;
				long saturation = 240;
				long luminosity = 100 - Math
						.round((100 * -Math.log(prob) / 20));
				if (luminosity < 0) {
					luminosity = 0;
				}
				buf.append("<span style='background-color:hsl(" + hue + ","
						+ saturation + "%," + luminosity + "%); color:hsl("
						+ hue + "," + saturation + "%,"
						+ ((luminosity + 50) % 100) + "%); " + extraStyle
						+ "' data='" + prob + "'>" + entry.getValue().token
						+ "</span>");
			}

			i++;
			prevPos = entry.getKey() + entry.getValue().token.length();
		}
		buf.append("</body></html>");
		FileUtils.writeStringToFile(outFile, buf.toString());
	}
}
