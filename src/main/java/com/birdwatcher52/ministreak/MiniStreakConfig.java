package com.birdwatcher52.ministreak;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ministreak")
public interface MiniStreakConfig extends Config
{
    @ConfigItem(
            keyName = "showSidebar",
            name = "Show Sidebar",
            description = "Show the MiniStreak panel in the sidebar."
    )
    default boolean showSidebar() { return true; }

    @ConfigItem(
            keyName = "debugMode",
            name = "Debug Mode",
            description = "Show testing buttons in the panel and enable ::forceroll.",
            position = 1
    )
    default boolean debugMode() { return true; }

    @ConfigItem(
            keyName = "streakEmblem",
            name = "Streak Emblem",
            description = "Choose the streak emblem to display in chat",
            position = 2
    )
    default StreakEmblems streakEmblem() { return StreakEmblems.CAT_1; }

    // NEW: toggle native (iron/HC/UIM/league/etc.) badge visibility
    @ConfigItem(
            keyName = "showNativeIcon",
            name = "Show Native Icon",
            description = "Include your account’s native crown/badge after the MiniStreak emblem.",
            position = 3
    )
    default boolean showNativeIcon() { return true; }
}
