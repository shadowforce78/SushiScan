package com.saumondeluxe.sushiscan.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Classe de conversion pour stocker/récupérer des objets Date dans Room
 */
public class DateConverter {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}