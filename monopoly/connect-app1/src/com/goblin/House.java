package com.goblin;

public class House implements Land{
    private Player owner;//房产所有者
    private int price;//房产的购入价格
    private int rent;//其他玩家走到该建筑上需要向拥有者支付的金额

    public House(int price, int rent) {
        this.owner = null;
        this.price = price;
        this.rent = rent;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public int getPrice() {
        return price;
    }

    public int getRent() {
        return rent;
    }
}
