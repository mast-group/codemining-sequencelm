/**
 * 
 */
package codemining.lm;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.io.filefilter.AbstractFileFilter;

/**
 * An interface for a code language model.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public interface ILanguageModel extends Serializable {

	/**
	 * Return the absolute entropy of the file (not normalized)
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	double getAbsoluteEntropy(final File file) throws IOException;

	double getAbsoluteEntropy(final String fileContent);

	/**
	 * Return the extrinstic entropy (e.g. per token) of the language model on a
	 * set of test files.
	 * 
	 * @param testFolder
	 *            the directory where the test file exist
	 * @return the cross-entropy of the test files (in the testFolder) for this
	 *         model
	 * @throws IOException
	 */
	double getExtrinsticEntropy(final File file) throws IOException;

	double getExtrinsticEntropy(final String fileContent);

	/**
	 * Return an immutable version of the language model
	 */
	ILanguageModel getImmutableVersion();

	/**
	 * 
	 * @return a file filter of the files that this LM is modeling.
	 */
	AbstractFileFilter modelledFilesFilter();

	/**
	 * Incrementally train a model from a collection of files.
	 * 
	 */
	void trainIncrementalModel(final Collection<File> files) throws IOException;

	/**
	 * Build a model from a set of files.
	 * 
	 */
	void trainModel(final Collection<File> files) throws IOException;

}
