package dev.ornamental.collection;

import static dev.ornamental.collection.RedBlackTreeChecker.checkTreeInvariants;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

public final class TreeListDeterministicTest {

	@Test
	public void bulkLoadTest() {
		List<Object> source = Collections.emptyList();
		TreeList<Object> copy = new TreeList<>(source);
		assertEquals(source, copy);
		checkTreeInvariants(copy);

		source = Collections.singletonList(new Object());
		copy = new TreeList<>(source);
		assertEquals(source, copy);
		checkTreeInvariants(copy);

		// the general case is tested in TreeListRandomizedTest
	}

	@Test
	public void addAllToEndsTest() {
		List<Object> source = Collections.emptyList();
		TreeList<Object> receptacle = new TreeList<>();
		receptacle.addAll(source);
		assertTrue(receptacle.isEmpty());
		checkTreeInvariants(receptacle);

		source = IntStream.range(0, 32).boxed().collect(Collectors.toList());
		receptacle.addAll(source);
		assertEquals(source, receptacle);
		checkTreeInvariants(receptacle);

		receptacle.addAll(0, source);
		assertEquals(receptacle.subList(0, source.size()), receptacle.subList(source.size(), 2 * source.size()));
		checkTreeInvariants(receptacle);

		receptacle.addAll(source);
		assertEquals(receptacle.subList(0, source.size()), receptacle.subList(source.size(), 2 * source.size()));
		assertEquals(receptacle.subList(0, source.size()), receptacle.subList(2 * source.size(), 3 * source.size()));
		checkTreeInvariants(receptacle);
	}

	@Test
	public void concatTest() {
		int leastListSize = 50;
		List<Object> referenceList = Stream.generate(Object::new)
			.limit(11 * leastListSize) // the second list must have higher
			.collect(Collectors.toCollection(TreeList::new));

		for (int leftSize : new int[] {leastListSize, referenceList.size() - leastListSize}) {
			// populate the lists without using addAll(Collection) because it internally uses concatenation
			TreeList<Object> list1 = new TreeList<>();
			referenceList.subList(0, leftSize).stream().forEachOrdered(list1::add);
			TreeList<Object> list2 = new TreeList<>();
			referenceList.subList(leftSize, referenceList.size()).stream().forEachOrdered(list2::add);

			TreeList<Object> concatenated = TreeList.concat(list1, list2);
			assertEquals(referenceList, concatenated);
			checkTreeInvariants(concatenated);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void concatWithSelf() {
		TreeList<Object> list1 = new TreeList<>();
		list1.addAll(Arrays.asList(-1, "a string", false));
		TreeList.concat(list1, list1);
	}
}
