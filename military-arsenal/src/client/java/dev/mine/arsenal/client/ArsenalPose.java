package dev.mine.arsenal.client;

import dev.mine.arsenal.core.Weapon;

public interface ArsenalPose {
    Weapon arsenal$weapon();
    boolean arsenal$left();
    boolean arsenal$aim();
    int arsenal$frame();
    void arsenal$set(Weapon weapon,boolean left,boolean aim,int frame);
}
