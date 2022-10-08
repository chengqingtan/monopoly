package com.goblin;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * 游戏客户端
 */
public class Client {
    private static Scanner scanner = new Scanner(System.in);
    private static ArrayList<Player> players = new ArrayList<>();
    private static Socket socket;
    private static int currentPLayerIndex = 1;//进行当前回合的玩家，从1~4
    private static int playerNumber;//当前游戏中玩家的总数
    private static int remainPlayerNumber;//当前游戏剩余的玩家数量
    private static int myPlayerIndex = 1;//本地客户端被分配到的标志，从1~4
    private static final GameMap gameMap = new GameMap();

    public static void main(String[] args) {
        //连接到服务器
        connectServer();
        //游戏开始运行
        gameRun();
    }

    private static void gameRun() {
        //游戏从此处开始
        System.out.println("======游戏正式开始=====");
        for (int i = 0; i < players.size(); i++) {
            System.out.println(players.get(i).getName());
        }

        System.out.println("本轮游戏共有"+players.size()+"名玩家参与，您是"+myPlayerIndex+"号玩家");
        playerNumber = players.size();
        remainPlayerNumber = players.size();
        while(remainPlayerNumber>1){
            Player currentPlayer = players.get(currentPLayerIndex - 1);
            if(currentPLayerIndex == myPlayerIndex){
                //轮到当前玩家行动
                System.out.println("=================您的回合==================");
                //1、判断玩家状态
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
                    System.out.println("您在狱中度过了这一回合");
                    continue;
                }
                System.out.println("轮到您行动了，您当前余额为："+currentPlayer.getBalance());
                //2、询问是否要掷骰子
                System.out.println("键入任意字符以掷骰子");
                String s = scanner.nextLine();
                //3、将掷骰子的消息发送给服务器，并接收服务器发来的掷骰子的点数
                int point = 0;
                try {
                    //将掷骰子的消息发送给服务器
                    socket.getOutputStream().write(GameEvent.ROLL_DICE.getValue());
                    //接收服务器发来的掷骰子的点数
                    point = socket.getInputStream().read();
                    System.out.println("您掷得的点数为："+point);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //4、移动玩家的位置
                int position1 = currentPlayer.getPosition();
                int position2 = currentPlayer.move(point);
                System.out.println("您从"+position1+"走到了"+position2);
                //5、判断玩家走到了哪一种格子上，并决定是否要采取行动
                //判断是否经过出发点
                if(position1>position2 || position2==0){
                    //如果初始位置大于移动后的位置，说明玩家经过了出生点
                    //如果玩家移动后的位置是0，也算经过出生点
                    //给予玩家经过出生点的奖金
                    currentPlayer.addBalance(Start.passStartBonus);
                    System.out.println("您经过了出生点，获得了奖金："+Start.passStartBonus);
                }
                Land currentLand = gameMap.getLand(position2);
                if(currentLand instanceof Prison){
                    //如果入狱，玩家状态设为IN_PRISON
                    currentPlayer.setPlayerStatus(PlayerStatus.IN_PRISON);
                    System.out.println("您进入了监狱");
                }
                else if(currentLand instanceof House) {
                    System.out.println("您走到了一处房产上");
                    //如果走到房产上，判断房产是否有所有者
                    if(((House) currentLand).getOwner()==null){
                        //房产没有所有者
                        //判断玩家余额是否足够购买房产
                        if(currentPlayer.getBalance()>=((House) currentLand).getPrice()) {
                            //玩家余额充足
                            System.out.println("这处房产没有所有者，请问您是否要购买？");
                            System.out.println("1->购买");
                            System.out.println("2->放弃购买");
                            System.out.println("3->查看余额");
                            System.out.println("4->查看房子价格");
                            while (true) {
                                String choice = scanner.nextLine();
                                if(choice.equals("1")){
                                    try {
                                        socket.getOutputStream().write(GameEvent.PURCHASE_HOUSE.getValue());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    purchase(currentPlayer, (House) currentLand);
                                    break;
                                }
                                else if(choice.equals("2")){
                                    try {
                                        socket.getOutputStream().write(GameEvent.PASS.getValue());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("您放弃了购买房产的机会");
                                    break;
                                }
                                else if(choice.equals("3")){
                                    System.out.println("您的余额为："+currentPlayer.getBalance());
                                }
                                else if(choice.equals("4")){
                                    System.out.println("房子价格为："+((House) currentLand).getPrice()+"，租金为："+((House) currentLand).getRent());
                                }
                                else{
                                    System.out.println("输入错误，请重新输入!");
                                }
                            }
                        }
                        else{
                            //玩家余额不足以购买房产
                            System.out.println("您的余额不足以购买房产");
                        }
                    }
                    else if(((House) currentLand).getOwner()!=currentPlayer){
                        //当房产属于其他玩家时，需要缴纳罚金
                        if (currentPlayer.reduceBalance(((House) currentLand).getRent())) {
                            //玩家余额足以缴纳罚金
                            System.out.println(currentPlayer.getName()+" 向 "+((House) currentLand).getOwner().getName()+" 支付了租金："+((House) currentLand).getRent());
                            //房产所有者获得租金
                            ((House) currentLand).getOwner().addBalance(((House) currentLand).getRent());
                        }
                        else {
                            //玩家余额不足以缴纳罚金，玩家失败，进行破产清算
                            currentPlayer.bankruptcy();
                            remainPlayerNumber--;
                        }
                    }
                }
                else if(currentLand instanceof Chance){
                    //如果走到了机会格子上，就要抽取机会
                    System.out.println("您走到了机会格子上，请输入任意字符以抽取机会：");
                    String s1 = scanner.nextLine();
                    try {
                        socket.getOutputStream().write(GameEvent.TEST_CHANCE.getValue());
                        ChanceEvent chance_result = ChanceEvent.valueOf(socket.getInputStream().read());
                        switch (chance_result){
                            case ADD_BALANCE:{
                                //如果是添加余额，再接受从服务器发来的金额（单位是100元）
                                int amount = socket.getInputStream().read() * 100;
                                currentPlayer.addBalance(amount);
                                System.out.println("您获得了奖金："+amount);
                                break;
                            }
                            case REDUCE_BALANCE:{
                                //如果是罚款，也需要接受从服务器发来的金额（单位是100元）
                                int amount = socket.getInputStream().read() * 100;
                                if (!currentPlayer.reduceBalance(amount)) {
                                    //如果无法缴纳罚款，玩家破产
                                    currentPlayer.bankruptcy();
                                    remainPlayerNumber--;
                                }
                                else {
                                    System.out.println("您缴纳了罚金："+amount);
                                }
                                break;
                            }
                            case JAIL:{
                                //如果是入狱选项，将玩家入狱
                                currentPlayer.moveToPrison();
                                System.out.println("您抽中了入狱，下回合无法行动");
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                //轮到其他玩家行动
                System.out.println("==================其他玩家的回合=====================");
                //1、判断玩家状态
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
                    System.out.println("玩家 "+currentPlayer.getName()+" 正在监狱服刑");
                    currentPlayer.setPlayerStatus(PlayerStatus.ALIVE);
                    currentPLayerIndex = nextPlayer(currentPLayerIndex,playerNumber);
                    continue;
                }
                System.out.println("轮到玩家 "+currentPlayer.getName()+" 行动");
                //1、接受来自服务器发来的掷骰子的点数
                int point = 0;
                try {
                    point = socket.getInputStream().read();
                    System.out.println("这位玩家掷出了点数："+point);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //2、移动玩家的位置
                int position1 = currentPlayer.getPosition();
                int position2 = currentPlayer.move(point);
                //3、判断玩家走到了哪一种格子上，以确定要接受的消息
                //判断是否经过出发点
                if(position1>position2 || position2==0){
                    //如果初始位置大于移动后的位置，说明玩家经过了出生点
                    //如果玩家移动后的位置是0，也算经过出生点
                    //给予玩家经过出生点的奖金
                    currentPlayer.addBalance(Start.passStartBonus);
                    System.out.println("这位玩家经过了出生点，获得奖金："+Start.passStartBonus);
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
                            //玩家余额充足，从服务端接受玩家的选择
                            try {
                                GameEvent choice = GameEvent.valueOf(socket.getInputStream().read());
                                if(choice==GameEvent.PURCHASE_HOUSE){
                                    //玩家购买了房产
                                    purchase(currentPlayer, (House) currentLand);
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
                            //房产所有者获得租金
                            ((House) currentLand).getOwner().addBalance(((House) currentLand).getRent());
                        }
                        else {
                            //玩家余额不足以缴纳罚金，玩家失败，进行破产清算
                            currentPlayer.bankruptcy();
                            remainPlayerNumber--;
                        }
                    }
                }
                else if(currentLand instanceof Chance){
                    //如果走到了机会格子上，就要抽取机会
                    System.out.println("这位玩家走到了机会格子上");
                    try {
                        ChanceEvent chance_result = ChanceEvent.valueOf(socket.getInputStream().read());
                        switch (chance_result){
                            case ADD_BALANCE:{
                                //如果是添加余额，再接受从服务器发来的金额（单位是100元）
                                int amount = socket.getInputStream().read() * 100;
                                currentPlayer.addBalance(amount);
                                System.out.println("这位玩家获得了奖金："+amount);
                                break;
                            }
                            case REDUCE_BALANCE:{
                                //如果是罚款，也需要接受从服务器发来的金额（单位是100元）
                                int amount = socket.getInputStream().read() * 100;
                                if (!currentPlayer.reduceBalance(amount)) {
                                    //如果无法缴纳罚款，玩家破产
                                    currentPlayer.bankruptcy();
                                    remainPlayerNumber--;
                                }
                                else {
                                    System.out.println("这位玩家缴纳了罚金："+amount);
                                }
                                break;
                            }
                            case JAIL:{
                                //如果是入狱选项，将玩家入狱
                                currentPlayer.moveToPrison();
                                System.out.println("这位玩家抽中了入狱，下回合无法行动");
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //玩家行动结束，向玩家收取税收
            currentPlayer.tax();
            //指定下一回合的玩家
            currentPLayerIndex = nextPlayer(currentPLayerIndex,playerNumber);
        }
        //运行到这里代表游戏已经结束
        System.out.println("============游戏结束=============");
        for (Player player : players) {
            if(player.getPlayerStatus()==PlayerStatus.ALIVE) {
                System.out.println(player.getName() + " 获得游戏胜利");
                System.out.println("他的最终余额是：" + player.getBalance());
                System.out.println("它拥有总价值" + player.getAllHousesPrice() + "的房产");
            }
        }
    }

    private static void connectServer() {
        try {
            System.out.println("请输入您要连接的服务器IP地址：");
            String serverIp = scanner.nextLine();
            socket = new Socket(serverIp,Server.serverPort);
            InputStream input = socket.getInputStream();
            GameEvent msg = GameEvent.valueOf(input.read());
            switch(msg){
                case PLAYER1_JOIN -> myPlayerIndex=1;
                case PLAYER2_JOIN -> myPlayerIndex=2;
                case PLAYER3_JOIN -> myPlayerIndex=3;
                case PLAYER4_JOIN -> myPlayerIndex=4;
            }
            while (true) {
                switch (msg){
                    case PLAYER1_JOIN: {
                        ArrayList<Player> players_temp = new ArrayList<>();
                        players_temp.add(new Player("Player1"));
                        players = players_temp;
                        System.out.println("Player1 加入游戏");
                        //这位用户成为房主
                        new Thread(new HostTask(socket)).start();
                        break;
                    }
                    case PLAYER2_JOIN: {
                        ArrayList<Player> players_temp = new ArrayList<>();
                        players_temp.add(new Player("Player1"));
                        players_temp.add(new Player("Player2"));
                        players = players_temp;
                        System.out.println("Player2 加入游戏");
                        break;
                    }
                    case PLAYER3_JOIN: {
                        ArrayList<Player> players_temp = new ArrayList<>();
                        players_temp.add(new Player("Player1"));
                        players_temp.add(new Player("Player2"));
                        players_temp.add(new Player("Player3"));
                        players = players_temp;
                        System.out.println("Player3 加入游戏");
                        break;
                    }
                    case PLAYER4_JOIN: {
                        ArrayList<Player> players_temp = new ArrayList<>();
                        players_temp.add(new Player("Player1"));
                        players_temp.add(new Player("Player2"));
                        players_temp.add(new Player("Player3"));
                        players_temp.add(new Player("Player4"));
                        players = players_temp;
                        System.out.println("Player4 加入游戏");
                        break;
                    }
                    case GAME_START:{
                        //游戏开始，结束当前循环
                        return;
                    }
                }
                input = socket.getInputStream();
                msg = GameEvent.valueOf(input.read());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 为房主专开一个线程供房主发送命令
     */
    private static class HostTask implements Runnable{
        private Socket socket;
        @Override
        public void run() {
            System.out.println("您成为了房主，键入help以查看房主可以执行的命令");
            while (true) {
                String inputFromKeyboard = scanner.next();
                if(inputFromKeyboard.equalsIgnoreCase("help")){
                    System.out.println("help->查看帮助");
                    System.out.println("start->开始游戏");
                }
                else if(inputFromKeyboard.equalsIgnoreCase("start")){
                    try {
                        socket.getOutputStream().write(HostCommand.START_GAME.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

            }
        }

        public HostTask(Socket socket) {
            this.socket = socket;
        }
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

    private static int nextPlayer(int currentPLayerIndex, int playerNumber) {
        return (currentPLayerIndex) % playerNumber + 1;
    }
}
