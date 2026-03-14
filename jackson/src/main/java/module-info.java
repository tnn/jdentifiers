module dk.ceti.jdentifiers.jackson {
    requires transitive dk.ceti.jdentifiers.id;
    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;

    exports dk.ceti.jdentifiers.jackson;
}
