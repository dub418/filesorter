package com.resanc.filesorter;

import java.util.ArrayList;

public class FSFileExtList {
	private String generator="FSSQLDatabase";
	private long version=1L;
	public ArrayList<FSFileExt> extList;
	@Override
	public String toString() {
		return "FSFileExtList [generator=" + generator + ", version=" + version + ", extList=" + extList + "]";
	}
	public String getGenerator() {
		return generator;
	}
	public void setGenerator(String generator) {
		this.generator = generator;
	}
	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}
	public ArrayList<FSFileExt> getExtList() {
		return extList;
	}
	public void setExtList(ArrayList<FSFileExt> extList) {
		this.extList = extList;
	}

}
