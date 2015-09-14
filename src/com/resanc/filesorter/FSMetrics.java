/**
 * 
 */
package com.resanc.filesorter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yury
 *
 */
public class FSMetrics {
	private long previousTime = 0;
	private long currentTime = 0;

	private static Logger log = Logger.getLogger(FSMetrics.class.getName());

	/**
	 * ������� ����� ��������� �������
	 * 
	 */
	public FSMetrics() {
		currentTime = System.currentTimeMillis();
	}

	/**
	 * �������� � ��� ������� �������
	 * 
	 * @param comment
	 */
	public long getTime(String comment, long mtr, String unit) {
		if ((comment == null) || (comment == "")) {
			comment = " process";
		}
		previousTime = currentTime;
		currentTime = System.currentTimeMillis();
		if (currentTime==previousTime) currentTime++;
		if (log.isLoggable(Level.INFO)) {
			log.info("Time of " + comment + " is " + (currentTime - previousTime) + " ms. " + ((mtr*1000L)/(currentTime - previousTime)) +" "+unit+" per second.");					
		}
		return currentTime - previousTime;
	}
}
