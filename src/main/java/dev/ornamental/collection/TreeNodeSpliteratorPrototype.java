package dev.ornamental.collection;

import java.util.Spliterator;

/**
 * Spliterator prototype. Does not implement {@link Spliterator} due to performance reasons:
 * method {@link #tryAdvance()} does not take a consumer and instead returns the node (or {@code null}).
 * @param <T> the node type
 */
final class TreeNodeSpliteratorPrototype<T extends WeightedNode<T>> {

	/**
	 * The remaining number of elements served by this spliterator prototype
	 */
	private int size;

	/**
	 * The path to the current node, starting from the root node
	 */
	private RankedRedBlackTree.NodeBuffer<T> currentNode;

	/**
	 * Creates a spliterator prototype positioned on the specified node
	 * @param initialNode the path to the node from the root node of the tree
	 * @param size the number of sequential tree nodes to be returned by {@link #tryAdvance()}
	 * unless {@link #trySplit()} is invoked and returns a spliterator prototype
	 */
	public TreeNodeSpliteratorPrototype(RankedRedBlackTree.NodeBuffer<T> initialNode, int size) {
		this.currentNode = initialNode;
		this.size = size;
	}

	/**
	 * Advances to the next node, if any left.
	 * @return the current node, if the spliterator is located on one; otherwise, {@code null}
	 */
	public T tryAdvance() {
		if (size > 0) {
			size--;
			T node = currentNode.get(currentNode.size() - 1);
			if (size > 0) {
				advance(currentNode, 1);
			}
			return node;
		}

		return null;
	}

	/**
	 * Tries to split this spliterator prototype in two. The operation succeeds if there are at least two
	 * elements left in this spliterator prototype.
	 * @return {@code null} if this spliterator prototype has one or no elements left; otherwise,
	 * a new spliterator prototype
	 */
	public TreeNodeSpliteratorPrototype<T> trySplit() {
		if (size < 2) {
			return null;
		}

		RankedRedBlackTree.NodeBuffer<T> copy = new RankedRedBlackTree.NodeBuffer<>(currentNode);
		int prefixSize = size / 2;
		TreeNodeSpliteratorPrototype<T> prefixSpliterator =
			new TreeNodeSpliteratorPrototype<>(currentNode, prefixSize);

		currentNode = copy;
		size -= prefixSize;
		advance(currentNode, prefixSize);

		return prefixSpliterator;
	}

	/**
	 * Returns the number of elements left in this spliterator prototype.
	 * @return the number of elements left in this spliterator prototype
	 */
	public long estimateSize() {
		return size;
	}

	/**
	 * Returns the number of elements left in this spliterator prototype.
	 * @return the number of elements left in this spliterator prototype
	 */
	public long getExactSizeIfKnown() {
		return size;
	}

	/**
	 * Returns the characteristics of this spliterator prototype.
	 * @return this spliterator prototype is ordered; it and its child spliterator prototypes,
	 * if any, report their respective exact sizes
	 */
	public int characteristics() {
		return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
	}

	/**
	 * Modifies the node path supplied so that it point to the node whose index
	 * is greater than the index of the node currently pointed to by the specified number
	 * @param path the path to the current node (also the output parameter)
	 * @param indexIncrement the number of nodes to advance by
	 * @param <Q> the type of the weighted node
	 */
	private static <Q extends WeightedNode<Q>> void advance(
		RankedRedBlackTree.NodeBuffer<Q> path, int indexIncrement) {

		// note that though this value may become negative during the traversal, the input must be non-negative
		assert indexIncrement >= 0;

		Q current = path.get(path.size() - 1);
		while (indexIncrement != 0) {
			if (indexIncrement > 0) {
				if (indexIncrement <= current.getRight().getWeight()) {
					current = current.getRight();
					path.add(current);
					indexIncrement -= current.getLeft().getWeight() + 1;
				} else {
					path.removeLast();
					Q parent = path.get(path.size() - 1);
					if (current == parent.getRight()) {
						indexIncrement += current.getLeft().getWeight() + 1;
					} else {
						indexIncrement -= current.getRight().getWeight() + 1;
					}
					current = parent;
				}
			} else {
				// the assertion in the beginning of the method guarantees
				// that in this case there is no need for ascension	to parent node
				current = current.getLeft();
				path.add(current);
				indexIncrement += current.getRight().getWeight() + 1;
			}
		}
	}
}
