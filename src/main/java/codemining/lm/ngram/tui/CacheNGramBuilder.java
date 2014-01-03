/**
 * 
 */
package codemining.lm.ngram.tui;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import codemining.lm.ngram.AbstractNGramLM;
import codemining.lm.ngram.cache.IdentifierOnlyCachedNGramLM;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

/**
 * Build a cache ngram model.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class CacheNGramBuilder {

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SerializationException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException, SerializationException {
		if (args.length != 4) {
			System.err
					.println("Usage <fullNGram> <typeNGram> <ParamCalibrationFiles> <output>");
			return;
		}

		final AbstractNGramLM fullNGram = ((AbstractNGramLM) Serializer
				.getSerializer().deserializeFrom(args[0]));

		final AbstractNGramLM typeNGram = ((AbstractNGramLM) Serializer
				.getSerializer().deserializeFrom(args[1]));

		final Collection<File> testFiles = FileUtils.listFiles(
				new File(args[2]), fullNGram.modelledFilesFilter(),
				DirectoryFileFilter.DIRECTORY);

		final IdentifierOnlyCachedNGramLM cachedLM = new IdentifierOnlyCachedNGramLM(
				fullNGram, typeNGram, fullNGram.getTokenizer()
						.getIdentifierType(), testFiles);
		Serializer.getSerializer().serialize(cachedLM, args[3]);
	}
}
