package com.birdwatcher52.ministreak;

import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

// 💾 Save/load a few primitive fields via ConfigManager (profile-scoped)
final class MiniStreakStorage
{
    private static final String GROUP = "ministreak";

    @Inject
    private ConfigManager configManager;

    public void save(StreakState s)
    {
        setInt("currentStreak", s.getCurrentStreak());
        setInt("bestStreak", s.getBestStreak());
        setInt("lifetimeXp", s.getLifetimeXp());
        setInt("freezeCount", s.getFreezeCount());
        setStr("lastDoneDateUTC", s.getLastDoneDateUTC());
        setStr("lastSeenDateUTC", s.getLastSeenDateUTC());
    }

    public void loadInto(StreakState s)
    {
        s.setCurrentStreak(getInt("currentStreak", 0));
        s.setBestStreak(getInt("bestStreak", 0));
        s.setLifetimeXp(getInt("lifetimeXp", 0));
        s.setFreezeCount(getInt("freezeCount", 1)); // kindness default
        s.setLastDoneDateUTC(getStr("lastDoneDateUTC", ""));
        s.setLastSeenDateUTC(getStr("lastSeenDateUTC", ""));
    }

    private void setInt(String key, int v) { configManager.setConfiguration(GROUP, key, v); }
    private int getInt(String key, int def) {
        Integer v = configManager.getConfiguration(GROUP, key, Integer.class);
        return v != null ? v : def;
    }

    private void setStr(String key, String v) { configManager.setConfiguration(GROUP, key, v); }
    private String getStr(String key, String def) {
        String v = configManager.getConfiguration(GROUP, key);
        return (v != null) ? v : def;
    }
}
