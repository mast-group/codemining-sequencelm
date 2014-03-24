/**
 * 
 */
package codemining.lm.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import codemining.languagetools.ITokenizer;
import codemining.languagetools.ITokenizer.FullToken;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.math.LongMath;

/**
 * A utility class that helps convert a vocabulary of T and its respective
 * sequences to integers.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class VocabularyToInt {

	final ITokenizer tokenizer;

	public static final String UNK_SYMBOL = "UNK_SYMBOL";

	/**
	 * Where to start assigning ids from.
	 */
	private int nextId;

	/**
	 * The symbol alphabet
	 */
	private final BiMap<String, Integer> alphabet;

	/**
	 * 
	 */
	public VocabularyToInt(final ITokenizer tokenizer,
			final Set<String> vocabulary) {
		this.tokenizer = tokenizer;
		alphabet = HashBiMap.create();
		alphabet.put(UNK_SYMBOL, Integer.MIN_VALUE);
		nextId = Integer.MIN_VALUE + 1;
		assignIdsToVocabulary(vocabulary);
	}

	private synchronized void assignIdsToVocabulary(final Set<String> vocabulary) {
		checkArgument(vocabulary.size() < LongMath.checkedAdd(
				Integer.MAX_VALUE, -Integer.MIN_VALUE),
				"Too large vocabulary. It cannot fit in an int. Consider pruning more");
		for (final String value : vocabulary) {
			alphabet.put(value, nextId);
			nextId++;
		}
	}

	/**
	 * @param code
	 * @return
	 */
	public int[] codeToIntSequence(final String code) {
		final List<FullToken> tokens = tokenizer.getTokenListFromCode(code
				.toCharArray());
		final int[] intTokens = new int[tokens.size()];
		for (int i = 0; i < intTokens.length; i++) {
			final Integer id = alphabet.get(tokens.get(i).token);
			if (id != null) {
				intTokens[i] = id;
			} else {
				intTokens[i] = alphabet.get(UNK_SYMBOL);
			}
		}
		return intTokens;
	}

	public int[] fileToIntSequence(final File file) throws IOException {
		final String code = FileUtils.readFileToString(file);
		return codeToIntSequence(code);
	}

	public BiMap<String, Integer> getAlphabet() {
		return alphabet;
	}
}
