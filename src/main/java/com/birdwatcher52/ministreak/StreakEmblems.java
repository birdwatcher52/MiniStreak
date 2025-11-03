package com.birdwatcher52.ministreak;

public enum StreakEmblems {
    CAT_1(3067),
    CAT_2(3068),
    CAT_3(3069),
    CAT_4(3070),
    GNOME_CHILD(3075);

    private final int spriteId; // sprite cache id
    StreakEmblems(int spriteId) { this.spriteId = spriteId; }
    public int getSpriteId() { return spriteId; }
}
