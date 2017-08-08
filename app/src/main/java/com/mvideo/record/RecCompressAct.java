package com.mvideo.record;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yixia.camera.util.FileSizeUtil;
import com.yixia.videoeditor.adapter.UtilityAdapter;

public class RecCompressAct extends BaseActivity implements View.OnClickListener{
    private String testPath = "/sdcard/MRecorded/";
    private static final int HANDLER_COMPRESS = 500;
    private String num = "10";
    private String videoFps = "25";//视频帧率
    private String audioKbps = "128K";//音频码率
    private String videoCRF = "25";//视频质量
    private TextView dirin,dirinsize,dirout,diroutsize;
    private Button compress_btn;
    private Chronometer timer;
    private EditText num_ed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rec_compress_act);
        dirin = (TextView)findViewById(R.id.dirin);
        dirinsize = (TextView)findViewById(R.id.dirinsize);
        dirout = (TextView)findViewById(R.id.dirout);
        diroutsize = (TextView)findViewById(R.id.diroutsize);
        compress_btn = (Button)findViewById(R.id.compress_btn);
        timer = (Chronometer)findViewById(R.id.timer);
        num_ed = (EditText)findViewById(R.id.num_ed);

        compress_btn.setOnClickListener(this);
        if(!num_ed.getText().toString().trim().equals("")){
            num = num_ed.getText().toString().trim();
        }else{
            num = "10";
        }

        num_ed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!num_ed.getText().toString().trim().equals("")){
                    num = num_ed.getText().toString().trim();
                }else{
                    num = "10";
                }

                dirin.setText("输入目录："+testPath + num + "old.mp4");
                dirinsize.setText(FileSizeUtil.getFileOrFilesSize(testPath+num+"old.mp4",3)+"MB");
            }
        });

    }

    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_COMPRESS:
                    videoToCompress();
                    break;

            }
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.compress_btn:

                myHandler.sendEmptyMessageDelayed(HANDLER_COMPRESS,100);
                //开始计时
                timer.setBase(SystemClock.elapsedRealtime());//计时器清零
                int hour = (int) ((SystemClock.elapsedRealtime() - timer.getBase()) / 1000 / 60);
                timer.setFormat("0"+String.valueOf(hour)+":%s");
                timer.start();
                break;
        }
    }
    private void videoToCompress(){

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                showProgressDialog();
            }
            @Override
            protected Boolean doInBackground(Void... params) {

//                Boolean result = compressVideo(testPath+num+"old.mp4",testPath+num+"new.mp4",videoFps,audioKbps);
                Boolean result = compressVideo(testPath+num+"old.mp4",testPath+num+"new.mp4", videoCRF);
                return result;
            }
            @Override
            protected void onPostExecute(Boolean result) {
                closeProgressDialog();
                if(result) {
                    timer.stop();
                    Toast.makeText(getApplicationContext(), "视频压缩完成", Toast.LENGTH_SHORT).show();

                    dirout.setText("输出目录："+testPath+num+"new.mp4");
                    diroutsize.setText(FileSizeUtil.getFileOrFilesSize(testPath+num+"new.mp4",3)+"MB");


                }else{
                    Toast.makeText(getApplicationContext(), "视频压缩失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }


    /**
     * 压缩视频
     * @param inDir 输入目录
     * @param outDir 输出目录
     * @param videoFramerate 视频帧率
     * @param audioFramerate 音频码率
     * @return
     */
    private Boolean compressVideo(String inDir,String outDir,String videoFramerate,String audioFramerate){

        //ffmpeg.exe -i "C:\test.mp4" -r 10 -b:a 32k "C:\test_mod.mp4"
        StringBuilder sb = new StringBuilder("ffmpeg");
        sb.append(" -i");
        sb.append(" "+inDir);
        sb.append(" -vcodec libx264");
        sb.append(" -preset");
        sb.append(" ultrafast");
        sb.append(" -r");
        sb.append(" "+videoFramerate);
        sb.append(" -b:a");
        sb.append(" "+audioFramerate);
        sb.append(" "+outDir);
        int i = UtilityAdapter.FFmpegRun("", sb.toString());

        if(i == 0){
            //命令执行
            return true;
        }else{
            Toast.makeText(getApplicationContext(), "视频合成失败", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 压缩视频
     * @param inDir 输入目录
     * @param outDir 输出目录
     * @param videoCRF 视频质量
     * @return
     */
    private Boolean compressVideo(String inDir,String outDir,String videoCRF){

        StringBuilder sb = new StringBuilder("ffmpeg");
        sb.append(" -i");
        sb.append(" "+inDir);
        sb.append(" -vcodec libx264");
        sb.append(" -preset");
        sb.append(" ultrafast");
        sb.append(" -crf");
        sb.append(" "+videoCRF);
        sb.append(" "+outDir);
        int i = UtilityAdapter.FFmpegRun("", sb.toString());

        if(i == 0){
            //命令执行
            return true;
        }else{
            Toast.makeText(getApplicationContext(), "视频合成失败", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
