package com.saumondeluxe.sushiscan.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Type converters for Room database
 * Handles conversion between complex types and database-storable primitives
 */
public class Converters {

    /**
     * Converts a List of Strings to a JSON String for database storage
     */
    @TypeConverter
    public static String fromStringList(List<String> value) {
        if (value == null) {
            return null;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.toJson(value, type);
    }

    /**
     * Converts a JSON String from the database back to a List of Strings
     */
    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, type);
    }
}