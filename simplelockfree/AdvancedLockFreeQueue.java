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
public class AdvancedLockFreeQueue<E> implements SimpleQueue<E> {

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
	 * Constructor of a lock-free queue.
	 * Creates an empty node and sets head and tail to point to it (i.e. empty queue = single empty node.)
	 *
	 */
	public AdvancedLockFreeQueue() {
		head = new AtomicReference<Node<E>>(new Node<E>());
		tail = new AtomicReference<Node<E>>(head.get());
	}

	// pridanie prvku na koniec
	public void enqueue(E e) {
		Node<E> node = new Node<E>(e);					// nový uzol s danou hodnotou bez následníka (bude koncom fronty)
		while (true) {									// skúšam urobiť enqueue - kedykoľvek to nevyjde, začnem odznova
			Node<E> last = tail.get();					// posledný prvok
			Node<E> nextLast = last.getNext();			// a jeho následník
			if (last == tail.get()) {					// je posledný prvok stále posledný (alebo prvok niekto medzitým pridal?)
				if (nextLast != null) {					// vyrušil som niekoho iného v procese enqueue (následník zmenený, ukazateľ na chvost ešte nie)?
					tail.compareAndSet(last, nextLast);	// nebudem zbytočne čakať, ale posuniem ukazateľ na chvost za kolegu a skúsim to celé znova
				} else if (last.casNext(null, node)) {	// skúsim pripojiť posledný prvok ako následníka k poslednému, ak sa to nepodarí, niekto iný to už robí, skúsim to celé znova
					tail.compareAndSet(last, node);		// posuniem aj ukazateľ na chvost
					return;								// teda už je všetko OK
				}
			}
		}
	}

	// získanie hodnoty v prvom uzle
	// pozn. k funguvaniu: Po načítaní hodnôt overím niekoľko podmienok, na základe ktorých
	// zistím, či na queue nerobí niekto iný update. V prípade, že áno, je len jedna možnosť, v akom
	// stave môže jeho update byť - prerušený predtým, ako stihol dotyčný posunúť
	// ukazateľ. Môžem to teda dokončiť za neho a skúsiť celý proces odznova - to znamená, že môžem dokončiť
	// aj zlyhaný update (nedôjde k zablokovaniu) a zároveň podmienkami overujem, že mi situácia vplyvom
	// ostatných príliš neušla (pri nesplnení podmienok sa do bloku nevchádza).
	public E head() {
		while (true) {								// skúšam dequeue, v prípade akéhokoľvek problému to skúsim znova
			Node<E> first = head.get();				// získam prvý, druhý a posledný uzol
			Node<E> second = first.getNext();
			Node<E> last = tail.get();
			if (first == head.get()) {				// zmenil niekto medzitým prvý prvok (odobral ho)?
				if (first == last) {				// mám prázdnu frontu?
					if (second == null) {			// mám naozaj prázdnu frontu alebo som len vyrušil niekoho iného uprostred operácie?
						return null;				// ozaj prázdna fronta, nemám čo vrátiť ako head -> null
					}
					tail.compareAndSet(last, second);// ak som sa dostal tu, vyrušil som niekoho uprostred operácie, nečakám, ale pomôžem mu a skúsim to znova (efektívne)
				} else {							// ukazateľ na chvost je nastavený konzistentne
					E value = second.getValue();	// získam požadovanú hodnotu (je v second, 1. node je prázdny)
					if (value != null) {			// hodnota bola ok (konzistentná), vrátim ju
						return value;				// môžem skončiť
					}
					head.compareAndSet(first, second);			// hodnota nebola konzistentná, môžem posunúť head o uzol vedľa a skúsiť šťastie znova
				}
			}
		}
	}

	// odstránenie prvého uzlu a vrátenie hodnoty v ňom
	// to isté, ako head, akurát prvok odstráni aj odstráni
	public E dequeue() {
		while (true) {
			Node<E> first = head.get();
			Node<E> second = first.getNext();
			Node<E> last = tail.get();
			if (first == head.get()) {
				if (first == last) {
					if (second == null) {
						return null;
					}
					tail.compareAndSet(last, second);
				} else if (head.compareAndSet(first, second)) {	// prvok potrebujem odstrániť/posunúť sa na ďalší, čiže CAS robím vždy
					E value = second.gasValue(null);
					if (value != null) {				// ak nie, skús znova
						return value;
					}
				}
			}
		}
	}

	// zistenie prázdnosti fronty
	// to isté ako head, akurát nevraciam hodnotu, ale či je uzol prázdny
	public boolean isEmpty() {
		while (true) {
			Node<E> first = head.get();
			Node<E> second = first.getNext();
			Node<E> last = tail.get();
			if (first == head.get()) {
				if (first == last) {
					if (second == null) {
						return true;
					}
					tail.compareAndSet(last, second);
				} else {
					E value = second.getValue();
					if (value != null) {
						return false;
					}
					head.compareAndSet(first, second);
				}
			}
		}
	}
}
