package com.gameroom.data;

import com.gameroom.data.game.GameWatcher;
import com.gameroom.data.game.entry.GameEntry;
import com.gameroom.data.game.scraper.IGDBScraper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by LM on 11/01/2017.
 */
public class LevenshteinDistance {

    public static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    public static int closestName(String searchedName, JSONArray searchResult) throws JSONException {
        int closestId = -1;
        int minDistance = -1;
        for (int i = 0; i < searchResult.length(); i++) {
            String name = searchResult.getJSONObject(i).getString("name");
            int id = searchResult.getJSONObject(i).getInt("id");

            int distance = distance(searchedName, name);
            if (minDistance == -1 || distance < minDistance) {
                minDistance = distance;
                closestId = id;
            }
            if (minDistance == 0) {
                break;
            }

        }
        return closestId;
    }

    public static GameEntry getClosestEntry(String searchedName, JSONArray searchResults, int maxDistance) throws JSONException, UnirestException {
        if (searchResults != null) {
            int minDistance = -1;
            int jsonIndex = 0;
            for (int i = 0; i < searchResults.length(); i++) {
                String name = searchResults.getJSONObject(i).getString("name");

                String cleanName = GameWatcher.formatNameForComparison(name);
                String cleanMSName = GameWatcher.formatNameForComparison(searchedName);
                int distance = LevenshteinDistance.distance(cleanMSName, cleanName);
                if (minDistance == -1 || distance < minDistance) {
                    minDistance = distance;
                    jsonIndex = i;
                }
                if (minDistance < maxDistance) {
                    break;
                }
            }
            if (minDistance >= 0 && minDistance < maxDistance) {
                return IGDBScraper.getGameEntries(searchResults).get(jsonIndex);
            }
        }
        return null;
    }

    /*public static List<Integer> getSortedIds(String searchedName, JSONArray resultArray) throws JSONException{
        ArrayList<SortingItem> items = new ArrayList<>();
        for (Object obj : resultArray) {
            JSONObject jsob = ((JSONObject) obj);
            items.add(new SortingItem(jsob.getInt("id"), distance(searchedName,jsob.getString("name"))));
        }
        items.sort(Comparator.comparingInt(o -> o.distance));

        ArrayList<Integer> sortedIds = new ArrayList<>();
        for(SortingItem item : items){
            sortedIds.add(item.id);
        }
        return sortedIds;
    }*/

    private static class SortingItem {
        int id;
        int distance;

        public SortingItem(int id, int distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}