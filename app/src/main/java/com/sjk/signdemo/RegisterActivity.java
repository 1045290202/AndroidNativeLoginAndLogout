package com.sjk.signdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RegisterActivity extends BaseActivity {
    Button register = null;
    EditText userName = null;
    EditText password = null;
    EditText check = null;
    private final static String TARGET = "http://sjk0106.cn/chatphp/signup.php";

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0: {
                    Toast.makeText(getApplicationContext(), "注册失败，该用户已存在", Toast.LENGTH_SHORT).show();
                    userName.setText("");
                    password.setText("");
                    check.setText("");
                    register.setText("注册");
                    break;
                }
                case 1: {
                    Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, SuccessActivity.class);
                    startActivity(intent);
                    SharedPreferences.Editor editor = getSharedPreferences("signIn", MODE_PRIVATE).edit();
                    editor.putBoolean("loggedIn", true);
                    editor.apply();
                    MainActivity.getMainActivity().finish();
                    finish();
                    break;
                }
                case 2: {
                    Toast.makeText(getApplicationContext(), "注册失败，请检查网络连接或稍后再试", Toast.LENGTH_SHORT).show();
                    register.setText("注册");
                }
                default: {
                    break;
                }
            }
            register.setClickable(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextView nowSignIn = findViewById(R.id.now_sign_in);
        nowSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        register = findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register.setClickable(false);
                final String userNameValue, passwordValue, passwordVal, checkValue;
                userName = findViewById(R.id.user_name_register);
                password = findViewById(R.id.password_register);
                check = findViewById(R.id.check_register);
                userNameValue = userName.getText().toString();
                passwordVal = password.getText().toString();
                checkValue = check.getText().toString();
                if (userNameValue.length() > 0 && passwordVal.length() > 0 && checkValue.length() > 0) {
                    if (passwordVal.equals(checkValue)) {
                        if (!passwordVal.equals(userNameValue)) {
                            register.setText("正在注册...");
                            passwordValue = MainActivity.getMD5(passwordVal);
                            if (MainActivity.E.equals(passwordValue)) {
                                Toast.makeText(getApplicationContext(), "错误，请稍后重试", Toast.LENGTH_SHORT).show();
                            } else {
                                final Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        register(userNameValue, passwordValue);
                                    }
                                });
                                thread.start();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "用户名与密码不能一致", Toast.LENGTH_SHORT).show();
                            password.setText("");
                            check.setText("");
                            register.setClickable(true);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                        password.setText("");
                        check.setText("");
                        register.setClickable(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "请填完整信息", Toast.LENGTH_SHORT).show();
                    register.setClickable(true);
                }
            }
        });
    }

    public void register(String userName, String password) {
        Message message = new Message();
        try {
            URL url = new URL(TARGET);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setReadTimeout(5000);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            String keyAndValue = "user_name=" + URLEncoder.encode(userName, "utf-8") + "&passwd=" + URLEncoder.encode(password, "utf-8");
            Log.d("要发送的数据", keyAndValue);
            DataOutputStream out = new DataOutputStream(httpURLConnection.getOutputStream());
            out.writeBytes(keyAndValue);
            out.flush();
            out.close();
            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStreamReader in = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(in);
                String inputLine, result = "";
                while ((inputLine = bufferedReader.readLine()) != null) {
                    result += inputLine;
                }
                in.close();
                Log.d("服务器返回的数据", result);
                if (MainActivity.CORRECTNESS.equals(result)) {
                    message.what = 1;
                    handler.sendMessage(message);
                } else if (MainActivity.ERROR.equals(result)) {
                    message.what = 0;
                    handler.sendMessage(message);
                }
            }
            httpURLConnection.disconnect();
        } catch (IOException e) {
            message.what = 2;
            handler.sendMessage(message);
        }
    }
}
