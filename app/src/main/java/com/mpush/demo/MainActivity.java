package com.mpush.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mpush.android.BuildConfig;
import com.mpush.android.MPush;
import com.mpush.android.Notifications;
import com.mpush.android.R;
import com.mpush.api.Constants;
import com.mpush.api.http.HttpCallback;
import com.mpush.api.http.HttpMethod;
import com.mpush.api.http.HttpRequest;
import com.mpush.api.http.HttpResponse;
import com.mpush.client.ClientConfig;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private String[] permissions = {Manifest.permission.READ_PHONE_STATE};
    protected static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifications.I.init(this.getApplicationContext());
        Notifications.I.setSmallIcon(R.mipmap.ic_notification);
        Notifications.I.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        SharedPreferences sp = this.getSharedPreferences("mpush.cfg", Context.MODE_PRIVATE);
        String alloc = sp.getString("allotServer", null);
        if (alloc != null) {
            EditText et = (EditText) findViewById(R.id.alloc);
            et.setText(alloc);
        }

        boolean allPermissionsGranted = checkPermissions(permissions);
        if (!allPermissionsGranted) {
            // 有权限未授权
            requestPermissions(permissions);
        }
    }

    //请求权限
    protected void requestPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return;
        }
        ActivityCompat.requestPermissions(this, permissions, ACTION_REQUEST_PERMISSIONS);
    }

    // 检测权限
    protected boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this.getApplicationContext(), neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    private void initPush(String allocServer, String userId, String alias, String tags) {
        //公钥有服务端提供和私钥对应
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCghPCWCobG8nTD24juwSVataW7iViRxcTkey/B792VZEhuHjQvA3cAJgx2Lv8GnX8NIoShZtoCg3Cx6ecs+VEPD2fBcg2L4JK7xldGpOJ3ONEAyVsLOttXZtNXvyDZRijiErQALMTorcgi79M5uVX9/jMv2Ggb2XAeZhlLD28fHwIDAQAB";

        ClientConfig cc = ClientConfig.build()
                .setPublicKey(publicKey)
                .setAllotServer(allocServer)
                .setDeviceId(getDeviceId())
                .setClientVersion(BuildConfig.VERSION_NAME)
                .setLogger(new MyLog(this, (EditText) findViewById(R.id.log)))
                .setLogEnabled(BuildConfig.DEBUG)
                .setEnableHttpProxy(true)
                .setUserId(userId)
                .setAlias(alias)
                .setTags(tags);
        MPush.I.checkInit(getApplicationContext()).setClientConfig(cc);
    }

    private String getDeviceId() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Activity.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions);
            return null;
        }
        String deviceId = tm.getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            String time = Long.toString((System.currentTimeMillis() / (1000 * 60 * 60)));
            deviceId = time + time;
        }
        return deviceId;
    }

    public void bindUser(View btn) {
        EditText etUser = (EditText) findViewById(R.id.from_userId);
        String userId = etUser.getText().toString().trim();
        EditText etAlias = (EditText) findViewById(R.id.from_alias);
        String alias = etAlias.getText().toString().trim();
        EditText etTags = (EditText) findViewById(R.id.from_tags);
        String tags = etTags.getText().toString().trim();
        if (!TextUtils.isEmpty(userId)) {
            MPush.I.bindAccount(userId, alias, tags);
        }
    }

    public void startPush(View btn) {
        EditText et = (EditText) findViewById(R.id.alloc);
        String allocServer = et.getText().toString().trim();

        if (TextUtils.isEmpty(allocServer)) {
            Toast.makeText(this, "请填写正确的alloc地址", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!allocServer.startsWith("http://")) {
            allocServer = "http://" + allocServer;
        }


        EditText etUser = (EditText) findViewById(R.id.from_userId);
        String userId = etUser.getText().toString().trim();
        EditText etAlias = (EditText) findViewById(R.id.from_alias);
        String alias = etAlias.getText().toString().trim();
        EditText etTags = (EditText) findViewById(R.id.from_tags);
        String tags = etTags.getText().toString().trim();

        initPush(allocServer, userId, alias, tags);

        MPush.I.checkInit(this.getApplication()).startPush();
        Toast.makeText(this, "start push", Toast.LENGTH_SHORT).show();
    }

    public void sendPush(View btn) throws Exception {
        EditText et1 = (EditText) findViewById(R.id.alloc);
        String allocServer = et1.getText().toString().trim();

        if (TextUtils.isEmpty(allocServer)) {
            Toast.makeText(this, "请填写正确的alloc地址", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!allocServer.startsWith("http://")) {
            allocServer = "http://" + allocServer;
        }

        EditText toET = (EditText) findViewById(R.id.to_userId);
        String to = toET.getText().toString().trim();

        EditText toETAlias = (EditText) findViewById(R.id.to_alias);
        String toAlias = toETAlias.getText().toString().trim();

        EditText toETTags = (EditText) findViewById(R.id.to_tags);
        String toTags = toETTags.getText().toString().trim();

        EditText fromET = (EditText) findViewById(R.id.from_userId);
        String from = fromET.getText().toString().trim();

        EditText helloET = (EditText) findViewById(R.id.httpProxy);
        String hello = helloET.getText().toString().trim();

        if (TextUtils.isEmpty(hello)) hello = "hello";

        JSONObject params = new JSONObject();
        if(to!=null && to.length()>0){
            params.put("userId", to);
        }
        if(toAlias!=null && toAlias.length()>0){
            params.put("alias", toAlias);
        }
        if(toTags!=null && toTags.length()>0){
            params.put("tags", toTags);
        }
        params.put("title", "新消息");
        params.put("content", from + " say:" + hello);

        final Context context = this.getApplicationContext();
        HttpRequest request = new HttpRequest(HttpMethod.POST, allocServer + "/push");
        byte[] body = params.toString().getBytes(Constants.UTF_8);
        request.setBody(body, "application/json; charset=utf-8");
        request.setTimeout((int) TimeUnit.SECONDS.toMillis(10));
        request.setCallback(new HttpCallback() {
            @Override
            public void onResponse(final HttpResponse httpResponse) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (httpResponse.statusCode == 200) {
                            Toast.makeText(context, new String(httpResponse.body, Constants.UTF_8), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, httpResponse.reasonPhrase, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled() {

            }
        });
        MPush.I.sendHttpProxy(request);
    }

    public void stopPush(View btn) {
        MPush.I.stopPush();
        Toast.makeText(this, "stop push", Toast.LENGTH_SHORT).show();
    }

    public void pausePush(View btn) {
        MPush.I.pausePush();
        Toast.makeText(this, "pause push", Toast.LENGTH_SHORT).show();
    }

    public void resumePush(View btn) {
        MPush.I.resumePush();
        Toast.makeText(this, "resume push", Toast.LENGTH_SHORT).show();
    }

    public void unbindUser(View btn) {
        MPush.I.unbindAccount();
        Toast.makeText(this, "unbind user", Toast.LENGTH_SHORT).show();
    }
}
