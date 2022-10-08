package com.goblin;

import java.util.Random;

public enum ChanceEvent {
    ADD_BALANCE(60),
    REDUCE_BALANCE(61),
    JAIL(62);


    private int value;
    private static Random random = new Random(System.currentTimeMillis());

    private ChanceEvent(int value) {
        this.value = value;
    }

    public static ChanceEvent valueOf(int index){
        return switch (index){
            case 60 -> ADD_BALANCE;
            case 61 -> REDUCE_BALANCE;
            case 62 -> JAIL;
            default -> null;
        };
    }

    public int getValue() {
        return value;
    }

    public static ChanceEvent createRandomChanceEvent(){
        return valueOf(random.nextInt(3)+60);
    }
}
