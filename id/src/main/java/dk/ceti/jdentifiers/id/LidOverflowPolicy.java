package dk.ceti.jdentifiers.id;

/**
 * Controls what happens when the 12-bit LID counter is exhausted within a single hour.
 *
 * @see KSortableIDGenerator.Builder#lidOverflowPolicy(LidOverflowPolicy)
 */
public enum LidOverflowPolicy {

    /**
     * Throw {@link IllegalStateException} when the counter exceeds 4,095.
     * Use this when there is no database-level uniqueness constraint to catch duplicates.
     */
    THROW,

    /**
     * Wrap the counter back to zero, accepting that duplicate LID values may be
     * produced within the same hour. This is safe when the LID is part of a composite
     * primary key whose uniqueness is enforced by the database.
     */
    WRAP
}
