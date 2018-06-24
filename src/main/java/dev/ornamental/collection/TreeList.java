package dev.ornamental.collection;

import static dev.ornamental.collection.NodeColour.BLACK;
import static dev.ornamental.collection.NodeColour.RED;

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * This class is a random access {@link java.util.List} implementation providing <em>O(log(n))</em>
 * time complexity for {@link #get(int)}, {@link #set(int, Object)}, {@link #add(int, Object)},
 * {@link #add(Object)}, and {@link #remove(int)} operations, <em>n</em> denoting the number of elements
 * on the list.<br/>
 * It also provides a constructor for bulk-loading from another collection in <em>O(n)</em> time
 * where <em>n</em> is the number of elements in the source collection.<br/>
 * Two different {@link TreeList} instances may be concatenated
 * in <em>O(log(n<sub>1</sub> + n<sub>2</sub>))</em> time, where <em>n<sub>1</sub></em>
 * and <em>n<sub>2</sub></em> are the sizes of the original lists
 * (both of which are cleared).<br/>
 * Although {@link #addAll(int, Collection)} method
 * generally requires <em>O(m log(m + n))</em> time, it runs in <em>O(m + log(m + n))</em> time if
 * the insertion location is either before the first or past the last element of the list
 * (<em>m</em> is the size of the added collection, <em>n</em> is the size of the list).
 * The better time complexity naturally applies to the {@link #addAll(Collection)} method.
 * @param <T> the type of values stored by the list
 */
public class TreeList<T> extends AbstractList<T> {

	/**
	 * This is the node class used by {@link TreeList}.<br/>
	 * Limitations of Java generic type inference do not allow making {@link Node}
	 * a generic type while preserving the tree merge functionality.
	 */
	protected static final class Node extends WeightedNode<Node> {

		/**
		 * The value held by the node
		 */
		private Object value;

		/**
		 * Creates a new node with the specified colour and subtree weight.
		 * @param isRed the flag showing if the node is red
		 */
		public Node(boolean isRed) {
			super(isRed);
		}

		/**
		 * Returns the adjunct value held by the node.
		 * @return the node's adjunct value
		 */
		public Object getValue() {
			return value;
		}

		@Override
		public void copyPayload(WeightedNode<Node> source) {
			value = ((Node)source).value;
		}

		@Override
		public void dropPayload() {
			value = null;
		}

		/**
		 * Replaces the adjunct value of the node.
		 * @param value the new adjunct value
		 * @return this node
		 */
		public Node withValue(Object value) {
			this.value = value;
			return this;
		}
	}

	/**
	 * This class is a {@link RankedRedBlackTree} specification for the custom {@link Node} type.
	 */
	protected static final class Tree extends RankedRedBlackTree<Node> {

		protected Tree() {
			super(COMMON_NIL);
		}

		@Override
		protected Node produceNode(boolean isRed) {
			return new Node(isRed);
		}
	}

	/**
	 * This class implements a {@link ListIterator} over the values stored by the list
	 * and is based on a tree node list iterator.
	 */
	protected class ListIteratorImpl implements ListIterator<T> {

		/**
		 * The underlying list iterator over the nodes of the tree
		 */
		protected TreeNodeListIterator<Node> nodeListIterator;

		/**
		 * The modification counter used to detect the list modifications performed
		 * without using this iterator
		 */
		protected int expectedModCount;

		/**
		 * Creates a new list iterator based on the supplied tree node iterator.
		 * @param nodeListIterator the underlying list iterator over the nodes of a tree
		 */
		public ListIteratorImpl(TreeNodeListIterator<Node> nodeListIterator) {
			this.nodeListIterator = nodeListIterator;
			this.expectedModCount = TreeList.this.modCount;
		}

		@Override
		public boolean hasNext() {
			return nodeListIterator.hasNext();
		}

		@SuppressWarnings("unchecked")
		@Override
		public T next() {
			checkModCount();
			return (T)nodeListIterator.next().getValue();
		}

		@Override
		public boolean hasPrevious() {
			return nodeListIterator.hasPrevious();
		}

		@SuppressWarnings("unchecked")
		@Override
		public T previous() {
			checkModCount();
			return (T)nodeListIterator.previous().getValue();
		}

		@Override
		public int nextIndex() {
			return nodeListIterator.nextIndex();
		}

		@Override
		public int previousIndex() {
			return nodeListIterator.previousIndex();
		}

		@Override
		public void set(T t) {
			checkModCount();
			if (!nodeListIterator.isModificationPossible()) {
				throw new IllegalStateException();
			}

			// modification is possible -> the current node cannot be null
			nodeListIterator.getCurrentNode().withValue(t);
		}

		@Override
		public void remove() {
			checkModCount();

			nodeListIterator.remove();

			updateModCount();
		}

		@Override
		public void add(T value) {
			checkModCount();

			Node node = new Node(RED).withValue(value);
			nodeListIterator.add(node);

			updateModCount();
		}

		/**
		 * Tries to detect a modification made to the list being iterated over,
		 * not using this iterator.
		 * @throws ConcurrentModificationException if an external modification is detected
		 */
		private void checkModCount() {
			if (expectedModCount != TreeList.this.modCount) {
				throw new ConcurrentModificationException();
			}
		}

		/**
		 * Increases the expected modification identifier after a modification operation
		 * performed using this iterator.
		 */
		private void updateModCount() {
			expectedModCount = ++TreeList.this.modCount;
		}
	}

	/**
	 * This class is a {@link Spliterator} implementation for the tree-based list.
	 */
	protected class SpliteratorImpl implements Spliterator<T> {

		/**
		 * The underlying pseudo-spliterator over the sequence of tree nodes
		 */
		protected final TreeNodeSpliteratorPrototype<Node> nodeSpliterator;

		/**
		 * The modification counter used to detect the list modifications performed
		 * without using this iterator
		 */
		protected final int expectedModCount;

		/**
		 * Creates a spliterator based on the specified node pseudo-spliterator.
		 * @param nodeSpliterator the underlying node pseudo-spliterator
		 * @param expectedModCount the last tree modification identifier
		 */
		public SpliteratorImpl(TreeNodeSpliteratorPrototype<Node> nodeSpliterator, int expectedModCount) {
			this.nodeSpliterator = nodeSpliterator;
			this.expectedModCount = expectedModCount;
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			checkModCount();
			Node node = nodeSpliterator.tryAdvance();
			if (node != null) {
				@SuppressWarnings("unchecked")
				T value = (T)node.getValue();
				action.accept(value);
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Spliterator<T> trySplit() {
			checkModCount();
			TreeNodeSpliteratorPrototype<Node> prefixSpliterator = nodeSpliterator.trySplit();
			return prefixSpliterator == null
				? null : new SpliteratorImpl(prefixSpliterator, expectedModCount);
		}

		@Override
		public long estimateSize() {
			return nodeSpliterator.estimateSize();
		}

		@Override
		public long getExactSizeIfKnown() {
			return nodeSpliterator.getExactSizeIfKnown();
		}

		@Override
		public int characteristics() {
			return nodeSpliterator.characteristics();
		}

		/**
		 * Tries to detect a modification made to the list being spliterated.
		 * @throws ConcurrentModificationException if a modification is detected
		 */
		private void checkModCount() {
			if (expectedModCount != TreeList.this.modCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	/**
	 * The common NIL node used across all the instances of {@link TreeList}
	 */
	protected static final Node COMMON_NIL = new Node(BLACK).withWeight(0);

	/**
	 * The initial size of the node stack buffer
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1 + RankedRedBlackTree.maxTreeDepth(31);

	/**
	 * This number indicates how big must the collection be so that its appending or
	 * prepending to the list using {@link #addAll(int, Collection)} use the
	 * bulk load into a new list + list concatenation operations instead of adding
	 * elements one by one.
	 */
	private static final int MIN_BULK_LOAD_ELEMENTS = 16;

	/**
	 * The underlying ranked red-black tree
	 */
	protected final Tree tree;

	/**
	 * Pre-allocated node list used in modification operations; the references to nodes
	 * are not explicitly freed after modifications, so this list may be storing nodes
	 * after their deletion; the deletion method(s) must therefore nullify the value reference
	 * of the deleted node manually; the list size grows automatically when needed and is limited
	 * by the maximum tree depth (which cannot exceed 2 * log2(MAX_INTEGER)) so there is no unlimited
	 * leak of {@link WeightedNode} objects.
	 */
	private final RankedRedBlackTree.NodeBuffer<Node> nodeBuffer;

	/**
	 * Creates an empty {@link TreeList} instance.
	 */
	public TreeList() {
		this.tree = new Tree();
		this.nodeBuffer = new RankedRedBlackTree.NodeBuffer<>(DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Bulk-loads the elements of the given {@link Collection} into a new empty {@link TreeList}
	 * in <em>O(n)</em> time (compared to <em>O(n log(n))</em> when inserting the elements one by one).
	 * @param source the source collection
	 */
	public TreeList(Collection<? extends T> source) {
		int size = source.size();
		if (size > RankedRedBlackTree.MAX_TREE_SIZE) {
			throw new IllegalArgumentException(String.format(
				"The source collection must not contain more than %s elements.", RankedRedBlackTree.MAX_TREE_SIZE));
		}

		this.tree = new Tree();
		if (size > 0) {
			int blackHeight = size == 1 ? 1 : 31 - Integer.numberOfLeadingZeros(size);
			this.tree.root = buildTree(source.iterator(), size, blackHeight);
		}
		this.nodeBuffer = new RankedRedBlackTree.NodeBuffer<>(
			1 + RankedRedBlackTree.maxTreeDepth(this.tree.root.getWeight()));
	}

	/**
	 * Creates a {@link TreeList} with the specified underlying {@link RankedRedBlackTree} instance.
	 * @param tree the backing tree for the new list
	 */
	protected TreeList(Tree tree) {
		this.tree = tree;
		this.nodeBuffer = new RankedRedBlackTree.NodeBuffer<>(
			1 + RankedRedBlackTree.maxTreeDepth(tree.root.getWeight()));
	}

	@Override
	public T get(int index) {
		if (index < 0 || index >= tree.root.getWeight()) {
			throw new IndexOutOfBoundsException();
		}

		@SuppressWarnings("unchecked")
		T value = (T)tree.find(index, null).getValue();
		return value;
	}

	@Override
	public int size() {
		return tree.root.getWeight();
	}

	@Override
	public boolean isEmpty() {
		return tree.root == tree.nil;
	}

	@Override
	public void clear() {
		tree.root = tree.nil;
		nodeBuffer.reinitialize(DEFAULT_BUFFER_SIZE);
		modCount++;
	}

	@Override
	public void add(int index, T value) {
		if (index < 0 || index > tree.root.getWeight()) {
			throw new IndexOutOfBoundsException();
		}
		tree.checkSizeLimit();

		if (tree.root == tree.nil) {
			tree.root = new Node(BLACK)
				.withLeft(tree.nil).withRight(tree.nil).withValue(value);
		} else {
			Node node = new Node(RED)
				.withLeft(tree.nil).withRight(tree.nil).withValue(value);

			// search for the insertion point (nil leaf to substitute with a value-node)
			nodeBuffer.clear();
			RankedRedBlackTree.NodeBuffer<Node> nodeStack = nodeBuffer;
			Node current = tree.root;
			while (current != tree.nil) {
				nodeStack.add(current);
				Node left = current.getLeft();
				int leftWeight = left.getWeight();
				if (index <= leftWeight) {
					if (left == tree.nil) {
						current.withLeft(node);
					}
					current = left;
				} else {
					index -= leftWeight + 1;
					Node right = current.getRight();
					if (right == tree.nil) {
						current.withRight(node);
					}
					current = right;
				}
			}

			nodeStack.add(node);
			tree.afterInsert(nodeStack);
		}

		modCount++;
	}

	@Override
	public T remove(int index) {
		if (index < 0 || index >= tree.root.getWeight()) {
			throw new IndexOutOfBoundsException();
		}

		// search for the deletion point
		Node node = tree.find(index, nodeBuffer);
		@SuppressWarnings("unchecked")
		T value = (T)node.getValue();
		tree.remove(nodeBuffer);

		modCount++;
		return value;
	}

	@Override
	public T set(int index, T element) {
		Node node = tree.find(index, null);
		@SuppressWarnings("unchecked")
		T oldValue = (T)node.getValue();
		node.withValue(element);
		return oldValue;
	}

	@Override
	public Iterator<T> iterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		if (index < 0 || index > tree.root.getWeight()) {
			throw new IndexOutOfBoundsException();
		}

		RankedRedBlackTree.NodeBuffer<Node> cursor =
			new RankedRedBlackTree.NodeBuffer<>(nodeBuffer.getCapacity());
		boolean before = true;
		if (tree.root != tree.nil) {
			if (index == tree.root.getWeight()) {
				before = false;
				index--;
			}
			tree.find(index, cursor);
		}

		return new ListIteratorImpl(new TreeNodeListIterator<>(tree, cursor, before));
	}

	@Override
	public Spliterator<T> spliterator() {
		if (tree.root == tree.nil) {
			return Spliterators.emptySpliterator();
		} else {
			RankedRedBlackTree.NodeBuffer<Node> buffer =
				new RankedRedBlackTree.NodeBuffer<>(nodeBuffer.getCapacity());
			tree.find(0, buffer);
			return new SpliteratorImpl(
				new TreeNodeSpliteratorPrototype<>(buffer, tree.root.getWeight()), modCount);
		}
	}

	/**
	 * Inserts the contents of the passed collection into the list starting from the given
	 * position, preserving the order of collection elements as returned by its iterator. The
	 * original elements whose indices start the insertion position will have their indices
	 * increased by the size of the collection.<br/>
	 * If the collection is prepended ({@code index == 0}) or appended ({@code index == size()})
	 * to this list, the operation runs in <em>O(m + log(m + n))</em> time, where <em>n</em> is the
	 * size of this list and <em>m</em> is the size of the collection. Otherwise, the operation runs in
	 * <em>O(m log(m + n))</em> time.
	 * @param index the index at which the first element of the collection will be inserted
	 * @param c the collection whose elements are to be added to this list
	 * @return {@code true} if and only if the passed collection is not empty
	 */
	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		if (index != 0 && index != tree.root.getWeight()
			|| c.size() < MIN_BULK_LOAD_ELEMENTS) {

			return super.addAll(index, c);
		} else {
			TreeList<T> adfix = new TreeList<>(c);
			Tree resultTree = new Tree();

			if (index == 0) {
				RankedRedBlackTree.merge(adfix.tree, tree, resultTree);
			} else { // index == tree.root.getWeight()
				RankedRedBlackTree.merge(tree, adfix.tree, resultTree);
			}
			tree.root = resultTree.root;

			int bufferCapacity = 1 + RankedRedBlackTree.maxTreeDepth(tree.root.getWeight());
			if (bufferCapacity > nodeBuffer.getCapacity()) {
				nodeBuffer.reinitialize(bufferCapacity);
			}

			modCount++;
			return true;
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return addAll(tree.root.getWeight(), c);
	}

	/**
	 * Concatenates two different {@link TreeList} instances in
	 * <em>O(log(n<sub>1</sub> + n<sub>2</sub>))</em> time, where
	 * <em>n<sub>1</sub></em> and <em>n<sub>2</sub></em> are the sizes of the two lists.
	 * Both original lists are cleared.
	 * @param prefix the list containing the head portion of the expected result
	 * @param suffix the list containing the tail portion of the expected result
	 * @return the new list being a concatenation of the two original lists
	 */
	public static <Q> TreeList<Q> concat(TreeList<? extends Q> prefix, TreeList<? extends Q> suffix) {
		if (prefix == suffix) {
			throw new IllegalArgumentException("The prefix and suffix lists must be different instances.");
		}

		Tree mergedTree = new Tree();
		RankedRedBlackTree.merge(prefix.tree, suffix.tree, mergedTree);
		Stream.of(prefix, suffix).forEachOrdered(list -> {
			list.modCount++;
			list.nodeBuffer.reinitialize(DEFAULT_BUFFER_SIZE);
		});
		return new TreeList<>(mergedTree);
	}

	/**
	 * Bulk-loads the given number of iterator elements into a new tree so that any two
	 * of its leave nodes have depths differing by no more than 1.
	 * @param values the value source
	 * @param length the number of elements to fetch from the source
	 * @param blackHeight the black height of the node
	 * @return the root node of the new tree
	 */
	private Node buildTree(Iterator<? extends T> values, int length, int blackHeight) {

		Node root;

		if (blackHeight == 1) {
			if (length == 1) {
				root = produceNode(values).withLeft(tree.nil).withRight(tree.nil);
			} else if (length == 2) {
				Node left = produceNode(values);
				left.withLeft(tree.nil).withRight(tree.nil).makeRed();
				root = produceNode(values).withLeft(left).withRight(tree.nil);
			} else { // length == 3
				Node left = produceNode(values).withLeft(tree.nil).withRight(tree.nil);
				left.makeRed();
				root = produceNode(values);
				Node right = produceNode(values).withLeft(tree.nil).withRight(tree.nil);
				right.makeRed();
				root.withLeft(left).withRight(right);
			}
		} else { // has both subtrees
			blackHeight--;
			Node left = buildTree(values, length / 2, blackHeight);
			root = produceNode(values);
			Node right = buildTree(values, length - 1 - length / 2, blackHeight);
			root.withLeft(left).withRight(right);
		}

		root.withWeight(length);
		return root;
	}

	/**
	 * Creates a new black leaf node with a value taken from the specified iterator.
	 * @param values the value source
	 * @return a new black leaf node with a value taken from the specified iterator
	 */
	private Node produceNode(Iterator<? extends T> values) {
		return new Node(BLACK).withValue(values.next());
	}
}
