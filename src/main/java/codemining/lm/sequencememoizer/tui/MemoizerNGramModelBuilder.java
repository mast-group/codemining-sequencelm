/**
 * 
 */
package codemining.lm.sequencememoizer.tui;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import codemining.languagetools.ITokenizer;
import codemining.lm.sequencememoizer.SequenceMemoizerLM;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class MemoizerNGramModelBuilder {

	private static final Logger LOGGER = Logger
			.getLogger(MemoizerNGramModelBuilder.class.getName());

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SerializationException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			SerializationException, IOException {
		if (args.length != 3) {
			System.err
					.println("Usage <TrainingFiles> <NGramModel.ser output> <tokenizationClass>");
			return;
		}

		final ITokenizer tokenizer;

		final Class<? extends ITokenizer> tokenizerName = (Class<? extends ITokenizer>) Class
				.forName(args[2]);
		tokenizer = tokenizerName.newInstance();

		final SequenceMemoizerLM dict = new SequenceMemoizerLM(tokenizer);

		LOGGER.info("Sequence memoizer creater for files in " + args[0]
				+ " using " + args[2] + " tokenizer");

		final Collection<File> files = FileUtils.listFiles(new File(args[0]),
				dict.modelledFilesFilter(), DirectoryFileFilter.DIRECTORY);
		dict.trainModel(files);

		LOGGER.info("Sequence Memoizer model build. Serializing...");
		dict.serializeToDisk(args[1]);

	}

}
