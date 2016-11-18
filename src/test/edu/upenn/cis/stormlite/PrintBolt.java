package test.edu.upenn.cis.stormlite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis455.mapreduce.worker.WorkerStatus;

/**
 * A trivial bolt that simply outputs its input stream to the
 * console
 * 
 * @author zives
 *
 */
public class PrintBolt implements IRichBolt {
	static Logger log = Logger.getLogger(PrintBolt.class);
	int neededVotesToComplete = 0;
	Fields myFields = new Fields();
	Map<String, String> config;
	
	
    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the PrintBolt, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
	@Override
	public void cleanup() {
		// Do nothing

	}

	@Override
	public void execute(Tuple input) {
		
		if (!input.isEndOfStream()) {
			System.out.println(getExecutorId() + ": " + input.toString());
			
			try {
				FileWriter fw = new FileWriter(getFileName(), true);    // append to new file
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw);
			    String key = input.getStringByField("key");
		        String value = input.getStringByField("value");
				out.println("(" + key + ", " + value + ")");
				out.close();     // Close writer
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			neededVotesToComplete--;
			if(neededVotesToComplete == 0) {
				WorkerStatus.setStatus("WAITING");
			}
		}
	}
	
	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.config = stormConf;
		
		/* get ready for output directory and file */
		File outputDirectory = new File(System.getProperty("user.home") + "/store/" + config.get("outputDir"));
		if(!outputDirectory.exists()) outputDirectory.mkdir();
		File outputFile = new File(System.getProperty("user.home") + "/store/" + config.get("outputDir") + "/" + "output.txt");
		if(outputFile.exists()) outputFile.delete();
		
        int reducers = Integer.parseInt(stormConf.get("reduceExecutors"));
        String[] workers = WorkerHelper.getWorkers(stormConf);
        int workerNum = workers.length;
		int reducerEOS = 1;
        neededVotesToComplete = reducerEOS * reducers + reducerEOS * reducers * (workerNum - 1);
	}

	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void setRouter(StreamRouter router) {
		// Do nothing
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(myFields);
	}

	@Override
	public Fields getSchema() {
		return myFields;
	}
	
	private String getFileName(){
		String output = config.get("outputDir");
		return System.getProperty("user.home") + "/store/" + output + "/" + "output.txt";
	}
	
}
