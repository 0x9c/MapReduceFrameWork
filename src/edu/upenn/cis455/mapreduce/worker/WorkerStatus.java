package edu.upenn.cis455.mapreduce.worker;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store all information about worker's status
 * @author dongtianxiang
 *
 */
public class WorkerStatus {
	private static int keysRead = 0;
	private static int keysWritten = 0;
	private static List<String> results = new ArrayList<String>();
	private static String status = "IDLE";
	private static String job = "NoJob";
	
	public static synchronized void addKeysRead(){
		keysRead++;
	}
	
	public static synchronized int getKeysRead(){
		return keysRead;
	}
	
	public static synchronized void addKeysWritten(){
		keysWritten++;
	}
	
	public static synchronized int getKeysWritten(){
		return keysWritten;
	}
	
	public static synchronized void addResult(String result){
		results.add(result);
	}
	
	public static synchronized List<String> getResult(){
		return results;
	}
	
	public static synchronized void setStatus(String s){
		status = s;
	}
	
	public static synchronized String getStatus(){
		return status;
	}
	
	public static synchronized void setJob(String j){
		job = j;
	}
	
	public static synchronized String getJob(){
		return job;
	}
	
	public static synchronized void clear(){
		keysRead = 0;
		keysWritten = 0;
		results = new ArrayList<String>();
		job = "NoJob";
	}
}


