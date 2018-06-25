package dev.ornamental.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public final class TreeListSpliteratorTest {

	private static final class SumAccumulator implements Consumer<Integer> {

		private long sum;

		@Override
		public void accept(Integer value) {
			sum += value;
		}

		public long getSum() {
			return sum;
		}
	}

	@Test
	public void spliteratorCompletenessTest() {
		final int n = 1_000_000;
		final long expectedSum = ((long)n) * (n - 1) / 2;

		TreeList<Integer> list = IntStream.range(0, n).boxed().collect(Collectors.toCollection(TreeList::new));

		// use spliterator implicitly
		assertEquals(expectedSum, list.parallelStream().mapToLong(i -> i).sum());

		// the same, manually & deterministically
		SumAccumulator sumAccumulator = new SumAccumulator();
		sumRecursively(sumAccumulator, list.spliterator());
		assertEquals(expectedSum, sumAccumulator.getSum());
	}

	@Test
	public void emptySpliteratorTest() {
		TreeList<Object> list = new TreeList<>();
		Spliterator<Object> spliterator = list.spliterator();
		assertFalse(spliterator.tryAdvance(o -> { }));
		assertNull(spliterator.trySplit());
	}

	@Test
	public void singletonSpliteratorTest() {
		TreeList<Object> list = new TreeList<>();
		list.add(new Object());

		Spliterator<Object> spliterator = list.spliterator();
		assertNull(spliterator.trySplit());
		assertTrue(spliterator.tryAdvance(o -> { }));
		assertFalse(spliterator.tryAdvance(o -> { }));
	}

	private void sumRecursively(
		SumAccumulator sumAccumulator, Spliterator<Integer> spliterator) {

		// take up to 5 elements to interleave calls to trySplit() with calls to tryAdvance(Consumer)
		for (int i = 0; i < 5; i++) {
			if (!spliterator.tryAdvance(sumAccumulator)) {
				break;
			}
		}

		Spliterator<Integer> splitPart = spliterator.trySplit();
		if (splitPart != null) {
			sumRecursively(sumAccumulator, splitPart);
			sumRecursively(sumAccumulator, spliterator);
		} else {
			spliterator.forEachRemaining(sumAccumulator);
		}
	}
}
