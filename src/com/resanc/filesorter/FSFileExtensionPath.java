/**
 * 
 */
package com.resanc.filesorter;

import java.util.logging.Logger;

/**
 * @author Yury
 *
 */
public class FSFileExtensionPath {
	/**
	 */
	public FSFileExtensionPath() {
		super();
		Ext = "";
		Path = "";
		PriorityLevel = 0;
	}
	/**
	 * @param ext
	 * @param path
	 */
	public FSFileExtensionPath(String ext, String path) {
		super();
		Ext = ext;
		Path = path;
		PriorityLevel = 0;
	}
	/**
	 * @param ext
	 * @param path
	 * @param priorityLevel
	 */
	public FSFileExtensionPath(String ext, String path, long priorityLevel) {
		super();
		Ext = ext;
		Path = path;
		PriorityLevel = priorityLevel;
	}
	/**
	 * @return the ext
	 */
	public String getExt() {
		return Ext;
	}
	/**
	 * @param ext the ext to set
	 */
	public void setExt(String ext) {
		Ext = ext;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return Path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		Path = path;
	}
	/**
	 * @return the priorityLevel
	 */
	public long getPriorityLevel() {
		return PriorityLevel;
	}
	/**
	 * @param priorityLevel the priorityLevel to set
	 */
	public void setPriorityLevel() {
		PriorityLevel = 0;
	}
	/**
	 * @param priorityLevel the priorityLevel to set
	 */
	public void setPriorityLevel(int priorityLevel) {
		PriorityLevel = priorityLevel;
	}
	private String Ext;
	private String Path;
	private long PriorityLevel;//0-none, 50-normal, 100-the highest
}//FSFileExtensionPath

class Extension {
	private static Logger log = Logger.getLogger(Extension.class.getName());
	String ext; // file extension
	String path;// path for such files

	public Extension(String se, String sp) {
		if ((se != null) && (sp != null)) {
			ext = se;
			path = sp;
		} else {
			log.severe("Error: null values fo ext. string");
		}
	}//Extension
}