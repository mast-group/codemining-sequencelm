/**
 * 
 */
package codemining.lm.ngram.tui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.TokenizerUtils;
import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.NGramLM;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class NGramModelBuilder {

	private static final Logger LOGGER = Logger
			.getLogger(NGramModelBuilder.class.getName());

	public static void main(final String[] args) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, SerializationException {

		if (args.length != 5) {
			System.err
					.println("Usage <TrainingFiles> <N> <NGramModel.ser output> <tokenizationClass> <WrapperSmootherClass>");
			return;
		}

		final ITokenizer tokenizer = TokenizerUtils.tokenizerForClass(args[3]);

		final NGramLM dict = new NGramLM(Integer.parseInt(args[1]), tokenizer);

		LOGGER.info("NGram Model creater started with " + args[1]
				+ "-gram for files in " + args[0] + " using " + args[3]
				+ " tokenizer");

		final Collection<File> files = FileUtils.listFiles(new File(args[0]),
				dict.modelledFilesFilter(), DirectoryFileFilter.DIRECTORY);

		dict.trainModel(files);

		LOGGER.info("Ngram model build. Adding Smoother...");
		final Class<? extends AbstractNGramLM> smoothedNgramClass = (Class<? extends AbstractNGramLM>) Class
				.forName(args[4]);
		final AbstractNGramLM ng = (AbstractNGramLM) smoothedNgramClass
				.getDeclaredConstructor(AbstractNGramLM.class)
				.newInstance(dict);

		LOGGER.info("Ngram model build. Serializing...");
		ng.serializeToDisk(args[2]);

	}
}
