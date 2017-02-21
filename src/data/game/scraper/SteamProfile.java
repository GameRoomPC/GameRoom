package data.game.scraper;

/**
 * Created by LM on 21/02/2017.
 */
public class SteamProfile {
    private String accountName;
    private String accountId;

    public SteamProfile(String accountName, String accountId) {
        this.accountName = accountName;
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountId() {
        return accountId;
    }
}
