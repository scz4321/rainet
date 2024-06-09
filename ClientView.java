package com.example.net;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ClientView extends View {
    public static final int TOP = 200;
    public static final int MARGINLEFT = 50, MARGINTOP = 100 + TOP;
    public static final int W = 70;
    public static final int CHESSW = 62;
    // 14个格子，15根线
    public static final int L = 15;
    public static final int BLOCKL = 14;
    public static final int BLACK = 2, WHITE = 1;

    private int width = W * BLOCKL;
    private int height = W * BLOCKL;
    private int player;

    private List<String> allList = new ArrayList<>();
    private List<String> myList = new ArrayList<>();
    private List<String> enemyList = new ArrayList<>();

    private boolean canPlay;
    private boolean isMove;

    private Socket socket;
    private int port;
    private String ip;
    private MainActivity mainActivity;
    private PrintWriter pw;
    private TextView tvTip;
    private Button btnRestart;

    public ClientView(Context context, String ip, int port, MainActivity mainActivity) {
        super(context);
        this.port = port;
        this.ip = ip;
        this.mainActivity = mainActivity;
        initMap();
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvTip = new TextView(context);
        tvTip.setText("连接中");
        tvTip.setX(500);
        tvTip.setY(20);
        mainActivity.addContentView(tvTip, params);
        btnRestart = new Button(context);
        btnRestart.setText("重新开始");
        btnRestart.setX(MARGINLEFT);
        btnRestart.setY(20);
        btnRestart.setEnabled(false);
        mainActivity.addContentView(btnRestart, params);
        btnRestart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                restartGame();
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
                    case MotionEvent.ACTION_MOVE:
                        isMove = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!canPlay) {
                            break;
                        }

                        if (!isMove) {
                            int x = (int) event.getX() - MARGINLEFT;
                            int y = (int) event.getY() - MARGINTOP;
                            if (x < -W / 2 || x > width + W / 2 || y < -W / 2 || y > width + W / 2) {
                                break;
                            }
                            // column
                            x = x / W + (x % W > W / 2 ? 1 : 0);
                            // row
                            y = y / W + (y % W > W / 2 ? 1 : 0);
                            String move = y + "|" + x;

                            if (allList.contains(move)) {
                                break;
                            }
                            allList.add(move);
                            myList.add(move);
                            sendMes("move|" + move);
                            canPlay = false;
                            tvTip.setText("对方下");
                            invalidate();
                            if (isWin()) {
                                Toast.makeText(getContext(), "白棋获胜！", Toast.LENGTH_SHORT).show();
                                tvTip.setText("我方获胜！");
                                sendMes("win");
                            }
                        }
                }
                return true;
            }
        });
    }

    public void startJoin() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ip, port);
                    setTip("已连接");
                    // 存储当前输入的 ip
                    mainActivity.setMyIp();
                    sendMes("join|");
                    new MyThread().start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }

    private boolean isWin() {
        return isCanLink();
    }

    private boolean isCanLink() {
        // 黑棋先，服务端为黑棋，客户端为白棋
        // 判断最后下的是谁
        int who = allList.size() % 2;

        String[] t = allList.get(allList.size() - 1).split("\\|");
        int r1 = Integer.valueOf(t[0]);
        int c1 = Integer.valueOf(t[1]);

        if (canVLink(who, r1, c1)) {
            return true;
        }
        if (canHLink(who, r1, c1)) {
            return true;
        }
        if (canLeftObliqueLink(who, r1, c1)) {
            return true;
        }
        if (canRightObliqueLink(who, r1, c1)) {
            return true;
        }
        return false;
    }

    private boolean canRightObliqueLink(int who, int r1, int c1) {
        int count = 0;
        int r2;
        int c2;
        if (who == 0) {
            // left top
            r2 = r1 - 1;
            c2 = c1 - 1;
            while (myList.contains(r2 + "|" + c2)) {
                count++;
                r2--;
                c2--;
            }
            // right down
            r2 = r1 + 1;
            c2 = c1 + 1;
            while (myList.contains(r2 + "|" + c2)) {
                count++;
                r2++;
                c2++;
            }
            if (count >= 4) {
                return true;
            }
        } else {
            // right top
            r2 = r1 - 1;
            c2 = c1 - 1;
            while (enemyList.contains(r2 + "|" + c2)) {
                count++;
                r2--;
                c2--;
            }
            // left down
            r2 = r1 + 1;
            c2 = c1 + 1;
            while (enemyList.contains(r2 + "|" + c2)) {
                count++;
                r2++;
                c2++;
            }
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }

    private boolean canLeftObliqueLink(int who, int r1, int c1) {
        int count = 0;
        int r2;
        int c2;
        if (who == 0) {
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

    private boolean canHLink(int who, int r1, int c1) {
        int count = 0;
        int c2;
        if (who == 0) {
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

    private boolean canVLink(int who, int r1, int c1) {
        int count = 0;
        int r2;
        if (who == 0) {
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

    class MyThread extends Thread {
        @Override
        public void run() {
            // move|r|c
            // join|
            // conn|
            // quit
            BufferedReader br = null;
            InputStreamReader isr = null;
            try {
                String t;
                while (true) {
                    sleep(100);
                    isr = new InputStreamReader(socket.getInputStream());
                    br = new BufferedReader(isr);
                    if (br.ready()) {
                        String cmd = br.readLine();
                        String[] array = cmd.split("\\|");
                        switch (array[0]) {
                            case "conn":
                                // 客户端一定为白棋
                                player = WHITE;
                                // 主机先下
                                canPlay = false;
                                setTip("对方下");
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnRestart.setEnabled(true);
                                        Toast.makeText(mainActivity, "你是白棋", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case "move":
                                t = array[1] + "|" + array[2];
                                allList.add(t);
                                enemyList.add(t);
                                canPlay = true;
                                setTip("我下");
                                if (isWin()) {
                                    post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "黑棋获胜!", Toast.LENGTH_SHORT).show();
                                            tvTip.setText("对方获胜!");
                                            canPlay = false;
                                        }
                                    });
                                }
                                invalidate();
                                break;
                            case "restart":
                                restartGame();
                                break;
                            case "win":
                                break;
                            case "quit":
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

    private void initMap() {
        allList.clear();
        myList.clear();
        enemyList.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw background
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#FFE869"));
        canvas.drawRect(0, TOP, 1080, 1400 + TOP, paint);
        // draw line
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        for (int i = 0; i < L; i++) {
            int hx = MARGINLEFT;
            int hy = MARGINTOP + i * W;
            int vx = MARGINLEFT + i * W;
            int vy = MARGINTOP;
            canvas.drawLine(hx, hy, MARGINLEFT + width, hy, paint);
            canvas.drawLine(vx, vy, vx, MARGINTOP + height, paint);
        }
        RectF rectF = new RectF();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int w = 20;
                int x = MARGINLEFT + W * 3 + j * W * 4 - w / 2;
                int y = MARGINTOP + W * 3 + i * W * 4 - w / 2;
                rectF.set(x, y, x + 20, y + 20);
                canvas.drawOval(rectF, paint);
            }
        }
        // draw chess
        String[] t;
        int r, c;
        for (int i = 0; i < allList.size(); i++) {
            if (i % 2 == 0) {
                paint.setColor(Color.BLACK);
            } else {
                paint.setColor(Color.WHITE);
            }
            t = allList.get(i).split("\\|");
            r = Integer.valueOf(t[0]);
            c = Integer.valueOf(t[1]);
            c = MARGINLEFT + c * W - CHESSW / 2;
            r = MARGINTOP + r * W - CHESSW / 2;
            rectF.set(c, r, c + CHESSW, r + CHESSW);
            canvas.drawOval(rectF, paint);
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

    public void restartGame() {
        allList.clear();
        myList.clear();
        enemyList.clear();
        canPlay = false;
        setTip("对方下");
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }
}
