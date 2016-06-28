package com.jikexueyuan.simpleaudioplayer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fangc on 2016/4/17.
 */

public class ParseTheLrcToOneTreeMAP {

    private Map<Long, String> treeMap;//需要时有序的 map，因为便于有序的取出歌词，为了用时间同步歌词!!

    public ParseTheLrcToOneTreeMAP(String lrcDir) {
        treeMap = new TreeMap<>();
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(lrcDir);
            inputStreamReader = new InputStreamReader(fileInputStream);
            bufferedReader = new BufferedReader(inputStreamReader);


            String str = "";
            while ((str = bufferedReader.readLine()) != null) {
//                System.out.println(str);
                Pattern pat = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d\\d)\\]");
                Matcher match = pat.matcher(str);
                Pattern pattern = Pattern.compile("\\d\\d");
                StringBuffer buf = new StringBuffer();
                if (match.find()) {
                    Matcher matcher = pattern.matcher(match.group());//嵌套
                    int keyTime = 0;
                    int temp = 1;
                    while (matcher.find()) {
//                        System.out.println((matcher.group()));
                        switch (temp) {
                            case 1:
                                keyTime = Integer.parseInt(matcher.group()) * 60 * 1000;
                                break;
                            case 2:
                                keyTime += Integer.parseInt(matcher.group()) * 1000;
                                break;
                            case 3:
                                keyTime += Integer.parseInt(matcher.group()) * 10;
                                break;
                        }
                        temp++;
                    }
                    match.replaceFirst("");//去掉时间标记
                    str = String.valueOf(match.appendTail(buf));//留下歌词正文内容
//                    Log.i("treeMap", String.valueOf(keyTime) + "--" + str);
                    treeMap.put((long) keyTime, str);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert bufferedReader != null;
                bufferedReader.close();
                inputStreamReader.close();
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public Map<Long, String> getTreeMap() {
        return treeMap;
    }
}
