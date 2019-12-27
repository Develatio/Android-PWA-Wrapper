package com.develatio.androidpwawrapper;

public class Constants {
    public Constants(){}
    // Root page
    public static final String WEBAPP_URL = "https://develat.io";
    public static final String WEBAPP_HOST = "develat.io"; // used for checking Intent-URLs
    public static final String APP_VERSION = "AndroidApp/" + BuildConfig.VERSION_NAME;

	// Constants
    // window transition duration in ms
    public static final int SLIDE_EFFECT = 2200;
    // show your app when the page is loaded XX %.
    // lower it, if you've got server-side rendering (e.g. to 35),
    // bump it up to ~98 if you don't have SSR or a loading screen in your web app
    public static final int PROGRESS_THRESHOLD = 65;
    // turn on/off mixed content (both https+http within one page) for API >= 21
    public static final boolean ENABLE_MIXED_CONTENT = true;
}
