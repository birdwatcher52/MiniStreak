package com.birdwatcher52.ministreak;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

class MiniStreakOverlay extends OverlayPanel
{
    private final StreakState state;

    MiniStreakOverlay(StreakState state)
    {
        this.state = state;
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text("MiniStreak")
                        .color(Color.ORANGE)
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Streak")
                        .right(String.valueOf(state.getCurrentStreak()))
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Best")
                        .right(String.valueOf(state.getBestStreak()))
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("XP")
                        .right(String.valueOf(state.getLifetimeXp()))
                        .build()
        );

        if (state.getLastRollUsedFreeze())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Freeze used")
                            .right("✔")
                            .build()
            );
        }

        return super.render(g);
    }
}
