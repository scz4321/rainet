package com.example.net;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServerView extends View {
    // 上边距
    public static final int TOP = 200;
    // 棋盘的左，上边距
    public static final int MARGINLEFT = 50, MARGINTOP = 100 + TOP;
    // 棋子的宽度，视情况而定
    public static final int CHESSW = 62;
    // 格子的宽度，视情况而定
    public static final int W = 70;
    // 14个格子，15条行列坐标
    public static final int L = 15;
    public static final int BLOCKL = 14;
    // 棋的颜色标记
    public static final int BLACK = 2, WHITE = 1;
    //    public static final int NULL = -1;
    // 棋盘的宽度
    private int width = W * BLOCKL;
    // 棋盘的高度
    private int height = W * BLOCKL;
    // 标记我方的棋的颜色
    private int player;
    // 存储所有走过的棋的信息，主要为坐标
    private List<String> allList = new ArrayList<>();
    // 存储我方走过的棋的信息
    private List<String> myList = new ArrayList<>();
    // 存储对方走过的棋的信息
    private List<String> enemyList = new ArrayList<>();
    // 是否可以走棋
    private boolean canPlay;
    // 是否有滑动事件
    private boolean isMove;

    // 用于开启服务器，所有的网络请求都要在线程中执行
    private ServerSocket serverSocket;
    // 用于与客户端交互
    private Socket socket;
    // 对方端口
    private int port;
    // 用于发送消息
    private PrintWriter pw;
    // 主活动
    private MainActivity mainActivity;
    // 提示文本框
    private TextView tvTip;
    // 重新开始
    private Button btnRestart;

    public ServerView(Context context, int port, MainActivity mainActivity) {
        super(context);
        this.port = port;
        this.mainActivity = mainActivity;
        // 初始化棋盘
        initMap();
        // 1. 加载多张图片作为 Drawable 对象
        Drawable[] layers = new Drawable[2];
        layers[0] = getResources().getDrawable(R.drawable.qipan1); // 第一张图片
        layers[1] = getResources().getDrawable(R.drawable.qipan2); // 第二张图片
        // 2. 创建 LayerDrawable 对象并设置它们的位置和大小
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        // 设置第一张图片的位置和大小
        layerDrawable.setLayerInset(0, 0, 0, 0, 0);
        // 设置第二张图片的位置和大小
         layerDrawable.setLayerInset(1, 0, 250, 0, 250); // 这里假设第二张图片在第一张图片之上，并向右和向下偏移了100个像素

        // 3. 设置 LayerDrawable 为窗口背景
        // getWindow().setBackgroundDrawable(layerDrawable);
        mainActivity.getWindow().setBackgroundDrawable(layerDrawable);
        // 添加提示文本框控件和重新开始按钮
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvTip = new TextView(context);
        tvTip.setText("等待连接...");
        tvTip.setX(500);
        tvTip.setY(20);
        mainActivity.addContentView(tvTip, params);
        btnRestart = new Button(context);
        btnRestart.setText("重新开始");
        btnRestart.setX(MARGINLEFT);
        btnRestart.setY(20);
        mainActivity.addContentView(btnRestart, params);
        btnRestart.setEnabled(false);
        btnRestart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重新开始游戏
                restartGame();
                // 发送消息给客户端
                sendMes("restart");
            }
        });

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMove = false;
                        break;
                    // 判断是否为滑动事件
                    case MotionEvent.ACTION_MOVE:
                        isMove = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!canPlay) {
                            break;

                        }
                        // 只可处理点击事件
                        if (!isMove) {
                            // 获取当前点击位置的 x, y 坐标
                            int x = (int) event.getX() - MARGINLEFT;
                            int y = (int) event.getY() - MARGINTOP;
                            // 是否在棋盘外
                            if (x < -W / 2 || x > width + W / 2 || y < -W / 2 || y > width + W / 2) {
                                break;
                            }
                            // 转化为棋盘的 col 列坐标
                            // x % W > W / 2 ? 1 : 0 为当前的位置的求模后是否满足大于一半的宽度，
                            // 若大于则把它安排到下一个位置，否则不变
                            x = x / W + (x % W > W / 2 ? 1 : 0);
                            // 转化为棋盘的 row 行坐标
                            y = y / W + (y % W > W / 2 ? 1 : 0);
                            // 设置移动信息
                            String move = y + "|" + x;
                            // 当前位置是否有棋存在
                            if (allList.contains(move)) {
                                break;
                            }
                            // 把当前移动位置添加到所有列表中
                            allList.add(move);
                            // 把当前移动位置添加到我方列表中
                            myList.add(move);
                            // 将移动消息发送给对面
                            sendMes("move|" + move);
                            // 轮到对方下
                            canPlay = false;
                            // 设置提示信息
                            tvTip.setText("对方下");
                            // 更新视图
                            invalidate();
                            // 判断是否赢了
                            if (isWin()) {
                                Toast.makeText(getContext(), "黑棋获胜！", Toast.LENGTH_SHORT).show();
                                tvTip.setText("我方获胜！");
                            }
                        }
                }
                return true;
            }
        });
    }
    // 开启服务器
    public void startConn() {
        // 只能在线程(异步)中执行
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    // 获取客户端信息，若无客户端连接则会一直暂停在这
                    socket = serverSocket.accept();
                    setTip("连接成功!");
                    // 发送已连接给客户端
                    sendMes("conn|");
                    // 开启接受消息的线程
                    new MyThread().start();
                    // 更新视图
                    invalidate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }
    // 是否赢了
    private boolean isWin() {
        // 好像有点多此一举...
        return isCanLink();
    }
    // 是否有了输赢
    private boolean isCanLink() {
        // 黑棋先，服务端为黑棋，客户端为白棋
        // 判断最后下的是谁，为0为白棋，为1为黑棋
        int who = allList.size() % 2;
        // 将行列坐标分割出来
        String[] t = allList.get(allList.size() - 1).split("\\|");
        // 行坐标
        int r1 = Integer.valueOf(t[0]);
        // 列坐标
        int c1 = Integer.valueOf(t[1]);
        // 垂直方向是否有五子
        if (canVLink(who, r1, c1)) {
            return true;
        }
        // 水平方向是否有五子
        if (canHLink(who, r1, c1)) {
            return true;
        }
        // 左下右上方向
        if (canLeftObliqueLink(who, r1, c1)) {
            return true;
        }
        // 左上右下方向
        if (canRightObliqueLink(who, r1, c1)) {
            return true;
        }
        return false;
    }
    // 左上右下方向
    private boolean canRightObliqueLink(int who, int r1, int c1) {
        // 记录连子的个数
        int count = 0;
        // 遍历要用到的行列坐标
        int r2;
        int c2;
        // 黑棋
        if (who == 1) {
            // left top
            r2 = r1 - 1;
            c2 = c1 - 1;
            // 往左上方向遍历，若存在则表示有连棋
            while (myList.contains(r2 + "|" + c2)) {
                count++;
                r2--;
                c2--;
            }
            // right down
            r2 = r1 + 1;
            c2 = c1 + 1;
            // 往右下方向遍历
            while (myList.contains(r2 + "|" + c2)) {
                count++;
                r2++;
                c2++;
            }
            // 若有四个以上则代表构成五子
            if (count >= 4) {
                return true;
            }
        } else {
            // 红棋
            // right top
            r2 = r1 - 1;
            c2 = c1 - 1;
            // 往左上方向遍历
            while (enemyList.contains(r2 + "|" + c2)) {
                count++;
                r2--;
                c2--;
            }
            // left down
            r2 = r1 + 1;
            c2 = c1 + 1;
            // 往右下方向遍历
            while (enemyList.contains(r2 + "|" + c2)) {
                count++;
                r2++;
                c2++;
            }
            // 若有四个以上则代表构成五子
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }
    // 左下右上方向
    private boolean canLeftObliqueLink(int who, int r1, int c1) {
        int count = 0;
        int r2;
        int c2;
        // 黑棋
        if (who == 1) {
            // right top
            r2 = r1 - 1;
            c2 = c1 + 1;
            while (myList.contains(r2 + "|" + c2)) {
                count++;
                r2--;
                c2++;
            }
            // left down
            r2 = r1 + 1;
            c2 = c1 - 1;
            while (myList.contains(r2 + "|" + c2)) {
                count++;
                r2++;
                c2--;
            }
            if (count >= 4) {
                return true;
            }
        } else {
            // 白棋
            // right top
            r2 = r1 - 1;
            c2 = c1 + 1;
            while (enemyList.contains(r2 + "|" + c2)) {
                count++;
                r2--;
                c2++;
            }
            // left down
            r2 = r1 + 1;
            c2 = c1 - 1;
            while (enemyList.contains(r2 + "|" + c2)) {
                count++;
                r2++;
                c2--;
            }
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }
    // 水平方向
    private boolean canHLink(int who, int r1, int c1) {
        int count = 0;
        int c2;
        // 黑棋
        if (who == 1) {
            // left
            c2 = c1 - 1;
            while (myList.contains(r1 + "|" + c2)) {
                count++;
                c2--;
            }
            // right
            c2 = c1 + 1;
            while (myList.contains(r1 + "|" + c2)) {
                count++;
                c2++;
            }
            if (count >= 4) {
                return true;
            }
        } else {
            // 白棋
            // left
            c2 = c1 - 1;
            while (enemyList.contains(r1 + "|" + c2)) {
                count++;
                c2--;
            }
            // right
            c2 = c1 + 1;
            while (enemyList.contains(r1 + "|" + c2)) {
                count++;
                c2++;
            }
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }
    // 垂直方向
    private boolean canVLink(int who, int r1, int c1) {
        int count = 0;
        int r2;
        // 黑棋
        if (who == 1) {
            // top
            r2 = r1 - 1;
            while (myList.contains(r2 + "|" + c1)) {
                count++;
                r2--;
            }
            // down
            r2 = r1 + 1;
            while (myList.contains(r2 + "|" + c1)) {
                count++;
                r2++;
            }
            if (count >= 4) {
                return true;
            }
        } else {
            // 白棋
            // top
            r2 = r1 - 1;
            while (enemyList.contains(r2 + "|" + c1)) {
                count++;
                r2--;
            }
            // down
            r2 = r1 + 1;
            while (enemyList.contains(r2 + "|" + c1)) {
                count++;
                r2++;
            }
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }
    // 接受消息的线程
    class MyThread extends Thread {
        @Override
        public void run() {
            BufferedReader br = null;
            InputStreamReader isr = null;
            try {
                String t;
                while (true) {
                    // 睡眠一段时间，不必每毫秒都执行
                    sleep(100);
                    isr = new InputStreamReader(socket.getInputStream());
                    br = new BufferedReader(isr);
                    // 是否接受到了消息
                    if (br.ready()) {
                        String cmd = br.readLine();
                        // 分割信息
                        String[] array = cmd.split("\\|");
                        switch (array[0]) {
                            // 一定是服务器接受到这个消息
                            case "join":
                                // 服务器一定为黑棋
                                player = BLACK;
                                // 我方先下
                                canPlay = true;
                                // 发送消息给客户端
                                sendMes("conn|");
                                setTip("我下");
                                // UI 更新一定在主线程中执行
                                // 重新开始按钮可以点了，这个方法可以赚到主线程中
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnRestart.setEnabled(true);
                                    }
                                });
                                //
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mainActivity, "你是黑棋", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case "move":
                                // 存储对方走的棋坐标
                                t = array[1] + "|" + array[2];
                                allList.add(t);
                                enemyList.add(t);
                                // 我方走棋
                                canPlay = true;
                                setTip("我下");
                                // 是否有了输赢
                                if (isWin()) {
                                    post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "白棋获胜!", Toast.LENGTH_SHORT).show();
                                            tvTip.setText("对方获胜!");
                                            canPlay = false;
                                        }
                                    });
                                }
                                invalidate();
                                break;
                            case "restart":
                                // 重新开始游戏
                                restartGame();
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setTip(final String s) {
        post(new Runnable() {
            @Override
            public void run() {
                tvTip.setText(s);
            }
        });
    }
    // 发送消息个客户端
    private void sendMes(final String s) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pw = new PrintWriter(socket.getOutputStream());
                    pw.println(s);
                    pw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    // 初始化棋盘，把列表全部清空
    private void initMap() {
        allList.clear();
        myList.clear();
        enemyList.clear();
    }


//    // 调用 invalidate 是执行
//    @Override
//    protected void onDraw(Canvas canvas) {
//        // 画笔，设置线条的样式
//        Paint paint = new Paint();
//        paint.setColor(Color.parseColor("#FFE869"));
//        // 设置棋盘的位置，视情况而定
//        canvas.drawRect(0, TOP, 1080, 1400 + TOP, paint);
//        // 设置画笔颜色为黑色，用于画棋盘坐标系
//        paint.setColor(Color.BLACK);
//        paint.setStrokeWidth(3);
//
//        for (int i = 0; i < L; i++) {
//            int hx = MARGINLEFT;
//            int hy = MARGINTOP + i * W;
//            int vx = MARGINLEFT + i * W;
//            int vy = MARGINTOP;
//            // 画竖线
//            canvas.drawLine(hx, hy, MARGINLEFT + width, hy, paint);
//            // 画横线
//            canvas.drawLine(vx, vy, vx, MARGINTOP + height, paint);
//        }
//        // 画初始的九个星位
//        RectF rectF = new RectF();
//        for (int i = 0; i < 3; i++) {
//            for (int j = 0; j < 3; j++) {
//                int w = 20;
//                int x = MARGINLEFT + W * 3 + j * W * 4 - w / 2;
//                int y = MARGINTOP + W * 3 + i * W * 4 - w / 2;
//                rectF.set(x, y, x + 20, y + 20);
//                // 画椭圆
//                canvas.drawOval(rectF, paint);
//            }
//        }
//        // 画棋子
//        String[] t;
//        int r, c;
//        for (int i = 0; i < allList.size(); i++) {
//            // 黑棋先，所以黑棋索引为双数，白棋为单数
//            if (i % 2 == 0) {
//                paint.setColor(Color.BLACK);
//            } else {
//                paint.setColor(Color.WHITE);
//            }
//            t = allList.get(i).split("\\|");
//            // 行坐标
//            r = Integer.valueOf(t[0]);
//            // 列坐标
//            c = Integer.valueOf(t[1]);
//            // 使棋子的中心点对其坐标顶点
//            c = MARGINLEFT + c * W - CHESSW / 2;
//            r = MARGINTOP + r * W - CHESSW / 2;
//            rectF.set(c, r, c + CHESSW, r + CHESSW);
//            // 画椭圆
//            canvas.drawOval(rectF, paint);
//        }
//    }
    // 重新开始游戏
    public void restartGame() {
        allList.clear();
        myList.clear();
        enemyList.clear();
        canPlay = true;
        setTip("我下");
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }
}

