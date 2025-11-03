package com.birdwatcher52.ministreak;

import java.time.LocalDate;
import java.time.ZoneOffset;

/** Hardcore: no shields. If yesterday wasn't fully completed, reset the streak. */
final class ResetService
{
    void handleDailyRoll(StreakState s)
    {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String todayStr = today.toString();

        String lastSeen = s.getLastSeenDateUTC();
        if (lastSeen == null || lastSeen.isEmpty())
        {
            s.setLastSeenDateUTC(todayStr);
            return;
        }

        if (todayStr.equals(lastSeen))
        {
            return; // same UTC day
        }

        // new UTC day → check if yesterday had both minis
        LocalDate yesterday = today.minusDays(1);
        boolean yesterdayComplete = s.bothDoneOn(yesterday.toString());

        if (!yesterdayComplete)
        {
            s.resetCurrentStreak();
        }

        s.setLastSeenDateUTC(todayStr);
    }
}
