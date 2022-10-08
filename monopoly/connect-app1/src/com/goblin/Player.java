package com.goblin;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 玩家信息
 * 应该记录的信息有：
 *      1、玩家昵称：暂定为 Player1、Player2
 *      2、玩家余额
 *      3、玩家当前状态：存活、失败、托管
 *      4、玩家名下的房产
 */
public class Player implements Serializable {
    public static final int initBalance = 2000;

    private String name;//玩家名
    private int position;//玩家所在位置0~23
    private int balance;//玩家余额
    private PlayerStatus playerStatus;//玩家当前状态
    private ArrayList<House> houses;//玩家拥有的房产

    public Player(String name) {
        this.name = name;
        this.position = 0;
        this.balance = initBalance;
        this.playerStatus = PlayerStatus.ALIVE;
        houses = new ArrayList<House>();
    }

    /**
     * 增加玩家的余额
     * @param amount 要增加的金额
     */
    public void addBalance(int amount){
        balance += amount;
    }

    /**
     * 减少玩家的余额
     * 假如玩家的余额不足，则不扣除金额并返回false
     * @param amount 要减少的金额
     * @return false代表余额不足，true代表余额足够
     */
    public boolean reduceBalance(int amount){
        if(amount > balance)
            return false;
        else {
            balance -= amount;
            return true;
        }
    }

    /**
     * 玩家在大小为MapSize的地图上移动
     * @param point 玩家移动的步数，等于骰子的点数
     * @return 玩家移动后所处的位置
     */
    public int move(int point){
        this.position = (position+point) % GameMap.MapSize;
        return getPosition();
    }

    /**
     * 玩家入狱
     * 将玩家移动到监狱，并且入狱
     */
    public void moveToPrison(){
        this.position = 12;
        this.setPlayerStatus(PlayerStatus.IN_PRISON);
    }

    /**
     * 玩家破产时调用该方法以进行破产清算
     */
    public void bankruptcy(){
        //所有的房产被没收
        for (House house : houses) {
            house.setOwner(null);
        }
        //玩家状态置为失败
        setPlayerStatus(PlayerStatus.LOSE);
        //广播消息
        System.out.println(getName()+" 破产，游戏失败");
    }

    /**
     * 增加玩家的房产
     * @param house 玩家购入的房产
     */
    public void addHouse(House house){
        houses.add(house);
    }

    /**
     * 向玩家收取税收
     * @return false代表玩家交不起税，已经破产
     */
    public boolean tax(){
        int tax = balance/10;
        if(!reduceBalance(tax)){
            //如果玩家交不起税，玩家破产清算
            bankruptcy();
            return false;
        }
        else{
            System.out.println("本回合收取税金："+tax);
            System.out.println("余额为："+getBalance());
            return true;
        }
    }

    public int getAllHousesPrice(){
        int allPrice = 0;
        for (House house : houses) {
            allPrice += house.getPrice();
        }
        return allPrice;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public int getBalance() {
        return balance;
    }

    public PlayerStatus getPlayerStatus() {
        return playerStatus;
    }

    public void setPlayerStatus(PlayerStatus playerStatus) {
        this.playerStatus = playerStatus;
    }
}
