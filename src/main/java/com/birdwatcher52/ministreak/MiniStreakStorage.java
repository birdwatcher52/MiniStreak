package com.birdwatcher52.ministreak;

import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

final class MiniStreakStorage
{
    private static final String GROUP = "ministreak";

    @Inject
    private ConfigManager configManager;

    void save(StreakState s)
    {
        setInt("currentStreak", s.getCurrentStreak());
        setInt("bestStreak", s.getBestStreak());
        setStr("lastSeenDateUTC", s.getLastSeenDateUTC());
        setStr("lastBirdhouseDateUTC", s.getLastBirdhouseDateUTC());
        setStr("lastHerbDateUTC", s.getLastHerbDateUTC());
        setStr("lastCompletionDateUTC", s.getLastCompletionDateUTC());
        // NEW
        setStr("lastAnnouncementDateUTC", s.getLastAnnouncementDateUTC());
    }

    void loadInto(StreakState s)
    {
        s.setCurrentStreak(getInt("currentStreak", 0));
        s.setBestStreak(getInt("bestStreak", 0));
        s.setLastSeenDateUTC(getStr("lastSeenDateUTC", ""));
        s.setLastBirdhouseDateUTC(getStr("lastBirdhouseDateUTC", ""));
        s.setLastHerbDateUTC(getStr("lastHerbDateUTC", ""));
        s.setLastCompletionDateUTC(getStr("lastCompletionDateUTC", ""));
        // NEW (back-compat default)
        s.setLastAnnouncementDateUTC(getStr("lastAnnouncementDateUTC", ""));
    }

    private void setInt(String key, int v) { configManager.setConfiguration(GROUP, key, v); }
    private int getInt(String key, int def)
    {
        Integer v = configManager.getConfiguration(GROUP, key, Integer.class);
        return v != null ? v : def;
    }

    private void setStr(String key, String v) { configManager.setConfiguration(GROUP, key, v); }
    private String getStr(String key, String def)
    {
        String v = configManager.getConfiguration(GROUP, key);
        return v != null ? v : def;
    }
}
