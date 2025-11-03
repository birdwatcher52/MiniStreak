package com.birdwatcher52.ministreak;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

final class MiniStreakPanel extends PluginPanel
{
    private final JLabel title = new JLabel("MiniStreak");
    private final JLabel streak = new JLabel("Streak: 0");
    private final JLabel best = new JLabel("Best: 0");
    private final JLabel today = new JLabel("Today: BIRD–  HERB–");
    private final JLabel nextReset = new JLabel("Next UTC reset: —");

    // Debug buttons (shown only if config.debugMode)
    private final JButton btnMarkBird = new JButton("Mark Birdhouse");
    private final JButton btnMarkHerb = new JButton("Mark Herb");
    private final JButton btnForceRoll = new JButton("Force UTC Roll");

    MiniStreakPanel()
    {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        streak.setFont(streak.getFont().deriveFont(14f));
        best.setFont(best.getFont().deriveFont(12f));
        today.setFont(today.getFont().deriveFont(12f));
        nextReset.setFont(nextReset.getFont().deriveFont(11f));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.weightx = 1; c.insets = new Insets(0,0,6,0);
        add(title, c);

        c.gridy++; add(streak, c);
        c.gridy++; add(best, c);
        c.gridy++; add(today, c);
        c.gridy++; c.insets = new Insets(6,0,10,0); add(nextReset, c);

        // Debug row (hidden by default; plugin will toggle visibility)
        JPanel debugRow = new JPanel(new GridLayout(0,1,0,6));
        debugRow.add(btnMarkBird);
        debugRow.add(btnMarkHerb);
        debugRow.add(btnForceRoll);

        c.gridy++; c.insets = new Insets(0,0,0,0);
        add(debugRow, c);

        // Start hidden; plugin toggles by config
        debugRow.setVisible(false);
    }

    void setDebugVisible(boolean v)
    {
        // The last component is our debug panel
        Component last = getComponent(getComponentCount()-1);
        last.setVisible(v);
        revalidate();
        repaint();
    }

    void wireDebug(Runnable markBird, Runnable markHerb, Runnable forceRoll)
    {
        btnMarkBird.addActionListener(e -> markBird.run());
        btnMarkHerb.addActionListener(e -> markHerb.run());
        btnForceRoll.addActionListener(e -> forceRoll.run());
    }

    void refresh(StreakState s)
    {
        SwingUtilities.invokeLater(() -> {
            streak.setText("Streak: " + s.getCurrentStreak());
            best.setText("Best: " + s.getBestStreak());

            String todayStr = LocalDate.now(ZoneOffset.UTC).toString();
            String bird = todayStr.equals(s.getLastBirdhouseDateUTC()) ? "✅" : "–";
            String herb = todayStr.equals(s.getLastHerbDateUTC()) ? "✅" : "–";
            today.setText("Today: BIRD " + bird + "   HERB " + herb);

            nextReset.setText("Next UTC reset: " + timeUntilUtcMidnight());
        });
    }

    private String timeUntilUtcMidnight()
    {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        Duration d = Duration.between(now, nextMidnight);
        long h = d.toHours();
        long m = d.minusHours(h).toMinutes();
        return String.format("%dh %02dm", h, m);
    }
}
