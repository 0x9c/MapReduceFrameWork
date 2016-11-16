package edu.upenn.cis455.mapreduce.master;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.bolt.MapBolt;
import edu.upenn.cis.stormlite.bolt.ReduceBolt;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.distributed.WorkerJob;
import edu.upenn.cis.stormlite.spout.FileSpout;
import edu.upenn.cis.stormlite.tuple.Fields;
import test.edu.upenn.cis.stormlite.PrintBolt;
import test.edu.upenn.cis.stormlite.mapreduce.WordFileSpout;

public class MasterServlet extends HttpServlet {

  static final long serialVersionUID = 455555001;
  static Map<String, WorkerInfo> workerSet = new HashMap<>();
  
  
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
       throws java.io.IOException {
	    
	    String url = request.getRequestURI();
	    if (url.endsWith("workerstatus")) {            /* The master server and worker server haven't been connected yet */
	    	String ip = request.getRemoteAddr();
			int port = Integer.parseInt(request.getParameter("port"));
			String status = request.getParameter("status");
			String job = request.getParameter("job");
			int keysRead = Integer.parseInt(request.getParameter("keysread"));
			int keysWritten = Integer.parseInt(request.getParameter("keyswritten"));
			String resultsFromQuery = request.getParameter("results");
			List<String> results = resultsDeserialization(resultsFromQuery);
			WorkerInfo worker = new WorkerInfo(port, status, job, keysRead, keysWritten, results);
			String wholeIP = ip + ":" + port;
			synchronized(workerSet) {
				workerSet.put(wholeIP, worker);
			}
			PrintWriter out = response.getWriter();
//			out.println("worker status for " + wholeIP + " checked In");
	    }
	    
	    if(url.endsWith("status")) {
		    PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head>");
			out.println("<h2>Developer: Tianxiang Dong</h2>");
			out.println("<h2>PennKey: dtianx </h2>");
			out.println("</head>");
			out.println("<br>");
			out.println("<body>");
			out.println("<table>");
			out.println("<h2>Worker Report</h2>");
			out.println("<tr><th>IP:port</th><th>Status</th><th>Job</th><th>Keys Read</th><th>Keys Written</th><th>Results</th></tr>");
			for (String key : workerSet.keySet()) {
				WorkerInfo worker = workerSet.get(key);
				if (isActive(worker)) {
					out.println("<tr>");
					out.println("<td>" + key + "</td>");
					out.println("<td>" + worker.getStatus() + "</td>");
					out.println("<td>" + worker.getJob() + "</td>");
					out.println("<td>" + worker.getKeysRead() + "</td>");
					out.println("<td>" + worker.getKeysWritten() + "</td>");
					out.println("<td>" + worker.getResults().toString() + "</td>");
					out.println("</tr>");
				}
			}
				out.println("</table>");
				out.println("<h2>Job Submission</h2>");
				out.println("<form action=\"status\" method=\"POST\">");
				out.println("Class Name of Job:<br>");
				out.println("<input type=\"text\" name=\"job\">");
				out.println("<br><br>");
				out.println("Input Directory:<br>");
				out.println("<input type=\"text\" name=\"inputdirectory\">");
				out.println("<br><br>");
				out.println("OutputDirectory:<br>");
				out.println("<input type=\"text\" name=\"outputdirectory\">");
				out.println("<br><br>");
				out.println("MapBolt executors:<br>");
				out.println("<input type=\"text\" name=\"mapnumber\">");
				out.println("<br><br>");
				out.println("ReduceBolt executors:<br>");
				out.println("<input type=\"text\" name=\"reducenumber\">");
				out.println("<br><br>");
				out.println("<input type=\"submit\" value=\"Submit\">");
				out.println("</body></html>");
	    }
	   
  }
  
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
	  String jobClass = request.getParameter("job");
	  String inputDir = request.getParameter("inputdirectory");
	  String outputDir = request.getParameter("outputdirectory");
	  int maps = Integer.parseInt(request.getParameter("mapnumber"));
	  int reduces = Integer.parseInt(request.getParameter("reducenumber"));
	  
	  Config config = new Config();
      //config.put("workerList", "[127.0.0.1:8000,127.0.0.1:8001]");
	  config.put("workerList", "[127.0.0.1:8000]");	
      config.put("job", "MyJob");                /* Non-use name of something else? */
      config.put("master", "127.0.0.1:8080");
      config.put("mapClass", jobClass);
      config.put("reduceClass", jobClass);
      config.put("spoutExecutors", "1");
      config.put("mapExecutors", maps + "");
      config.put("reduceExecutors", reduces + "");
      
      String[] workers = WorkerHelper.getWorkers(config);
      int i = 0;
      for (String dest: workers) {
  	    	config.put("workerIndex", String.valueOf(i++));
  	        
    	    WorkerInfo w = workerSet.get(dest);
//    	    if(w == null || !isActive(w)) continue;     /* What if the worker is offline??? */
    	  
    	    FileSpout spout = new WordFileSpout();
	  		MapBolt bolt = new MapBolt();
	  		ReduceBolt bolt2 = new ReduceBolt();
	  		PrintBolt printer = new PrintBolt();
	  		
	  		TopologyBuilder builder = new TopologyBuilder();
	  		
	  		builder.setSpout("WORD_SPOUT", spout, Integer.valueOf(config.get("spoutExecutors")));
	  		builder.setBolt("MAP_BOLT", bolt, Integer.valueOf(config.get("mapExecutors"))).fieldsGrouping("WORD_SPOUT", new Fields("key", "value"));
	  		builder.setBolt("REDUCE_BOLT", bolt2, Integer.valueOf(config.get("reduceExecutors"))).fieldsGrouping("MAP_BOLT", new Fields("key"));
	  		builder.setBolt("PRINT_BOLT", printer, 1).firstGrouping("REDUCE_BOLT");
	  		
	  		Topology topo = builder.createTopology();
	  		
	  		WorkerJob job = new WorkerJob(topo, config);
	  		
	  		ObjectMapper mapper = new ObjectMapper();
	  		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
	  		try {
	  			if (sendJob(dest, "POST", config, "definejob", 
	  					mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job)).getResponseCode() != 
	  					HttpURLConnection.HTTP_OK) {
	  				throw new RuntimeException("Job definition request failed");
	  			}
	  			if (sendJob(dest, "POST", config, "runjob", "").getResponseCode() != 
	  					HttpURLConnection.HTTP_OK) {
	  				throw new RuntimeException("Job execution request failed");
	  				}
	  		
	  		} catch (IOException e) {
	  			// TODO Auto-generated catch block
	  			e.printStackTrace();
	  		    System.exit(0);
	  		}        
      }
      response.sendRedirect("status");
  }
  
  static HttpURLConnection sendJob(String dest, String reqType, Config config, String job, String parameters) throws IOException {
		URL url = new URL(dest + "/" + job);
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(reqType);
		
		if (reqType.equals("POST")) {
			conn.setRequestProperty("Content-Type", "application/json");
			
			OutputStream os = conn.getOutputStream();
			byte[] toSend = parameters.getBytes();
			os.write(toSend);
			os.flush();
		} else
			conn.getOutputStream();
		
		return conn;
  }
  
  public List<String> resultsDeserialization(String resultsFromQuery) {
		String list = resultsFromQuery;
		if (list.startsWith("["))
			list = list.substring(1);
		if (list.endsWith("]"))
			list = list.substring(0, list.length() - 1);
		
		String[] pairs = list.split(",");
		
		List<String> res = new ArrayList<>();
		for (String item: pairs){
			item.replaceAll("-", ":");
			res.add(item);
		}	
		return res;
	}
  
//  public List<String> resultsDeserialization(String resultsFromQuery) {   // ',' for pair separating , '-' for key value separating
//	  String[] split = resultsFromQuery.split(",");
//	  List<String> res = new ArrayList<>();
//	  for(String pair: split) {
//		  String[] p = pair.split("-");
//		  if(p.length == 2) {
//			  String key = p[0];
//			  String value = p[1];
//			  res.add(key + " : " + value);
//		  }
//	  }
//	  return res;
//  }
  
  public boolean isActive(WorkerInfo worker){
	  Date date = Calendar.getInstance().getTime();
	  long current = date.getTime();
	  return current - worker.getLastReport() < 30000;
  }
}
  
