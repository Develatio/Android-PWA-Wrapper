package com.develatio.androidpwawrapper;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;

import com.develatio.androidpwawrapper.ui.UIManager;
import com.develatio.androidpwawrapper.webview.WebViewHelper;

public class MainActivity extends AppCompatActivity {
    private WebViewHelper webViewHelper;
    private boolean intentHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Theme
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Helpers
        // Globals
        UIManager uiManager = new UIManager(this);
        webViewHelper = new WebViewHelper(this, uiManager);

        // Setup App
        webViewHelper.setupWebView();
        uiManager.changeRecentAppsIcon();

        // Check for Intents
        try {
            Intent i = getIntent();
            String intentAction = i.getAction();
            // Handle URLs opened in Browser
             if (!intentHandled && intentAction != null && intentAction.equals(Intent.ACTION_VIEW)){
                    Uri intentUri = i.getData();
                    String intentText = "";
                    if (intentUri != null){
                        intentText = intentUri.toString();
                    }
                    // Load up the URL specified in the Intent
                    if (!intentText.equals("")) {
                        intentHandled = true;
                        webViewHelper.loadIntentUrl(intentText);
                    }
             } else {
                 // Load up the Web App
                 webViewHelper.loadHome();
             }
        } catch (Exception e) {
            // Load up the Web App
            webViewHelper.loadHome();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == webViewHelper.FILECHOOSER_RESULTCODE) {
                if (webViewHelper.mFilePathCallback == null) {
                    return;
                }
                webViewHelper.mFilePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                webViewHelper.mFilePathCallback = null;
            }
        } else if (requestCode == webViewHelper.FILECHOOSER_RESULTCODE) {
            if (webViewHelper.mUploadMessage == null) {
                return;
            }
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            webViewHelper.mUploadMessage.onReceiveValue(result);
            webViewHelper.mUploadMessage = null;
        }
    }

    @Override
    protected void onPause() {
        webViewHelper.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        webViewHelper.onResume();
        // retrieve content from cache primarily if not connected
        webViewHelper.forceCacheIfOffline();
        super.onResume();
    }

    // Handle back-press in browser
    @Override
    public void onBackPressed() {
        if (!webViewHelper.goBack()) {
            super.onBackPressed();
        }
    }
}
