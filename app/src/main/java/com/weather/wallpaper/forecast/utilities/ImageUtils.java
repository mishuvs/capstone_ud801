package com.weather.wallpaper.forecast.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.weather.wallpaper.forecast.MainActivity;
import com.weather.wallpaper.forecast.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Vaibhav on 9/16/2017.
 */

@GlideModule
public class ImageUtils extends AppGlideModule{
    private static String fullImageUrl,smallImageUrl, username, userProfileLink;
    private static final String TAG = ImageUtils.class.getSimpleName();

    public static void setRealImage(final Context context, final ImageView realImage, final ImageView blurredImage, int imageNumber){

        JSONArray urlString = getUrlStringFromUrlJson(context,"imagesSet",imageNumber);
        if(urlString==null){
            return;
        }
        try {
            fullImageUrl = urlString.getString(0);
            smallImageUrl = urlString.getString(1);
            username = urlString.getString(2);
            userProfileLink = urlString.getString(3) + "?utm_source=weatherapprobux&utm_medium=referral&utm_campaign=api-credit";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GlideApp.with(realImage.getContext())
                .load(fullImageUrl)
                .placeholder(realImage.getDrawable())
                //TODO:set ERROR and FALLBACK images as well
                .thumbnail(GlideApp.with(realImage.getContext())
                                .load(smallImageUrl)
                                .override(realImage.getWidth(),realImage.getHeight())
                                .centerCrop()
                )
                .override(realImage.getWidth(),realImage.getHeight())
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                        if(!isFirstResource){
//                            realImage.setImageDrawable(resource);
//                            setBlurredImage(realImage.getContext(),resource,blurredImage);
//                        }
                        realImage.setImageDrawable(resource);
                        setBlurredImage(realImage.getContext(),resource,blurredImage);
                        return true;
                    }
                })
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(realImage);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity mainActivity = (MainActivity) context;
                View view0 = mainActivity.todayLayoutManager.findViewByPosition(0);
                if(view0!=null){
                    TextView photoCredit = view0.findViewById(R.id.photo_credits);
                    photoCredit.setText(username);
                    photoCredit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent implicit = new Intent(Intent.ACTION_VIEW, Uri.parse(userProfileLink));
                            context.startActivity(implicit);
                        }
                    });
                }
            }
        }, 500);

    }

    private static void setBlurredImage(Context context, Drawable realImageDrawable, ImageView blurredImage) {
        if(blurredImage!=null){
            BitmapDrawable background = (BitmapDrawable) realImageDrawable;
            Bitmap bitmapBlurred = Blur.fastblur(context, background.getBitmap(), 25);
            Drawable drawableBlurred = new BitmapDrawable(context.getResources(), bitmapBlurred);
            blurredImage.setImageDrawable(drawableBlurred);
        }
    }

    public static void unsetBlurredImage(ImageView blurredImage){
        blurredImage.setImageResource(0);
    }

    public static void saveUrlJson(Context context, JSONObject urlJson, String key){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(key, urlJson.toString()).apply();
    }

    static JSONArray getUrlStringFromUrlJson(Context context, String key, int imageNumber){
        try {
            JSONObject urlJson = new JSONObject(PreferenceManager.getDefaultSharedPreferences(context).getString(key,"[]"));
            return urlJson.getJSONArray(Integer.toString(imageNumber));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
