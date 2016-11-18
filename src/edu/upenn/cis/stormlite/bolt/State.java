package edu.upenn.cis.stormlite.bolt;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Entity;

/**
 * Entity Class used to store StateByKey information into Database
 * @author cis555
 *
 */

@Entity
public class State {
	@PrimaryKey
	private String key;
	private List<String> list;
	
	public State(){}
	
	public State(String key) {
		this.key = key;
		this.list = new ArrayList<>();
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return this.key;
	}
	
	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}
	
	public void addList(String element){
		this.list.add(element);
	}
	
	public void removeList(String element){
		this.list.remove(element);
	}
}
