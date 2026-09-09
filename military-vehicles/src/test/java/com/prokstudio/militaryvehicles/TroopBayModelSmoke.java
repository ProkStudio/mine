package com.prokstudio.militaryvehicles;

/** Runs the troop bay geometry checks without Gradle or a Minecraft runtime. */
public final class TroopBayModelSmoke {
    public static void main(String[] args) {
        System.out.println("TroopBayModelSmoke PASS: 5 cases, "+new TroopBayModelCases().runAll()+" assertions (not Minecraft scenarios)");
    }
}
