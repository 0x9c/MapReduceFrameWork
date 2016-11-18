package test.edu.upenn.cis.stormlite.mapreduce;

import edu.upenn.cis.stormlite.spout.FileSpout;

public class WordFileSpout extends FileSpout {
	private final String baseStore = "/store";
	private final String fileName = "words.txt";
	
	@Override
	public String getFilename() {
		String input = this.config.get("inputDir");
		return System.getProperty("user.home") + baseStore + "/" + input + "/" + fileName;
	}

}
