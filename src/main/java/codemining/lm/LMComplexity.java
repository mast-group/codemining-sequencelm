/**
 * 
 */
package codemining.lm;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.util.parallel.ParallelThreadPool;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.google.common.collect.ImmutableList;

/**
 * Terminal User Interface: get the average complexity per project or file.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class LMComplexity {

	private static final Logger LOGGER = Logger.getLogger(LMComplexity.class
			.getName());

	private final ILanguageModel langModel;

	long fileCount = 0;

	double totalEntropy = 0;

	public LMComplexity(final ILanguageModel model) {
		langModel = model.getImmutableVersion();
	}

	public LMComplexity(final String lmPath) throws ClassNotFoundException,
			SerializationException {
		langModel = ((ILanguageModel) Serializer.getSerializer()
				.deserializeFrom(lmPath)).getImmutableVersion();

		LOGGER.info("Read " + langModel.getClass().getSimpleName()
				+ " language model from " + lmPath);
	}

	/**
	 * @param projectDir
	 * @return
	 */
	public double getAvgProjectCrossEntropy(final File projectDir) {
		fileCount = 0;
		totalEntropy = 0;
		final Lock l = new ReentrantLock();
		final ParallelThreadPool threadPool = new ParallelThreadPool();

		final Collection<File> fileList;
		if (projectDir.isDirectory()) {
			fileList = FileUtils.listFiles(projectDir,
					langModel.modelledFilesFilter(),
					DirectoryFileFilter.DIRECTORY);
		} else {
			fileList = ImmutableList.of(projectDir);
		}

		for (final File file : fileList) {
			threadPool.pushTask(new Runnable() {

				@Override
				public void run() {
					try {
						final double fileCrossEntropy = langModel
								.getExtrinsticEntropy(file);
						l.lock();
						totalEntropy += fileCrossEntropy;
						fileCount++;
					} catch (Exception e) {
						LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
					}
					l.unlock();
				}

			});
		}

		threadPool.waitForTermination();

		final double modelAvgCrossEntropy = totalEntropy / fileCount;
		return modelAvgCrossEntropy;
	}

	public final ILanguageModel getLanguageModel() {
		return langModel;
	}

	public double getTotalProjectEntropy(final File projectDir) {
		fileCount = 0;
		totalEntropy = 0;
		final Lock l = new ReentrantLock();
		final ParallelThreadPool threadPool = new ParallelThreadPool();

		final Collection<File> fileList;
		if (projectDir.isDirectory()) {
			fileList = FileUtils.listFiles(projectDir,
					langModel.modelledFilesFilter(),
					DirectoryFileFilter.DIRECTORY);
		} else {
			fileList = ImmutableList.of(projectDir);
		}

		for (final File file : fileList) {
			threadPool.pushTask(new Runnable() {

				@Override
				public void run() {
					try {
						final double fileCrossEntropy = langModel
								.getAbsoluteEntropy(file);
						l.lock();
						totalEntropy += fileCrossEntropy;
						fileCount++;
					} catch (Exception e) {
						LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
					}
					l.unlock();
				}

			});

		}

		threadPool.waitForTermination();

		return totalEntropy;
	}

}
