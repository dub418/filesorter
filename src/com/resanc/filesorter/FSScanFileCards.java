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
 * ��������� ������, �������� � ����������� ������.
 * ������������ ��� ����� ������ ��� ������������ ������ �� �����
 * @author Yury
 *
 */
public class FSScanFileCards {
	


	//���������� ������ ��������� ������
    private ArrayList<FSFileCard> filesList; //������ ���� ��������� ������
    private FSDiscLabels labels;
    private long counterDirectoties=0; //�������� �����
    private long counterFiles=0; //������� ������
    
    //���������� ��������� � ������� ��������� ������
    private boolean setCheckSumCalculateON=true; //��� ���������� ������� CRC32
    
	// ��������� ���������� �������� ������
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

	/** �������������� � ������ ��� ������
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
    
    /** ���������� �������� � ����� � ��������� ���������������� ������� ������ ������ (��������)
     * @param fp ������ ������
     */
    public void addToFileCards(File fp) throws Exception
    {
    File[] fpFiles = fp.listFiles();
    long lfiles=0;
    long ldirs=0;
         
    //�������� ������� ����������, ��� ��� �� ��������� null (����� ������� ������ ��� ������ �������� ������� ������ �������� ������)
    if (fp!=null)
      {
       if (fpFiles!=null)
       {  
            for (File fElem : fpFiles)
                {
                 //�������� �� ������� ���������
                 if (fElem!=null) 
                  { 
                    //��������� ��� ������   
                    if (fElem.isFile()) 
                       {
                          FSFileCard rec = new FSFileCard(fElem.getCanonicalPath(),fElem.getName(),fElem.length(),fElem.lastModified(),this.setCheckSumCalculateON, this.labels); 
                          if (!filesList.add(rec)){log.warning("Application can not add new record into file list for "+fElem.getAbsolutePath());}
                          addCounterFiles();
                          if (log.isLoggable(Level.INFO)) {log.info(getCounterFiles()+" point adds file to cards " + fElem.getAbsolutePath()+ " can=" +fElem.getCanonicalPath()+" ln="+fElem.lastModified());}
                       }
                          
                    //��������� ��� �����
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
                       	}//���.�����
                  }//�� null
                }//for
       }//fpFiles
      }//fp
    }//addToFileCards
}
