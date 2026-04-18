package dk.ceti.jdentifiers.id;

/**
 * Supplies a node ID and its bit width for {@link KSortableIDGenerator}.
 *
 * <p>Pairs the bit width with the resolution logic so they cannot diverge.
 * The builder validates both values at {@code build()} time.
 *
 * <p>Implementations are evaluated exactly once when the generator is
 * {@linkplain KSortableIDGenerator.Builder#build() built}. The returned
 * {@link #nodeId()} must fit within {@link #nodeBits()} bits, i.e. it must
 * be in {@code [0, 2^nodeBits)}.
 *
 * @see NodeIdStrategies
 */
public interface NodeIdSupplier {

    /**
     * Number of bits allocated to the node ID within the 22-bit ID payload.
     * The remaining {@code 22 - nodeBits} bits are used for the counter.
     *
     * @return node bits in [1, 21]
     */
    int nodeBits();

    /**
     * Resolves the node ID. Called exactly once during generator construction.
     *
     * @return node ID in [0, 2^nodeBits)
     * @throws IllegalStateException if the node ID cannot be resolved
     */
    int nodeId();
}
