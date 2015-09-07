/**
 * Класс хранит и возвращает метки дисков данного компьютера 
 */
package com.resanc.filesorter;

import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileSystemView;

/**
 * @author Yury
 *
 */
class OneLabel 
	{
	public String drv;
	public String lbl;
	
	public OneLabel(String st)
	{
		  drv = st;
	 	  FileSystemView view = FileSystemView.getFileSystemView();
		  String s=st+"/";
		  File dir = new File(s);
		  s = view.getSystemDisplayName(dir).trim();//like "Sys (C:)"
		  s = s.substring(0, s.lastIndexOf("("));
		  if (s==null) {lbl = "";}
		  else {lbl = s.trim();}//like "Sys"	
	}
	
	};
public class FSDiscLabels {
	private ArrayList<OneLabel> labels;
    public FSDiscLabels(){ labels = new ArrayList<OneLabel>();}
    public int setLabel(String dr)
    {
    	for (OneLabel eLab : labels)
    	{
    		if (eLab.drv.equals(dr)) { return 1;}
    	}
    	labels.add(new OneLabel(dr));
    	return 0;
    }
    
    public String getLabel(String dr)
    {
    	for (OneLabel eLab : labels)
    	{
    		if (eLab.drv.equals(dr)) { return eLab.lbl;}
    	}
    	OneLabel nw = new OneLabel(dr);
    	labels.add(nw);
    	return nw.lbl;
    }   
}
