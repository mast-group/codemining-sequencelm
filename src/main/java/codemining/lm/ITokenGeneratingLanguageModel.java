/**
 * 
 */
package codemining.lm;

import java.util.List;

import codemining.languagetools.ITokenizer;

/**
 * A language model that generates tokens.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public interface ITokenGeneratingLanguageModel<T> extends ILanguageModel {

	/**
	 * Generate sentences from language model
	 * 
	 * @return
	 */
	public List<T> generateSentence();

	/**
	 * Return the tokenizer for this LM.
	 * 
	 * @return the tokenizer. null if it is not available.
	 */
	public ITokenizer getTokenizer();
}
