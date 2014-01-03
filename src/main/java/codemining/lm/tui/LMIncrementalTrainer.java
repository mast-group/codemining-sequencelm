/**
 * 
 */
package codemining.lm.tui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.exception.ExceptionUtils;

import codemining.lm.ILanguageModel;
import codemining.lm.LMComplexity;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Train models on various increments of data.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class LMIncrementalTrainer {
	/**
	 * Train a model runnable.
	 * 
	 */
	private static class ModelTrainRunnable implements Runnable {

		final String model;
		final String outModelPrefix;
		final int typeCode;
		final Entry<Long, Set<File>> trainSet;

		/**
		 * Constructor.
		 * 
		 * @param modelDir
		 *            the directory of the initial model
		 * @param outputDir
		 *            the output model directory and prefix
		 * @param type
		 * @param trainF
		 */
		ModelTrainRunnable(final String modelDir, final String outputDir,
				final int type, final Entry<Long, Set<File>> trainF) {
			model = modelDir;
			outModelPrefix = outputDir;
			typeCode = type;
			trainSet = trainF;
		}

		@Override
		public void run() {
			try {
				final ILanguageModel model = (ILanguageModel) Serializer
						.getSerializer().deserializeFrom(this.model);

				LOGGER.info("Loaded initial model...");
				model.trainModel(trainSet.getValue());

				Serializer.getSerializer().serialize(
						model,
						outModelPrefix + "Mode" + typeCode + trainSet.getKey()
								+ ".ser");
			} catch (FileNotFoundException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			} catch (IOException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			} catch (SerializationException e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}

		}
	}

	private static final Logger LOGGER = Logger.getLogger(LMComplexity.class
			.getName());

	final static ExecutorService threadPool = Executors.newFixedThreadPool(3);

	/**
	 * @param args
	 * @throws IOException
	 * @throws SerializationException
	 */
	public static void main(final String[] args) throws IOException,
			SerializationException {
		if (args.length < 4) {
			System.err
					.println("Usage <TrainProjectDir> <OutputSer> <Multiplier> <InitialLM1> [<InitialLM2> ...]");
			System.exit(-1);
		}

		final ILanguageModel baseModel = (ILanguageModel) Serializer
				.getSerializer().deserializeFrom(args[3]);

		long dataCount = 0;
		long lastInc = 0;
		long byteStep = 1000; // Start from LOC
		// and use this step-size in "log-space"
		final double stepRate = Double.parseDouble(args[2]);

		final Collection<File> files = FileUtils.listFiles(new File(args[0]),
				baseModel.modelledFilesFilter(), DirectoryFileFilter.DIRECTORY);

		Collections.shuffle((List<?>) files);

		final Map<Long, Set<File>> trainBatches = Maps.newTreeMap();

		Set<File> currentBatch = Sets.newTreeSet();
		// Create file batches.
		for (final File file : files) {
			try {
				currentBatch.add(file);
				dataCount += FileUtils.readLines(file).size();

				if (dataCount - lastInc > byteStep) {
					LOGGER.info("Train Batch Full, next");
					lastInc = dataCount / byteStep * byteStep;
					trainBatches.put(lastInc, currentBatch);
					final Set<File> nextBatch = Sets.newTreeSet();
					nextBatch.addAll(currentBatch);
					currentBatch = nextBatch;
					byteStep *= stepRate;
				}
			} catch (Exception e) {
				LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
			}
		}

		for (final Entry<Long, Set<File>> entry : trainBatches.entrySet()) {
			LOGGER.info("Training batch " + entry.getKey());
			for (int i = 3; i < args.length; i++) {
				threadPool.submit(new ModelTrainRunnable(args[i], args[1],
						i - 3, entry));
			}

		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			LOGGER.warning("Thread Pool Interrupted "
					+ ExceptionUtils.getFullStackTrace(e));
		}
	}
}
