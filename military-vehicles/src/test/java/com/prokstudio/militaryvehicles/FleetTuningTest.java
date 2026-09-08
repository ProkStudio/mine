package com.prokstudio.militaryvehicles;
import org.junit.jupiter.api.Test;
class FleetTuningTest {
    @Test void defaultsPreserveLegacyEconomy() throws Exception {FleetTuningCases.defaultsPreserveLegacyEconomy();}
    @Test void partialFilesInheritDocumentedDefaults() throws Exception {FleetTuningCases.partialFilesInheritDocumentedDefaults();}
    @Test void rejectsUnknownAndDuplicateKeys() throws Exception {FleetTuningCases.rejectsUnknownAndDuplicateKeys();}
    @Test void rejectsUnsupportedSchemaAndMalformedValues() throws Exception {FleetTuningCases.rejectsUnsupportedSchemaAndMalformedValues();}
    @Test void integerBoundsCannotBeBypassed() throws Exception {FleetTuningCases.integerBoundsCannotBeBypassed();}
    @Test void canonicalRoundTripIsDeterministic() throws Exception {FleetTuningCases.canonicalRoundTripIsDeterministic();}
    @Test void createsMissingFileWithoutOverwritingExistingEdits() throws Exception {FleetTuningCases.createsMissingFileWithoutOverwritingExistingEdits();}
    @Test void invalidReloadKeepsLastGoodSnapshot() throws Exception {FleetTuningCases.invalidReloadKeepsLastGoodSnapshot();}
    @Test void boundsBytesAndRejectsMalformedUtf8() throws Exception {FleetTuningCases.boundsBytesAndRejectsMalformedUtf8();}
    @Test void separateStoresDoNotLeakServerState() throws Exception {FleetTuningCases.separateStoresDoNotLeakServerState();}
    @Test void configuredTransfersConserveFuelAndReserve() throws Exception {FleetTuningCases.configuredTransfersConserveFuelAndReserve();}
    @Test void configuredRepairsRequireKitsAndRespectCapacity() throws Exception {FleetTuningCases.configuredRepairsRequireKitsAndRespectCapacity();}
    @Test void weaponDisableAndReloadLimitsApply() throws Exception {FleetTuningCases.weaponDisableAndReloadLimitsApply();}
    @Test void supportDisableStopsAccounting() throws Exception {FleetTuningCases.supportDisableStopsAccounting();}
    @Test void engineDebitCannotUnderflow() throws Exception {FleetTuningCases.engineDebitCannotUnderflow();}
    @Test void restoringCannotShortenConfiguredCooldown() throws Exception {FleetTuningCases.restoringCannotShortenConfiguredCooldown();}
    @Test void firstPhysicalHitBlocksTargetsBehindIt() throws Exception {FleetTuningCases.firstPhysicalHitBlocksTargetsBehindIt();}
    @Test void raySelectionIsOrderIndependentAndFinite() throws Exception {FleetTuningCases.raySelectionIsOrderIndependentAndFinite();}
    @Test void serviceTargetsMustBeGroundedDryAndStationary() throws Exception {FleetTuningCases.serviceTargetsMustBeGroundedDryAndStationary();}
}
