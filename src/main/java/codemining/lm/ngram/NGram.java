/**
 * 
 */
package codemining.lm.ngram;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * A lazy ngram the holds on to its underlying representation i.e. List<String>.
 * This representation is immutable, i.e. once an object is created the ngram
 * cannot be altered.
 * 
 * Note of course that the underlying representation (i.e. the List) can change
 * 
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class NGram<T> implements Iterable<T> {

	class NGramElementIterable implements Iterator<T> {

		int currentPos = 0;

		@Override
		public boolean hasNext() {
			return currentPos < stop - start;
		}

		@Override
		public T next() {
			if (currentPos >= stop - start) {
				throw (new NoSuchElementException());
			}
			final T obj = get(currentPos);
			currentPos++;
			return obj;
		}

		@Override
		public void remove() {
			throw (new UnsupportedOperationException());
		}

	}

	/**
	 * Given a sentence construct an ngram at that position.
	 * 
	 * @param position
	 *            the position where the ngram terminates (i.e. where the
	 *            current word position, context will be retrieved). Position
	 *            should be in the range of [0, sentence.size()).
	 * @param sentence
	 *            the sentence (list of tokens)
	 * @return an n-gram
	 */
	public static <T> NGram<T> constructNgramAt(final int position,
			final List<T> sentence, final int nGramSize) {
		checkArgument(position >= 0);
		checkArgument(position < sentence.size(), sentence);

		int start = position - nGramSize + 1;
		if (start < 0)
			start = 0;

		return new NGram<T>(sentence, start, position + 1);
	}

	/**
	 * Substitute an n-gram's from token with another name.
	 * 
	 * @return
	 */
	public static NGram<String> substituteTokenWith(final NGram<String> ngram,
			final String from, final String element) {
		final List<String> ngramCopy = ngram.toList();
		final int nSize = ngram.size();
		for (int i = 0; i < nSize; i++) {
			final String token = ngramCopy.get(i);
			if (token.contains(from)) {
				ngramCopy.set(i, token.replace(from, element));
			}
		}
		return new NGram<String>(ngramCopy);
	}

	/**
	 * The underlying sentence as an immutable list.
	 */
	private final List<T> underlyingSentence;

	// Starting position of the ngram in the list
	private final int start;

	// Ending position in the ngram (i.e. not included)
	private final int stop;

	/**
	 * Construct an ngram from the given list.
	 * 
	 * @param ngram
	 */
	public NGram(final List<T> ngram) {
		checkNotNull(ngram);
		underlyingSentence = ngram;
		start = 0;
		stop = ngram.size();
	}

	/**
	 * Constructor
	 * 
	 * @param sentence
	 *            the underlying sentence
	 * @param from
	 *            from index
	 * @param to
	 *            up to index (element not included)
	 */
	public NGram(final List<T> sentence, final int from, final int to) {
		checkArgument(from >= 0);
		checkArgument(from <= to);
		checkArgument(sentence.size() >= to);
		// We use the collections version (instead of the guava immutableList)
		// to avoid copying of data. It is a bit more unsafe, but that's ok.
		underlyingSentence = Collections.unmodifiableList(sentence);
		start = from;
		stop = to;
	}

	public NGram(final NGram<T> ngram, final int from, final int to) {
		checkNotNull(ngram);
		underlyingSentence = ngram.underlyingSentence;
		start = ngram.start + from;
		stop = ngram.start + to;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NGram<?>)) {
			return false;
		}
		final NGram<T> other = (NGram<T>) obj;
		final int thisNGramSize = size();

		if (other.size() != thisNGramSize) {
			return false;
		}
		for (int i = 0; i < thisNGramSize; i++) {
			if (!other.get(i).equals(get(i))) {
				return false;
			}
		}
		return true;
	}

	public T get(final int index) {
		checkArgument(start + index < stop);
		return underlyingSentence.get(start + index);
	}

	public NGram<T> getPrefix() {
		checkArgument(start < stop);
		return new NGram<T>(underlyingSentence, start, stop - 1);
	}

	public NGram<T> getSuffix() {
		checkArgument(start < stop);
		return new NGram<T>(underlyingSentence, start + 1, stop);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(start, stop, underlyingSentence);
	}

	@Override
	public Iterator<T> iterator() {
		return new NGramElementIterable();
	}

	/**
	 * Return the size of the ngram.
	 * 
	 * @return
	 */
	public int size() {
		return stop - start;
	}

	public List<T> toList() {
		return Lists.newArrayList(underlyingSentence.subList(start, stop));
	}

	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append('<');
		for (int i = start; i < stop; i++) {
			buf.append(underlyingSentence.get(i));
			buf.append(", ");
		}
		buf.append('>');
		return buf.toString();
	}

}
