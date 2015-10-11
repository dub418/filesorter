package com.resanc.filesorter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

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
	
	/**
	 * Читает файлы формата офисе doc/docx
	 * 
	 * @param fn
	 */
	public String getDocFileText(String fn, boolean docx) {
		File file = null;
		WordExtractor extractor = null;
		XWPFWordExtractor xextractor = null;
		String res = "";
		try {
			file = new File(fn);
			FileInputStream fis = new FileInputStream(file.getAbsolutePath());
			if (docx) {
				HWPFDocument document = new HWPFDocument(fis);
				extractor = new WordExtractor(document);
				String[] fileData = extractor.getParagraphText();
				for (int i = 0; i < fileData.length; i++) {
					if (fileData[i] != null) {
						res = res + fileData[i];
						System.out.println(fileData[i]);
					}
				}
			} else {
				XWPFDocument document = new XWPFDocument(fis);
				xextractor = new XWPFWordExtractor(document);
				String fileData = xextractor.getText();
				System.out.println(fileData);
				// .getText();// .getParagraphText();
			}
			fis.close();
			// file.close();
		} catch (Exception exep) {
			exep.printStackTrace();
		}
		return res;
	}
	
	
    /**Читает текст из pdf файла построчно. Использует iText и BouncyCastle
     * @param fn
     * @return
     * @throws IOException
     */
    public String getPdfFileText(String fn) throws IOException
    {
    	        String text="";
    			// считаем, что программе передается один аргумент - имя файла
    	        System.out.println("fn="+fn);
    	        PdfReader reader = new PdfReader(fn);
    	        System.out.println("reader="+reader.toString());

    	        // не забываем, что нумерация страниц в PDF начинается с единицы.
    	        for (int i = 1; i <= reader.getNumberOfPages(); ++i) {
    	        	System.out.print(i+". ");
    	        	TextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
    	        	System.out.print(i+". ");
    	            text = PdfTextExtractor.getTextFromPage(reader, i, strategy);
    	            System.out.println(text);
    	        }
    	        System.out.println("="+reader.getFileLength());
    	        // убираем за собой
    	        reader.close();
    	return text;
    }
    
	private void addToList(File fp) {
		File[] fpFiles = fp.listFiles();
		// проверка входных параметров, что они не указывают null (папки нижнего
		// уровня без файлов содержат нулевые списки входящих файлов)
		if (fp != null) {
			if (fpFiles != null) {
				for (File fElem : fpFiles) {
					// проверка на нулевые указатели
					if (fElem != null) {
						if (fElem.isFile()) {
							try {
								// занесение имени файла в список, обработка и
								// классификация
								String ext1 = FSSQLDatabase.getExt(fElem.getCanonicalPath());
								if (ext1.equals(".pdf")) {
								System.out.println(ext1+" "+fElem.getCanonicalPath());
								System.out.println("---------------------------------------------------+");
								try {
								String txt=this.getPdfFileText(fElem.getCanonicalPath()); 
								System.out.println("---------------------------------------------------=");
								}catch (Exception e){System.out.println("Error "+e.getMessage());}
								}//pdf
								if (ext1.equals(".doc")||ext1.equals(".docx")) {
									System.out.println(ext1+" "+fElem.getCanonicalPath());
									System.out.println("---------------------------------------------------+");
									try {
									String txt=this.getDocFileText(fElem.getCanonicalPath(),(ext1.equals(".docx"))); 
									System.out.println("---------------------------------------------------=");
									}catch (Exception e){System.out.println("Error "+e.getMessage());}
									}//doc/docx
							} catch (IOException ex) {
								log.severe("I/O Error in file list. ErrorMsg = " + ex.getMessage());
							}

						} // is file
							// обработка для папок
						if (fElem.isDirectory()) {
							try {
								addToList(fElem);
							} catch (Exception ex) {
								log.severe("Recursion problem to add to file list. ErrorMsg = " + ex.getMessage());
							}
						} // is Directory
					} // fElem <> null
				} // for
			} // не null fpFiles
		} // fp
	}// addToFileList
}
