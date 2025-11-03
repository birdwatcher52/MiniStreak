package com.birdwatcher52.ministreak;

import java.time.LocalDate;
import java.time.ZoneOffset;

public class StreakState
{
    private int currentStreak = 0;
    private int bestStreak = 0;

    // UTC date bookkeeping (YYYY-MM-DD)
    private String lastSeenDateUTC = "";         // last date we observed (for day roll)
    private String lastBirdhouseDateUTC = "";    // last date birdhouse was set up
    private String lastHerbDateUTC = "";         // last date herb was planted
    private String lastCompletionDateUTC = "";   // prevents double-counting once both minis are done

    // NEW: once-per-day Notifier de-dupe
    private String lastAnnouncementDateUTC = "";

    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }

    public String getLastSeenDateUTC() { return lastSeenDateUTC; }
    public String getLastBirdhouseDateUTC() { return lastBirdhouseDateUTC; }
    public String getLastHerbDateUTC() { return lastHerbDateUTC; }
    public String getLastCompletionDateUTC() { return lastCompletionDateUTC; }
    public String getLastAnnouncementDateUTC() { return lastAnnouncementDateUTC; }

    public void setCurrentStreak(int v) { currentStreak = Math.max(0, v); }
    public void setBestStreak(int v) { bestStreak = Math.max(bestStreak, Math.max(0, v)); } // monotonic best
    public void setLastSeenDateUTC(String v) { lastSeenDateUTC = v != null ? v : ""; }
    public void setLastBirdhouseDateUTC(String v) { lastBirdhouseDateUTC = v != null ? v : ""; }
    public void setLastHerbDateUTC(String v) { lastHerbDateUTC = v != null ? v : ""; }
    public void setLastCompletionDateUTC(String v) { lastCompletionDateUTC = v != null ? v : ""; }
    public void setLastAnnouncementDateUTC(String v) { lastAnnouncementDateUTC = v != null ? v : ""; }

    public void resetCurrentStreak() { currentStreak = 0; }

    public void markBirdhouseTodayUTC()
    {
        lastBirdhouseDateUTC = LocalDate.now(ZoneOffset.UTC).toString();
    }

    public void markHerbTodayUTC()
    {
        lastHerbDateUTC = LocalDate.now(ZoneOffset.UTC).toString();
    }

    public boolean bothDoneTodayUTC()
    {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        return today.equals(lastBirdhouseDateUTC) && today.equals(lastHerbDateUTC);
    }

    public boolean bothDoneOn(String yyyymmdd)
    {
        return yyyymmdd.equals(lastBirdhouseDateUTC) && yyyymmdd.equals(lastHerbDateUTC);
    }

    public boolean birdhouseDoneTodayUTC()
    {
        final String today = LocalDate.now(ZoneOffset.UTC).toString();
        return today.equals(lastBirdhouseDateUTC);
    }

    public boolean herbDoneTodayUTC()
    {
        final String today = LocalDate.now(ZoneOffset.UTC).toString();
        return today.equals(lastHerbDateUTC);
    }
}
