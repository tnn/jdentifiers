package dk.ceti.jdentifiers.id;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Globally unique, 128-bit identifier.
 *
 * <p>Wraps a {@link UUID} with a phantom type parameter for compile-time type safety.
 * Supports any UUID variant (v4, v7, etc.).
 *
 * @param <T> phantom type for compile-time type safety
 */
public class GID<T extends IDAble> implements Comparable<GID<?>>, Serializable {

    @Serial
    private static final long serialVersionUID = 4886811489207381608L;

    private static final int GID_STRING_LENGTH = 36;
    private static final int GID_HEX_LENGTH = 32;

    private final UUID uuid;

    private GID(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
    }

    /**
     * Creates a GID from a UUID string representation.
     *
     * <p>Behaves exactly like {@link UUID#fromString}: it accepts the same
     * inputs, including non-canonical forms such as {@code "1-1-1-1-1"} (short
     * groups) and a leading {@code '+'} sign on a group (the "sign hole", e.g.
     * {@code "+e83dd89-..."} is coerced to {@code "0e83dd89-..."}). This keeps
     * the method free of surprises for callers who expect {@code UUID.fromString}
     * semantics. For canonical-only validation, use {@link #parseStrict}.
     *
     * @param <R>    the entity type
     * @param gidStr UUID string representation
     * @return the parsed GID
     * @throws IllegalArgumentException if the string is not a valid UUID
     * @throws NullPointerException     if gidStr is null
     */
    public static <R extends IDAble> GID<R> fromString(CharSequence gidStr) {
        Objects.requireNonNull(gidStr, "gidStr must not be null");
        return new GID<>(UUID.fromString(gidStr.toString()));
    }

    private static boolean isSign(char c) {
        return c == '+' || c == '-';
    }

    /**
     * Parses a UUID string strictly, accepting only the canonical 36-character
     * {@code 8-4-4-4-12} hex form (dashes at positions 8, 13, 18, 23),
     * case-insensitively, and returning empty otherwise. Never throws.
     *
     * <p>Unlike {@link #fromString} (which mirrors the lenient
     * {@link UUID#fromString}), this rejects the non-canonical forms
     * {@code UUID.fromString} would otherwise accept: short groups such as
     * {@code "1-1-1-1-1"}, oversized groups, and a leading {@code '+'}/{@code '-'}
     * sign on any group (the "sign hole" — {@code UUID.fromString} parses each
     * group as a <em>signed</em> hex number, so a sign at a group start, indices
     * 0/9/14/19/24, would be silently consumed rather than rejected).
     *
     * @param <T>    the entity type
     * @param gidStr UUID string, or null
     * @return the parsed GID, or empty if null or not canonical
     */
    public static <T extends IDAble> Optional<GID<T>> parseStrict(CharSequence gidStr) {
        if (gidStr == null || !isCanonicalUuid(gidStr)) {
            return Optional.empty();
        }
        try {
            return Optional.of(fromString(gidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean isCanonicalUuid(CharSequence s) {
        return s.length() == GID_STRING_LENGTH
            && s.charAt(8) == '-' && s.charAt(13) == '-'
            && s.charAt(18) == '-' && s.charAt(23) == '-'
            && !isSign(s.charAt(0)) && !isSign(s.charAt(9))
            && !isSign(s.charAt(14)) && !isSign(s.charAt(19)) && !isSign(s.charAt(24));
    }

    /**
     * Parses a UUID string in either representation, returning empty for null
     * or unrecognized input; never throws. Accepts both:
     * <ul>
     *   <li>the dashed forms that {@link #fromString} accepts (canonical UUID,
     *       and the lenient {@link UUID#fromString} variants); and</li>
     *   <li>the dashless 32-character hex form (16 bytes, no dashes) — e.g. a
     *       W3C {@code traceparent} trace-id.</li>
     * </ul>
     * The dashless form is matched first by its exact 32-char length.
     *
     * @param <T>    the entity type
     * @param gidStr UUID string (dashed or 32-char hex), or null
     * @return the parsed GID, or empty
     */
    public static <T extends IDAble> Optional<GID<T>> parseLenient(CharSequence gidStr) {
        if (gidStr == null) {
            return Optional.empty();
        }
        if (gidStr.length() == GID_HEX_LENGTH) {
            return parseHex(gidStr);
        }
        try {
            return Optional.of(fromString(gidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static <T extends IDAble> Optional<GID<T>> parseHex(CharSequence hex) {
        try {
            long msb = 0L;
            for (int i = 0; i < 16; i++) {
                msb = (msb << 4) | HexCodec.getHexValue(hex.charAt(i));
            }
            long lsb = 0L;
            for (int i = 16; i < GID_HEX_LENGTH; i++) {
                lsb = (lsb << 4) | HexCodec.getHexValue(hex.charAt(i));
            }
            return Optional.of(new GID<>(new UUID(msb, lsb)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Wraps the given UUID.
     *
     * @param <R>  the entity type
     * @param uuid the UUID value
     * @return a new GID
     */
    public static <R extends IDAble> GID<R> fromUuid(UUID uuid) {
        return new GID<>(uuid);
    }

    /**
     * Converts UUIDs to an unmodifiable list of GIDs.
     *
     * @param <T>   the entity type
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
     * @param id  the GID to re-type
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
     *
     * <p>Note: this differs from {@link UUID#compareTo} on JDK versions before 20,
     * where UUID uses signed comparison. This implementation always uses unsigned
     * comparison, matching the corrected behavior in JDK 20+
     * (<a href="https://bugs.openjdk.org/browse/JDK-7025832">JDK-7025832</a>).
     */
    @Override
    public int compareTo(GID<?> o) {
        int msb = Long.compareUnsigned(
            uuid.getMostSignificantBits(),
            o.uuid.getMostSignificantBits()
        );
        return msb != 0 ? msb : Long.compareUnsigned(
            uuid.getLeastSignificantBits(),
            o.uuid.getLeastSignificantBits()
        );
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

    /**
     * Returns the 128 bits as 32 lowercase hex characters without dashes, the form a
     * W3C {@code traceparent} trace-id uses. {@link #parseLenient} reads it back.
     *
     * @return the dashless hex form
     */
    public String toHexString() {
        final byte[] hexChars = new byte[GID_HEX_LENGTH];
        writeHex(uuid.getMostSignificantBits(), hexChars, 0);
        writeHex(uuid.getLeastSignificantBits(), hexChars, GID_HEX_LENGTH / 2);
        return new String(hexChars, StandardCharsets.ISO_8859_1);
    }

    private static void writeHex(long bits, byte[] hexChars, int offset) {
        for (int i = GID_HEX_LENGTH / 2 - 1; i >= 0; i--) {
            hexChars[offset + i] = HexCodec.HEX_DIGITS[(int) (bits & 0xF)];
            bits >>>= 4;
        }
    }

}
