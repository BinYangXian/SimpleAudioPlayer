package com.jikexueyuan.simpleaudioplayer;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/*app主要功能：解析lrc文件，为方便PlayActivity同步播放歌词的数据集合（可用treeMap），lrc来源为lrcDir
解析出lrc文件中的多个时间片段（key）与lyric字符串（value）
 存于HashMap中，当检测到mediaPlayTimer变化后的值 属于的key时间片段（key）变化时，
 就用textView刷新显示对应的lyric字符串（value）；（正常播放时候，只在变化的时间点刷新textView，指定位置播放功能，暂不考虑）。
问题来了，如何检测mediaPlayTimer的变化，检测频率多少（需精确到毫秒才行，每毫秒检测一次），消耗的系统资源较大吧？是否
 可以用key事先标记全部给mediaPlayer，在key所在时间点时主动通知刷新value？mediaPlay没有类似的方法！！
可用 定时同步（加上每次更新歌词显示前用mediaPlayer.getCurrentPosition方法修正一次）能完美实现 同步！！*/

public class SimpleAudioPlayer extends AppCompatActivity implements View.OnClickListener {
    private MediaPlayer mediaPlayer;
    private Button btn_stop,btn_pause_and_play;
    private ParseTheLrcToOneTreeMAP parseTheLrcToOneTreeMAP;
    private TextView synLyric_tv;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_pause_and_play = (Button) findViewById(R.id.btn_pause_and_play);

        synLyric_tv = (TextView) findViewById(R.id.synLyric_tv);
        btn_stop.setOnClickListener(this);
        btn_pause_and_play.setOnClickListener(this);

        //通过MediaPlayer播放音频文件
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            try {
                uri = getIntent().getData();
                mediaPlayer.setDataSource(getApplicationContext(), uri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mediaPlayer=null;
                    }
                });
                synchronizationLyricAndPlayWithMedia();
            } catch (IOException e) {
                System.out.println(e);
            }
        }

    }

    private void synchronizationLyricAndPlayWithMedia() {
        //默认歌词文件与mp3文件所在同一路径，解析出歌词文件路径
        String lrcFileName = uri.getPath();
        Pattern pat = Pattern.compile("\\.mp3$");
        StringBuffer buf = new StringBuffer();
        Matcher match = pat.matcher(lrcFileName);
        if (match.find()) {
            match.appendReplacement(buf, ".lrc");
            lrcFileName = String.valueOf(buf);
        }

        File file = new File(lrcFileName); //判断该名字的歌词文件是否存在指定路径（与mp3文件同名，同路径），存在再同步歌词
        if (file.exists()) {
            Log.i("Note-SimpleAudioPlayer", "找到歌词文件");
            parseTheLrcToOneTreeMAP = new ParseTheLrcToOneTreeMAP(lrcFileName); //解析歌词文件
            Thread thread = new Thread(new synchronizationLyricThread());
            thread.start();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_stop:
                if (mediaPlayer!=null){
                    mediaPlayer.stop();
                    mediaPlayer.release();//释放资源
                    mediaPlayer = null;
                }
                break;
            case R.id.btn_pause_and_play:
                if (mediaPlayer!=null){
                    if (mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        btn_pause_and_play.setText("继续播放");
                    }else {
                        mediaPlayer.start();
                        btn_pause_and_play.setText("暂停播放");
                    }
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();//释放资源
            mediaPlayer = null;
        }
    }

    private class synchronizationLyricThread implements Runnable {
        Iterator<Map.Entry<Long, String>> iterator = parseTheLrcToOneTreeMAP.getTreeMap().entrySet().iterator();//entrySet():return a set of the mappings
        long lastTime = 0;

        @Override
        public void run() {

            while (mediaPlayer != null && iterator.hasNext()) {
                Map.Entry<Long, String> entry = iterator.next();

                Long time = entry.getKey() - lastTime;
                Bundle bundle = new Bundle();
                bundle.putString("msg", entry.getValue());
                Message message = myHandler.obtainMessage();
                message.setData(bundle);
                long syncLrcTimeOffset = 0;
                try {
                    if (time < 100) {
                        Thread.sleep(time);//首次定时同步
                    } else {
                        Thread.sleep(time - 100);//非首次定时同步,提前100微秒醒来，以便下边再次同步
                    }
                    if (mediaPlayer != null) {
                        syncLrcTimeOffset = entry.getKey() - (long) mediaPlayer.getCurrentPosition();//修正第二次同步时间
                    }
                    if (syncLrcTimeOffset > 0) {//小于0的情况在lastTime中负补偿以修正。
                        Thread.sleep(syncLrcTimeOffset);//第二次定时同步。
                        syncLrcTimeOffset = 0;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                message.sendToTarget();
                lastTime = entry.getKey() - syncLrcTimeOffset;
            }
        }
    }

    private MyHandler myHandler = new MyHandler();

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            synLyric_tv.setText(msg.getData().getString("msg"));
        }
    }

}
