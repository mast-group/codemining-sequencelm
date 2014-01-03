/**
 * 
 */
package codemining.java.codedataextractors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import codemining.java.codedata.MethodRetriever;
import codemining.java.codedata.metrics.IFileMetricRetriever;
import codemining.lm.LMComplexity;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

/**
 * A class that prints the entropies & complexity of each method.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class PerMethodMetrics {

	private static final Logger LOGGER = Logger
			.getLogger(PerMethodMetrics.class.getName());

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SerializationException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			SerializationException {
		if (args.length < 2) {
			System.err.println("Usage <projectDir> <lmModel> [ <metric1> ...]");
			// TODO: lm models can present their own per method
			// IFileMetricRetriever
			return;
		}

		final LMComplexity comp = new LMComplexity(args[1]);
		final IFileMetricRetriever[] metrics = new IFileMetricRetriever[args.length - 2];
		for (int i = 2; i < args.length; i++) {
			metrics[i - 2] = ((Class<? extends IFileMetricRetriever>) Class
					.forName(args[i])).newInstance();
		}

		final File baseDir = new File(args[0]);
		for (final File f : FileUtils.listFiles(baseDir, comp
				.getLanguageModel().modelledFilesFilter(),
				DirectoryFileFilter.DIRECTORY)) {
			LOGGER.info("Analyzing file " + f.getAbsolutePath());
			try {
				Map<String, MethodDeclaration> methods = MethodRetriever
						.getMethodNodes(f);
				for (final Entry<String, MethodDeclaration> entry : methods
						.entrySet()) {
					final double entropy = comp.getLanguageModel()
							.getAbsoluteEntropy(entry.getValue().toString());
					final double perTokenXent = comp.getLanguageModel()
							.getExtrinsticEntropy(entry.getValue().toString());
					String output = f.getAbsolutePath() + "." + entry.getKey()
							+ " " + entropy + " " + perTokenXent + " ";
					for (int i = 0; i < metrics.length; i++) {
						final double secondaryMetric = metrics[i]
								.getMetricForASTNode(entry.getValue());
						output += secondaryMetric + " ";
					}
					System.out.println(output);
				}

			} catch (IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}

	}
}
