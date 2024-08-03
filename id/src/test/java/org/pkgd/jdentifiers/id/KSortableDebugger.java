package org.pkgd.jdentifiers.id;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.pkgd.jdentifiers.id.KSortableIDGenerator.customEpoc;

public class KSortableDebugger {
    public static String asStrong(ID<IDAble> id) {
        final var sb = new StringBuilder();
        sb.append("Debugging identifier ");
        sb.append(id.toString());
        sb.append('\n');

        // Milli second precision
        // 2^41 = rollover in 2089
        // 2020 ->
        long timeInMs = id.asLong() >>> 24;
        long timeEpoch = customEpoc + timeInMs;
        sb.append("Time component (bits 1-40): ");
        sb.append(ID.fromLong(timeInMs));
        sb.append("(hex), ");
        sb.append(timeEpoch);
        sb.append(" (decimal), ");
        sb.append(DateTimeFormatter.ISO_DATE_TIME.format(Instant.ofEpochMilli(timeEpoch).atZone(ZoneId.of("UTC"))));
        sb.append('\n');

        // Configured machine id / process id
        // 10 bits, 1024 machines
        sb.append("Node component (bits 41-51): ");
        var nodeId = id.asLong() >> 24;
        sb.append(nodeId);
        sb.append('\n');

        // 2^12 = 4k per millisecond
        sb.append("Counter component (bits 52-63): ");
        var counter = id.asLong() >> 12;
        sb.append(counter);
        sb.append('\n');

        return sb.toString();
    }

    public static String asString(LID<IDAble> id) {

        // Time component
        // 2^19, 4.194,304 second precision (~1h), between tick, rollover in 2089
        // 13 bits = 8192 unique per tick ?!
        // Maybe generate scoped id, instead of generate(scopeId)
        // Maybe omit the node / machine component and just go with the counter
        // ***Maybe just random?***

        return "";
    }

    // GID
    // first 48-bit: time, millisecond unix epoch
    // rest: random

}
