package com.msdemo.v2.common.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtil {

	public static DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	public static DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	public static DateTimeFormatter SHORT_DATETIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");
	public static DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	public static String date(){
		return formatNow(DATE);
	}
	
	public static String date(Date date){
		return formatDate(date,DATE);
	}
	
	public static String dateTime(){
		return formatNow(DATETIME);
	}
	
	public static String dateTime(Date date){
		return formatDate(date,DATETIME);
	}
	
	public static String shortDateTime(){
		return formatNow(SHORT_DATETIME);
	}
	
	public static String shortDateTime(Date date){
		return formatDate(date,SHORT_DATETIME);
	}
	
	public static String timestamp(){
		return formatNow(TIMESTAMP);
	}
	
	public static String timestamp(Date date){
		return formatDate(date,TIMESTAMP);
	}
	public static String timestamp(long mills){
		return formatLong(mills,TIMESTAMP);
	}
	
	
	public static String formatLong(long mills, DateTimeFormatter formatter){
		return ZonedDateTime.ofInstant ( 
				Instant.ofEpochMilli(mills),ZoneOffset.ofHours(+8)).format(formatter);
	}
	public static String formatDate(Date date, DateTimeFormatter formatter){
		return ZonedDateTime.ofInstant ( 
				Instant.ofEpochMilli(date.getTime()),ZoneOffset.ofHours(+8)).format(formatter);
	}
	public static String formatNow(DateTimeFormatter formatter){
		return ZonedDateTime.ofInstant ( 
				Instant.now(),ZoneOffset.ofHours(+8)).format(formatter);
	}
}
