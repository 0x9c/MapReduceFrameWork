package edu.upenn.cis.stormlite.bolt;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.mapreduce.Job;

/**
 * A simple adapter that takes a MapReduce "Job" and calls the "reduce"
 * on a per-tuple basis
 * 
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ReduceBolt implements IRichBolt {
	static Logger log = Logger.getLogger(ReduceBolt.class);

	
	Job reduceJob;

    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the WordCounter, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
	Fields schema = new Fields("key", "value");
	
	boolean sentEof = false;
	
	/**
	 * Buffer for state, by key
	 */
	//Map<String, List<String>> stateByKey = new HashMap<>();
	
	DBWrapper db;
	
	/**
     * This is where we send our output stream
     */
    private OutputCollector collector;
    
    private TopologyContext context;
    
    int neededVotesToComplete = 0;
    
    public ReduceBolt() {
    }
    
    /**
     * Initialization, just saves the output stream destination
     */
    @Override
    public void prepare(Map<String,String> stormConf, 
    		TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.context = context;

        if (!stormConf.containsKey("reduceClass"))
        	throw new RuntimeException("Mapper class is not specified as a config option");
        else {
        	String mapperClass = stormConf.get("reduceClass");
        	
        	try {
				reduceJob = (Job)Class.forName(mapperClass).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Unable to instantiate the class " + mapperClass);
			}
        }
        if (!stormConf.containsKey("mapExecutors")) {
        	throw new RuntimeException("Reducer class doesn't know how many map bolt executors");
        }
        
        String basePath = System.getProperty("user.home") + "/store/";
        String executorFile = "worker" + stormConf.get("workerIndex") + "_executor" + executorId;
        File storeFile = new File(basePath + executorFile);
        if(storeFile.exists()) {
        	// if previous one exists, delete it.
        	File[] files = storeFile.listFiles();
        	for(File f : files) {
        		if(f.isFile()) {
        			f.delete();
        		}
        	}
        }
        System.out.println("------------> " + "getting DB instance");
        db = new DBWrapper(basePath + executorFile);
        
        // TODO: determine how many EOS votes needed
        int spouts = Integer.parseInt(stormConf.get("spoutExecutors"));
        int mappers = Integer.parseInt(stormConf.get("mapExecutors"));
        int reducers = Integer.parseInt(stormConf.get("reduceExecutors"));
        String[] workers = WorkerHelper.getWorkers(stormConf);
        int workerNum = workers.length;
        
        int mapperEOS = 1;
        neededVotesToComplete = mapperEOS * mappers + mapperEOS * mappers * reducers * (workerNum - 1);
    }

    /**
     * Process a tuple received from the stream, buffering by key
     * until we hit end of stream
     */
    @Override
    public synchronized void execute(Tuple input) {
    	if (sentEof) {
	        if (!input.isEndOfStream())
	        	throw new RuntimeException("We received data after we thought the stream had ended!");
    		// Already done!
		} else if (input.isEndOfStream()) {
			
			neededVotesToComplete--;
			log.debug("EndOfStream received in ReduceBolt --> neededVotesToComplete = " + neededVotesToComplete);
			if(neededVotesToComplete == 0) {
				for(String key: db.getKeySet()) {
					reduceJob.reduce(key, db.getListByKey(key).iterator(), collector);
				}
//				for(String key: stateByKey.keySet()) {
//					reduceJob.reduce(key,stateByKey.get(key).iterator(), collector);
//				}
				sentEof = true;
				collector.emitEndOfStream();
				db.close();
			}
    	} else {
    		// TODO: this is a plain ol' hash map, replace it with BerkeleyDB
    		
    		String key = input.getStringByField("key");
	        String value = input.getStringByField("value");
	        log.info(getExecutorId() + " received " + key + " / " + value);
	        
	        
//	        synchronized (stateByKey) {
//		        if (!stateByKey.containsKey(key))
//		        	stateByKey.put(key, new ArrayList<>());
//		        else
//		        	log.debug("Adding item to " + key + " / " + stateByKey.get(key).size());
//		        stateByKey.get(key).add(value);
	        	
	        	db.addValueToList(key, value);
	        	
		        log.debug("ReduceBolt -->>> " + key + " : " + value);
//	        }
    	}        
    }

    /**
     * Shutdown, just frees memory
     */
    @Override
    public void cleanup() {
    }

    /**
     * Lets the downstream operators know our schema
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(schema);
    }

    /**
     * Used for debug purposes, shows our exeuctor/operator's unique ID
     */
	@Override
	public String getExecutorId() {
		return executorId;
	}

	/**
	 * Called during topology setup, sets the router to the next
	 * bolt
	 */
	@Override
	public void setRouter(StreamRouter router) {
		this.collector.setRouter(router);
	}

	/**
	 * The fields (schema) of our output stream
	 */
	@Override
	public Fields getSchema() {
		return schema;
	}
}
