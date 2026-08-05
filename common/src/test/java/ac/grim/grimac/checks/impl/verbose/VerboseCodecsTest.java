package ac.grim.grimac.checks.impl.verbose;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerboseCodecsTest {

    @Test
    void unsupportedEntityUsesUnknownSentinel() {
        assertEquals(VerboseCodecs.ENTITY_UNKNOWN,
                VerboseCodecs.entityIdOrUnknown(-1));
    }

    @Test
    void supportedEntityKeepsProtocolId() {
        assertEquals(42, VerboseCodecs.entityIdOrUnknown(42));
    }
}
