package com.gameroom.data.game.scraper;

import java.util.HashMap;

/**
 * Created by LM on 21/02/2017.
 */
public class SteamProfile {
    private String accountName;
    private String accountId;
    private static HashMap<String, SteamProfile> SCANNED_PROFILES = new HashMap<>();

    public SteamProfile(String accountName, String accountId) {
        this.accountName = accountName;
        this.accountId = accountId;
        SCANNED_PROFILES.put(accountName,this);
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountId() {
        return accountId;
    }

    public static SteamProfile fromAccountName(String accountName){
        return SCANNED_PROFILES.get(accountName);
    }

    @Override
    public int hashCode(){
        return accountId.hashCode();
    }

    @Override
    public boolean equals(Object object){
        if (object == null){
            return false;
        }
        if(object instanceof SteamProfile){
            if(object.hashCode() == hashCode()){
                return true;
            }
        }
        return false;
    }
}
