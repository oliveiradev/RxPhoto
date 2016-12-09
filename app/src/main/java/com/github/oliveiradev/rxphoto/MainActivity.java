package com.github.oliveiradev.rxphoto;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.github.oliveiradev.lib.RxPhoto;
import com.github.oliveiradev.lib.shared.TypeRequest;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class MainActivity extends AppCompatActivity {

    private Subscription subscription = Subscriptions.empty();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LinearLayout thumbsContent = (LinearLayout) findViewById(R.id.thumbs);
        final ImageView image = (ImageView) findViewById(R.id.image);

        findViewById(R.id.get).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                RxPhoto.requestBitmap(v.getContext(), TypeRequest.GALLERY)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                image.setImageBitmap(bitmap);
                            }
                        })
                        .subscribe();
            }
        });

        findViewById(R.id.take).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                subscription.unsubscribe();
                subscription = RxPhoto.requestBitmap(v.getContext(), TypeRequest.CAMERA)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                image.setImageBitmap(bitmap);
                            }
                        })
                        .subscribe();
            }
        });

        findViewById(R.id.get_thumb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                subscription.unsubscribe();
                subscription = RxPhoto.requestThumbnails(v.getContext(), TypeRequest.GALLERY,
                        new Pair(60, 60), new Pair(120, 120), new Pair(240, 240))
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                final ImageView newImage = new ImageView(MainActivity.this);
                                newImage.setImageBitmap(bitmap);
                                newImage.setPadding(10,10,10,10);
                                thumbsContent.addView(newImage);
                            }
                        })
                        .subscribe();
            }
        });

        findViewById(R.id.transform).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                subscription.unsubscribe();
                subscription = RxPhoto.requestBitmap(v.getContext(), TypeRequest.GALLERY)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap(RxPhoto.transformToThumbnail(new Pair(240, 240), new Pair(120, 120), new Pair(60, 60)))
                        .doOnNext(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                final ImageView newImage = new ImageView(MainActivity.this);
                                newImage.setImageBitmap(bitmap);
                                newImage.setPadding(10,10,10,10);
                                thumbsContent.addView(newImage);
                            }
                        })
                        .subscribe();
            }
        });
    }
}
