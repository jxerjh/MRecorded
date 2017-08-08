package com.mvideo.record;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class RecSecAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rec_sec_act);

        Button camera = (Button)findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(RecSecAct.this,RecMainAct.class));
            }
        });
        Button compress = (Button)findViewById(R.id.compress);
        compress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RecSecAct.this,RecCompressAct.class));
            }
        });

        Button videoplay = (Button)findViewById(R.id.video_play);
        videoplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RecSecAct.this,RecVideoPlayAct.class));
            }
        });
    }
}
