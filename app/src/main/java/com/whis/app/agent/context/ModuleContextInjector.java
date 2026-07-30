package com.whis.app.agent.context;

import android.content.Context;

import com.whis.app.core.WhisFlags;

import java.util.ArrayList;
import java.util.List;

/**
 * Module context injector using shared {@link WhisFlags} (AI_AGENT_PLAN.md Section 4.2 & Adjustment #4).
 */
public class ModuleContextInjector {

    private ModuleContextInjector() {
        // Utility class
    }

    /**
     * Retrieve recent flags from SMS and Call modules via shared {@link WhisFlags}.
     *
     * @param context      android context
     * @param maxAgeHours  maximum flag age in hours
     * @return list of {@link WhisFlags.FlagEntry}
     */
    public static List<WhisFlags.FlagEntry> getRecentFlags(Context context, int maxAgeHours) {
        if (context == null) return new ArrayList<>();

        List<WhisFlags.FlagEntry> allFlags = WhisFlags.getFlags(context);
        List<WhisFlags.FlagEntry> recent = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - (maxAgeHours * 3600 * 1000L);

        for (WhisFlags.FlagEntry flag : allFlags) {
            if (flag.getTimestamp() >= cutoff) {
                recent.add(flag);
            }
        }

        return recent;
    }
}
