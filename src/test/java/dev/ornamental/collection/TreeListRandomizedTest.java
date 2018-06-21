package dev.ornamental.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TreeListRandomizedTest {

	@org.junit.Test
	public void get() {
		TreeList<Object> list = produceRandomList(Object::new);
		HashSet<Object> encounteredObjects = new HashSet<>();
		for (int i = 0; i < list.size(); i++) {
			encounteredObjects.add(list.get(i));
		}

		assertEquals(list.size(), encounteredObjects.size());
		checkTreeInvariants(list);
	}

	@org.junit.Test
	public void size() {
		TreeList<Void> list = new TreeList<>();
		assertEquals(0, list.size());

		Random random = new Random();
		int count = random.nextInt(10_000);
		for (int i = 0; i < count; i++) {
			list.add(random.nextInt(i + 1), null);
			assertEquals(i + 1, list.size());
		}
		checkTreeInvariants(list);

		for (int i = 0; i < count; i++) {
			list.remove(random.nextInt(count - i));
			assertEquals(count - i - 1, list.size());
		}

		checkTreeInvariants(list);
	}

	@org.junit.Test
	public void add() {
		TreeList<Double> list = new TreeList<>();
		list.add(0, 0.0);
		list.add(1, 1.0);

		Random random = new Random();
		int count = 10_000 + random.nextInt(10_000);
		for (int i = list.size(); i < count; i++) {
			int position = random.nextInt(i + 1);
			double value;
			if (position == 0) {
				value = list.get(0) - 1.0;
			} else if (position == list.size()) {
				value = list.get(list.size() - 1) + 1.0;
			} else {
				value = (list.get(position - 1) + list.get(position)) / 2.0;
			}
			list.add(position, value);
		}

		checkTreeInvariants(list);
		assertEquals(list.size(), count);

		double previous = list.get(0);
		for (int i = 1; i < count; i++) {
			double current = list.get(i);
			assertTrue(previous <= current);
			previous = current;
		}
	}

	@org.junit.Test
	public void set() {
		TreeList<Object> list = produceRandomList(Object::new);
		for (int i = 0; i < list.size(); i++) {
			list.set(i, null);
		}
		for (int i = 0; i < list.size(); i++) {
			assertNull(list.get(i));
		}
	}

	@org.junit.Test
	public void remove() {
		Random random = new Random();
		TreeList<Integer> list = produceRandomList(random::nextInt);

		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i) % 2 == 0) {
				list.remove(i);
			}
		}

		checkTreeInvariants(list);
		for (int i = 0; i < list.size(); i++) {
			assertNotEquals(0, list.get(i) % 2);
		}

		for (int i = 0, s = list.size(); i < s; i++) {
			list.remove(random.nextInt(list.size()));
		}

		checkTreeInvariants(list);
		assertTrue(list.isEmpty());
	}

	@org.junit.Test
	public void concat() {
		Random random = new Random();

		int leftCount = random.nextInt(30_000);
		int rightCount = random.nextInt(30_000);
		TreeList<Integer> left = new TreeList<>();
		TreeList<Integer> right = new TreeList<>();

		IntStream
			.range(0, leftCount)
			.map(i -> random.nextInt(i + 1))
			.forEachOrdered(i -> left.add(i, null));
		IntStream
			.range(0, rightCount)
			.map(i -> random.nextInt(i + 1))
			.forEachOrdered(i -> right.add(i, null));

		// after the underlying tree structures are created, set the values in sorted order
		int i = 0;
		for (TreeList<Integer> list : Arrays.asList(left, right)) {
			ListIterator<Integer> listIterator = list.listIterator(0);
			while (listIterator.hasNext()) {
				listIterator.next();
				listIterator.set(i++);
			}
		}

		TreeList<Integer> concatenated = TreeList.concat(left, right);
		assertTrue(left.isEmpty());
		assertTrue(right.isEmpty());
		assertEquals(leftCount + rightCount, concatenated.size());
		checkTreeInvariants(concatenated);

		i = 0;
		for (Integer j : concatenated) {
			assertEquals(i, j.intValue());
			i++;
		}
		assertEquals(leftCount + rightCount, i);
	}

	private static <T> TreeList<T> produceRandomList(Supplier<T> valueSupplier) {
		TreeList<T> result = new TreeList<>();

		Random random = new Random();
		int count = 10_000 + random.nextInt(10_000);
		IntStream
			.range(0, count)
			.map(i -> random.nextInt(i + 1))
			.forEachOrdered(i -> result.add(i, valueSupplier.get()));

		return result;
	}

	private static void checkTreeInvariants(TreeList<?> list) {
		RedBlackTreeChecker.checkTreeInvariants(list.tree);
	}
}
