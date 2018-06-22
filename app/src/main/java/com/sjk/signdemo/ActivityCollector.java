package com.sjk.signdemo;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {
    public static List<Activity> activities = new ArrayList<>();

    public static void addActivity(Activity activity) {
        activities.add(activity);
    }

    public static void removeActivity(Activity activity) {
        activities.remove(activity);
        if (activities.size() <= 0) {
            killProcess();
        }
    }

    public static void killProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
