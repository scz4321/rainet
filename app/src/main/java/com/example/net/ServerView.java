package com.example.net;
//172.30.4.74
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class ServerView extends View {
    // 用于开启服务器，所有的网络请求都要在线程中执行
    private ServerSocket serverSocket;
    // 用于与客户端交互
    private Socket socket;
    // 用于发送消息
    private PrintWriter pw;

    // 棋的颜色
    public static final int BLACK = 1;
    public static final int RED = 2;
    // 设置棋盘的位置，视情况而定
    public static final int MARGINTOP = 200;
    public static final int MARGINLEFT = 55;
    // 每个格子的宽度
    public static final int W = 122;
    // 棋的总数
    public static final int ALLCHESS = 16;
    // 棋盘的行
    public static final int ROW = 8;
    // 棋盘的列
    public static final int COL = 8;
    // 没有棋的棋盘坐标的标记
    public static final int NULL = -1;
    // 接受消息端口
    private int receiverPort;
    // 发送消息端口
    private int sendPort;
    // 对方 ip
    private String ip;
    // 对方端口
    private int port;
    // 主活动
    private MainActivity context;
    // 主活动
    private MainActivity mainActivity;
    // 所有的棋
    private Chess[] allChess = new Chess[ALLCHESS];
    // 棋盘
    private int[][] map = new int[ROW][COL];
    // 当前是否可以点击
    private boolean canPlay;
    // 判断是否移动了，只可处理点击事件
    private boolean isMove;
    // 设置我方的棋的颜色
    private int player;
    // 记录第一次选择的棋
    private Chess firstSelect;
    // 用于提示消息
    private TextView tvTip;
    // 判断当前是否赢了
    private boolean isWin;

    private Button btnNewGame;

    // 通过构造方法将一些重要参数传入进来
    public ServerView(Context context, int port, MainActivity mainActivity) {
        super(context);
        this.port = port;
        this.mainActivity = mainActivity;
        this.ip = ip;
        this.context = (MainActivity) context;
        // 添加一个可以重新开始的按钮
        ViewGroup.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 120);

        btnNewGame = new Button(context);
        // 开始游戏后才可以点击
        btnNewGame.setEnabled(false);
        btnNewGame.setX(10);
        btnNewGame.setY(10);
        btnNewGame.setBackgroundResource(R.drawable.btn_blue);
        btnNewGame.setText("重新开始");
        btnNewGame.setTextColor(Color.WHITE);
        btnNewGame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重新开始游戏
                restartGame();
                sendMes("restart|");
            }
        });
        this.context.addContentView(btnNewGame, param);
        // 添加用于提示的文本框
        tvTip = new TextView(context);
        tvTip.setText("等待连接...");
        tvTip.setX(400);
        tvTip.setY(20);
        this.context.addContentView(tvTip, param);
        // 初始化棋盘
        initMapAndChess();
        // 设置触屏事件
        setOnTouchListener(new OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMove = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        // 若当前不可以点击则不执行点击事件
                        if (!canPlay) {
                            return false;
                        }
                        if (!isMove) {
                            // 获取点击的 x 坐标
                            int x = ((int) event.getX() - MARGINLEFT);
                            // 获取点击的 y 坐标
                            int y = ((int) event.getY() - MARGINTOP - MARGINLEFT - 100);
                            // 转化为棋盘的 col 列坐标
                            x = x / W ;
                            // 转化为棋盘的 row 行坐标
                            y = y / W ;
                            // 若超出棋盘边界则不执行
                            if (x < 0 || x >= COL || y < 0 || y >= ROW) {
                                break;
                            }
                            // 如果为第一次点击
                            if (firstSelect == null) {
                                // 若当前点击的位置是空的
                                if (map[y][x] == NULL) {
                                    break;
                                }
                                // 创建一个临时变量来存储当前位置上的棋
                                Chess temp = allChess[map[y][x]];
                                // 若点击的是对方的棋
                                if (temp.getPlayer() != player) {
                                    break;
                                }
                                // 存起来
                                firstSelect = temp;
                                // 更新视图
                                updateOnUI();
                            } else {
                                // 已选择第一个棋后
                                // 若当前位置为空棋时
                                if (map[y][x] == NULL) {
                                    // 若能移动
                                    if (canMove(y, x)) {
                                        // 获取第一次选择的棋的编号, 范围为 0,1,2,3...31;
                                        int pos = map[firstSelect.getPosX()][firstSelect.getPosY()];
                                        // 将第一次选择的棋编号给第二次选择的位置
                                        map[y][x] = pos;
                                        // 将第一次选择的棋编号置空
                                        map[firstSelect.getPosX()][firstSelect.getPosY()] = NULL;
                                        // 将第一次选择的棋的位置改变为当前位置
                                        firstSelect.setPos(y, x);
                                        // 轮到对方下
                                        canPlay = false;
                                        // 将存储的第一个棋置空
                                        firstSelect = null;
                                        // 发送我方移动信息给客户单，“|” 为分隔符，用于分割信息，
                                        // 最后要用 "|" 结尾，不然最后一个信息个出错
                                        sendMes("move|" + pos + "|" + y + "|" + x + "|");
                                        // 设置提示消息
                                        tvTip.setText("对方下");
                                        // 更新视图
                                        updateOnUI();
                                    }
                                } else {
                                    // 若当前的位置不为空棋
                                    // 获取当前的棋编号
                                    int pos = map[y][x];
                                    // 若当前的棋为我方棋时，则把第一次选择的棋替换为当前棋
                                    if (allChess[pos].getPlayer() == player) {
                                        firstSelect = allChess[pos];
                                        updateOnUI();
                                    } else {
                                        // 是否可以移动
                                        if (canMove(y, x)) {
                                            // 将第一次选择的棋编号置空
                                            map[firstSelect.getPosX()][firstSelect.getPosY()] = NULL;
                                            // 将第一次选择的棋编号给第二次选择的位置
                                            map[y][x] = firstSelect.getNum();
                                            // 将第一次选择的棋的位置改变为当前位置
                                            firstSelect.setPos(y, x);
                                            // 发送我方移动信息给客户单
                                            sendMes("move|" + firstSelect.getNum() + "|" + y + "|" + x + "|");
                                            // 若当前吃掉的棋为帅时
                                            if ("帅".equals(allChess[pos].getName())) {
                                                sendMes("winner|");
                                                tvTip.setText("我方获胜");
                                            } else {
                                                tvTip.setText("对方下");
                                            }
                                            // 将存储的第一个棋置空
                                            firstSelect = null;
                                            // 将吃掉得棋置空
                                            allChess[pos] = null;
                                            // 轮到对方下
                                            canPlay = false;
                                            updateOnUI();
                                        }
                                    }
                                }
                            }
                        }
                }
                return true;
            }
        });
    }
    // 开启服务器
    public void startConn() {
        // 只能在线程(异步)中执行 172.30.4.74
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
                    new MessageThread().start();
                    // 更新视图
                    updateOnUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }
    // 接受信息的线程
    class MessageThread extends Thread {
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
                                        btnNewGame.setEnabled(true);
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
                            // 接受到了移动信息
                            case "move":
                                // 判断是否赢了，若赢了则后面的不执行
                                if (isWin) {
                                    continue;
                                }
                                // 对方走的棋编号
                                int originalPos = Integer.valueOf(array[1]) - 8;
                                // 要走的行坐标
                                int y2 = ROW - Integer.valueOf(array[2]) - 1;
                                // 要走的列坐标
                                int x2 = COL - Integer.valueOf(array[3]) - 1;
                                // 我方当前的对方要走的棋行列坐标
                                int y1 = allChess[originalPos].getPosX();
                                int x1 = allChess[originalPos].getPosY();
                                // 存储要走向的坐标在棋盘的编号
                                int movePos = map[y2][x2];
                                // 将原来的位置置空
                                map[y1][x1] = NULL;
                                // 要走的位置设置为对方的棋编号
                                map[y2][x2] = originalPos;
                                // 更新其坐标
                                allChess[originalPos].setPos(y2, x2);
                                // 判断要走的位置是否有棋，若有，则置空
                                if (movePos != NULL && allChess[movePos] != null) {
                                    allChess[movePos] = null;
                                }
                                // 更新视图
                                updateOnUI();
                                // 我方可以下棋
                                canPlay = true;
                                setTip("我下");
                                break;
                            // 对方赢了
                            case "winner":
                                isWin = true;
                                whoWin();
                                break;
                            // 重新开始游戏
                            case "restart":
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

    // 设置提示信息
    private void setTip(final String s) {
        context.runOnUiThread(new Runnable() {
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

    // 判断是否可以移动
    private boolean canMove(int r2, int c2) {
        // 要移动的棋名
        String name = firstSelect.getName();
        // 存储第一次的行坐标
        int r1 = firstSelect.getPosX();
        // 存储第一次的列坐标
        int c1 = firstSelect.getPosY();

        if ("none".equals(name)) {
            // 只能直线移动
            if (r1 != r2 && c1 != c2) {
                return false;
            }
            // 只可移动一格
            if (Math.abs(c1 - c2) != 1 && Math.abs(r2 - r1) != 1) {
                return false;
            }
            return true;
        } else if ("link".equals(name) || "virus".equals(name)) {
            // 只能直线移动
            if (r1 != r2 && c1 != c2) {
                return false;
            }
            // 只能走一格
            if (Math.abs(c1 - c2) != 1 && Math.abs(r2 - r1) != 1) {
                return false;
            }
        }
        return true;
    }

    private void initMapAndChess() {
        // 将编号全置空
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                map[i][j] = NULL;
            }
        }
        // 16 个棋子在地图上的 x 坐标，红棋先
        // 前8个棋的 x 坐标
        int[] mapX = {0, 1, 2, 3, 4, 5, 6, 7};
        // 前8个棋的 y 坐标
        int[] mapY = {0, 0, 0, 1, 1, 0, 0, 0};
        // 前8个棋的棋名
        String[] strings = {"none", "none", "none", "none", "none", "none", "none", "none",
                    "none", "none", "none", "none", "none", "none", "none", "none"};
        // 临时存储行和列
        int row, col;
        for (int i = 0; i < allChess.length; i++) {
            // 小于8为红旗
            if (i < 8) {
                row = mapY[i];
                col = mapX[i];
                // 初始化棋子
                allChess[i] = new Chess(RED, strings[i], i);
                // 给相应的棋盘位置安排编号
                map[row][col] = i;
                // 设置棋子在棋盘中的初始位置
                allChess[i].setPos(row, col);
            } else {
                row = ROW - mapY[i - 8] - 1;
                col = COL - mapX[i - 8] - 1;
                allChess[i] = new Chess(BLACK, strings[i - 8], i);
                map[row][col] = i;
                allChess[i].setPos(row, col);
            }
        }
//        showChess();
    }

    //    private void showChess() {
//        String s;
//        for (int i = 0; i < ROW; i++) {
//            s = "";
//            for (int j = 0; j < COL; j++) {
//                s += map[i][j] + " ";
//            }
//            Log.d(TAG, "showChess: " + s);
//        }
//        for (int i = 0; i < allChess.length; i++) {
//            Log.d(TAG, "showChess: " + allChess[i].getName() + "-" + allChess[i].getNum() + "-" + allChess[i].getPosX() + "-" + allChess[i].getPosY());
//        }
//    }


//    public void startServer() {
//        // 将主活动的辅助控件隐藏掉
//        context.viewGone();
//        tvTip.setText("等待连接...");
//        // 开启接收信息的线程
//        new MessageThread().start();
//    }


    // 每次调用 updateOnUI 就会执行这个方法
    @Override
    protected void onDraw(Canvas canvas) {
        // 画笔，用于设置线条样式
        Paint paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        // 设置棋盘图片，宽高视手机分辨率而定
        canvas.drawBitmap(getBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.qipan2), 1080, 1285), 0, MARGINTOP, paint);
        // 画棋
        for (Chess allChes : allChess) {
            // 若没有被吃掉
            if (allChes != null) {
                // x 坐标为列坐标乘以格子的宽度然后减去一半的格子宽度，让棋子的中心对齐坐标顶点
                int x = allChes.getPosY() * W + MARGINLEFT;
                int y = allChes.getPosX() * W + MARGINTOP + MARGINLEFT + 100;
                canvas.drawBitmap(getBitmap(BitmapFactory.decodeResource(getResources(), allChes.getImageId()), W, W), x, y, paint);
            }
        }
        // 若第一次选择了则画一个矩阵边框来显示已选中
        if (firstSelect != null) {
            paint.setColor(Color.RED);
            int x = firstSelect.getPosY() * W + MARGINLEFT;
            int y = firstSelect.getPosX() * W + MARGINTOP + MARGINLEFT + 100;
            // 画线
            int[] posX = {x, x + W, x + W, x, x};
            int[] posY = {y, y, y + W, y + W, y};
            Path path = new Path();
            path.moveTo(posX[0], posY[0]);
            for (int i = 1; i < posX.length; i++) {
                path.lineTo(posX[i], posY[i]);
            }
            canvas.drawPath(path, paint);
        }
    }
    // 自定义图片宽高
    private Bitmap getBitmap(Bitmap rootImg, int w, int h) {
        int rW = rootImg.getWidth();
        int rH = rootImg.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(w * 1.0f / rW, h * 1.0f / rH);
        return Bitmap.createBitmap(rootImg, 0, 0, rW, rH, matrix, true);
    }
    // 重新开始游戏
    private void restartGame() {
        // 重新初始化棋盘
        initMapAndChess();
        // 若是黑棋则先下
        canPlay = true;
        String tip = "已重新开始游戏，我下";

        isWin = false;
        // 给提示，在线程中更新 UI 时需转到主线程上
        setTip(tip);
        // 刷新视图
        updateOnUI();
    }

    // 有赢家后
    private void whoWin() {
        canPlay = false;
        setTip("对方获胜");
    }
    // 更新视图
    private void updateOnUI() {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

}
