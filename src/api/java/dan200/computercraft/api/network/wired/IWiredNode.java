package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.network.IPacketNetwork;

import javax.annotation.Nonnull;

/**
 * Wired nodes act as a layer between {@link IWiredElement}s and {@link IWiredNetwork}s.
 *
 * Firstly, a node acts as a packet network, capable of sending and receiving modem messages to connected nodes. These
 * methods may be safely used on any thread.
 *
 * When sending a packet, the system will attempt to find the shortest path between the two nodes based on their
 * element's position. Note that packet senders and receivers can have different locations from their associated
 * element: the distance between the two will be added to the total packet's distance.
 *
 * Wired nodes also provide several convenience methods for interacting with a wired network. These should only ever
 * be used on the main server thread.
 */
public interface IWiredNode extends IPacketNetwork
{
    /**
     * The associated element for this network node.
     *
     * @return This node's element.
     */
    @Nonnull
    IWiredElement getElement();

    /**
     * The network this node is currently connected to. Note that this may change
     * after any network operation, so it should not be cached.
     *
     * This should only be used on the server thread.
     *
     * @return This node's network.
     */
    @Nonnull
    IWiredNetwork getNetwork();

    /**
     * Create a connection from this node to another.
     *
     * This should only be used on the server thread.
     *
     * @param node The other node to connect to.
     * @return {@code true} if a connection was created or {@code false} if the connection already exists.
     * @see IWiredNetwork#connect(IWiredNode, IWiredNode)
     * @see IWiredNode#disconnectFrom(IWiredNode)
     */
    default boolean connectTo(@Nonnull IWiredNode node)
    {
        return getNetwork().connect( this, node );
    }

    /**
     * Destroy a connection between this node and another.
     *
     * This should only be used on the server thread.
     *
     * @param node The other node to disconnect from.
     * @return {@code true} if a connection was destroyed or {@code false} if no connection exists.
     * @throws IllegalArgumentException If {@code node} is not on the same network.
     * @see IWiredNetwork#disconnect(IWiredNode, IWiredNode)
     * @see IWiredNode#connectTo(IWiredNode)
     */
    default boolean disconnectFrom(@Nonnull IWiredNode node)
    {
        return getNetwork().disconnect( this, node );
    }

    /**
     * Sever all connections this node has, removing it from this network.
     *
     * This should only be used on the server thread.
     *
     * @return Whether this node was removed from the network. One cannot remove a node from a network where it is the
     * only element.
     * @throws IllegalArgumentException If the node is not in the network.
     * @see IWiredNetwork#remove(IWiredNode)
     */
    default boolean remove()
    {
        return getNetwork().remove( this );
    }

    /**
     * Mark this node's peripherals as having changed.
     *
     * This should only be used on the server thread.
     *
     * @see IWiredElement#getPeripherals()
     */
    default void invalidate()
    {
        getNetwork().invalidate( this );
    }
}
