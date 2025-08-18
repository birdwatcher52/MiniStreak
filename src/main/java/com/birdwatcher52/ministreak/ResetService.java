package com.birdwatcher52.ministreak;

import java.time.LocalDate;
import java.time.ZoneOffset;

// 🔧 Stateless-ish service that enforces UTC day roll + freeze
final class ResetService
{
    // Call this on every GameTick (O(1) work)
    public void handleDailyRoll(StreakState state)
    {
        final LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        // If we’ve never seen a date, initialize and bail
        if (state.getLastSeenDateUTC() == null || state.getLastSeenDateUTC().isEmpty())
        {
            state.setLastSeenDateUTC(todayUtc.toString());
            return;
        }

        // No roll if we’re still on the same UTC date
        if (todayUtc.toString().equals(state.getLastSeenDateUTC()))
        {
            return;
        }

        // Day rolled: evaluate freeze vs reset using lastDoneDateUTC
        final LocalDate lastSeen = LocalDate.parse(state.getLastSeenDateUTC());
        final LocalDate yesterday = todayUtc.minusDays(1);

        final String lastDoneStr = state.getLastDoneDateUTC();
        final LocalDate lastDone = (lastDoneStr == null || lastDoneStr.isEmpty()) ? null : LocalDate.parse(lastDoneStr);

        boolean missedExactlyOneDay = (lastDone != null) && lastDone.isEqual(yesterday) == false && lastDone.isEqual(todayUtc) == false && lastDone.isEqual(lastSeen);
        // Simpler / kinder rule:
        // - If you didn’t complete yesterday (i.e., lastDone != yesterday), you missed a day.

        boolean usedFreeze = false;
        if (lastDone == null || !yesterday.equals(lastDone))
        {
            // Missed yesterday → spend Freeze if available, else reset streak
            if (state.getFreezeCount() > 0)
            {
                state.setFreezeCount(state.getFreezeCount() - 1);
                usedFreeze = true;
            }
            else
            {
                state.resetCurrentStreak();
            }
        }

        // Update last-seen day AFTER applying logic
        state.setLastSeenDateUTC(todayUtc.toString());

        // (Optional) you can surface a tiny debug flag if you want to show this in overlay later
        state.setLastRollUsedFreeze(usedFreeze);
    }
}
