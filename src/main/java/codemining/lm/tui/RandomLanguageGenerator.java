/**
 * 
 */
package codemining.lm.tui;

import java.io.IOException;
import java.util.List;

import codemining.languagetools.ITokenizer.FullToken;
import codemining.lm.ITokenGeneratingLanguageModel;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

/**
 * Generate Random text from Language Model
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class RandomLanguageGenerator {
	public static void main(final String[] args) throws IOException,
			ClassNotFoundException, SerializationException {
		if (args.length != 1) {
			System.err.println("Usage <lmFile>");
			return;
		}

		final ITokenGeneratingLanguageModel<FullToken> lm = (ITokenGeneratingLanguageModel<FullToken>) Serializer
				.getSerializer().deserializeFrom(args[0]);

		final List<FullToken> toks = lm.generateSentence();

		for (final FullToken f : toks) {
			System.out.print(f.token + " ");
		}
		System.out.println();

	}
}
