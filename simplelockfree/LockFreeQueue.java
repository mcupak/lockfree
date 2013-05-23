package simplelockfree;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free implementácia fronty.<br/>
 * Implementované ako reťaz nezávislých parametrizovaných uzlov, ktorými môže byť
 * objek ľubovoľného typu.<br/>
 *<br/>
 * Základný princíp:<br/>
 * <p> Nechceme, aby bola pri update kopírovaná celá fronta a pomocou CAS menený
 * len ukazateľ na ňu (neefektívne). Operácie preto prebiehajú na úrovni uzlov.
 * </p>
 * <p> Využíva AtomicReference, čo je trieda z java.util.concurrent, ktorá predstavuje
 * fakticky púzdro na pointer (v Jave pointery ako také nie sú), na ktorom môžme volať
 * CAS a GAS, teda robiť compare-and-set a get-and-set atomicky. Tým je zaručené, že každý
 * prístup k danému objetku bude mať k dispozícii aktuálnu hodnotu (po zmene).
 * </p>
 *
 * <p>Viac informácií je dostupných v podobe rozsiahlych komentárov v zdrojovom kóde.</p>
 *
 * @param <E> typ bázového elementu
 * @author Miroslav Cupak
 * @version 1.0
 */
public class LockFreeQueue<E> implements SimpleQueue<E> {

	// jeden uzol vo fronte
	private static class Node<E> {

		private AtomicReference<E> value;			// zapúzdraný pointer, môžme použiť CAS
		private AtomicReference<Node<E>> next;		// referencia na nasledujúci uzol

		// konštruktory
		public Node(E value) {
			if (value == null) {
				throw new IllegalArgumentException("value");
			}
			this.value = new AtomicReference<E>(value);
			this.next = new AtomicReference<Node<E>>(null);
		}

		public Node(E value, Node<E> next) {
			if (value == null) {
				throw new IllegalArgumentException("value");
			}
			this.value = new AtomicReference<E>(value);
			this.next = new AtomicReference<Node<E>>(next);
		}

		public Node() {
			this.value = new AtomicReference<E>(null);
			this.next = new AtomicReference<Node<E>>(null);
		}

		// získa hodnotu v uzle
		public E getValue() {
			return value.get();
		}

		// set na hodnotu v uzle
		public void setValue(E value) {
			this.value.set(value);
		}
		// get-and-set na hodnotu v uzle

		public E gasValue(E value) {
			return this.value.getAndSet(value);
		}

		// compare-and-set na hodnotu v uzle
		public boolean casValue(E expected, E value) {
			return this.value.compareAndSet(expected, value);
		}

		// získa následníka
		public Node<E> getNext() {
			return next.get();
		}

		// set na hodnotu v uzle
		public void setNext(Node<E> value) {
			this.next.set(value);
		}

		// get-and-set na následníka
		public Node<E> gasNext(Node<E> value) {
			return this.next.getAndSet(value);
		}

		// compare-and-set na následníka
		public boolean casNext(Node<E> expected, Node<E> value) {
			return this.next.compareAndSet(expected, value);
		}

		@Override
		public String toString() {
			return getValue().toString();
		}
	}

	private AtomicReference<Node<E>> head;	// ukazateľ na prvý prvok fronty
	private AtomicReference<Node<E>> tail;	// ukazateľ na poslendý prvok fronty

	/**
	 * Constructor for a lock-free queue. Creates an empty queue (queue without nodes).
	 */
	public LockFreeQueue() {
		head = new AtomicReference<Node<E>>(null);
		tail = new AtomicReference<Node<E>>(null);
	}

	// pridanie prvku na koniec
	// prebieha v 2 fázach - zmena tail a prilinkovanie
	public void enqueue(E e) {
		Node<E> node = new Node<E>(e);
		Node<E> last = tail.getAndSet(node);
		if (last == null) {						// prázdna fronta
			head.set(node);						// nastavíme head
		} else {
			last.setNext(node);					// prilinkujeme na koniec
		}
	}

	// odobranie uzla
	public E dequeue() {
		Node<E> first = null;
		while (first == null) {							// skúšaj, kým nenájdeš
			first = head.getAndSet(null);
			if (first == null && tail.get() == null) {	// prázdny list
				return null;
			}
		}
		Node<E> second = first.getNext();
		if (second != null) {							// vo fronte je ďalší prvok
			head.set(second);							// posunieme head
		} else if (!tail.compareAndSet(first, null)) {	// nie je ďalší prvok a niekto iný robí enqueue
			while (first.getNext() == null) {
			}				// tak počkám, kým skončí
			head.set(first.getNext());					// a posuniem head
		}
		return first.getValue();
	}

	// test na prázdnosť
	public boolean isEmpty() {
		return tail.get() == null;
	}

	// získanie prvého prvku
	public E head() {
		Node<E> first = null;
		while (true) {							// skúšaj, kým nenájdeš
			first = head.get();
			if (first != null) {
				return first.getValue();
			} else if (tail.get() == null) {	// prázdna fronta
				return null;
			}
		}
	}
}
