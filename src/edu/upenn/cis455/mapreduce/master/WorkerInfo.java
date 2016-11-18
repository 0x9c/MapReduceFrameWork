package edu.upenn.cis455.mapreduce.master;

import java.util.Calendar;
import java.util.List;

/**
 * Class to store progress information about worker
 * @author dongtianxiang
 *
 */
public class WorkerInfo {
	private int port;
	private String status;
	private String job;
	private int keysRead;
	private int keysWritten;
	private List<String> results;  // up to first 100 outputs
	private long lastReport;
	
	public WorkerInfo(int port, String status, String job, int keysRead, int keysWritten, List<String> results){
		this.setPort(port);
		this.setStatus(status);
		this.setJob(job);
		this.setKeysRead(keysRead);
		this.setKeysWritten(keysWritten);
		this.setResults(results);
		setLastReport(Calendar.getInstance().getTime().getTime());
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public int getKeysRead() {
		return keysRead;
	}

	public void setKeysRead(int keysRead) {
		this.keysRead = keysRead;
	}

	public int getKeysWritten() {
		return keysWritten;
	}

	public void setKeysWritten(int keysWritten) {
		this.keysWritten = keysWritten;
	}

	public List<String> getResults() {
		return results;
	}

	public void setResults(List<String> results) {
		this.results = results;
	}

	public long getLastReport() {
		return lastReport;
	}

	public void setLastReport(long lastReport) {
		this.lastReport = lastReport;
	}
	
}
