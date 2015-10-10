package com.resanc.filesorter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class FSTextFileList {
	private ArrayList<FSTextFile> filL;
	private static Logger log = Logger.getLogger(FSTextFileList.class.getName());

	public long getFilesFromDisc(String path) {
		if ((path!=null)&&(!path.isEmpty())){
		File fp=new File(path);
		this.addToList(fp);}
		return 1;
	}
	
    /**������ ����� �� pdf ����� ���������. ���������� iText � BouncyCastle
     * @param fn
     * @return
     * @throws IOException
     */
    private String getPdfFileText(String fn) throws IOException
    {
    	        String text="";
    			// �������, ��� ��������� ���������� ���� �������� - ��� �����
    	        System.out.println("fn="+fn);
    	        PdfReader reader = new PdfReader(fn);
    	        System.out.println("reader="+reader.toString());

    	        // �� ��������, ��� ��������� ������� � PDF ���������� � �������.
    	        for (int i = 1; i <= reader.getNumberOfPages(); ++i) {
    	        	System.out.print(i+". ");
    	        	TextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
    	        	System.out.print(i+". ");
    	            text = PdfTextExtractor.getTextFromPage(reader, i, strategy);
    	            System.out.println(text);
    	        }
    	        System.out.println("="+reader.getFileLength());
    	        // ������� �� �����
    	        reader.close();
    	return text;
    }
    
	private void addToList(File fp) {
		File[] fpFiles = fp.listFiles();
		// �������� ������� ����������, ��� ��� �� ��������� null (����� �������
		// ������ ��� ������ �������� ������� ������ �������� ������)
		if (fp != null) {
			if (fpFiles != null) {
				for (File fElem : fpFiles) {
					// �������� �� ������� ���������
					if (fElem != null) {
						if (fElem.isFile()) {
							try {
								// ��������� ����� ����� � ������, ��������� �
								// �������������
								String ext1 = FSSQLDatabase.getExt(fElem.getCanonicalPath());
								if (ext1.equals(".pdf")) {
								System.out.println(ext1+" "+fElem.getCanonicalPath());
								System.out.println("---------------------------------------------------+");
								try {
								String txt=this.getPdfFileText(fElem.getCanonicalPath()); 
								System.out.println("---------------------------------------------------=");
								}catch (Exception e){System.out.println("Error "+e.getMessage());}
								}
							} catch (IOException ex) {
								log.severe("I/O Error in file list. ErrorMsg = " + ex.getMessage());
							}

						} // is file
							// ��������� ��� �����
						if (fElem.isDirectory()) {
							try {
								addToList(fElem);
							} catch (Exception ex) {
								log.severe("Recursion problem to add to file list. ErrorMsg = " + ex.getMessage());
							}
						} // is Directory
					} // fElem <> null
				} // for
			} // �� null fpFiles
		} // fp
	}// addToFileList
}
