package com.birdwatcher52.ministreak;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ministreak")
public interface MiniStreakConfig extends Config
{
    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Toggle whether the MiniStreak overlay is visible."
    )
    default boolean showOverlay()
    {
        return true;
    }
}
