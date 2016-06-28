package com.jikexueyuan.simpleaudioplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*功能：测试----通过隐式Intent启动PlayActivity 播放指定路径audio资源（由于不能通过uri读取as中的资源文件，
 为统一在SimpleAudioPlayer中通过intent的getData方法得到播放文件的Uri，所以在测试前拷贝播放文件到手机中）*/
public class TestSimpleAudioPlayer extends AppCompatActivity {
    private Button btn_play;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //拷贝mp3
        fileName = "music.mp3";
        if (!fileExist(fileName)) {
            copyToMobile(fileName);
            Log.i("Note", fileName + "准备拷贝");
        }
        //拷贝mp3歌词到mp3所在目录
        fileName = "music.lrc";
        if (!fileExist(fileName)) {
            copyToMobile(fileName);
            Log.i("Note", fileName + "准备拷贝");
        }
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //隐式Intent
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri mUri = Uri.parse("file://" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/myplayer/" + "music.mp3");
                intent.setDataAndType(mUri, "audio/*");//通配符的使用，使得能播放
                startActivity(intent);
                finish();// TODO: 2016/6/14  
            }
        });


    }

    //------------拷贝mp3到手机中，由于不能通过uri读取as中的资源文件，为同统一播放方法---------------
    private String getDir() {

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/myplayer/";
    }

    private boolean fileExist(String fileName) {
        File file = new File(getDir() + fileName);
        if (file.exists()) {
            Log.i("Note", "已经存在" + fileName + "路径为：" + file.getAbsolutePath());
            return true;
        }
        return false;
    }

    private void copyToMobile(final String fileName) {

        new Thread() {
            @Override
            public void run() {
                super.run();
                File dir = new File(getDir());
                if (!dir.exists()) {
                    dir.mkdir();
                }
                InputStream fis = null; //fis（输入流）是指从一个字节流中读取数据 的对象。（用于读取文件中的数据流
                OutputStream fos = null;//fos（输出流）是指将字节流数据写入目标 的对象。（用于把数据写入到文件中
                //从资源文件中获取到了raw下的music.MP3到输入流，可以用来把数据读取出来
                if (fileName.equals("music.mp3")) {
                    fis = getResources().openRawResource(R.raw.music);//从这个（赋值号右侧的）字节流读取数据
                } else if (fileName.equals("music.lrc")) {
                    fis = getResources().openRawResource(R.raw.musiclrc);//从这个（赋值号右侧的）字节流读取数据
                }
                //创建一个被写入数据到文件
                File to = null;
                if (fileName.equals("music.mp3")) {
                    to = new File(getDir(), fileName);
                } else if (fileName.equals("music.lrc")) {
                    to = new File(getDir(), "music.lrc");
                }
                try {
                    Log.i("Note", "开始拷贝");

                    assert to != null;
                    fos = new FileOutputStream(to);//创建一个字节输出流来把数据写入到to（所指文件中） 的对象。

                    byte[] buf = new byte[4096];//临时创建一个数组来存储数据，作为下边的缓存.
                    while (true) {//循环读取（每次读取4k字节）缓存在buf[]中，然后再将buf[]依次写入到输出流fos中，直到fis为空（r==-1）。
                        assert fis != null;
                        int r = fis.read(buf);
                        if (r == -1) {
                            Log.i("Note", "拷贝完毕");
                            break;
                        }
                        fos.write(buf);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("Note", "IOException");
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();

    }
}
