package dev.ornamental.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public final class TreeListIteratorTest {

	private static final class ListPair<T> {

		private final LinkedList<T> linkedList;

		private final TreeList<T> treeList;

		public ListPair(Supplier<T> elementSupplier, int size) {
			linkedList = Stream.generate(elementSupplier).limit(size)
				.collect(Collectors.toCollection(LinkedList::new));

			treeList = new TreeList<>(linkedList);
		}

		public LinkedList<T> linked() {
			return linkedList;
		}

		public TreeList<T> tree() {
			return treeList;
		}

		public void assertEqual() {
			assertEquals(linkedList, treeList);
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void emptyIteratorTestFailureA() {
		ListIterator<?> li = new TreeList<>().listIterator(-1);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void emptyIteratorTestFailureB() {
		ListIterator<?> li = new TreeList<>().listIterator(1);
	}

	@Test
	public void emptyIteratorTest() {
		ListIterator<?> li = new TreeList<>().listIterator();
		assertFalse(li.hasNext());
		assertFalse(li.hasPrevious());
		assertEquals(0, li.nextIndex());
		assertEquals(-1, li.previousIndex());
	}

	@Test
	public void iteratorTraversalTest() {
		final int listSize = 10;

		ListPair<Object> listPair = new ListPair<>(Object::new, listSize);
		for (int i = 0; i <= listSize; i++) {
			ListIterator<Object> li1 = listPair.linked().listIterator(i);
			ListIterator<Object> li2 = listPair.tree().listIterator(i);

			while (li1.hasNext()) {
				assertEquals(li1.nextIndex(), li2.nextIndex());
				assertEquals(li1.previousIndex(), li2.previousIndex());
				assertTrue(li2.hasNext());
				assertEquals(li1.next(), li2.next());
			}
			assertFalse(li2.hasNext());
			assertEquals(li1.nextIndex(), li2.nextIndex());
			assertEquals(li1.previousIndex(), li2.previousIndex());

			while (li1.hasPrevious()) {
				assertEquals(li1.nextIndex(), li2.nextIndex());
				assertEquals(li1.previousIndex(), li2.previousIndex());
				assertTrue(li2.hasPrevious());
				assertEquals(li1.previous(), li2.previous());
			}
			assertFalse(li2.hasPrevious());
			assertEquals(li1.nextIndex(), li2.nextIndex());
			assertEquals(li1.previousIndex(), li2.previousIndex());
		}
	}

	@Test
	public void iteratorSetterTest() {
		final int listSize = 10;

		ListPair<Object> listPair = new ListPair<>(Object::new, listSize);
		ListIterator<Object> li1 = listPair.linked().listIterator();
		ListIterator<Object> li2 = listPair.tree().listIterator();

		while (li1.hasNext()) {
			li1.next();
			li2.next();
			Object o = new Object();
			li1.set(o);
			li2.set(o);
		}
		listPair.assertEqual();

		while (li1.hasPrevious()) {
			li1.previous();
			li2.previous();
			Object o = new Object();
			li1.set(o);
			li2.set(o);
		}
		listPair.assertEqual();
	}

	@Test
	public void iteratorRemoveTest() {

		ListPair<Object> listPair = new ListPair<>(Object::new, 30);
		ListIterator<Object> li1 = listPair.linked().listIterator();
		ListIterator<Object> li2 = listPair.tree().listIterator();

		while (listPair.linked().size() > 1) {

			while (li1.hasNext()) {
				li1.next();
				li2.next();

				if (li1.hasNext()) {
					li1.next();
					li2.next();

					li1.remove();
					li2.remove();
				}
			}
			listPair.assertEqual();

			while (li1.hasPrevious()) {
				li1.previous();
				li2.previous();

				if (li1.hasPrevious()) {
					li1.previous();
					li2.previous();

					li1.remove();
					li2.remove();
				}
			}
			listPair.assertEqual();
		}

		li1.next();
		li1.remove();
		li2.next();
		li2.remove();
		listPair.assertEqual();
	}

	@Test
	public void iteratorAddTest() {

		ListPair<Object> listPair = new ListPair<>(Object::new, 25);
		ListIterator<Object> li1 = listPair.linked().listIterator();
		ListIterator<Object> li2 = listPair.tree().listIterator();

		while (li1.hasNext()) {
			Object o = new Object();
			li1.add(o);
			li2.add(o);

			li1.next();
			li2.next();
		}
		listPair.assertEqual();

		while (li1.hasPrevious()) {
			Object o = new Object();
			li1.add(o);
			li2.add(o);

			for (int i = 1; i <= 2; i++) {
				li1.previous();
				li2.previous();
			}
		}
		listPair.assertEqual();
	}
}
