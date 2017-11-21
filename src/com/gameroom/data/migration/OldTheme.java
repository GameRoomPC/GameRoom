package com.gameroom.data.migration;

import com.google.gson.Gson;

/**
 * Created by LM on 02/03/2017.
 */
public enum OldTheme {
    ACTION("action"),
    FANTASY("fantasy"),
    SCIENCE_FICTION("science_fiction"),
    HORROR("horror"),
    THRILLER("thriller"),
    SURVIVAL("survival"),
    HISTORICAL("historical"),
    STEALTH("stealth"),
    COMEDY("comedy"),
    BUSINESS("business"),
    DRAMA("drama"),
    NON_FICTION("non_fiction"),
    SANDBOX("sandbox"),
    EDUCATIONAL("educational"),
    KIDS("kids"),
    OPEN_WORLD("open_world"),
    WARFARE("warfare"),
    PARTY("party"),
    EXPLORE_EXPAND_EXPLOIT_EXTERMINATE("4x"),
    EROTIC("erotic"),
    MYSTERY("mystery");

    private String key;

    OldTheme(String key) {
        this.key = key;
    }

    public static String toJson(OldTheme[] genres) {
        Gson gson = new Gson();
        return gson.toJson(genres, OldTheme[].class);
    }

    public static OldTheme[] fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, OldTheme[].class);
    }

    public String getKey() {
        return key;
    }
}
