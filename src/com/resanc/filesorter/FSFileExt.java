package com.resanc.filesorter;

public class FSFileExt {
	private String Ext;
	private String Path;
	private long PriorityLevel;
	public String getExt() {
		return Ext;
	}
	public void setExt(String ext) {
		Ext = ext;
	}
	public String getPath() {
		return Path;
	}
	public void setPath(String path) {
		Path = path;
	}
	public long getPriorityLevel() {
		return PriorityLevel;
	}
	public void setPriorityLevel(long priorityLevel) {
		PriorityLevel = priorityLevel;
	}
	@Override
	public String toString() {
		return "FSFileExt [Ext=" + Ext + ", Path=" + Path + ", PriorityLevel=" + PriorityLevel + "]";
	}
	
}
