package com.goblin;

public enum GameEvent {
    PLAYER1_JOIN(0),//玩家1加入游戏
    PLAYER2_JOIN(1),//玩家2加入游戏
    PLAYER3_JOIN(2),//玩家3加入游戏
    PLAYER4_JOIN(3),//玩家4加入游戏
    PLAYER1_LOSE(4),//玩家1输掉游戏
    PLAYER2_LOSE(5),//玩家2输掉游戏
    PLAYER3_LOSE(6),//玩家3输掉游戏
    PLAYER4_LOSE(7), //玩家4输掉游戏
    GAME_START(8),//游戏开始
    PURCHASE_HOUSE(9),//购买房产
    ROLL_DICE(10),//玩家掷骰子
    PASS(11),//统一代表玩家不行动（如不购买房产……）
    TEST_CHANCE(12);//走到机会格子上，抽取机会


    private int value;

    private GameEvent(int value) {
        this.value = value;
    }

    public static GameEvent valueOf(int index){
        return switch (index) {
            case 0 -> PLAYER1_JOIN;
            case 1 -> PLAYER2_JOIN;
            case 2 -> PLAYER3_JOIN;
            case 3 -> PLAYER4_JOIN;
            case 4 -> PLAYER1_LOSE;
            case 5 -> PLAYER2_LOSE;
            case 6 -> PLAYER3_LOSE;
            case 7 -> PLAYER4_LOSE;
            case 8 -> GAME_START;
            case 9 -> PURCHASE_HOUSE;
            case 10 -> ROLL_DICE;
            case 11 -> PASS;
            case 12 -> TEST_CHANCE;
            default -> null;
        };
    }

    public int getValue(){
        return this.value;
    }
}
