/**
 * 
 */
package codemining.lm.ngram.tui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import codemining.languagetools.ITokenizer;
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

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, SerializationException {


		if (args.length != 5 && args.length != 4) {
			System.err
					.println("Usage <TrainingFiles> <N> <NGramModel.ser output> <tokenizationClass> <WrapperSmootherClass> or <File w/ projects to use> <N> <tokenizationClass> <WrapperSmootherClass>" );
			return;
		}
		
		if(args.length == 4){
			BufferedReader br = null;
			try {
	 			String sCurrentLine;
	 			br = new BufferedReader(new FileReader(args[0]));
	 
				while ((sCurrentLine = br.readLine()) != null) {
					if (sCurrentLine.trim().length() > 0) {
						String proj = sCurrentLine.trim().split(" ")[1];
						//System.out.println(proj);
						String input = "/afs/inf.ed.ac.uk/group/ML/s0954584/java_projects/train/" + proj;
						String output = "/afs/inf.ed.ac.uk/group/ML/s0954584/java_projects/projModels/" + proj + ".filtered.laplace.ser";
						buildModel(input,args[1],output,args[2],args[3]);
						//System.exit(3);
					}
				}
	 
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)
						br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		if (args.length == 5) {
			buildModel(args[0],args[1],args[2],args[3],args[4]);
		}
	}
	
	private static void buildModel(String sInput, String sN, String sOutput, String sTokenizer, String sSmoother) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException, SerializationException{
		final ITokenizer tokenizer;
		
		final Class<? extends ITokenizer> tokenizerName = (Class<? extends ITokenizer>) Class
				.forName(sTokenizer);
		tokenizer = tokenizerName.newInstance();

		final NGramLM dict = new NGramLM(Integer.parseInt(sN), tokenizer);

		LOGGER.info("NGram Model creater started with " + sN
				+ "-gram for files in " + sInput + " using " + sTokenizer
				+ " tokenizer");

		final Collection<File> files = FileUtils.listFiles(new File(sInput),
				dict.modelledFilesFilter(), DirectoryFileFilter.DIRECTORY);
		
		dict.trainModel(files);

		LOGGER.info("Ngram model build. Adding Smoother...");
		final Class<? extends AbstractNGramLM> smoothedNgramClass = (Class<? extends AbstractNGramLM>) Class
				.forName(sSmoother);
		final AbstractNGramLM ng = (AbstractNGramLM) smoothedNgramClass
				.getDeclaredConstructor(AbstractNGramLM.class)
				.newInstance(dict);

		LOGGER.info("Ngram model build. Serializing...");
		ng.serializeToDisk(sOutput);
	}
}
