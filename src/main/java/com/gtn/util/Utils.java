package com.gtn.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * <p>
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * <p/>
 * Created by uditha on 2025-03-12.
 */
public class Utils {

    /**
     * This utils method use for find get a specific object in a given path of a object
     * which has List and Maps
     * [key_map_1]/[key_map_2]/{value}:[matching_attribute_name]:[attribute_to_get]:
     * here {value} is attibute value to match in List
     * ex:-watchList/sharedWatchlists/{value}:watchListName:symbols
     *
     * @param path
     * @param map
     * @return
     */
    public static Object getMapData(String path, JSONObject map) {
        String[] pathData = path.split("/");

        Object subMap = map;
        for (String item : pathData) {
            String[] params = item.split(":");
            if (params.length > 1) {
                for (Object obj : ((JSONArray) subMap)) {
                    JSONObject tmpMap = ((JSONObject) obj);
                    if (tmpMap.get(params[0]).equals(params[1])) {
                        subMap = tmpMap.get(params[2]);
                    }

                }
            } else {
                subMap = ((JSONObject) subMap).get(item);
            }
        }

        return subMap;
    }

    public static JSONObject returnStatus(int status, String response){
        return new JSONObject().put("http_status", status).put("auth_status", response);
    }

    public static JSONObject returnStatus(int status, Map<String, Object> response){
        return new JSONObject().put("http_status", status).put("auth_status", response);
    }
}
