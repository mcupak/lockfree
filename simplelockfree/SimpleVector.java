package simplelockfree;

/**
 * Šablonované rozhranie pre vektor.
 *
 * <p>
 * Rozhranie bolo vytvorené, pretože štandardné Java Core API rozhranie pre List
 * je príliš univerzálne pre tento účel a vyžaduje implementáciu mnohých metód na spoluprácu
 * s inými kolekciami, ktoré v našom vektore nie sú potrebné (addAll, containsAll...).
 * </p>
 *
 * @param <E> typ bázového elementu
 * @author Miroslav Cupak
 * @version 1.0
 */
public interface SimpleVector<E> {

	/**
	 * Test na prázdnosť vektora.
	 * 
	 * @return true, ak je vektor prázdny (neobsahuje žiadne prvky), false inak
	 */
	boolean isEmpty();

	/**
	 * Zistí počet prvkov vo vektore.
	 *
	 * @return počet prvkov vo vektore
	 */
	int getSize();

	/**
	 * Získa hodnotu vyskytujúcu sa na danom indexe.
	 *
	 * @param index
	 * @return hodnota alebo null, ak je zadaný neplatný index
	 */
	E getElement(int index);

	/**
	 * Vloží danú hodnotu na miesto dané indexom.
	 * Prvky nachádzajúce sa za týmto miestom posunie.
	 *
	 * @param element hodnota
	 * @param index index
	 * @return true, ak insertElement prebehol ok, false, ak bol zadaný neplatný index
	 */
	boolean insertElement(E element, int index);

	/**
	 * Odstráni prvok na danom indexe.
	 * Prvky nachádzajúce sa za ním posunie.
	 *
	 * @param index
	 * @return true, ak bol prvok odstránený, false, ak bol zadaný neplatný index
	 */
	boolean removeElement(int index);

	/**
	 * Vloží nový prvok na koniec vektora.
	 *
	 * @param element prvok
	 */
	void pushElement(E element);

	/**
	 * Získa prvok na konci vektora a odstráni ho.
	 *
	 * @return prvok vektora alebo null, ak je vektor prázdny
	 */
	E popElement();

	/**
	 * Zmaže vektor.
	 *
	 */
	void clear();
}
