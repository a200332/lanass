package com.host900.lanass;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import snail007.proxysdk.Proxysdk;

public class MainActivity extends AppCompatActivity {
    TextView server, port, key, wspwd, ssport, sspwd, status, poweredBy;
    Button cmdstart, cmdstop;
    AppCompatActivity app;
    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getSupportActionBar().setTitle(getString(R.string.title));
        check();
        initUI();
        stoppedUI();
        bindEnvent();
    }

    void check() {
        //检测通知使用权是否启用
        if (!isNotificationListenersEnabled()) {
            //跳转到通知使用权页面
            gotoNotificationAccessSetting();
        }
        //重启监听服务
        toggleNotificationListenerService(this);
    }

    //各种权限的判断
    private void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, NeNotificationService01.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(context, NeNotificationService01.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        //Toast.makeText(MainActivity.this, "监听服务启动中...", Toast.LENGTH_SHORT).show();
    }

    public boolean isNotificationListenersEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean gotoNotificationAccessSetting() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;

        } catch (ActivityNotFoundException e) {//普通情况下找不到的时候需要再特殊处理找一次
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$NotificationAccessSettingsActivity");
                intent.setComponent(cn);
                intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
                startActivity(intent);
                return true;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Toast.makeText(this, "对不起，您的手机暂不支持", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
    }

    void bindEnvent() {
        poweredBy.setOnClickListener(openURL("https://github.com/snail007/goproxy/"));

        cmdstart.setOnClickListener(new View.OnClickListener() {
            final String ip = server.getText().toString();
            final String portArg = port.getText().toString();
            final String pwd = wspwd.getText().toString();
            final String clientKey = key.getText().toString();
            final String ssportArg = ssport.getText().toString();
            final String sspwdArg = sspwd.getText().toString();

            @Override
            public void onClick(View v) {
                if(ip.isEmpty()||portArg.isEmpty()||pwd.isEmpty()||clientKey.isEmpty()||ssportArg.isEmpty()||sspwdArg.isEmpty()){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(app,"请输入必要的参数！", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                String args = String.format("client -P %s:%s -T ws --ws-password %s --timeout 15000 --k %s", ip, portArg, pwd, clientKey);
                String args1 = String.format("sps --disable-http --disable-socks -t tcp -h aes-256-cfb -j %s -p 127.0.0.1:%s --timeout 15000", sspwdArg, ssportArg);
                Log.d(TAG, args);
                Log.d(TAG, args1);
                final String err = Proxysdk.start("client", args, "");
                final String err1 = Proxysdk.start("sps", args1, "");
                if (!err.isEmpty() || !err1.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(app, err + err1, Toast.LENGTH_LONG).show();
                            Proxysdk.stop("client");
                            Proxysdk.stop("sps");
                            stoppedUI();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            startedUI();
                        }
                    });
                }
            }
        });
        cmdstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Proxysdk.stop("client");
                        Proxysdk.stop("sps");
                        stoppedUI();
                    }
                });
            }
        });
    }

    public View.OnClickListener openURL(final String u) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse(u);
                intent.setData(content_url);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        };
    }

    void startedUI() {
        cmdstart.setEnabled(false);
        cmdstop.setEnabled(true);
        server.setEnabled(false);
        port.setEnabled(false);
        key.setEnabled(false);
        wspwd.setEnabled(false);
        ssport.setEnabled(false);
        sspwd.setEnabled(false);
        status.setText("运行中");
    }

    void stoppedUI() {
        cmdstart.setEnabled(true);
        cmdstop.setEnabled(false);
        server.setEnabled(true);
        port.setEnabled(true);
        key.setEnabled(true);
        wspwd.setEnabled(true);
        ssport.setEnabled(true);
        sspwd.setEnabled(true);
        status.setText("已停止");
    }

    void initUI() {
        app = this;
        cmdstart = findViewById(R.id.button);
        cmdstop = findViewById(R.id.button2);
        poweredBy = findViewById(R.id.textView12);
        poweredBy.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        poweredBy.getPaint().setAntiAlias(true);//抗锯齿

        SharedPreferences config = getSharedPreferences("config", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = config.edit();

        final String args_server = config.getString("server", "");
        final String args_port = config.getString("port", "");
        final String args_key = config.getString("key", "mobile");
        final String args_wspwd = config.getString("wspwd", "");
        final String args_ssport = config.getString("ssport", "");
        final String args_sspwd = config.getString("sspwd", "");

        server = findViewById(R.id.editText);
        port = findViewById(R.id.editText2);
        key = findViewById(R.id.editText4);
        wspwd = findViewById(R.id.editText3);
        ssport = findViewById(R.id.editText5);
        sspwd = findViewById(R.id.editText6);
        status = findViewById(R.id.textView11);

        server.setText(args_server);
        port.setText(args_port);
        key.setText(args_key);
        wspwd.setText(args_wspwd);
        ssport.setText(args_ssport);
        sspwd.setText(args_sspwd);

        server.addTextChangedListener(watcher(editor, "server", (EditText) server));
        port.addTextChangedListener(watcher(editor, "port", (EditText) port));
        key.addTextChangedListener(watcher(editor, "key", (EditText) key));
        wspwd.addTextChangedListener(watcher(editor, "wspwd", (EditText) wspwd));
        ssport.addTextChangedListener(watcher(editor, "ssport", (EditText) ssport));
        sspwd.addTextChangedListener(watcher(editor, "sspwd", (EditText) sspwd));
    }

    public TextWatcher watcher(final SharedPreferences.Editor editor, final String key, final EditText editText) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                editor.putString(key, editText.getText().toString());
                editor.commit();
            }
        };
    }

}
