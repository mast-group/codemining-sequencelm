/**
 * 
 */
package codemining.lm.ngram.tui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.languagetools.CodePrinter;
import codemining.languagetools.ColoredToken;
import codemining.languagetools.ITokenizer;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGram;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

import com.google.common.collect.Lists;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class HeatmapVizualizer extends CodePrinter {

	private static final Logger LOGGER = Logger
			.getLogger(HeatmapVizualizer.class.getName());

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SerializationException
	 */
	public static void main(final String[] args) throws ClassNotFoundException,
			SerializationException {
		if (args.length != 3) {
			System.err.println("Usage <fileDir> <lmModel>");
			return;
		}

		final AbstractNGramLM lm = AbstractNGramLM.readFromSerialized(args[1]);
		final HeatmapVizualizer visualizer = new HeatmapVizualizer(
				lm.getTokenizer(), new Color(200f / 255, 200f / 255, 1));
		for (final File f : FileUtils.listFiles(new File(args[0]),
				lm.modelledFilesFilter(), DirectoryFileFilter.DIRECTORY)) {
			try {
				final List<ColoredToken> coloredTokens = visualizer
						.colorTokens(lm, f);
				visualizer.getHTMLwithColors(coloredTokens, f); // TODO write
																	// it
																	// somewhere
																	// (get
																	// params
																	// from
																	// args)
			} catch (final Exception e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}

	}

	private HeatmapVizualizer(final ITokenizer tokenizer, final Color bgColor) {
		super(tokenizer, bgColor);
	}

	private List<ColoredToken> colorTokens(final AbstractNGramLM lm,
			final File f) throws IOException {
		final ITokenizer tokenizer = lm.getTokenizer();
		final List<String> tokenList = tokenizer.tokenListFromCode(FileUtils
				.readFileToString(f).toCharArray());
		final List<ColoredToken> coloredTokens = Lists.newArrayList();
		for (int i = 0; i < tokenList.size(); i++) {
			final NGram<String> ngram = NGram.constructNgramAt(i, tokenList,
					lm.getN());
			if (ngram.size() > 1) {
				final double prob = lm.getProbabilityFor(ngram);
				String extraStyle = "";

				if (lm.getTrie().isUNK(tokenList.get(i))) {
					extraStyle = "text-decoration:underline; ";
				}

				final float hue = 0;
				final float saturation = 2.40f;
				float luminosity = (float) (100. - Math.round((100 * -Math
						.log(prob) / 20))) / 100;
				if (luminosity < 0) {
					luminosity = 0;
				}

				coloredTokens.add(new ColoredToken(tokenList.get(i), Color
						.getHSBColor(hue, saturation,
								((luminosity + 50) % 100) / 100), Color
						.getHSBColor(hue, saturation, luminosity), extraStyle));
			} else {
				coloredTokens.add(new ColoredToken(tokenList.get(i),
						Color.BLACK));
			}
		}

		return coloredTokens;
	}
}
