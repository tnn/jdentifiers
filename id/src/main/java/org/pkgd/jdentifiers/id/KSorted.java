package org.pkgd.jdentifiers.id;

public interface KSorted {
    /**
     * A local identifier with a long lifespan.
     * <p>
     * E.g. when the wrap-around time should be 100 years
     */
    <T extends IDAble> LID<T> permanentLocalIdentifier();


    /**
     * Short-lived Local Identifier.
     * <p>
     * E.g. when the wrap-around time is a day.
     */
    <T extends IDAble> LID<T> temporaryLocalIdentifier();
}
