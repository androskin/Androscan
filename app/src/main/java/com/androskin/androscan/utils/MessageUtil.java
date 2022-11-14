package com.androskin.androscan.utils;

import android.os.Bundle;
import android.os.Message;

public class MessageUtil {
    public static Message createMessage(int what, String title, String msg) {
        Message message = new Message();
        message.what = what;
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("msg", msg);
        message.setData(bundle);

        return message;
    }
}
