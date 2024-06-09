package com.example.net;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.example.net.R;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends ComponentActivity {

    private TextView txtMain;
    private Button serverBtn;
    private TextView txtClient;
    private EditText ipText;
    private Button clientBtn;
    private int ip = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        txtMain = (TextView) findViewById(R.id.txtMain);
        serverBtn = (Button) findViewById(R.id.serverBtn);
        txtClient = (TextView) findViewById(R.id.txtClient);
        ipText = (EditText) findViewById(R.id.ipText);
        clientBtn = (Button) findViewById(R.id.clientBtn);
        ipText.setText(getPreference("myIp"));
        // 显示本机的 ip 地址
        setIp();
        // 根据按钮来判断作为主机还是客户端
        final ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerView serverView = new ServerView(MainActivity.this, ip, MainActivity.this);
                addContentView(serverView, params);
                serverView.startConn();
                // 将当前控件隐藏掉
                viewGone();
            }
        });
        clientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ipText.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "IP 不能为空！", Toast.LENGTH_SHORT).show();
                    return;
                }
                ClientView clientView = new ClientView(MainActivity.this, ipText.getText().toString(), ip, MainActivity.this);
                addContentView(clientView, params);
                clientView.startJoin();
                // 将当前控件隐藏掉
                viewGone();
            }
        });
    }
    // 存储当前输入的 ip
    public void setMyIp() {
        setPreference("myIp", ipText.getText().toString());
    }

    // 用于获取本机 ip 地址
    public void setIp() {
        String s;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface face = en.nextElement();
                for (Enumeration<InetAddress> enAddr = face.getInetAddresses(); enAddr.hasMoreElements();) {
                InetAddress addr = enAddr.nextElement();
                if (!addr.isLoopbackAddress()) {
                    s = addr.getHostAddress();
                    // 只获取局域网地址
                    if ("192".equals(s.substring(0, 3))) {
                        txtMain.setText(txtMain.getText().toString() + "  主机ip: " + s);
                    }
                }
            }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    // 隐藏主活动的辅助空间
    public void viewGone() {
        txtMain.setVisibility(View.GONE);
        serverBtn.setVisibility(View.GONE);
        txtClient.setVisibility(View.GONE);
        ipText.setVisibility(View.GONE);
        clientBtn.setVisibility(View.GONE);
    }
    // 用于获取上次的ip输入地址
    public String getPreference(String key) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, "192.168.");
    }
    // 用于存储数据到本地
    public void setPreference(String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(key, value).apply();
    }
}

