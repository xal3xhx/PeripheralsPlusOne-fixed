package dan200.computercraft.api.network.wired;

import javax.annotation.Nonnull;

/**
 * A wired network is composed of one of more {@link IWiredNode}s, a set of connections between them, and a series
 * of peripherals.
 *
 * Networks from a connected graph. This means there is some path between all nodes on the network. Further more, if
 * there is some path between two nodes then they must be on the same network. {@link IWiredNetwork} will automatically
 * handle the merging and splitting of networks (and thus changing of available nodes and peripherals) as connections
 * change.
 *
 * This does mean one can not rely on the network remaining consistent between subsequent operations. Consequently,
 * it is generally preferred to use the methods provided by {@link IWiredNode}.
 *
 * @see IWiredNode#getNetwork()
 */
public interface IWiredNetwork
{
    /**
     * Create a connection between two nodes.
     *
     * This should only be used on the server thread.
     *
     * @param left  The first node to connect
     * @param right The second node to connect
     * @return {@code true} if a connection was created or {@code false} if the connection already exists.
     * @throws IllegalStateException    If neither node is on the network.
     * @throws IllegalArgumentException If {@code left} and {@code right} are equal.
     * @see IWiredNode#connectTo(IWiredNode)
     * @see IWiredNetwork#connect(IWiredNode, IWiredNode)
     */
    boolean connect(@Nonnull IWiredNode left, @Nonnull IWiredNode right);

    /**
     * Destroy a connection between this node and another.
     *
     * This should only be used on the server thread.
     *
     * @param left  The first node in the connection.
     * @param right The second node in the connection.
     * @return {@code true} if a connection was destroyed or {@code false} if no connection exists.
     * @throws IllegalArgumentException If either node is not on the network.
     * @throws IllegalArgumentException If {@code left} and {@code right} are equal.
     * @see IWiredNode#disconnectFrom(IWiredNode)
     * @see IWiredNetwork#connect(IWiredNode, IWiredNode)
     */
    boolean disconnect(@Nonnull IWiredNode left, @Nonnull IWiredNode right);

    /**
     * Sever all connections this node has, removing it from this network.
     *
     * This should only be used on the server thread.
     *
     * @param node The node to remove
     * @return Whether this node was removed from the network. One cannot remove a node from a network where it is the
     * only element.
     * @throws IllegalArgumentException If the node is not in the network.
     * @see IWiredNode#remove()
     */
    boolean remove(@Nonnull IWiredNode node);

    /**
     * Mark this node's peripherals as having changed.
     *
     * This should only be used on the server thread.
     *
     * @param node The node to mark as invalid.
     * @throws IllegalArgumentException If the node is not in the network.
     * @see IWiredElement#getPeripherals()
     */
    void invalidate(@Nonnull IWiredNode node);
}
