package dk.ceti.jdentifiers.id;

/**
 * Factory for creating new ID instances.
 */
public interface IDGenerator {

    /**
     * Generates a new 32-bit local identifier.
     *
     * @param <T> the entity type
     * @return a new LID
     */
    <T extends IDAble> LID<T> localIdentifier();

    /**
     * Generates a new 64-bit identifier.
     *
     * @param <T> the entity type
     * @return a new ID
     */
    <T extends IDAble> ID<T> identifier();

    /**
     * Generates a new 128-bit global identifier.
     *
     * @param <T> the entity type
     * @return a new GID
     */
    <T extends IDAble> GID<T> globalIdentifier();
}
