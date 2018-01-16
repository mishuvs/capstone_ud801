package com.weather.wallpaper.forecast.utilities;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Vaibhav on 9/13/2017.
 */

public class UnsplashImagesJsonUtils {

    private static final String LOG_TAG = UnsplashImagesJsonUtils.class.getSimpleName();

    private static final String UNSPLASH_LIST = "results";
    private static final String UNSPLASH_IMAGE_URL_LIST = "urls";
    private static final String UNSPLASH_FULL_IMAGE_URL = "full";
    private static final String UNSPLASH_SMALL_IMAGE_URL = "small";
    private static final String UNSPLASH_THUMBNAIL_IMAGE_URL = "thumb";
//    private static String UNSPLASH_MESSAGE_CODE;

    public static JSONArray getUnsplashImageUrlString(Context context, String unsplashJsonStr) throws JSONException {
        JSONObject unsplashJson = new JSONObject(unsplashJsonStr);

//        /* Is there an error? */
//        if (unsplashJson.has(UNSPLASH_MESSAGE_CODE)) {
//            int errorCode = unsplashJson.getInt(UNSPLASH_MESSAGE_CODE);
//
//            switch (errorCode) {
//                case HttpURLConnection.HTTP_OK:
//                    break;
//
//                case HttpURLConnection.HTTP_NOT_FOUND:
//                    /* Location invalid */
//                    return null;
//
//                default:
//                    /* Server probably down */
//                    return null;
//            }
//
        JSONObject imageUrlsObject = unsplashJson.getJSONObject(UNSPLASH_IMAGE_URL_LIST);
        JSONObject userInfo = unsplashJson.getJSONObject("user");
        String username = userInfo.getString("name");
        JSONObject userLinks = userInfo.getJSONObject("links");
        String linkString = userLinks.getString("html");

        JSONArray imageUrls = new JSONArray();
        imageUrls.put(0,imageUrlsObject.getString(UNSPLASH_FULL_IMAGE_URL));
        imageUrls.put(1,imageUrlsObject.getString(UNSPLASH_THUMBNAIL_IMAGE_URL));
        imageUrls.put(2,username);
        imageUrls.put(3,linkString);
        return imageUrls;
    }

}
