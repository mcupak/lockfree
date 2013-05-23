package simplelockfree;

/**
 * Šablonované rozhranie pre jednoduchú frontu (FIFO).
 *
 *<p>
 * Rozhranie bolo vytvorené, pretože štandardné Java Core API rozhranie pre frontu
 * je príliš univerzálne pre tento účel a vyžaduje implementáciu mnohých metód, ktoré
 * v štandardne definovanej neohraničenej fronte nie sú potrebné (size, offer, iterator, toArray...).
 * </p>
 *
 * @param <E> typ elementu vo fronte
 * @author Miroslav Cupák
 * @version 2.0 (reimplementované, výrazné zefektívnenie)
 */
public interface SimpleQueue<E> {

	/**
	 * Zistí, či je fronta prázdna.
	 *
	 * @return true ak je fronta prázdna, false inak
	 */
	boolean isEmpty();

	/**
	 * Získa hlavu fronty (prvok, ktorý v nej bol najdlhšie).
	 *
	 * @return hlavu fronty
	 */
	E head();

	/**
	 * Vloží daný element na koniec fronty.
	 *
	 * @param e element, ktorý chceme vložiť
	 */
	void enqueue(E e);

	/**
	 * Získa a odstráni hlavu fronty (prvok, ktorý v nej bol najdlhšie).
	 *
	 * @return hlavu fronty, ak fronta nie je prázdna, inak null
	 */
	E dequeue();
}
