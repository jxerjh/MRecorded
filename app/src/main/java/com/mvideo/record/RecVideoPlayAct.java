package com.mvideo.record;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * 播放页
 */

public class RecVideoPlayAct extends BaseActivity {

    private RecVideoView vv_play;
    private ImageView iv_video_screenshot;
    private int windowWidth;
    private int windowHeight;
    private String rotation;
    private String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.rec_video_play_act);

        vv_play = (RecVideoView) findViewById(R.id.vv_play);
        iv_video_screenshot = (ImageView) findViewById(R.id.iv_video_screenshot);

        windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        windowHeight = getWindowManager().getDefaultDisplay().getHeight();

        Intent intent = getIntent();
        path = intent.getStringExtra("path");
//        String videoScreenshot = intent.getStringExtra("thump");
//        Bitmap bitmap = BitmapFactory.decodeFile( videoScreenshot);
//        iv_video_screenshot.setImageBitmap(bitmap);
        vv_play.setVideoPath(path);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        Bitmap bit = mmr.getFrameAtTime();
        iv_video_screenshot.setImageBitmap(bit);

        rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION); // 视频旋转方向


        vv_play.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                /**自适应屏幕宽度播放视频*/
                int videoW = mp.getVideoWidth();
                int videoH = mp.getVideoHeight();
                int windowW = getWindowManager().getDefaultDisplay().getWidth();
                int windowH = getWindowManager().getDefaultDisplay().getHeight();
                ViewGroup.LayoutParams layoutParams = vv_play.getLayoutParams();
                if(videoW < windowW){
                    //自适应屏幕宽度
                    float widthF = windowW/(videoW*1f);
                    layoutParams.width = getWindowManager().getDefaultDisplay().getWidth();
                    layoutParams.height = (int) (videoH *widthF);
                }else if(videoW == windowW){
                    //直接显示
                    layoutParams.width = videoW;
                    layoutParams.height = videoH;
                }else if(videoW > windowW){
                    //缩小显示
                    float widthF = videoW*1f/windowW;
                    layoutParams.width = videoW;
                    //  int newH = (int)(videoH/widthF);
                    int newH = (int)((windowW/16)*9);
                    if(newH > windowH){
                        layoutParams.height = windowH;
                    }else{
                        layoutParams.height = newH;
                    }
                }else{
                    layoutParams.width = videoW;
                    layoutParams.height = videoH;
                }

                vv_play.setLayoutParams(layoutParams);
                vv_play.setLooping(true);
                vv_play.start();
            }
        });
    }
}
