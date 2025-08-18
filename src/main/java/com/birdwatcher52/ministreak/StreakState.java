package com.birdwatcher52.ministreak;

import java.time.LocalDate;
import java.time.ZoneOffset;

// 📦 Single source of truth for streak numbers + dates
public class StreakState
{
    private int currentStreak = 0;
    private int bestStreak = 0;
    private int lifetimeXp = 0;
    private int freezeCount = 1;

    private String lastDoneDateUTC = ""; // YYYY-MM-DD (UTC)
    private String lastSeenDateUTC = ""; // tracks day rolls
    private boolean lastRollUsedFreeze = false; // transient UX hint

    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public int getLifetimeXp() { return lifetimeXp; }
    public int getFreezeCount() { return freezeCount; }
    public String getLastDoneDateUTC() { return lastDoneDateUTC; }
    public String getLastSeenDateUTC() { return lastSeenDateUTC; }
    public boolean getLastRollUsedFreeze() { return lastRollUsedFreeze; }

    public void setFreezeCount(int v) { this.freezeCount = Math.max(0, v); }
    public void setLastDoneDateUTC(String v) { this.lastDoneDateUTC = v != null ? v : ""; }
    public void setLastSeenDateUTC(String v) { this.lastSeenDateUTC = v != null ? v : ""; }
    public void setLastRollUsedFreeze(boolean v) { this.lastRollUsedFreeze = v; }

    public void incrementStreakAndMarkToday()
    {
        currentStreak++;
        if (currentStreak > bestStreak) bestStreak = currentStreak;
        lifetimeXp += 100; // MVP: +100 per mini
        lastDoneDateUTC = LocalDate.now(ZoneOffset.UTC).toString();
    }

    public void resetCurrentStreak()
    {
        currentStreak = 0;
    }

    // setters for storage hydrate
    public void setCurrentStreak(int v) { currentStreak = Math.max(0, v); }
    public void setBestStreak(int v) { bestStreak = Math.max(0, v); }
    public void setLifetimeXp(int v) { lifetimeXp = Math.max(0, v); }
}
