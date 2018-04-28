package com.yourorg.sample;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import java.net.*;
import java.io.*;

import com.example.netshare.WifiDirectShare;

public class MainActivity extends AppCompatActivity implements WifiDirectShare.GroupCreatedListener{



    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    //We just want one instance of node running in the background.
    public static boolean _startedNodeAlready=false;
    private WebView browser;
    private WifiDirectShare share;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context me = this;
        final Button serverButton = (Button) findViewById(R.id.ServerButton);
        browser = (WebView) findViewById(R.id.webview);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setJavaScriptEnabled(true);
        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox) ;



        //버튼 눌러서 서버가동하게 변경
        serverButton.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v) {

                        if( !_startedNodeAlready ) {
                            _startedNodeAlready = true;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //The path where we expect the node project to be at runtime.
                                    String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";
                                    if (wasAPKUpdated()) {
                                        //Recursively delete any existing nodejs-project.
                                        File nodeDirReference = new File(nodeDir);
                                        if (nodeDirReference.exists()) {
                                            deleteFolderRecursively(new File(nodeDir));
                                        }
                                        //Copy the node project from assets into the application's data path.
                                        copyAssetFolder(getApplicationContext().getAssets(), "nodejs-project", nodeDir);

                                        saveLastUpdateTime();
                                    }
                                    startNodeWithArguments(new String[]{"node",
                                            nodeDir + "/index.js"
                                    });
                                }
                            }).start();

                            serverButton.setText("서버가동중");

                        }

                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Network operations should be done in the background.
                                new AsyncTask<Void,Void,String>() {
                                    @Override
                                    protected String doInBackground(Void... params) {
                                        String nodeResponse="";
                                        try {
                                            URL localNodeServer = new URL("http://localhost:3000/");
                                            BufferedReader in = new BufferedReader(
                                                    new InputStreamReader(localNodeServer.openStream()));
                                            String inputLine;
                                            while ((inputLine = in.readLine()) != null)
                                                nodeResponse=nodeResponse+inputLine;
                                            in.close();
                                        } catch (Exception ex) {
                                            nodeResponse=ex.toString();
                                        }
                                        return nodeResponse;
                                    }
                                    @Override
                                    protected void onPostExecute(String result) {
                                        //  textViewVersions.setText(Html.fromHtml(result));
                                        browser.loadDataWithBaseURL("http://localhost:3000/",result,"text/html","UTF-8",null);
                                    }
                                }.execute();
                            }
                        },3000);


                    }
                }
        );


        final Button buttonVersions = (Button) findViewById(R.id.WiFiButton);
       // final TextView textViewVersions = (TextView) findViewById(R.id.tvVersions);

        buttonVersions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkBox.setChecked(true);
                if(!WifiCheck.isWifiEnabled(getApplicationContext())) {
                    registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(final Context context, final Intent intent) {
                            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                                boolean noConnectivity = intent.getBooleanExtra(
                                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                                if (!noConnectivity) {
                                    startShare();
                                    unregisterReceiver(this);
                                }
                            }
                        }
                    }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                    WifiCheck.enableWifi(getApplicationContext());
                } else
                    startShare();
            }

        });

        checkBox.setOnClickListener(new CheckBox.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()) {
                  share.stop();
                }
            }
        }) ;

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native Integer startNodeWithArguments(String[] arguments);

    private boolean wasAPKUpdated() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
        long previousLastUpdateTime = prefs.getLong("NODEJS_MOBILE_APK_LastUpdateTime", 0);
        long lastUpdateTime = 1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return (lastUpdateTime != previousLastUpdateTime);
    }

    private void saveLastUpdateTime() {
        long lastUpdateTime = 1;
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NODEJS_MOBILE_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("NODEJS_MOBILE_APK_LastUpdateTime", lastUpdateTime);
        editor.commit();
    }

    private static boolean deleteFolderRecursively(File file) {
        try {
            boolean res=true;
            for (File childFile : file.listFiles()) {
                if (childFile.isDirectory()) {
                    res &= deleteFolderRecursively(childFile);
                } else {
                    res &= childFile.delete();
                }
            }
            res &= file.delete();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            boolean res = true;

            if (files.length==0) {
                //If it's a file, it won't have any assets "inside" it.
                res &= copyAsset(assetManager,
                        fromAssetPath,
                        toPath);
            } else {
                new File(toPath).mkdirs();
                for (String file : files)
                res &= copyAssetFolder(assetManager,
                        fromAssetPath + "/" + file,
                        toPath + "/" + file);
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


    @Override
    public void onGroupCreated(String ssid, String password) {
        ((TextView) findViewById(R.id.text_view_info)).setText("SSID : "+ssid+ "\nPassword : "+password);
        //((TextView) findViewById(R.id.text_view_hint)).setText("After connecting, set the proxy settings to" +
         //       "\nhost : "+ Constant.DEFAULT_GROUP_OWNER_IP+"\nport : "+Constant.PROXY_PORT+"\non the other device.");
    }

    @Override
    protected void onDestroy() {
        share.stop();
        super.onDestroy();
    }


    protected void startShare() {
        share = new WifiDirectShare(MainActivity.this, this);
        share.start();
    }
}
