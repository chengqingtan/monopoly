package com.goblin;

import jdk.swing.interop.SwingInterOpUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
 * 先构建服务端用于客户端远程连接操作
 */
public class Server extends Thread{
    public static final int serverPort = 20207;//游戏服务器端口
    public static final Random random = new Random(System.currentTimeMillis());

    private ArrayList<Socket> sockets = new ArrayList<>();
    private ArrayList<Player> players = new ArrayList<>();
    private int currentPLayerIndex = 1;//从1~4
    private GameMap gameMap = new GameMap();

    public static void main(String[] args) {
        new Server().start();
    }

    @Override
    public void run() {
        Thread connectThread = new Thread(new Connect());
        connectThread.start();
        waitForHostCommand();
        //游戏开始进行
        //当前游戏中玩家的总数
        int playerNumber = players.size();
        int remainPlayerNumber = players.size();
        while(remainPlayerNumber>1){
            //1、判断玩家状态
            Player currentPlayer = players.get(currentPLayerIndex - 1);
            if(currentPlayer.getPlayerStatus()==PlayerStatus.LOSE){
                //玩家阵亡，直接结束回合,轮到下一个玩家
                currentPLayerIndex = nextPlayer(currentPLayerIndex,playerNumber);
                continue;
            }
            else if(currentPlayer.getPlayerStatus()==PlayerStatus.ROBOT_CONTROL){
                //电脑控制，自动行动（未完成）
            }
            else if(currentPlayer.getPlayerStatus()==PlayerStatus.IN_PRISON){
                //玩家入狱，结束回合
                currentPlayer.setPlayerStatus(PlayerStatus.ALIVE);
                currentPLayerIndex = nextPlayer(currentPLayerIndex,playerNumber);
                continue;
            }
            //2、接受玩家掷骰子的命令
            int point = 0;
            try {
                if (GameEvent.valueOf(sockets.get(currentPLayerIndex-1).getInputStream().read())==GameEvent.ROLL_DICE) {
                    //掷骰子
                    point = rollDice();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);//在此处直接结束服务端程序
            }
            //3、将骰子点数发送给所有玩家
            try {
                for (Socket socket : sockets) {
                    socket.getOutputStream().write(point);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //4、移动玩家的位置
            int position1 = currentPlayer.getPosition();
            int position2 = currentPlayer.move(point);
            //5、判断玩家走到了哪一种格子上，并决定是否要采取行动
            //判断是否经过出发点
            if(position1>position2 || position2==0){
                //如果初始位置大于移动后的位置，说明玩家经过了出生点
                //如果玩家移动后的位置是0，也算经过出生点
                //给予玩家经过出生点的奖金
                currentPlayer.addBalance(Start.passStartBonus);
            }
            Land currentLand = gameMap.getLand(position2);
            if(currentLand instanceof Prison){
                //如果入狱，玩家状态设为IN_PRISON
                currentPlayer.setPlayerStatus(PlayerStatus.IN_PRISON);
            }
            else if(currentLand instanceof House) {
                //如果走到房产上，判断房产是否有所有者
                if(((House) currentLand).getOwner()==null){
                    //房产没有所有者
                    //判断玩家余额是否足够购买房产
                    if(currentPlayer.getBalance()>=((House) currentLand).getPrice()) {
                        //玩家余额充足，从客户端接受玩家的选择
                        try {
                            GameEvent choice = GameEvent.valueOf(sockets.get(currentPLayerIndex-1).getInputStream().read());
                            if(choice==GameEvent.PURCHASE_HOUSE){
                                //玩家购买了房产
                                purchase(currentPlayer, (House) currentLand);
                                //将玩家买房的选择告知其他玩家
                                for (int i = 0; i < sockets.size(); i++) {
                                    if(i != currentPLayerIndex-1){
                                        sockets.get(i).getOutputStream().write(GameEvent.PURCHASE_HOUSE.getValue());
                                    }
                                }
                            }
                            else {
                                //将玩家不买房的选择告知其他玩家
                                for (int i = 0; i < sockets.size(); i++) {
                                    if(i != currentPLayerIndex-1){
                                        sockets.get(i).getOutputStream().write(GameEvent.PASS.getValue());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        //玩家余额不足以购买房产
                        System.out.println(currentPlayer.getName()+" 的余额不足以购买这处房产");
                    }
                }
                else if(((House) currentLand).getOwner()!=currentPlayer){
                    //当房产属于其他玩家时，需要缴纳罚金
                    if (currentPlayer.reduceBalance(((House) currentLand).getRent())) {
                        //玩家余额足以缴纳罚金
                        System.out.println(currentPlayer.getName()+" 向 "+((House) currentLand).getOwner().getName()+" 支付了租金："+((House) currentLand).getRent());
                    }
                    else {
                        //玩家余额不足以缴纳罚金，玩家失败，进行破产清算
                        currentPlayer.bankruptcy();
                        remainPlayerNumber--;
                    }
                    //房产所有者获得租金
                    ((House) currentLand).getOwner().addBalance(((House) currentLand).getRent());
                }
            }
            else if(currentLand instanceof Chance){
                //如果走到了机会格子上，就要抽取机会
                try {
                    if (GameEvent.valueOf(sockets.get(currentPLayerIndex-1).getInputStream().read()) != GameEvent.TEST_CHANCE) {
                        System.out.println("[ERROR]机会抽取错误");
                    }
                    ChanceEvent chanceEvent = ChanceEvent.createRandomChanceEvent();
                    switch (chanceEvent){
                        case ADD_BALANCE:{
                            //如果是添加余额，向所有客户端发送金额
                            int amount = random.nextInt(6)+5;//玩家获得500~1000元的奖金
                            for (Socket socket : sockets) {
                                socket.getOutputStream().write(ChanceEvent.ADD_BALANCE.getValue());
                                socket.getOutputStream().write(amount);
                            }
                            currentPlayer.addBalance(amount*100);
                            break;
                        }
                        case REDUCE_BALANCE:{
                            //如果是罚款，向所有客户端发送金额
                            int amount = random.nextInt(5);//玩家缴纳1~5的罚款,单位是100元
                            for (Socket socket : sockets) {
                                socket.getOutputStream().write(ChanceEvent.REDUCE_BALANCE.getValue());
                                socket.getOutputStream().write(amount);
                            }
                            if (!currentPlayer.reduceBalance(amount*100)) {
                                //如果无法缴纳罚款，玩家破产
                                currentPlayer.bankruptcy();
                                remainPlayerNumber--;
                            }
                            break;
                        }
                        case JAIL:{
                            //如果是入狱选项，将玩家入狱
                            for (Socket socket : sockets) {
                                socket.getOutputStream().write(ChanceEvent.JAIL.getValue());
                            }
                            currentPlayer.moveToPrison();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //5、玩家回合结束，收取税金
            currentPlayer.tax();
            //6、轮到下一个玩家
            currentPLayerIndex = nextPlayer(currentPLayerIndex,playerNumber);
        }
    }

    private int nextPlayer(int currentPLayerIndex, int playerNumber) {
        return (currentPLayerIndex) % playerNumber + 1;
    }

    /**
     * 等待房主发送命令
     */
    private void waitForHostCommand(){
        try {
            while(sockets.size()==0){
                Thread.sleep(1000);
            }
            InputStream inputFromHost = sockets.get(0).getInputStream();
            HostCommand inputCommand = HostCommand.valueOf(inputFromHost.read());
            if(inputCommand == HostCommand.START_GAME){
                //通知所有用户开始游戏
                for (int i = 0; i < sockets.size(); i++) {
                    sockets.get(i).getOutputStream().write(GameEvent.GAME_START.getValue());
                }
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新开辟一个线程用于处理用户连接服务器
     */
    private class Connect implements Runnable{
        @Override
        public void run() {
            try {
                    //接受来自客户端的连接
                    ServerSocket serverSocket = new ServerSocket(serverPort);
                for(int m=0;m<4;++m) {
                    Socket socket = null;
                    if (!currentThread().isInterrupted()) {
                        socket = serverSocket.accept();
                    }
                    else
                        return;
                    //将客户端连接加入到list中存储起来
                    String playerName = "Player"+(players.size()+1);
                    System.out.println(playerName+"连接至服务器");
                    players.add(new Player(playerName));
                    sockets.add(socket);
                    //告诉所有玩家有新玩家加入游戏的消息
                    for (int i = 0; i < sockets.size(); i++) {
                        OutputStream outputStream = sockets.get(i).getOutputStream();
                        switch (sockets.size()){
                            case 1:
                                outputStream.write(GameEvent.PLAYER1_JOIN.getValue());
                                break;
                            case 2:
                                outputStream.write(GameEvent.PLAYER2_JOIN.getValue());
                                break;
                            case 3:
                                outputStream.write(GameEvent.PLAYER3_JOIN.getValue());
                                break;
                            case 4:
                                outputStream.write(GameEvent.PLAYER4_JOIN.getValue());
                                break;
                            default:
                                System.out.println("Error!");
                        }
                    }


                }
            } catch (IOException e) {
                System.out.println("[Error]玩家与服务器断开连接");
                e.printStackTrace();
            }
        }
    }

    private static int rollDice(){
        return random.nextInt(6) + 1;
    }

    /**
     * 玩家购买房产
     * @param player 购房玩家
     * @param house 待售房产
     */
    private static void purchase(Player player, House house){
        //1、扣除玩家的金额
        player.reduceBalance(house.getPrice());
        //2、将房产所有者设置为玩家
        house.setOwner(player);
        //3、将房产添加到玩家的房产列表
        player.addHouse(house);
    }
}
