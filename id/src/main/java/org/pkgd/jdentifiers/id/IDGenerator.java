package org.pkgd.jdentifiers.id;

/**
 * Factory for creating new ID instances.
 */
public interface IDGenerator {

    <T extends IDAble> LID<T> localIdentifier();

    <T extends IDAble> ID<T> identifier();

    <T extends IDAble> GID<T> globalIdentifier();
}
