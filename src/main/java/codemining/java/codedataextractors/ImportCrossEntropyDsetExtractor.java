/**
 * 
 */
package codemining.java.codedataextractors;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;

import codemining.java.codedata.PackageInfoExtractor;
import codemining.java.codeutils.JavaASTExtractor;
import codemining.lm.LMComplexity;
import codemining.util.serialization.ISerializationStrategy.SerializationException;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class ImportCrossEntropyDsetExtractor {

	private static final Logger LOGGER = Logger
			.getLogger(ImportCrossEntropyDsetExtractor.class.getName());

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SerializationException
	 */
	public static void main(final String[] args) throws ClassNotFoundException,
			SerializationException {
		if (args.length != 2) {
			System.err.println("Usage <Folder> <LMmodel>");
			return;
		}
		final ImportCrossEntropyDsetExtractor ex = new ImportCrossEntropyDsetExtractor(
				args[1]);
		ex.printDataForAllFiles(new File(args[0]));

	}

	final LMComplexity complexity;

	public ImportCrossEntropyDsetExtractor(final String lmModelPath)
			throws ClassNotFoundException, SerializationException {
		complexity = new LMComplexity(lmModelPath);
	}

	void getDataForFile(final File f) throws IOException {
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final CompilationUnit cu = astExtractor.getAST(f);
		final PackageInfoExtractor pkgInfo = new PackageInfoExtractor(cu);

		final double xEntropy = complexity.getLanguageModel()
				.getExtrinsticEntropy(f);
		System.out.print(f.getAbsolutePath() + " " + xEntropy + " "
				+ pkgInfo.getPackageName() + " ");
		for (final String pkg : pkgInfo.getImports()) {
			System.out.print(pkg + " ");
		}
		System.out.println();
	}

	void printDataForAllFiles(final File directory) {
		for (final File f : FileUtils.listFiles(directory, new RegexFileFilter(
				".*\\.java$"), DirectoryFileFilter.DIRECTORY)) {
			try {
				getDataForFile(f);
			} catch (IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}

		}
	}

}
