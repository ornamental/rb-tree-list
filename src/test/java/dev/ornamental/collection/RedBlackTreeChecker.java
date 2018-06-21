package dev.ornamental.collection;

/**
 * This utility class checks the ranked red-black tree invariants.
 */
public final class RedBlackTreeChecker {

	private RedBlackTreeChecker() { }

	/**
	 * Performs the invariant checking for the specified tree.
	 * @param tree the tree to subject to invariant checking
	 * @param <T> the node type used by the tree
	 * @throws AssertionError if a ranked red-black tree invariant violation is detected
	 */
	public static <T extends WeightedNode<T>> void checkTreeInvariants(RankedRedBlackTree<T> tree) {
		if (tree.root == tree.nil) {
			return;
		}
		if (!tree.root.isBlack()) {
			throw new AssertionError("The root is not black.");
		}
		checkSubtree(tree.root, tree.nil);
	}

	private static <Q extends WeightedNode<Q>> int checkSubtree(Q node, Q nil) {
		int leftBlackHeight = node.getLeft() == nil ? 0 : checkSubtree(node.getLeft(), nil);
		int rightBlackHeight = node.getRight() == nil ? 0 : checkSubtree(node.getRight(), nil);

		if (node.getLeft().getWeight() + node.getRight().getWeight() + 1 != node.getWeight()) {
			throw new AssertionError("Node weight does not match child nodes' weights.");
		}

		if (leftBlackHeight < 0) {
			return leftBlackHeight;
		} else if (rightBlackHeight < 0) {
			return rightBlackHeight;
		} else if (leftBlackHeight != rightBlackHeight) {
			throw new AssertionError("The black height invariant does not hold.");
		} else if (node.isRed() && (node.getLeft().isRed() || node.getRight().isRed())) {
			throw new AssertionError("There is a red node having a red parent node in the tree.");
		} else {
			if (node.isBlack()) {
				leftBlackHeight++;
			}
			return leftBlackHeight;
		}
	}
}
