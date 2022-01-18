package com.noboauto.applet.audio;

/**
 * 时间工具类
 *
 * @author zhongjiaxing
 * @date 2021/7/13
 */
public class TimeUtil {

    /**
     * 返回时分秒格式的时间
     *
     * @param time 毫秒时间
     * @return
     */
    public static String asTime(long time) {
        long hours = time / (1000 * 60 * 60);
        long minutes = time % (1000 * 60 * 60) / (1000 * 60);
        long seconds = time % (1000 * 60) / 1000;
        String timeText = String.valueOf(hours);
        if (hours < 10) {
            timeText = "0" + timeText;
        }
        if (minutes < 10) {
            timeText = timeText + ":0" + minutes;
        } else {
            timeText = timeText + ":" + minutes;
        }
        if (seconds < 10) {
            timeText = timeText + ":0" + seconds;
        } else {
            timeText = timeText + ":" + seconds;
        }
        return timeText;
    }
}
