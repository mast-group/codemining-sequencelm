package codemining.lm.ngram.cache;

public interface ICache<T> {

	/**
	 * Return the probability of element, given the current cache view.
	 * 
	 * @param element
	 * @return
	 */
	public double getProbabilityFor(T element);

	/**
	 * Generate a random element from the cache.
	 */
	public T getRandomElement();

	/**
	 * Push an element to the cache.
	 * 
	 * @param element
	 *            the element to push
	 */
	public void pushElement(T element);

}