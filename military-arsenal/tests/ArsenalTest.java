import org.junit.jupiter.api.Test;

public final class ArsenalTest {
    @Test void catalogAndGeometry() { ArsenalCoreChecks.catalog(); }
    @Test void magazineAndAmmoConservation() { ArsenalCoreChecks.reload(); }
    @Test void cadenceAndPacketSpam() { ArsenalCoreChecks.trigger(); }
    @Test void animationAndFalloff() { ArsenalCoreChecks.animation(); }
}
