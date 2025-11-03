package com.birdwatcher52.ministreak;

import java.time.LocalDate;
import java.time.ZoneOffset;

final class DailyCompletionService
{
    void onBirdhouseMarked(StreakState s)
    {
        s.markBirdhouseTodayUTC();
        maybeFinishToday(s);
    }

    void onHerbMarked(StreakState s)
    {
        s.markHerbTodayUTC();
        maybeFinishToday(s);
    }

    /** Increment the streak at most once per UTC day when both minis are done. */
    private void maybeFinishToday(StreakState s)
    {
        if (!s.bothDoneTodayUTC())
        {
            return; // not both completed yet
        }

        final String today = LocalDate.now(ZoneOffset.UTC).toString();
        if (today.equals(s.getLastCompletionDateUTC()))
        {
            return; // already counted today
        }

        s.setCurrentStreak(s.getCurrentStreak() + 1);
        s.setBestStreak(s.getCurrentStreak());
        s.setLastCompletionDateUTC(today); // idempotence guard
    }
}
