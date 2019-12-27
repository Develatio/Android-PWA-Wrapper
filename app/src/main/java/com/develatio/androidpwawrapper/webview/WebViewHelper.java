package com.develatio.androidpwawrapper.webview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.develatio.androidpwawrapper.Constants;
import com.develatio.androidpwawrapper.R;
import com.develatio.androidpwawrapper.ui.UIManager;

public class WebViewHelper {
    // Instance variables
    private final Activity activity;
    private final UIManager uiManager;
    private final WebView webView;
    private final WebSettings webSettings;

    public ValueCallback<Uri[]> mFilePathCallback;
    public ValueCallback<Uri> mUploadMessage;
    public int FILECHOOSER_RESULTCODE = 1;

    public WebViewHelper(Activity activity, UIManager uiManager) {
        this.activity = activity;
        this.uiManager = uiManager;
        this.webView = activity.findViewById(R.id.webView);
        this.webSettings = webView.getSettings();
    }

    /**
     * Simple helper method checking if connected to Network.
     * Doesn't check for actual Internet connection!
     * @return {boolean} True if connected to Network.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Wifi or Mobile Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }

    // manipulate cache settings to make sure our PWA gets updated
    private void useCache(Boolean use) {
        if (use) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
    }

    // public method changing cache settings according to network availability.
    // retrieve content from cache primarily if not connected,
    // allow fetching from web too otherwise to get updates.
    public void forceCacheIfOffline() {
        useCache(!isNetworkAvailable());
    }

    // handles initial setup of webview
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebView() {
        // accept cookies
        CookieManager.getInstance().setAcceptCookie(true);
        // enable JS
        webSettings.setJavaScriptEnabled(true);
        // must be set for our js-popup-blocker:
        webSettings.setSupportMultipleWindows(true);

        // File access
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // PWA settings
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCachePath(activity.getApplicationContext().getCacheDir().getAbsolutePath());
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // enable mixed content mode conditionally
        if (Constants.ENABLE_MIXED_CONTENT
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        // retrieve content from cache primarily if not connected
        forceCacheIfOffline();

        // make it possible to detect the version of the app
        class JsObject {
            @NonNull
            @JavascriptInterface
            public String toString() {
                return Constants.APP_VERSION;
            }
        }
        webView.addJavascriptInterface(new JsObject(), "app_version");
        String userAgent = webSettings.getUserAgentString();
        userAgent = userAgent + " " + Constants.APP_VERSION;
        webSettings.setUserAgentString(userAgent);

        // enable HTML5-support
        webView.setWebChromeClient(new WebChromeClient() {
            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                activity.startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = null;
                }

                mFilePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                intent.setType("*/*");
                try {
                    activity.startActivityForResult(intent, FILECHOOSER_RESULTCODE);
                } catch (ActivityNotFoundException e) {
                    mFilePathCallback = null;
                    Toast.makeText(activity.getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            //simple yet effective redirect/popup blocker
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                Message href = view.getHandler().obtainMessage();
                view.requestFocusNodeHref(href);
                final String popupUrl = href.getData().getString("url");
                if (popupUrl != null) {
                    //it's null for most rouge browser hijack ads
                    webView.loadUrl(popupUrl);
                    return true;
                }
                return false;
            }

            // update ProgressBar
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                uiManager.setLoadingProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });

        // Set up Webview client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                handleUrlLoad(view, url);
            }

            // handle loading error by showing the offline screen
            @Deprecated
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    handleLoadError(errorCode);
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // new API method calls this on every error for each resource.
                    // we only want to interfere if the page itself got problems.
                    String url = request.getUrl().toString();
                    if (view.getUrl().equals(url)) {
                        handleLoadError(error.getErrorCode());
                    }
                }
            }
        });
    }

    // Lifecycle callbacks
    public void onPause() {
        webView.onPause();
    }

    public void onResume() {
        webView.onResume();
    }

    // show "no app found" dialog
    private void showNoAppDialog(Activity thisActivity) {
        new AlertDialog.Builder(thisActivity)
            .setTitle(R.string.noapp_heading)
            .setMessage(R.string.noapp_description)
            .show();
    }
    // handle load errors
    private void handleLoadError(int errorCode) {
        if (errorCode != WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
            uiManager.setOffline(true);
        } else {
            // Unsupported Scheme, recover
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    goBack();
                }
            }, 100);
        }
    }

    // handle external urls
    private void handleUrlLoad(WebView view, String url) {
        // prevent loading content that isn't ours
        if (!url.startsWith(Constants.WEBAPP_URL)) {
            // stop loading
            view.stopLoading();
            // stopping only would cause the PWA to freeze, need to reload the app as a workaround
            //view.reload();

            // open external URL in Browser/3rd party apps instead
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(intent);
                } else {
                    showNoAppDialog(activity);
                }
            } catch (Exception e) {
                showNoAppDialog(activity);
            }
            // return value for shouldOverrideUrlLoading
        } else {
            // let WebView load the page!
            // activate loading animation screen
            uiManager.setLoading(true);
            // return value for shouldOverrideUrlLoading
        }
    }

    // handle back button press
    public boolean goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    // load app startpage
    public void loadHome() {
        webView.loadUrl(Constants.WEBAPP_URL);
    }

    // load URL from intent
    public void loadIntentUrl(String url) {
        if (!url.equals("") && url.contains(Constants.WEBAPP_HOST)) {
            webView.loadUrl(url);
        } else {
            // Fallback
            loadHome();
        }
    }
}
