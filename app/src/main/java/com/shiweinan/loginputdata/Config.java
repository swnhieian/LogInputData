package com.shiweinan.loginputdata;

public class Config {
    public enum Posture {TwoThumbs, LeftThumb, RightThumb};
    public enum Language {English, Chinese};
    public static Posture posture = Posture.TwoThumbs;
    public static Language language = Language.English;
    public static boolean isStarted = false;

    public Config() {

    }


}
