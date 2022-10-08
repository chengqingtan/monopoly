package com.goblin;

public class GameMap {
    public static final int MapSize = 24;
    public Land[] gameMap = new Land[MapSize];

    public Land getLand(int index){
        if(index<=MapSize && index>=0)
            return gameMap[index];
        else
            return null;
    }

    public GameMap() {
        gameMap[0] = new Start();
        gameMap[1] = new House(1000,500);
        gameMap[2] = new House(1000,500);
        gameMap[3] = new House(1000,500);
        gameMap[4] = new Chance();
        gameMap[5] = new House(1000,500);
        gameMap[6] = new House(1000,500);
        gameMap[7] = new Chance();
        gameMap[8] = new House(1000,500);
        gameMap[9] = new House(1000,500);
        gameMap[10] = new House(1000,500);
        gameMap[11] = new House(1000,500);
        gameMap[12] = new Prison();
        gameMap[13] = new House(1000,500);
        gameMap[14] = new House(1000,500);
        gameMap[15] = new Chance();
        gameMap[16] = new House(1000,500);
        gameMap[17] = new House(1000,500);
        gameMap[18] = new House(1000,500);
        gameMap[19] = new Chance();
        gameMap[20] = new House(1000,500);
        gameMap[21] = new House(1000,500);
        gameMap[22] = new House(1000,500);
        gameMap[23] = new House(1000,500);
    }
}
