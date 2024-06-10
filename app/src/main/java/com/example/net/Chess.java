package com.example.net;

public class Chess {
    // 标记当前棋的颜色是红棋还是黑棋
    public static final int BLACK = 1;
    public static final int RED = 2;
    // 存储当前棋子是红棋还是黑棋
    private int player;
    // 当前棋子的名字
    private String name;
    // 图片 id
    private int imageId;
    // 图片编号
    // 0-15 红色，16-31 黑色
    // 0-帅
    // 1,2-士
    // 3,4-相
    // 5,6-马
    // 7,8-车
    // 9,10-炮
    // 11-15-卒
    // 16-黑帅
    // ...
    private int num;
    // 当前棋在棋盘中的位置，posX 为行，posY 为列
    private int posX, posY;

    public Chess(int player, String name, int num) {
        this.player = player;
        this.name = name;
        this.num = num;
         // 黑旗的图片 id
        int[] blackId = {R.drawable.none1, R.drawable.link1, R.drawable.virus1};
        // 红旗的图片 id
        int[] redId = {R.drawable.none2, R.drawable.link2, R.drawable.virus2};
        // 所有的棋的种类
        String[] names = {"none","link","virus"};
        // 根据当前棋的颜色来匹配不同的图片
        if (player == RED) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(name)) {
                    imageId = redId[i];
                    break;
                }
            }
        } else {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(name)) {
                    imageId = blackId[i];
                    break;
                }
            }
        }
    }
    // 获取棋编号
    public int getNum() {
        return num;
    }
    // 获取棋的颜色
    public int getPlayer() {
        return player;
    }
    // 获取棋名
    public String getName() {
        return name;
    }
    // 获取棋的图片id
    public int getImageId() {
        return imageId;
    }
    // 设置棋的行列坐标
    public void setPos(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
    }
    // 设置棋的航坐标
    public int getPosX() {
        return posX;
    }
    // 设置棋的列坐标
    public int getPosY() {
        return posY;
    }
    // 将当前的棋坐标水平翻转
    public void reverse() {
        posX = 9 - posX;
    }
}

