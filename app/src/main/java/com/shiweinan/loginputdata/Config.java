package com.shiweinan.loginputdata;

public class Config {
    public enum Posture {TwoThumbs, LeftThumb, RightThumb};
    public enum Language {English, Chinese};
    public enum Mode {Normal, Fast, Random};
    public static Mode mode = Mode.Normal;
    public static int totalTaskNo = 30;
    public static int totalTaskNoRandom = 20;
    public static Posture posture = Posture.TwoThumbs;
    public static Language language = Language.Chinese;
    public static boolean isStarted = false;
    public static boolean woz  = false;

    public Config() {

    }


}
