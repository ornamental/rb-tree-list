package dev.ornamental.collection;

/**
 * Represents a node of a red-black tree with additional field containing the
 * weight of the subtree having this node as its root.
 * The classes using this one have to extend it in order to add custom data to the nodes.
 * @param <S> the concrete extending type
 */
abstract class WeightedNode<S extends WeightedNode<S>> {

	/**
	 * The number indicating both colour of the node and its weight;
	 * the red nodes have positive markers, the black ones have negative markers
	 */
	private int marker;

	/**
	 * The left child node
	 */
	private S left;

	/**
	 * The right child node
	 */
	private S right;

	/**
	 * Creates a node with given colour and weight equal to 1.
	 * @param isRed the flag indicating if the new node must be red ({@code true})
	 * or black ({@code false})
	 */
	public WeightedNode(boolean isRed) {
		this.marker = isRed ? 2 : -2;
	}

	/**
	 * Returns the left child node.
	 * @return the left child node
	 */
	public S getLeft() {
		return left;
	}

	/**
	 * Sets the left child node.
	 * @param left the node to be set as the left child
	 * @return this node
	 */
	@SuppressWarnings("unchecked")
	public S withLeft(S left) {
		this.left = left;
		return (S)this;
	}

	/**
	 * Returns the right child node.
	 * @return the right child node
	 */
	public S getRight() {
		return right;
	}

	/**
	 * Sets the right child node.
	 * @param right the node to be set as the right child
	 * @return this node
	 */
	@SuppressWarnings("unchecked")
	public S withRight(S right) {
		this.right = right;
		return (S)this;
	}

	/**
	 * Determines if the node is red.
	 * @return {@code true} if the node is a red one, {@code false} if it is black
	 */
	public boolean isRed() {
		return marker > 0;
	}

	/**
	 * Determines if the node is black.
	 * @return {@code true} if the node is a black one, {@code false} if it is red
	 */
	public boolean isBlack() {
		return marker < 0;
	}

	/**
	 * Renders the node red.
	 */
	public void makeRed() {
		if (marker < 0) {
			marker = -marker;
		}
	}

	/**
	 * Renders the node black.
	 */
	public void makeBlack() {
		if (marker > 0) {
			marker = -marker;
		}
	}

	/**
	 * Returns the weight of the node.
	 * @return the weight of the node
	 */
	public int getWeight() {
		return Math.abs(marker) - 1;
	}

	/**
	 * Sets the weight of the node.
	 * @param weight the new weight of the node
	 * @return this node
	 */
	@SuppressWarnings("unchecked")
	public S withWeight(int weight) {
		if (weight < 0) {
			throw new IllegalArgumentException("A non-negative weight is expected.");
		}
		marker = marker > 0 ? (weight + 1) : -(weight + 1);

		return (S)this;
	}

	/**
	 * Makes this node's colour the same as another one's.
	 * @param other the node to copy the colour from
	 */
	public void copyColour(WeightedNode<S> other) {
		if (marker > 0 && other.marker < 0 || marker < 0 && other.marker > 0) {
			marker = -marker;
		}
	}

	/**
	 * This method has to be overridden by concrete implementations to copy
	 * the added data fields from the other node to this one. This implementation does nothing
	 * as the class has no payload fields.
	 * @param source the node to copy the additional data from
	 */
	public abstract void copyPayload(WeightedNode<S> source);

	/**
	 * This method has to be overridden by concrete implementations to set
	 * all the added data fields of reference types to {@code null} to allow
	 * the corresponding objects to be GC-ed. This implementation does nothing
	 * as the class has no payload fields.
	 */
	public abstract void dropPayload();
}
