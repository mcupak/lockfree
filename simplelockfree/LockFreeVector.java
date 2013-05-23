package simplelockfree;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free implementácia vektoru.
 *
 * @param <E> bázový typ elementu
 * @author Miroslav Cupák
 * @version 1.0
 */
public class LockFreeVector<E> implements SimpleVector<E> {

	private final AtomicReference<Object[]> data;					// odkaz na pole, na ktorom môžem volať CAS

	/**
	 * Vytvorí prázdny vektor.
	 */
	public LockFreeVector() {
		this.data = new AtomicReference<Object[]>(new Object[0]);
	}

	// test na prázdnosť vektora
	public boolean isEmpty() {
		return getSize() == 0;
	}

	// zistenie veľkosti vektora
	public int getSize() {
		return data.get().length;
	}

	// vloženie prvku
	// vyžaduje posunutie prvkov za indexom, preto alokujem nové pole a potom ho atomicky vymením za staré
	public boolean insertElement(E element, int index) {
		while (true) {									// skúšam, kým mi to niekto nenaruší
			Object[] o = data.get();
			if ((index < 0) || (index > o.length)) {	// som mimo rozsah?
				return false;
			}
			Object[] n = new Object[o.length + 1];
			for (int i = 0; i < index; i++) {			// plním nové pole
				n[i] = o[i];
			}
			n[index] = element;
			for (int i = index; i < o.length; i++) {
				n[i + 1] = o[i];
			}
			if (data.compareAndSet(o, n)) {				// zamením nové pole za staré, ak ho niekto medzitým zmenil, skúsim znova
				return true;
			}
		}
	}

	// odstránenie prvku
	// vyžaduje posunutie prvkov za indexom, preto alokujem nové pole a potom ho atomicky vymením za staré
	public boolean removeElement(int index) {
		while (true) {									// skúšam, kým sa mi to nepodarí
			Object[] o = data.get();
			if ((index < 0) || (index >= o.length)) {	// mimo rozsah?
				return false;
			}
			Object[] n = new Object[o.length - 1];
			for (int i = 0; i < index; i++) {			// naplním nové pole
				n[i] = o[i];
			}
			for (int i = index + 1; i < o.length; i++) {
				n[i - 1] = o[i];
			}
			if (data.compareAndSet(o, n)) {				// zamením nové pole za staré, ak ho niekto medzitým zmenil, skúsim znova
				return true;
			}
		}
	}

	// vloženie prvku na koniec
	// mení sa rozsah poľa a keďže vektor si miesto alokuje automaticky, vytvorím nové pole a atomicky ho zamením za staré
	public void pushElement(E element) {
		while (true) {									// skúšam, kým sa to nepodarí
			Object[] o = data.get();
			Object[] n = new Object[o.length + 1];
			for (int i = 0; i < o.length; i++) {		// naplním nové pole
				n[i] = o[i];
			}
			n[o.length] = element;
			if (data.compareAndSet(o, n)) {				// zamením nové pole za staré, ak ho niekto medzitým zmenil, skúsim znova
				return;
			}
		}
	}

	// odstráni element na konci a vráti ho
	// duálne k push -> automaticky realokuje pole na menšie, preto vytvorím kópiu poľa a atomicky ho zamením za pôvodné
	@SuppressWarnings("unchecked")
	public E popElement() {
		while (true) {									// skúšam, kým sa to nepodarí
			Object[] o = data.get();
			if (o.length == 0) {						// prázdne pole
				return null;
			}
			Object[] n = new Object[o.length - 1];
			Object value = o[o.length - 1];
			for (int i = 0; i < (o.length - 1); i++) {	// naplním nové pole
				n[i] = o[i];
			}
			if (data.compareAndSet(o, n)) {				// zamením nové pole za staré, ak ho niekto medzitým zmenil, skúsim znova
				return (E) value;						// vrátim odobraný prvok
			}
		}
	}

	// vytvorí prázne vektor
	public void clear() {
		data.set(new Object[0]);
	}

	// získa prvok na mieste danom indexom
	@SuppressWarnings("unchecked")
	public E getElement(int index) {
		Object[] temp = data.get();
		if ((index < 0) || (index >= temp.length)) {	// index mimo rozsah?
			return null;
		}
		return (E) temp[index];
	}
}
