package dev.ornamental.collection;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * This class is a {@link ListIterator} implementation over the {@link RankedRedBlackTree} nodes
 * internally used by the {@link TreeList} class.
 * @param <T> the {@link WeightedNode} implementation used by the {@link RankedRedBlackTree}
 * to iterate over the nodes of
 */
final class TreeNodeListIterator<T extends WeightedNode<T>> implements ListIterator<T> {

	/**
	 * The tree over which the iteration occurs
	 */
	private final RankedRedBlackTree<T> tree;

	/**
	 * Path from the tree root to the node currently pointed to by the iterator;
	 * the iterator is positioned before the node unless the last iteration method invoked
	 * was {@link #next()} and neither {@link #add(Object)} nor {@link #remove()} has been
	 * invoked afterwards, or the iterator is located after the last element of the tree;
	 * otherwise, the iterator is positioned after the node; see {@link #before}
	 * flag to determine which is the case.
	 */
	private final RankedRedBlackTree.NodeBuffer<T> currentNode;

	/**
	 * The flag set if and only if the iterator is located before the element of the tree at the end of
	 * {@link #currentNode} node buffer
	 */
	private boolean before;

	/**
	 * The rank of the element at the end of the node buffer (0 for an empty list)
	 */
	private int rank;

	/**
	 * The flag showing that the iterator is in a state where modification operations are accessible
	 */
	private boolean modificationPossible = false;

	/**
	 * Creates a new list iterator with the specified initial state.
	 * @param tree the tree to iterate over the nodes of
	 * @param initialPosition the path to the initial node; may be empty if the tree is empty;
	 * the first element is the tree root, each subsequent element is a child of the previous one
	 * @param before the flag showing if the iterator is positioned before ({@code true} or
	 * after ({@code false}) the initial node
	 */
	public TreeNodeListIterator(RankedRedBlackTree<T> tree,
		RankedRedBlackTree.NodeBuffer<T> initialPosition, boolean before) {

		this.tree = tree;
		this.currentNode = initialPosition;
		this.before = before;

		if (initialPosition.size() > 0) {
			T current = currentNode.get(0);
			this.rank = current.getLeft().getWeight();

			for (int i = 1; i < currentNode.size(); i++) {
				T child = currentNode.get(i);
				if (current.getLeft() == child) {
					this.rank = this.rank + child.getLeft().getWeight() - child.getWeight();
				} else {
					this.rank = this.rank + child.getLeft().getWeight() + 1;
				}
				current = child;
			}
		} else {
			this.rank = 0;
			this.before = true;
		}
	}

	@Override
	public boolean hasNext() {
		return
			tree.root != tree.nil // currentNode.size() > 0
			&& (before || rank < tree.root.getWeight() - 1);
	}

	@Override
	public T next() {
		if (!hasNext()) {
			throw new NoSuchElementException("The iteration has reached the end of the list.");
		}

		T current = currentNode.get(currentNode.size() - 1);
		if (before) {
			before = false;
		} else {
			if (current.getRight() != tree.nil) {
				current = current.getRight();
				currentNode.add(current);
				while (current.getLeft() != tree.nil) {
					current = current.getLeft();
					currentNode.add(current);
				}
			} else { // leftTurns > 0, see hasNext()
				while (true) {
					currentNode.removeLast();
					T parent = currentNode.get(currentNode.size() - 1);
					if (current == parent.getLeft()) {
						current = parent;
						break;
					}
					current = parent;
				}
			}

			rank++;
		}

		modificationPossible = true;
		return current;
	}

	@Override
	public boolean hasPrevious() {
		return
			tree.root != tree.nil // currentNode.size() > 0
			&& (!before || rank > 0);
	}

	@Override
	public T previous() {
		if (!hasPrevious()) {
			throw new NoSuchElementException("The iteration has reached the beginning of the list.");
		}

		T current = currentNode.get(currentNode.size() - 1);
		if (!before) {
			before = true;
		} else {
			if (current.getLeft() != tree.nil) {
				current = current.getLeft();
				currentNode.add(current);
				while (current.getRight() != tree.nil) {
					current = current.getRight();
					currentNode.add(current);
				}
			} else {
				while (true) {
					currentNode.removeLast();
					T parent = currentNode.get(currentNode.size() - 1);
					if (current == parent.getRight()) {
						current = parent;
						break;
					}
					current = parent;
				}
			}

			rank--;
		}

		modificationPossible = true;
		return current;
	}

	@Override
	public int nextIndex() {
		return before ? rank : rank + 1;
	}

	@Override
	public int previousIndex() {
		return before ? rank - 1 : rank;
	}

	@Override
	public void remove() {
		if (!modificationPossible) {
			throw new IllegalStateException();
		}

		tree.remove(currentNode);
		if (tree.root == tree.nil) { // the tree became empty
			currentNode.clear();
		} else {
			if (rank == tree.root.getWeight()) { // deleted the highest-ranked node
				rank--;
				before = false;
			} else if (rank == 0) {
				before = true;
			} else if (!before) {
				rank--;
			} // otherwise, the rank does not change

			tree.find(rank, currentNode); // relocate the iterator
		}

		modificationPossible = false;
	}

	@Override
	public final void add(T node) {
		assert node.isRed();
		assert node.getWeight() == 1;

		if (currentNode.size() == 0) { // empty tree
			node.makeBlack();
			tree.root = node;
			currentNode.add(node);
			rank = 0;
			before = false;
		} else {
			tree.checkSizeLimit();
			T parent = currentNode.get(currentNode.size() - 1);
			boolean addToLeft;
			if (before) {
				if (parent.getLeft() == tree.nil) {
					addToLeft = true;
				} else {
					addToLeft = false;
					parent = parent.getLeft();
					currentNode.add(parent);
					while (parent.getRight() != tree.nil) {
						parent = parent.getRight();
						currentNode.add(parent);
					}
				}
			} else {
				if (parent.getRight() == tree.nil) {
					addToLeft = false;
				} else {
					addToLeft = true;
					parent = parent.getRight();
					currentNode.add(parent);
					while (parent.getLeft() != tree.nil) {
						parent = parent.getLeft();
						currentNode.add(parent);
					}
				}
			}

			node.withLeft(tree.nil).withRight(tree.nil);
			if (addToLeft) {
				parent.withLeft(node);
			} else {
				parent.withRight(node);
			}
			currentNode.add(node);

			tree.afterInsert(currentNode); // preserves the path to the inserted node
			if (!before) {
				rank++;
			}
			before = false;
		}

		modificationPossible = false;
	}

	@Override
	public void set(T t) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the modification admissibility flag.
	 * @return {@code true} if and only if the list iterator state admits an invocation
	 * of any of the modification methods
	 */
	public boolean isModificationPossible() {
		return modificationPossible;
	}

	/**
	 * Returns the node the iterator is positioned on (the last node returned by a
	 * successful invocation of {@link #next()} or {@link #previous()}).
	 * @return the last node returned by this iterator's {@link #next()} or {@link #previous()} method
	 */
	public T getCurrentNode() {
		int size = currentNode.size();
		return size == 0 ? null : currentNode.get(size - 1);
	}
}
