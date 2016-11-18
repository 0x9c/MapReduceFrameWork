package edu.upenn.cis.stormlite.bolt;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

/**
 * Basic class to connect Berkeley DB, storing all temporary data in database
 * @author cis555
 *
 */
public class DBWrapper {
	
	private String envDirectory = null;
	
	private Environment myEnv;
	private EntityStore store;
	
	private static DBWrapper DBinstance = null;
	
	public DBWrapper(String envDirectory){
		//Initialize myEnv
		close();
		this.envDirectory = envDirectory;
		try{
			EnvironmentConfig envConfig = new EnvironmentConfig();
			//Create new myEnv if it does not exist
			envConfig.setAllowCreate(true);
			//Allow transactions in new myEnv
			envConfig.setTransactional(true);
			//Create new myEnv
			File dir = new File(envDirectory);
			if(!dir.exists())
			{
				dir.mkdir();
				dir.setReadable(true);
				dir.setWritable(true);
			}
			myEnv = new Environment(dir,envConfig);
			
			//Create new entity store object
			StoreConfig storeConfig = new StoreConfig();
			storeConfig.setAllowCreate(true);
			storeConfig.setTransactional(true);
			store = new EntityStore(myEnv,"DBEntityStore",storeConfig);
		}
		catch(DatabaseException e)
		{
			e.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void sync(){
		if(store != null) store.sync();
		if(myEnv != null) myEnv.sync();
	}
	
	public Environment getEnvironment()
	{
		return myEnv;
	}
	
	public EntityStore getStoreUser()
	{
		return store;
	}
	
	public EntityStore getStoreCrawler()
	{
		return store;
	}
	
	//Close method
	public void close()
	{
		sync();
		//Close store first as recommended
		if(store!=null)
		{
			try{
				store.close();
			}
			catch(DatabaseException e)
			{
				e.printStackTrace();
			}
		}
		
		
		if(myEnv!=null)
		{
			try{
				myEnv.close();
			}
			catch(DatabaseException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void putState(String key) {
		PrimaryIndex<String, State> statePIndex = store.getPrimaryIndex(String.class, State.class);
		State state = new State(key);
		putState(state);
	}
	
	public void putState(State state) {
		PrimaryIndex<String, State> statePIndex = store.getPrimaryIndex(String.class, State.class);
		statePIndex.put(state);
	}
	
	public State getState(String key) {
		PrimaryIndex<String, State> statePIndex = store.getPrimaryIndex(String.class, State.class);
		State state = statePIndex.get(key);
		return state;
	}
	
	public void addValueToList(String key, String value) {
		State state = getState(key);
		if(state == null) {
			state = new State(key);
		}
		state.addList(value);
		putState(state);
	}
	
	public List<String> getListByKey(String key) {
		State state = getState(key);
		if(state == null) return null;
		return state.getList();
	}
	
	public Set<String> getKeySet(){
		Set<String> keys = new HashSet<>();
		PrimaryIndex<String, State> stateIndex = store.getPrimaryIndex(String.class, State.class);
		EntityCursor<State> cursor = stateIndex.entities();
		try{
			for(State state : cursor) keys.add(state.getKey());
		} finally {
			cursor.close();
		}
		return keys;
	}

}

