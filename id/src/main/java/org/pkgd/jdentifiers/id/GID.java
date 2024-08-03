package org.pkgd.jdentifiers.id;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Globally unique, 128-bit identifier.
 * <p>
 * Implements UUIDv7 draft.
 */
public class GID<T extends IDAble> implements Comparable<GID<T>> {

    private final UUID uuid;

    private GID(UUID uuid) {
        this.uuid = uuid;
    }

    public static <R extends IDAble> GID<R> fromString(String gidStr) {
        return new GID<>(UUID.fromString(gidStr));
    }

    public static <R extends IDAble> GID<R> fromUuid(UUID uuid) {
        return new GID<>(uuid);
    }


    public static <T extends IDAble> List<GID<T>> fromUUIDs(Iterable<UUID> uuids) {
        Objects.requireNonNull(uuids);

        final List<GID<T>> ids = new ArrayList<>();
        for (final UUID uuid : uuids) {
            ids.add(GID.fromUuid(uuid));
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public static <I extends IDAble> GID<I> cast(GID<? extends IDAble> id) {
        return (GID<I>) id;
    }

    public static <T extends IDAble> GID<T> fromHexString(String uuidStr) {
        return new GID<>(UUID.fromString(uuidStr));
    }

    public UUID asUUID() {
        return uuid;
    }

    @Override
    public int compareTo(GID<T> o) {
        return 0;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return uuid.toString();
    }

    public String toHexString() {
        return toString();
    }
}
