package dev.ornamental.collection;

import static dev.ornamental.collection.NodeColour.RED;

/**
 * This class represents a red-black tree storing a subtree size with each node.
 */
abstract class RankedRedBlackTree<T extends WeightedNode<T>> {

	/**
	 * This class represents a node buffer used to store the path to the modification location
	 * during the modification operations.<br/>
	 * This class does no checks of methods' argument values.
	 * @param <S> the value stored in the nodes
	 */
	protected static class NodeBuffer<S extends WeightedNode<S>> {

		private Object[] buffer;

		private int size = 0;

		public NodeBuffer(int initialCapacity) {
			buffer = new Object[initialCapacity > 1 ? initialCapacity : 1];
		}

		public NodeBuffer(NodeBuffer<S> original) {
			this.buffer = original.buffer.clone();
			this.size = original.size;
		}

		public int size() {
			return size;
		}

		/**
		 * Does not release the references previously held in the buffer in order to work in <em>O(1)</em>.
		 * It does not much matter when the class is used in {@link RankedRedBlackTree} because all
		 * the elements stored herein during the insertion are also stored in the tree as well;
		 * all the elements stored herein during the deletion except for the last one remain in the tree;
		 * all that has to be done is manually release the deleted node's value on deletion.
		 */
		public void clear() {
			size = 0;
		}

		public int getCapacity() {
			return buffer.length;
		}

		public void add(S value) {
			if (size == buffer.length) {
				Object[] newBuffer = new Object[buffer.length + 2];
				System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
				buffer = newBuffer;
			}
			buffer[size] = value;
			size++;
		}

		public void removeLast() {
			buffer[--size] = null;
		}

		/**
		 * Returns an element of the buffer by its index. The non-negative indices address
		 * the elements of the buffer; index {@code -1} is also valid and addresses {@code null} element.
		 * @param i the element of the buffer to return or -1 to return {@code null}
		 * @return a non-null buffer element, if the index is non-negative; {@code null} if
		 * the index is {@code -1}
		 */
		@SuppressWarnings("unchecked")
		public S get(int i) {
			return i == -1 ? null : (S)buffer[i];
		}

		public void set(int i, S value) {
			buffer[i] = value;
		}

		public void remove(int i) {
			for (int j = i; j < size - 1;) {
				buffer[j] = buffer[++j];
			}
			size--;
		}

		public void truncate(int size) {
			this.size = size;
		}

		public void reinitialize(int capacity) {
			size = 0;
			buffer = new Object[capacity > 1 ? capacity : 1];
		}
	}

	/**
	 * The maximum number of nodes in the tree derived from the maximum subtree weight
	 */
	public static final int MAX_TREE_SIZE = Integer.MAX_VALUE - 1;

	/**
	 * The root node of the tree
	 */
	protected T root;

	/**
	 * The common black leaf node
	 */
	protected final T nil;

	/**
	 * Creates a new empty ranked red-black tree.
	 * @param nil the singleton nil node to be used by the tree; it must be a
	 * black node with zero weight; in order for the merge operation to work
	 * properly, the same object must be used
	 */
	protected RankedRedBlackTree(T nil) {
		assert nil.getWeight() == 0;
		assert nil.isBlack();

		this.nil = nil;
		this.root = nil;
	}

	/**
	 * Creates a node with no child references (the weight is 1) and empty payload.
	 * @param isRed the flag showing if the created node must be red or black
	 * @return the new node
	 */
	protected abstract T produceNode(boolean isRed);

	/**
	 * Checks if an element may be added to the tree.
	 */
	protected void checkSizeLimit() {
		if (root.getWeight() == MAX_TREE_SIZE) {
			throw new IllegalStateException(
				String.format("The collection size limit of %s is reached.", MAX_TREE_SIZE));
		}
	}

	/**
	 * Performs red-black tree invariant corrections after a new node is inserted.
	 * The newly inserted element must be a red node with weight of 1 and nil children.
	 * It must not be the root node (in which case no invariant correction is needed
	 * as long as the inserted node is black).
	 * @param nodeStack the nodeBuffer of nodes where the first element is the tree root,
	 * each element is a child of the previous one, and the last one is the newly inserted node
	 */
	protected void afterInsert(NodeBuffer<T> nodeStack) {
		assert nodeStack.size() > 1;

		for (int i = 0; i < nodeStack.size() - 1; i++) {
			T current = nodeStack.get(i);
			current.withWeight(current.getWeight() + 1);
		}

		insertFixup(nodeStack);
	}

	/**
	 * Performs node removal given the path to the node from the root. The payload of the node
	 * is dropped after the removal to allow the corresponding objects to be GC-ed (because the
	 * node reference may still be preserved in the unused part of the node stack).
	 * @param nodeStack the node buffer where the first element is the tree root,
	 * each element is a child of the previous one, and the last one is the node to remove
	 */
	protected void remove(NodeBuffer<T> nodeStack) {
		T node = nodeStack.get(nodeStack.size() - 1);

		if (node.getLeft() != nil && node.getRight() != nil) {
			// find the successor node
			T current = node.getRight();
			nodeStack.add(current);
			while ((current = current.getLeft()) != nil) {
				nodeStack.add(current);
			}
			current = nodeStack.get(nodeStack.size() - 1);

			// the successor node will be deleted instead of the node previously destined for it
			// must move the payload from the successor to preserve it
			node.copyPayload(current);
			node = current;
		}

		for (int i = 0; i < nodeStack.size(); i++) {
			T current = nodeStack.get(i);
			current.withWeight(current.getWeight() - 1);
		}

		T child = node.getLeft();
		if (child == nil) {
			child = node.getRight();
		}

		if (node == root) {
			root = child;
			nodeStack.clear();
		} else {
			nodeStack.removeLast();
			T parent = nodeStack.get(nodeStack.size() - 1);
			if (parent.getLeft() == node) {
				parent.withLeft(child);
			} else {
				parent.withRight(child);
			}
		}

		nodeStack.add(child);

		if (root != nil) {
			assert nodeStack.size() > 0;

			if (node.isBlack()) {
				removeFixup(nodeStack);
			}
		}

		node.dropPayload();
	}

	/**
	 * Restores the red-black tree invariants after a node insertion.
	 * @param nodeStack the path to the inserted node;
	 * each element is the child node of the previous element
	 */
	private void insertFixup(NodeBuffer<T> nodeStack) {
		int currentIndex = nodeStack.size() - 1;
		T parent;
		while (currentIndex > 0
			&& (parent = nodeStack.get(currentIndex - 1)).isRed()) {

			// grandparent must exist because the root is black
			T grandparent = nodeStack.get(currentIndex - 2);
			if (grandparent.getLeft() == parent) {
				T uncle = grandparent.getRight();
				if (uncle.isRed()) {
					parent.makeBlack();
					uncle.makeBlack();
					grandparent.makeRed();
					currentIndex -= 2; // consider grandparent at the next step
				} else {
					T current = nodeStack.get(currentIndex);
					if (parent.getRight() == current) {
						leftRotate(parent, nodeStack.get(currentIndex - 2));
						// fix the nodeBuffer after the rotation: swap the current with its former parent
						// (the current index is unchanged -> starts pointing at former parent)
						nodeStack.set(currentIndex - 1, current);
						nodeStack.set(currentIndex, parent);

						// update parent and grandparent
						parent = nodeStack.get(currentIndex - 1);
						grandparent = nodeStack.get(currentIndex - 2);
					}

					parent.makeBlack();
					grandparent.makeRed();
					rightRotate(grandparent, nodeStack.get(currentIndex - 3));
					// fix the nodeBuffer after rotation (though that is only useful for maintaining iterator state)
					nodeStack.remove(currentIndex - 2);
				}
			} else {
				// symmetrical case
				T uncle = grandparent.getLeft();
				if (uncle.isRed()) {
					parent.makeBlack();
					uncle.makeBlack();
					grandparent.makeRed();
					currentIndex -= 2;
				} else {
					T current = nodeStack.get(currentIndex);
					if (parent.getLeft() == current) {
						rightRotate(parent, grandparent);
						nodeStack.set(currentIndex - 1, current);
						nodeStack.set(currentIndex, parent);

						parent = nodeStack.get(currentIndex - 1);
						grandparent = nodeStack.get(currentIndex - 2);
					}

					parent.makeBlack();
					grandparent.makeRed();
					leftRotate(grandparent, nodeStack.get(currentIndex - 3));
					nodeStack.remove(currentIndex - 2);
				}
			}
		}

		root.makeBlack();
	}

	/**
	 * Restores the red-black tree invariants after a node removal.
	 * @param nodeStack the path to the deleted node (the node itself not included),
	 * from the tree root; each element is the child node of the previous element
	 */
	private void removeFixup(NodeBuffer<T> nodeStack) {
		int index = nodeStack.size() - 1;
		T current;
		while (index != 0 && (current = nodeStack.get(index)).isBlack()) {
			T parent = nodeStack.get(index - 1);
			if (parent.getLeft() == current) {
				T sibling = parent.getRight();
				if (sibling.isRed()) {
					sibling.makeBlack();
					parent.makeRed();
					leftRotate(parent, nodeStack.get(index - 2));
					// the nodeBuffer has to be modified by inserting the former sibling as the parent's new parent;
					// the lower part of the nodeBuffer may become invalid which does not matter because it is unused
					if (index == nodeStack.size() - 1) {
						nodeStack.add(current);
					} else {
						nodeStack.set(index + 1, current);
					}
					nodeStack.set(index, parent);
					nodeStack.set(index - 1, sibling);
					index++;

					sibling = parent.getRight();
				}
				if (sibling.getLeft().isBlack() && sibling.getRight().isBlack()) {
					sibling.makeRed();
					index--;
				} else {
					if (sibling.getRight().isBlack()) {
						sibling.getLeft().makeBlack();
						sibling.makeRed();
						rightRotate(sibling, parent);
						sibling = parent.getRight();
					}
					sibling.copyColour(parent);
					parent.makeBlack();
					sibling.getRight().makeBlack();
					leftRotate(parent, nodeStack.get(index - 2));
					// no need to correct the nodeBuffer according to the rotation result as this is the last iteration
					index = 0;
				}
			} else {
				// symmetrical case
				T sibling = parent.getLeft();
				if (sibling.isRed()) {
					sibling.makeBlack();
					parent.makeRed();
					rightRotate(parent, nodeStack.get(index - 2));
					if (index == nodeStack.size() - 1) {
						nodeStack.add(current);
					} else {
						nodeStack.set(index + 1, current);
					}
					nodeStack.set(index, parent);
					nodeStack.set(index - 1, sibling);
					index++;

					sibling = parent.getLeft();
				}
				if (sibling.getRight().isBlack() && sibling.getLeft().isBlack()) {
					sibling.makeRed();
					index--;
				} else {
					if (sibling.getLeft().isBlack()) {
						sibling.getRight().makeBlack();
						sibling.makeRed();
						leftRotate(sibling, parent);
						sibling = parent.getLeft();
					}
					sibling.copyColour(parent);
					parent.makeBlack();
					sibling.getLeft().makeBlack();
					rightRotate(parent, nodeStack.get(index - 2));
					index = 0;
				}
			}
		}

		nodeStack.get(index).makeBlack();
	}

	/**
	 * Performs the right rotation of a node.<br/>
	 * It is not checked whether the supplied parent is correct.
	 * @param node the node
	 * @param parent the node's parent node
	 */
	private void leftRotate(T node, T parent) {
		T rt = node.getRight();

		node.withRight(rt.getLeft());
		rt.withLeft(node);
		if (parent != null) {
			if (parent.getLeft() == node) {
				parent.withLeft(rt);
			} else {
				parent.withRight(rt);
			}
		} else {
			root = rt;
		}

		rt.withWeight(node.getWeight());
		node.withWeight(1 + node.getLeft().getWeight() + node.getRight().getWeight());
	}

	/**
	 * Performs the right rotation of a node.<br/>
	 * It is not checked whether the supplied parent is correct.
	 * @param node the node
	 * @param parent the node's parent node
	 */
	private void rightRotate(T node, T parent) {
		T lf = node.getLeft();

		node.withLeft(lf.getRight());
		lf.withRight(node);
		if (parent != null) {
			if (parent.getLeft() == node) {
				parent.withLeft(lf);
			} else {
				parent.withRight(lf);
			}
		} else {
			root = lf;
		}

		lf.withWeight(node.getWeight());
		node.withWeight(1 + node.getLeft().getWeight() + node.getRight().getWeight());
	}

	/**
	 * Finds a tree node by its rank in the tree. Optionally stores the path to the node from the tree root.
	 * @param rank the rank of the node to find
	 * @param path either a node buffer sufficient to store the path to the node or {@code null} if
	 * there is no need to store the node path
	 * @return the node having the requested rank
	 */
	protected T find(int rank, RankedRedBlackTree.NodeBuffer<T> path) {
		if (rank < 0 || rank >= root.getWeight()) {
			throw new IndexOutOfBoundsException();
		}

		boolean fillPath = path != null;
		if (fillPath) {
			path.clear();
		}

		T current = root;
		while (true) {
			if (fillPath) {
				path.add(current);
			}

			int leftWeight = current.getLeft().getWeight();
			int direction = Integer.compare(leftWeight, rank);
			if (direction == 0) {
				return current;
			} else if (direction < 0) {
				rank -= leftWeight + 1;
				current = current.getRight();
			} else {
				current = current.getLeft();
			}
		}
	}

	/**
	 * Merges two trees so that the 0-based ranks in the second tree are increased
	 * by the number of elements in the first tree. The operation executes
	 * in <em>O(log(n<sub>1</sub> + n<sub>2</sub>))</em> time, n<sub>1</sub> and n<sub>2</sub>
	 * being the sizes of the trees.
	 * Both original trees will be emptied as the result of this operation.
	 * @param left the tree to serve as the left side of the merge; its elements' ranks will remain unchanged
	 * @param right the tree to serve as the right side of the merge (the appended one); each of its element's
	 * rank will be incremented by the amount of elements in the first tree
	 * @param result the recipient tree instance whose root will be replaced with the root of the produced
	 * tree; the current contents of this tree will be lost
	 */
	protected static <Q extends WeightedNode<Q>> void
		merge(RankedRedBlackTree<Q> left, RankedRedBlackTree<Q> right, RankedRedBlackTree<Q> result) {

		if (right.root == right.nil) {
			result.root = left.root;
		} else if (left.root == left.nil) {
			result.root = right.root;
		} else {
			long newRootWeight = left.root.getWeight() + (long)right.root.getWeight();
			if (newRootWeight > RankedRedBlackTree.MAX_TREE_SIZE) {
				throw new IllegalStateException(String.format(
					"The resulting collection size limit of %s would be exceeded.", RankedRedBlackTree.MAX_TREE_SIZE));
			}

			NodeBuffer<Q> leftBuffer = new NodeBuffer<>(maxTreeDepth(left.root.getWeight()));
			NodeBuffer<Q> rightBuffer = new NodeBuffer<>(maxTreeDepth(right.root.getWeight()));

			int leftBlackHeight = getBlackHeight(left, leftBuffer, true); // memorize the rightmost node path
			int rightBlackHeight = getBlackHeight(right, rightBuffer, false); // memorize the leftmost node path

			if (leftBlackHeight >= rightBlackHeight) {
				// the right tree will be appended to the right of this one
				Q leftmost = result.produceNode(RED);
				leftmost.copyPayload(rightBuffer.get(rightBuffer.size() - 1));
				right.remove(rightBuffer);

				if (leftmost.isBlack()) { // the black height of the right tree may have decreased
					rightBlackHeight = getBlackHeight(right, rightBuffer, false);
				}

				if (leftBlackHeight == rightBlackHeight) {
					leftmost.makeBlack();
					leftmost.withLeft(left.root);
					leftmost.withRight(right.root);
					leftmost.withWeight((int)newRootWeight);
					result.root = leftmost;
				} else {
					// find the rightmost black vertex having the same black height as the right tree
					int cursor = leftBuffer.size() - 1;
					while (rightBlackHeight > 1) { // the black leaves are not on the stacks, so compare to 1
						if (leftBuffer.get(cursor).isBlack()) {
							rightBlackHeight--;
						}
						cursor--;
					}
					cursor++;

					leftmost.makeRed();
					leftmost.withLeft(leftBuffer.get(cursor));
					leftmost.withRight(right.root);

					// the cursor is not on the root because leftBlackHeight > rightBlackHeight;
					// also, the cursor points to the right child of its parent
					leftBuffer.get(cursor - 1).withRight(leftmost);
					leftBuffer.set(cursor, leftmost);
					leftBuffer.truncate(cursor + 1);

					// update weights up the stack
					for (int i = cursor; i >= 0; i--) {
						Q node = leftBuffer.get(i);
						node.withWeight(1 + node.getLeft().getWeight() + node.getRight().getWeight());
					}

					// the only invariant violation possible is that the new red node has a red parent;
					// this can be handled just like in case with node insertion
					left.insertFixup(leftBuffer);
					result.root = left.root;
				}
			} else {
				// left tree will be prepended to the left of the right tree; the code is symmetric to the above
				// except that the case leftBlackHeight == rightBlackHeight is not possible
				Q rightmost = result.produceNode(RED);
				rightmost.copyPayload(leftBuffer.get(leftBuffer.size() - 1));
				left.remove(leftBuffer);

				if (rightmost.isBlack()) {
					leftBlackHeight = getBlackHeight(left, leftBuffer, true);
				}

				int cursor = rightBuffer.size() - 1;
				while (leftBlackHeight > 1) {
					if (rightBuffer.get(cursor).isBlack()) {
						leftBlackHeight--;
					}
					cursor--;
				}
				cursor++;

				rightmost.withLeft(left.root);
				rightmost.withRight(rightBuffer.get(cursor));
				rightmost.makeRed();

				rightBuffer.get(cursor - 1).withLeft(rightmost);
				rightBuffer.set(cursor, rightmost);
				rightBuffer.truncate(cursor + 1);

				for (int i = cursor; i >= 0; i--) {
					Q node = rightBuffer.get(i);
					node.withWeight(1 + node.getLeft().getWeight() + node.getRight().getWeight());
				}

				right.insertFixup(rightBuffer);
				result.root = right.root;
			}
		}

		// the original trees have to be cleared
		left.root = left.nil;
		right.root = right.nil;
	}

	/**
	 * Gives a maximum tree depth estimate for the given node count.
	 * @param size the number of nodes in the tree
	 * @return the tree depth estimate (the worst overestimate is by 2)
	 */
	protected static int maxTreeDepth(int size) {
		// may overestimate the depth by 1 or 2 due to integral log2 calculation
		return 2 * (32 - Integer.numberOfLeadingZeros(size));
	}

	/**
	 * Descends to the rightmost of the leftmost node of the tree counting the black height of its root
	 * at the same time.
	 * @param tree the tree to find the black height of
	 * @param buffer the buffer to store the path to the rightmost (leftmost) node in
	 * @param rightmost the flag showing if the rightmost ({@code true}) of the leftmost ({@code false})
	 * node has to be located
	 * @return the black height of the tree
	 */
	private static <Q extends WeightedNode<Q>> int getBlackHeight(
		RankedRedBlackTree<Q> tree, NodeBuffer<Q> buffer, boolean rightmost) {

		buffer.clear();
		Q current = tree.root;
		int blackHeight = 1; // for the always black root
		while (current != tree.nil) {
			buffer.add(current);
			current = (rightmost ? current.getRight() : current.getLeft());
			if (current.isBlack()) {
				blackHeight++;
			}
		}

		return blackHeight;
	}
}
