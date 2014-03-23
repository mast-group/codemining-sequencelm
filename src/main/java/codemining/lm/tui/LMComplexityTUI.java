/**
 * 
 */
package codemining.lm.tui;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.lm.LMComplexity;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class LMComplexityTUI {

	private static final Logger LOGGER = Logger.getLogger(LMComplexityTUI.class
			.getName());

	/**
	 * Terminal User Interface for computing the
	 * 
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws SerializationException
	 */
	public static void main(final String[] args) throws ClassNotFoundException,
			IOException, SerializationException {
		if (args.length < 2 || args.length > 3) {
			System.err
					.println("Usage <projectDir> <lmModel> [printAbsolutePerFile]");
			return;
		}

		final LMComplexity comp = new LMComplexity(args[1]);

		final File baseDir = new File(args[0]);
		LOGGER.info("Crawling project folders in " + args[0]);
		if (args.length == 2 || args[2].equals("false")) {
			for (final File project : baseDir.listFiles()) {
				printProjectAvgCrossEntropy(project, comp);
			}
		} else {
			for (final File f : FileUtils.listFiles(baseDir, comp
					.getLanguageModel().modelledFilesFilter(),
					DirectoryFileFilter.DIRECTORY)) {
				try {
					final double crossEntropy = comp.getLanguageModel()
							.getExtrinsticEntropy(f);
					final double entropy = comp.getLanguageModel()
							.getAbsoluteEntropy(f);
					System.out.println(f.getAbsolutePath() + "," + crossEntropy
							+ "," + entropy);
				} catch (final IOException e) {
					LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
				}
			}
		}

		LOGGER.info("Complexity printer finished");
	}

	public static void printProjectAvgCrossEntropy(final File projectDir,
			final LMComplexity comp) {
		final double modelAvgCrossEntropy = comp
				.getAvgProjectCrossEntropy(projectDir);

		System.out.print(projectDir.getAbsolutePath() + " ");
		System.out.print(modelAvgCrossEntropy + " ");
		System.out.println();
	}
}
