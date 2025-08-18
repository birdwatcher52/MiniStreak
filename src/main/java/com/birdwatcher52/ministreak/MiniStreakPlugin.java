package com.birdwatcher52.ministreak;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "MiniStreak",
        description = "Keep your daily MiniStreak alive with one mini-task per day",
        tags = {"streak", "daily", "qol"}
)
public class MiniStreakPlugin extends Plugin
{
    @Inject private OverlayManager overlayManager;
    @Inject private MiniStreakConfig config;
    @Inject private ConfigManager configManager;

    private final ResetService resetService = new ResetService();
    @Inject private MiniStreakStorage storage;

    private StreakState state;
    private MiniStreakOverlay overlay;

    @Override
    protected void startUp()
    {
        state = new StreakState();
        storage.loadInto(state); // hydrate from disk

        overlay = new MiniStreakOverlay(state);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        storage.save(state); // persist on shutdown
        overlay = null;
        state = null;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // O(1) daily check — cheap, safe to run every tick
        resetService.handleDailyRoll(state);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted cmd)
    {
        final String c = cmd.getCommand();

        // Dev: increment streak counter quickly
        if ("teststreak".equalsIgnoreCase(c))
        {
            state.setCurrentStreak(state.getCurrentStreak() + 1);
            if (state.getCurrentStreak() > state.getBestStreak())
            {
                state.setBestStreak(state.getCurrentStreak());
            }
            storage.save(state);
        }

        // Dev: simulate a real mini completion (marks today’s UTC date)
        if ("markmini".equalsIgnoreCase(c))
        {
            state.incrementStreakAndMarkToday();
            storage.save(state);
        }
    }

    @Provides
    MiniStreakConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(MiniStreakConfig.class);
    }
}
