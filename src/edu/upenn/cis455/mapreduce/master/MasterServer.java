package edu.upenn.cis455.mapreduce.master;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Main class to start master server using Embeded jetty
 * @author dongtianxiang
 *
 */
public class MasterServer {
	
	public static void main(String[] args) throws Exception{
		Properties props = new Properties();
    	props.load(new FileInputStream("./resources/log4j.properties"));
    	PropertyConfigurator.configure(props);
    	
    	HandlerCollection handlers = new HandlerCollection();
		WebAppContext web = new WebAppContext();
		web.setResourceBase(".");
		web.setContextPath("/");
		web.setDefaultsDescriptor("./target/master/WEB-INF/web.xml");
		handlers.addHandler(web);
		
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setResourceBase(".");
		
		handlers.addHandler(resourceHandler);
		
    	Server server = new Server(8080);
		server.setHandler(handlers);
		server.start();
		server.join();
	}
}
