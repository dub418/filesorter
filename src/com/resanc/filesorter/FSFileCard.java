/** Пакет содержит Классы для управления файлами домашнего архива
 * 
 */
package com.resanc.filesorter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import javax.swing.filechooser.FileSystemView;

/**
 * Класс предназначен для хранения в памяти информации об одном файле
 * Используется при сканировании диска
 * 
 * @author Yury
 *
 */
public class FSFileCard {

	// свойства, описывающие файл
	private long fileSize = 0;
	private long checkSumCRC32 = 0;
	private int ErrStatus = 0;
	private boolean checkSumCalculated = false;
	private String shortName = "";
	private String fullPath = "";
	private long lastModification = 0;
	private String discLabel="";

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSFileCard.class.getName());

	// статусные константы
	public static final int OK = 0;
	public static final int NOT_EXIST = -1;
	public static final int NOT_A_FOLDER = -2;
	public static final int NOT_AVAILABLE = -3;
	public static final int OUT_OF_MEMORY = -4;

	public static final boolean WITH_CRC32 = true;
	public static final boolean WITHOUT_CRC32 = false;

	public static final int TO_SHORT_STRING_FORM = 1;
	public static final int TO_FULL_STRING_FORM = 2;
	public static final int TO_CSV_STRING_FORM = 3;

	public FSFileCard(String pth, String nm, long ln, long dt, boolean isCalc, FSDiscLabels lab) {
		this.fullPath = pth;
		this.shortName = nm;
		this.fileSize = ln;
		this.lastModification = dt;
		if (isCalc) {
			this.calculateCRC32();
			this.checkSumCalculated = true;
		}
		 	  //String path;
		      
		 	  //FileSystemView view = FileSystemView.getFileSystemView();
		 	  //String s=fullPath.substring(0,fullPath.indexOf(File.separatorChar))+"/";
			  //File dir = new File(s);
			  //s = view.getSystemDisplayName(dir).trim();//like "Sys (C:)"
			  //s = s.substring(0, s.lastIndexOf("("));
			  //if (s==null) {this.discLabel = "";}
			  //else {this.discLabel = s.trim();}//like "Sys"
			  
	}
	
	public String getLastModificationAsString()
	{
		SimpleDateFormat sdf = new SimpleDateFormat();
	    sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
	    String ds=sdf.format(String.valueOf(lastModification));
	    return ds;
	}
	
	/**
	 * @return the fileSize
	 */
	public long getFileSize() {
		return fileSize;
	}
	
	/**
	 * @return the discLabel
	 */
	public String getDiscLabel() {
		return discLabel;
	}

	/**
	 * @return the checkSumCRC32
	 */
	public long getCheckSumCRC32() {
		return checkSumCRC32;
	}

	/**
	 * @return the errStatus
	 */
	public int getErrStatus() {
		return ErrStatus;
	}

	/**
	 * @return the checkSumCalculated
	 */
	public boolean isCheckSumCalculated() {
		return this.checkSumCalculated;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FSFileCard [" + "shortName=" + shortName + ", fullPath=" + fullPath + ", fileSize=" + fileSize
				+ ", checkSumCRC32=" + checkSumCRC32 + ", ErrStatus=" + ErrStatus + ", checkSumCalculated="
				+ checkSumCalculated + ", lastModification=" + lastModification + "discLabel-"+discLabel+"]";
	}

	public String toString(int iForm) {
		switch (iForm) {
		case TO_FULL_STRING_FORM:
			return "Файл: " + shortName + "(путь: " + fullPath + ") размер: " + fileSize + " CRC32: " + checkSumCRC32
					+ " изменен:" + new Date(this.lastModification)+" метка тома:"+discLabel;
		case TO_CSV_STRING_FORM:
			String sep = ";";
			return "" + this.fileSize + sep + this.checkSumCRC32 + sep + this.shortName + sep + this.lastModification
					+ sep + this.fullPath + sep + new Date(this.lastModification) + sep + this.ErrStatus + sep
					+ this.checkSumCalculated + sep + discLabel;
		case TO_SHORT_STRING_FORM:
			return "Файл: " + shortName + "(путь: " + fullPath + ") размер: " + fileSize + " изменен :"
					+ new Date(this.lastModification);
		default:
			return toString();
		}
	}// toString

	public long calculateCRC32() {
		File fp1 = new File(this.fullPath);
		byte[] buf = null;
		CRC32 chSum = new CRC32();

		try {
			FileInputStream fis = null;
			fis = new FileInputStream(fp1);

			int blocksize = 50000000;
			if (fis.available() > blocksize) {
				buf = new byte[blocksize];
			} else {
				buf = new byte[fis.available()];
			}

			int bytesRead = 0;
			while (fis.available() != 0) {
				bytesRead = fis.read(buf);
				chSum.update(buf);
			}

			this.checkSumCRC32 = chSum.getValue();
			fis.close();
		} catch (FileNotFoundException e) {
			ErrStatus = NOT_EXIST;
			log.warning("This file is not exist (filename is " + fullPath + ") with error message: " + e.getMessage());
		} catch (IOException e) {
			ErrStatus = NOT_AVAILABLE;
			log.warning(
					"This file is not available (filename is " + fullPath + ") with error message: " + e.getMessage());
		} catch (Exception e) {
			ErrStatus = OUT_OF_MEMORY;
			log.warning("This file results out of memory (filename is " + fullPath + ") with error message: "
					+ e.getMessage());
		}
		if (checkSumCRC32 == 0) {
			log.warning(" STRANGE WARNING: " + this.fullPath + " Size=" + this.fileSize + " CRC=" + checkSumCRC32
					+ " bufsize=" + buf.length + " Buf=" + buf.toString());
		}
		return this.checkSumCRC32;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @return the fullPath
	 */
	public String getFullPath() {
		return fullPath;
	}

	/**
	 * @return the lastModification
	 */
	public long getLastModification() {
		return lastModification;
	}

}