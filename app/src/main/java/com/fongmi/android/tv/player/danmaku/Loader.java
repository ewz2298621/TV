package com.fongmi.android.tv.player.danmaku;

import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.io.InputStream;

import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.parser.android.AndroidFileSource;

public class Loader implements ILoader {

    private static final int TIMEOUT = 30 * 1000;

    private AndroidFileSource dataSource;

    public Loader(Danmaku item) {
        try {
            load(item.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load(String url) throws IllegalDataException {
        try {
            OkHttp.cancel("danmaku");
            load(OkHttp.newCall(OkHttp.client(TIMEOUT), UrlUtil.convert(url), "danmaku").execute().body().byteStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load(InputStream stream) throws IllegalDataException {
        dataSource = new AndroidFileSource(stream);
    }

    @Override
    public AndroidFileSource getDataSource() {
        return dataSource;
    }
}