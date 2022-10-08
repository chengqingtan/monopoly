package com.goblin;

public enum HostCommand {
    START_GAME(50),
    ADD_ROBOT(51);


    private int value;

    private HostCommand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static HostCommand valueOf(int index){
        return switch (index) {
            case 50 -> START_GAME;
            case 51 -> ADD_ROBOT;
            default -> null;
        };
    }
}
