package dk.ceti.jdentifiers.id;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Globally unique, 128-bit identifier.
 * <p>
 * Wraps a {@link UUID} with a phantom type parameter for compile-time type safety.
 * Supports any UUID variant (v4, v7, etc.).
 *
 * @param <T> phantom type for compile-time type safety
 */
public class GID<T extends IDAble> implements Comparable<GID<?>>, Serializable {

    @Serial
    private static final long serialVersionUID = 4886811489207381608L;

    private final UUID uuid;

    private GID(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
    }

    /**
     * Creates a GID from a UUID string representation.
     * <p>
     * Accepts {@link CharSequence} for API consistency with {@link ID#fromString}
     * and {@link LID#fromString}. Note: internally calls {@code toString()} on the
     * input because {@link UUID#fromString} requires a {@link String}.
     *
     * @param <R> the entity type
     * @param gidStr UUID string representation
     * @return the parsed GID
     * @throws IllegalArgumentException if the string is not a valid UUID
     * @throws NullPointerException if gidStr is null
     */
    public static <R extends IDAble> GID<R> fromString(CharSequence gidStr) {
        Objects.requireNonNull(gidStr, "gidStr must not be null");
        return new GID<>(UUID.fromString(gidStr.toString()));
    }

    /**
     * Wraps the given UUID.
     *
     * @param <R> the entity type
     * @param uuid the UUID value
     * @return a new GID
     */
    public static <R extends IDAble> GID<R> fromUuid(UUID uuid) {
        return new GID<>(uuid);
    }

    /**
     * Converts UUIDs to an unmodifiable list of GIDs.
     *
     * @param <T> the entity type
     * @param uuids the UUID values
     * @return unmodifiable list
     */
    public static <T extends IDAble> List<GID<T>> fromUUIDs(Iterable<UUID> uuids) {
        Objects.requireNonNull(uuids);

        final List<GID<T>> ids;
        if (uuids instanceof Collection<UUID> c) {
            ids = new ArrayList<>(c.size());
        } else {
            ids = new ArrayList<>();
        }
        for (final UUID uuid : uuids) {
            Objects.requireNonNull(uuid, "uuid in collection must not be null");
            ids.add(GID.fromUuid(uuid));
        }
        return Collections.unmodifiableList(ids);
    }

    /**
     * Re-types a GID. Safe because the phantom type is erased at runtime.
     *
     * @param <I> the target entity type
     * @param id the GID to re-type
     * @return the same instance, re-typed
     */
    @SuppressWarnings("unchecked")
    public static <I extends IDAble> GID<I> cast(GID<? extends IDAble> id) {
        return (GID<I>) id;
    }

    /**
     * Returns the underlying {@link UUID}.
     *
     * @return the UUID value
     */
    public UUID asUUID() {
        return uuid;
    }

    /**
     * Compares GIDs using unsigned ordering of the underlying UUID bits.
     * <p>
     * Note: this differs from {@link UUID#compareTo} on JDK versions before 20,
     * where UUID uses signed comparison. This implementation always uses unsigned
     * comparison, matching the corrected behavior in JDK 20+
     * (<a href="https://bugs.openjdk.org/browse/JDK-7025832">JDK-7025832</a>).
     */
    @Override
    public int compareTo(GID<?> o) {
        int msb = Long.compareUnsigned(uuid.getMostSignificantBits(), o.uuid.getMostSignificantBits());
        return msb != 0 ? msb : Long.compareUnsigned(uuid.getLeastSignificantBits(), o.uuid.getLeastSignificantBits());
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * Compares based on the underlying {@link UUID} value only.
     * The phantom type parameter {@code T} is erased at runtime and is not considered.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final GID<? extends IDAble> other = (GID<? extends IDAble>) obj;
        return uuid.equals(other.uuid);
    }

    @Override
    public String toString() {
        return uuid.toString();
    }

}
