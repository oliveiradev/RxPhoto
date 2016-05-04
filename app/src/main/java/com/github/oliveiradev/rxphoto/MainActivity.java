package com.github.oliveiradev.rxphoto;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.github.oliveiradev.lib.RxPhoto;
import com.github.oliveiradev.lib.Transformers;
import com.github.oliveiradev.lib.TypeRequest;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView image = (ImageView) findViewById(R.id.image);

        findViewById(R.id.get).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxPhoto.request(v.getContext(),TypeRequest.GALLERY)
                        .compose(Transformers.<Bitmap>applySchedeulers())
                        .doOnNext(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                Log.d("XXXXXXXXXXXX",""+bitmap);
                                image.setImageBitmap(bitmap);
                            }
                        })
                        .subscribe();
            }
        });

        findViewById(R.id.take).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxPhoto.request(v.getContext(),TypeRequest.CAMERA)
                        .compose(Transformers.<Bitmap>applySchedeulers())
                        .doOnNext(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                Log.d("XXXXXXXXXXXX",""+bitmap);
                                image.setImageBitmap(bitmap);
                            }
                        })
                        .subscribe();
            }
        });
    }

}
