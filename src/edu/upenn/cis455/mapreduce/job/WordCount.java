package edu.upenn.cis455.mapreduce.job;

import java.util.Iterator;

import edu.upenn.cis455.mapreduce.Context;
import edu.upenn.cis455.mapreduce.Job;
import edu.upenn.cis455.mapreduce.worker.WorkerStatus;

public class WordCount implements Job {

  public void map(String key, String value, Context context)
  {
      // Your map function for WordCount goes here
	  context.write(value, "1");
	  WorkerStatus.addKeysRead();
	  WorkerStatus.setStatus("MAPPING");
  }
  
  public void reduce(String key, Iterator<String> values, Context context)
  {
      // Your reduce function for WordCount goes here
	  Integer count = 0;
	  while(values.hasNext()) {
		  count += Integer.parseInt(values.next());
	  }
	  context.write(key, count.toString());
	  WorkerStatus.addKeysWritten();
	  WorkerStatus.addResult(key + ":" + count);
	  WorkerStatus.setStatus("REDUCING");
  }
  
}
