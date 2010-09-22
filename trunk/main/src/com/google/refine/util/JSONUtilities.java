package com.google.refine.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

public class JSONUtilities {
    static public String getString(JSONObject obj, String key, String def) {
        try {
            return obj.getString(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public int getInt(JSONObject obj, String key, int def) {
        try {
            return obj.getInt(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public boolean getBoolean(JSONObject obj, String key, boolean def) {
        try {
            return obj.getBoolean(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public double getDouble(JSONObject obj, String key, double def) {
        try {
            return obj.getDouble(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public long getLong(JSONObject obj, String key, long def) {
        try {
            return obj.getLong(key);
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public Date getDate(JSONObject obj, String key, Date def) {
        try {
            Date d = ParsingUtilities.stringToDate(obj.getString(key));
            
            return d != null ? d : def;
        } catch (JSONException e) {
            return def;
        }
    }
    
    static public int[] getIntArray(JSONObject obj, String key) {
        try {
            JSONArray a = obj.getJSONArray(key);
            int[] r = new int[a.length()];
            
            for (int i = 0; i < r.length; i++) {
                r[i] = a.getInt(i);
            }
            
            return r;
        } catch (JSONException e) {
            return new int[0];
        }
    }
    
    static public String[] getStringArray(JSONObject obj, String key) {
        try {
            JSONArray a = obj.getJSONArray(key);
            String[] r = new String[a.length()];
            
            for (int i = 0; i < r.length; i++) {
                r[i] = a.getString(i);
            }
            
            return r;
        } catch (JSONException e) {
            return new String[0];
        }
    }
    
    static public void getStringList(JSONObject obj, String key, List<String> list) {
        try {
            JSONArray a = obj.getJSONArray(key);
            int count = a.length();
            
            for (int i = 0; i < count; i++) {
                list.add(a.getString(i));
            }
        } catch (JSONException e) {
        }
    }
    
    static public void writeStringList(JSONWriter writer, List<String> list) throws JSONException {
        writer.array();
        for (String s : list) {
            writer.value(s);
        }
        writer.endArray();
    }
    
    static public void putField(JSONObject obj, String key, Object value) throws JSONException {
    	if (value instanceof Integer) {
    		obj.put(key, ((Integer) value).intValue());
    	} else if (value instanceof Long) {
    		obj.put(key, ((Long) value).intValue());
    	} else if (value instanceof Number) {
    		obj.put(key, ((Double) value).doubleValue());
    	} else if (value instanceof Boolean) {
    		obj.put(key, (Boolean) value);
    	} else if (value instanceof Date) {
    		obj.put(key, ParsingUtilities.dateToString((Date) value));
    	} else if (value instanceof Calendar) {
    		obj.put(key, ParsingUtilities.dateToString(((Calendar) value).getTime()));
    	} else if (value instanceof String) {
    		obj.put(key, (String) value);
    	} else {
    		obj.put(key, value.toString());
    	}
    }
    
    static public Object[] toArray(JSONArray a) throws JSONException {
        int l = a.length();
        
        Object[] a2 = new Object[l];
        for (int i = 0; i < l; i++) {
            a2[i] = a.get(i);
        }
        
        return a2;
    }
    
    static public List<String> toStringList(JSONArray a) throws JSONException {
        int l = a.length();
        
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < l; i++) {
            list.add(a.getString(i));
        }
        
        return list;
    }
}
