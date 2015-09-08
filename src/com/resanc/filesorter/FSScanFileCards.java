/**
 * 
 */
package com.resanc.filesorter;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;

/**
 * Картотека файлов, хранимая в оперативной памяти.
 * Используется для сбора данных при сканировании файлов на диске
 * @author Yury
 *
 */
public class FSScanFileCards {
	


	//переменные данных картотеки файлов
    private ArrayList<FSFileCard> filesList; //список всех найденных файлов
    private FSDiscLabels labels;
    private long counterDirectoties=0; //счетчика папок
    private long counterFiles=0; //счетчик файлов
    
    //переменные настройки и статуса картотеки файлов
    private boolean setCheckSumCalculateON=true; //при заполнении считать CRC32
    
	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSScanFileCards.class.getName());    
    
	//----
	private long addCounterDirectories() { return this.counterDirectoties++; }
    private long addCounterFiles()    { return this.counterFiles++; }
    private long getCounterDirectories() { return this.counterDirectoties; }
    private long getCounterFiles()    { return this.counterFiles; }
    
    public FSScanFileCards()
    {
    	filesList=new ArrayList<FSFileCard>();
    	labels=new FSDiscLabels();
    }
    
    public ArrayList<FSFileCard> getScanFileCards() { return filesList; };

	/** Преобразование в строку для вывода
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FSScanFileCards [filesList=" + filesList + ", counterDirectoties=" + counterDirectoties
				+ ", counterFiles=" + counterFiles + ", setCheckSumCalculateON=" + setCheckSumCalculateON
				+ ", getCounterDirectories()=" + getCounterDirectories() + ", getCounterFiles()=" + getCounterFiles()
				+ ", getFileCardsSize()=" + getFileCardsSize() + "]";
	}
    
    public long getFileCardsSize() { return filesList.size(); }
    
    /** Добавление сведений о файле в картотеку последовательным обходом дерева файлов (рекурсия)
     * @param fp начало обхода
     */
    public void addToFileCards(File fp) throws Exception
    {
    File[] fpFiles = fp.listFiles();
    long lfiles=0;
    long ldirs=0;
         
    //проверка входных параметров, что они не указывают null (папки нижнего уровня без файлов содержат нулевые списки входящих файлов)
    if (fp!=null)
      {
       if (fpFiles!=null)
       {  
            for (File fElem : fpFiles)
                {
                 //проверка на нулевые указатели
                 if (fElem!=null) 
                  { 
                    //обработка для файлов   
                    if (fElem.isFile()) 
                       {
                          FSFileCard rec = new FSFileCard(fElem.getCanonicalPath(),fElem.getName(),fElem.length(),fElem.lastModified(),this.setCheckSumCalculateON, this.labels); 
                          if (!filesList.add(rec)){log.warning("Application can not add new record into file list for "+fElem.getAbsolutePath());}
                          addCounterFiles();
                          if (log.isLoggable(Level.INFO)) {log.info(getCounterFiles()+" point adds file to cards " + fElem.getAbsolutePath()+ " can=" +fElem.getCanonicalPath()+" ln="+fElem.lastModified());}
                       }
                          
                    //обработка для папок
                    if (fElem.isDirectory())
                       {
                    	  addCounterDirectories();
                    	  if (log.isLoggable(Level.INFO)) {log.info(getCounterDirectories()+" folders adds. Now size of file list is "+this.filesList.size()+ " on " + fElem.getAbsolutePath());}
                          try 
                          	{  	addToFileCards(fElem);
                          	} catch(Exception ex)
                           		{ log.warning("Recursion problem with addition to file list. Params: "+
                           					  "FoldersCount = " + getCounterDirectories()+", FilesCount = "+getCounterFiles()+
                            		   			  "\r\nPath = " + fElem.getPath() + "\r\n File = " + fElem.toString() + "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
                           		  throw ex;
                           		}
                       	}//обр.папок
                  }//не null
                }//for
       }//fpFiles
      }//fp
    }//addToFileCards
}
