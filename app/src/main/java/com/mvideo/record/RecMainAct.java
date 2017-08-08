package com.mvideo.record;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yixia.camera.MediaRecorderBase;
import com.yixia.camera.MediaRecorderNative;
import com.yixia.camera.VCamera;
import com.yixia.camera.model.MediaObject;
import com.yixia.camera.util.FileUtils;
import com.yixia.camera.util.Log;
import com.yixia.videoeditor.adapter.UtilityAdapter;

import java.util.LinkedList;

/**
 * 主页
 */
public class RecMainAct extends BaseActivity implements View.OnClickListener{

    private static final int REQUEST_KEY = 100;
    private static final int HANDLER_RECORD = 200;
    private static final int HANDLER_EDIT_VIDEO = 201;

    private MediaRecorderNative mMediaRecorder;
    private MediaObject mMediaObject;
    private FocusSurfaceView sv_ffmpeg;
    private RecButton rb_start;
    private RelativeLayout rl_bottom;
    private RelativeLayout rl_bottom2;
    private ImageView iv_back;
    private TextView tv_hint;
    private TextView textView;
    private RecVideoView vv_play;
    private ImageView changeCamera;
    private RelativeLayout vv_play_backly;
    /**最大录制时间*/
    private int maxDuration = 8000;
    /**本次段落是否录制完成*/
    private boolean isRecordedOver;
    private AlbumOrientationEventListener mAlbumOrientationEventListener;
    /**监听方向传感器的方向，用于.mp4文件写入角度*/
    private float mRotate;
    /**是否需要写入角度(当手机横着拍摄，需求竖屏播放时的处理)*/
    private boolean isAddRotate = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.rec_main_act);

        sv_ffmpeg = (FocusSurfaceView) findViewById(R.id.sv_ffmpeg);
        rb_start = (RecButton) findViewById(R.id.rb_start);
        vv_play = (RecVideoView) findViewById(R.id.vv_play);
        vv_play_backly = (RelativeLayout) findViewById(R.id.vv_play_backly);
        ImageView iv_finish = (ImageView) findViewById(R.id.iv_finish);
        iv_back = (ImageView) findViewById(R.id.iv_back);
        tv_hint = (TextView) findViewById(R.id.tv_hint);
        rl_bottom = (RelativeLayout) findViewById(R.id.rl_bottom);
        rl_bottom2 = (RelativeLayout) findViewById(R.id.rl_bottom2);
        ImageView iv_next = (ImageView) findViewById(R.id.iv_next);
        ImageView iv_close = (ImageView) findViewById(R.id.iv_close);
        changeCamera = (ImageView) findViewById(R.id.change_camera);
        /**传感器监听*/
        mAlbumOrientationEventListener = new AlbumOrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL);
        if (mAlbumOrientationEventListener.canDetectOrientation()) {
            mAlbumOrientationEventListener.enable();
        } else {
            Log.d("TODO", "Can't Detect Orientation");
        }
        /**是否支持前置摄像头*/
        if (MediaRecorderBase.isSupportFrontCamera()) {
            changeCamera.setOnClickListener(this);
        } else {
            changeCamera.setVisibility(View.GONE);
        }

        initMediaRecorder();

        sv_ffmpeg.setTouchFocus(mMediaRecorder);

        rb_start.setMax(maxDuration);

        rb_start.setOnGestureListener(new RecButton.OnGestureListener() {
            @Override
            public void onLongClick() {
                isRecordedOver = false;
                mMediaRecorder.startRecord();
                rb_start.setSplit();
                myHandler.sendEmptyMessageDelayed(HANDLER_RECORD, 100);
            }
            @Override
            public void onClick() {
            }
            @Override
            public void onLift() {
                isRecordedOver = true;
                mMediaRecorder.stopRecord();
                changeButton(mMediaObject.getMediaParts().size() > 0);
            }
            @Override
            public void onOver() {
                isRecordedOver = true;
                rb_start.closeButton();
                mMediaRecorder.stopRecord();
                videoFinish();
            }
        });

        iv_back.setOnClickListener(this);
        iv_finish.setOnClickListener(this);
        iv_next.setOnClickListener(this);
        iv_close.setOnClickListener(this);
        changeCamera.setOnClickListener(this);
    }

    private void changeButton(boolean flag){

        if(flag){
//            tv_hint.setVisibility(View.VISIBLE);
            rl_bottom.setVisibility(View.VISIBLE);
        }else{
//            tv_hint.setVisibility(View.GONE);
            rl_bottom.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化视频拍摄状态
     */
    private void initMediaRecorderState(){

        vv_play.setVisibility(View.GONE);
        vv_play_backly.setVisibility(View.GONE);
        vv_play.pause();
        mAlbumOrientationEventListener.enable();
        isAddRotate = false;

        changeCamera.setVisibility(View.VISIBLE);
        rb_start.setVisibility(View.VISIBLE);
        rl_bottom2.setVisibility(View.GONE);
        changeButton(false);
//        tv_hint.setVisibility(View.VISIBLE);

        LinkedList<MediaObject.MediaPart> list = new LinkedList<>();
        list.addAll(mMediaObject.getMediaParts());

        for (MediaObject.MediaPart part : list){
            mMediaObject.removePart(part, true);
        }

        rb_start.setProgress(mMediaObject.getDuration());
        rb_start.cleanSplit();
    }

    private void videoFinish() {

        changeButton(false);
        rb_start.setVisibility(View.GONE);

        textView = showProgressDialog();

        myHandler.sendEmptyMessage(HANDLER_EDIT_VIDEO);
    }

    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_RECORD://拍摄视频的handler
                    mAlbumOrientationEventListener.disable();
                    Log.i("TODO","mRotate="+mRotate);
                    if(mRotate == 0){
                        //屏幕朝上
                        isAddRotate = false;
                    }else if(mRotate == 270){
                        //屏幕朝左，按钮在右
                        isAddRotate = true;
                    }else if(mRotate == 90){
                        //屏幕朝右，按钮在左
                        isAddRotate = true;
                    }
                    if(!isRecordedOver){
                        if(rl_bottom.getVisibility() == View.VISIBLE) {
                            changeButton(false);
                        }
                        rb_start.setProgress(mMediaObject.getDuration());
                        myHandler.sendEmptyMessageDelayed(HANDLER_RECORD, 30);
                    }
                    break;
                case HANDLER_EDIT_VIDEO://合成视频的handler
                    int progress = UtilityAdapter.FilterParserAction("", UtilityAdapter.PARSERACTION_PROGRESS);
                    if(textView != null) textView.setText("视频编译中 "+progress+"%");
                    if (progress == 100) {
                        syntVideo();
                    } else if (progress == -1) {
                        closeProgressDialog();
                        Toast.makeText(getApplicationContext(), "视频合成失败", Toast.LENGTH_SHORT).show();
                    } else {
                        sendEmptyMessageDelayed(HANDLER_EDIT_VIDEO, 30);
                    }
                    break;
            }
        }
    };

    /**
     * 合成视频
     */
    private void syntVideo(){

        //ffmpeg -i "concat:ts0.ts|ts1.ts|ts2.ts|ts3.ts" -c copy -bsf:a aac_adtstoasc out2.mp4
        StringBuilder sb = new StringBuilder("ffmpeg");
        sb.append(" -i");
        String concat="concat:";
        for (MediaObject.MediaPart part : mMediaObject.getMediaParts()){
            concat+=part.mediaPath;
            concat += "|";
        }
        concat = concat.substring(0, concat.length()-1);
        sb.append(" "+concat);
        sb.append(" -c");
        sb.append(" copy");
        if(isAddRotate){
            sb.append(" -metadata:s:v:0");
            sb.append(" rotate="+mRotate);
        }
        sb.append(" -bsf:a");
        sb.append(" aac_adtstoasc");
        sb.append(" -y");
//        String output = RecApplication.VIDEO_PATH+"/finish.mp4";
        String output = mMediaObject.getOutputVideoPath();
        sb.append(" "+output);

        int i = UtilityAdapter.FFmpegRun("", sb.toString());

        closeProgressDialog();
        if(i == 0){
            rl_bottom2.setVisibility(View.VISIBLE);
            vv_play.setVisibility(View.VISIBLE);
            vv_play_backly.setVisibility(View.VISIBLE);
            changeCamera.setVisibility(View.GONE);

            vv_play.setVideoPath(output);
            vv_play.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    /**int vvplayW = vv_play.getVideoWidth();
                    int vvplayH = vv_play.getVideoHeight();
                    ViewGroup.LayoutParams layoutParams = vv_play.getLayoutParams();
                    int windowW = getWindowManager().getDefaultDisplay().getWidth();
                    int windowH = getWindowManager().getDefaultDisplay().getHeight();
                    layoutParams.width = vvplayW;
                    layoutParams.height = vvplayH;

                    if(mRotate == 0){
                        //全屏播
                        layoutParams.width =windowW;
                        layoutParams.height = windowH;
                    }
                    vv_play.setLayoutParams(layoutParams);*/

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
            if(vv_play.isPrepared()){
                vv_play.setLooping(true);
                vv_play.start();
            }

        }else{
            Toast.makeText(getApplicationContext(), "视频合成失败", Toast.LENGTH_SHORT).show();
        }

    }

    /** 截图 */
    protected void captureThumbnails() {
        /**方式一*/
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean  thumpSuccess= FileUtils.captureThumbnails(mMediaObject.getOutputVideoPath(), mMediaObject.getOutputVideoThumbPath(), MediaRecorderBase.VIDEO_WIDTH + "x" + MediaRecorderBase.VIDEO_HEIGHT, String.valueOf(1));

                return thumpSuccess;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), "截图失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();

        /**方式二
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        Bitmap bit = mmr.getFrameAtTime();
        iv_video_screenshot.setImageBitmap(bit);
         */
    }

    /**
     * 初始化录制对象
     */
    private void initMediaRecorder() {

        mMediaRecorder = new MediaRecorderNative();
        String key = String.valueOf(System.currentTimeMillis());
        //设置缓存文件夹
        mMediaObject = mMediaRecorder.setOutputDirectory(key, VCamera.getVideoCachePath());
        //设置视频预览源
        mMediaRecorder.setSurfaceHolder(sv_ffmpeg.getHolder());
        //准备
        mMediaRecorder.prepare();
        //滤波器相关
        UtilityAdapter.freeFilterParser();
        UtilityAdapter.initFilterParser();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRecorder.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMediaRecorder.stopPreview();
    }

    @Override
    public void onBackPressed() {
        if(rb_start.getSplitCount() == 0) {
            super.onBackPressed();
        }else{
            initMediaRecorderState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMediaObject.cleanTheme();
        mMediaRecorder.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_KEY){
                initMediaRecorderState();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                if(rb_start.isDeleteMode()){//判断是否要删除视频段落
                    MediaObject.MediaPart lastPart = mMediaObject.getPart(mMediaObject.getMediaParts().size() - 1);
                    mMediaObject.removePart(lastPart, true);
                    rb_start.setProgress(mMediaObject.getDuration());
                    rb_start.deleteSplit();
                    changeButton(mMediaObject.getMediaParts().size() > 0);
                    iv_back.setImageResource(R.mipmap.video_delete);
                }else if(mMediaObject.getMediaParts().size() > 0){
                    rb_start.setDeleteMode(true);
                    iv_back.setImageResource(R.mipmap.video_delete_click);
                }
                break;
            case R.id.iv_finish:
                videoFinish();
                break;
            case R.id.iv_next:
                rb_start.setDeleteMode(false);

                Intent intent = new Intent(RecMainAct.this, RecVideoPlayAct.class);
//                intent.putExtra("path", RecApplication.VIDEO_PATH +"/finish.mp4");
                intent.putExtra("path", mMediaObject.getOutputVideoPath());
                intent.putExtra("thump", mMediaObject.getOutputVideoThumbPath());
                startActivity(intent);
                break;
            case R.id.iv_close:
                initMediaRecorderState();
                break;
            case R.id.change_camera:
                if (mMediaRecorder != null) {
                    mMediaRecorder.switchCamera();
                }
                break;
        }
    }

    /**
     * 自定义方向传感器监听
     */
    private class AlbumOrientationEventListener extends OrientationEventListener {

        public AlbumOrientationEventListener(Context context) {
            super(context);
        }

        public AlbumOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                return;
            }

            //保证只返回四个方向
            int newOrientation = ((orientation + 45) / 90 * 90) % 360;
            if (newOrientation != mRotate) {
                //返回的mOrientation就是手机方向，为0°、90°、180°和270°中的一个
                mRotate = newOrientation;
            }
            Log.i("TODO","mOrientation="+mRotate);
        }
    }
}
