package com.whis.app.ui.theme;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for GlassCapability.
 */
public class GlassCapabilityTest {

    @Test
    public void testGlassCapabilityCheckDoesNotCrash() {
        // Confirm canUseRealBlur() runs without throwing on JVM / test environment
        boolean canBlur = GlassCapability.canUseRealBlur();
        assertNotNull(canBlur);
    }
}
