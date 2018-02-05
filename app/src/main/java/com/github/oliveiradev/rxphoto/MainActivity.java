package com.github.oliveiradev.rxphoto;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.github.oliveiradev.lib.Rx2Photo;
import com.github.oliveiradev.lib.shared.TypeRequest;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private Disposable disposable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LinearLayout thumbsContent = findViewById(R.id.thumbs);
        final ImageView image = findViewById(R.id.image);

        findViewById(R.id.get).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();

                Rx2Photo.with(v.getContext())
                        .requestBitmap(TypeRequest.GALLERY, 300, 300)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) throws Exception {
                                image.setImageBitmap(bitmap);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("TAG", throwable.getMessage(), throwable);
                            }
                        });
            }
        });

        findViewById(R.id.take).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                if (disposable != null) {
                    disposable.dispose();
                }
                disposable = Rx2Photo.with(v.getContext())
                        .requestBitmap(TypeRequest.CAMERA)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) throws Exception {
                                image.setImageBitmap(bitmap);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("TAG", throwable.getMessage(), throwable);
                            }
                        });
            }
        });

        findViewById(R.id.combine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();

                Rx2Photo.with(v.getContext())
                        .titleCombine("Custom chooser title")
                        .requestBitmap(TypeRequest.COMBINE)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) throws Exception {
                                image.setImageBitmap(bitmap);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("TAG", throwable.getMessage(), throwable);
                            }
                        });
            }
        });

        findViewById(R.id.combine_multiple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                if (disposable != null) {
                    disposable.dispose();
                }
                disposable = Rx2Photo.with(v.getContext())
                        .requestMultiPath()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<String>>() {
                            @Override
                            public void accept(List<String> bitmap) throws Exception {
                                Log.d("TAG", String.valueOf(bitmap.size()));
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("TAG", throwable.getMessage(), throwable);
                            }
                        });
            }
        });

        findViewById(R.id.get_thumb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                if (disposable != null) {
                    disposable.dispose();
                }
                disposable = Rx2Photo.with(v.getContext())
                        .requestThumbnails(TypeRequest.GALLERY, new Pair(60, 60), new Pair(120, 120), new Pair(240, 240))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) throws Exception {
                                final ImageView newImage = new ImageView(MainActivity.this);
                                newImage.setImageBitmap(bitmap);
                                newImage.setPadding(10, 10, 10, 10);
                                thumbsContent.addView(newImage);

                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("TAG", throwable.getMessage(), throwable);
                            }
                        });
            }
        });

        findViewById(R.id.transform).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setImageBitmap(null);
                thumbsContent.removeAllViews();
                if (disposable != null) {
                    disposable.dispose();
                }
                disposable = Rx2Photo.with(v.getContext())
                        .requestBitmap(TypeRequest.GALLERY)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap(Rx2Photo.transformToThumbnail(new Pair(240, 240), new Pair(120, 120), new Pair(60, 60)))
                        .subscribe(new Consumer<Bitmap>() {
                            @Override
                            public void accept(Bitmap bitmap) throws Exception {
                                final ImageView newImage = new ImageView(MainActivity.this);
                                newImage.setImageBitmap(bitmap);
                                newImage.setPadding(10, 10, 10, 10);
                                thumbsContent.addView(newImage);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("TAG", throwable.getMessage(), throwable);
                            }
                        });
            }
        });
    }
}
