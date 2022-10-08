package com.goblin;

/**
 * 玩家当前的游戏状态
 */
public enum PlayerStatus {
    ALIVE,//玩家仍然存活
    LOSE,//玩家已失败
    ROBOT_CONTROL,//机器人控制，自动操作
    IN_PRISON//玩家入狱
}
