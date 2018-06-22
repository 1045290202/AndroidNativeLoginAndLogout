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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends SuccessActivity {
    private static MainActivity mainActivity = null;
    private Button signIn = null;
    protected final static String E = "e";
    private final static String TARGET = "http://sjk0106.cn/chatphp/signin.php";
    protected final static String CORRECTNESS = "1";
    protected final static String ERROR = "0";

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0: {
                    Toast.makeText(getApplicationContext(), "登录失败，请检查用户名或密码", Toast.LENGTH_SHORT).show();
                    signIn.setText("登录");
                    break;
                }
                case 1: {
                    Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, SuccessActivity.class);
                    startActivity(intent);
                    SharedPreferences.Editor editor = getSharedPreferences("signIn", MODE_PRIVATE).edit();
                    editor.putBoolean("loggedIn", true);
                    editor.apply();
                    finish();
                    break;
                }
                case 2: {
                    Toast.makeText(getApplicationContext(), "登录失败，请检查网络连接或稍后再试", Toast.LENGTH_SHORT).show();
                    signIn.setText("登录");
                }
                default: {
                    break;
                }
            }
            signIn.setClickable(true);
        }
    };

    public MainActivity() {
        mainActivity = this;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("signIn", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("loggedIn", false)) {
            setContentView(R.layout.activity_success);
            logOff();
        } else {
            setContentView(R.layout.activity_main);

            signIn = findViewById(R.id.sign_in);
            signIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signIn.setClickable(false);
                    final String userNameValue, passwordValue, passwordVal;
                    EditText userName = findViewById(R.id.user_name);
                    EditText password = findViewById(R.id.password);
                    userNameValue = userName.getText().toString();
                    passwordVal = password.getText().toString();
                    if (userNameValue.length() > 0 && passwordVal.length() > 0) {
                        signIn.setText("正在登录...");
                        passwordValue = getMD5(passwordVal);
                        if (E.equals(passwordValue)) {
                            Toast.makeText(getApplicationContext(), "错误，请稍后重试", Toast.LENGTH_SHORT).show();
                        } else {
                            final Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    signIn(userNameValue, passwordValue);
                                }
                            });
                            thread.start();
                        }
                    } else {
                        password.setText("");
                        signIn.setClickable(true);
                        Toast.makeText(getApplicationContext(), "未输入用户名或密码", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            TextView toRegister = findViewById(R.id.to_register);
            toRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    public void signIn(String userName, String password) {
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
                if (CORRECTNESS.equals(result)) {
                    message.what = 1;
                    handler.sendMessage(message);
                } else if (ERROR.equals(result)) {
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

    public static String getMD5(String info) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes("UTF-8"));
            byte[] encryption = md5.digest();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0, l = encryption.length; i < l; i++) {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1) {
                    stringBuilder.append("0").append(Integer.toHexString(0xff & encryption[i]));
                } else {
                    stringBuilder.append(Integer.toHexString(0xff & encryption[i]));
                }
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return ERROR;
        }
    }

    /*@Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }*/
}
