/**
 * 
 */
package com.resanc.filesorter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Yury
 *
 */
public class FSZipArc {
	public File directory; // folder for result files
	private File TempDirectory;// folder for temporary files
	// служебные внутренние свойства класса
		private static Logger log = Logger.getLogger(FSZipArc.class.getName());

	public  void pack(File directory, String to) throws IOException {
		URI base = directory.toURI();
		Deque<File> queue = new LinkedList<File>();
		queue.push(directory);
		OutputStream out = new FileOutputStream(new File(to));
		Closeable res = out;

		try {
			ZipOutputStream zout = new ZipOutputStream(out);
			res = zout;
			while (!queue.isEmpty()) {
				directory = queue.pop();
				for (File child : directory.listFiles()) {
					String name = base.relativize(child.toURI()).getPath();
					if (child.isDirectory()) {
						queue.push(child);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					} else {
						zout.putNextEntry(new ZipEntry(name));
						InputStream in = new FileInputStream(child);
						try {
							byte[] buffer = new byte[1024];
							while (true) {
								int readCount = in.read(buffer);
								if (readCount < 0) {
									break;
								}
								zout.write(buffer, 0, readCount);
							}
						} finally {
							in.close();
						}
						zout.closeEntry();
					}
				}
			}
		} finally {
			res.close();
		}
	}
	
	private static String FS_ZIP_TEMP_DIR="TempUnpackDir";
	public static File unpackTempDirectory=null;
	
	public static int unpack(File fpath) {
		System.out.println("unpack started...");
		int res=-1;
		File ftmp = new File(FS_ZIP_TEMP_DIR);
		ftmp.mkdir();
		unpackTempDirectory = ftmp;
		
		try {
			System.out.println("ftmp="+ftmp.getCanonicalPath());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//пытаемся распаковать архив в эту временную папку
		try {
		ZipFile zip = new ZipFile(fpath);
		Enumeration entries = zip.entries();
		LinkedList<ZipEntry> zfiles = new LinkedList<ZipEntry>();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (entry.isDirectory()) {
				new File(FS_ZIP_TEMP_DIR + "/" + entry.getName()).mkdir();
			} else {
				zfiles.add(entry);
			}
		}
		for (ZipEntry entry : zfiles) {
			InputStream in = zip.getInputStream(entry);
			OutputStream out = new FileOutputStream(FS_ZIP_TEMP_DIR + "/" + entry.getName());
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) >= 0)
				out.write(buffer, 0, len);
			in.close();
			out.close();
		}
		zip.close();
		res=0;}
		catch (ZipException e){}
		catch (IOException e){}
		catch (SecurityException e){}
		return res;
	}
	
	public static void unpack(String path, String dir_to) throws IOException {
		ZipFile zip = new ZipFile(path);
		Enumeration entries = zip.entries();
		LinkedList<ZipEntry> zfiles = new LinkedList<ZipEntry>();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (entry.isDirectory()) {
				new File(dir_to + "/" + entry.getName()).mkdir();
			} else {
				zfiles.add(entry);
			}
		}
		for (ZipEntry entry : zfiles) {
			InputStream in = zip.getInputStream(entry);
			OutputStream out = new FileOutputStream(dir_to + "/" + entry.getName());
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) >= 0)
				out.write(buffer, 0, len);
			in.close();
			out.close();
		}
		zip.close();
	}

	/**Просматривает указанную папку и подпапки, находит в них zip и fb2, 
	 * управляет извлечением строк-дайджестов из них
	 * @param fp
	 */
	public void getFb2DigestList(File fp)
	{
		pp("Начинаем обходить папку и искать в ней архивы и книги. Путь начала: "+fp.toString());
		File[] fpFiles = fp.listFiles();
			// проверка входных параметров, что они не указывают null (папки нижнего
			// уровня без файлов содержат нулевые списки входящих файлов)
			if (fp != null) {
				if (fpFiles != null) {
					pp("       fpFiles");
					for (File fElem : fpFiles) {
						// проверка на нулевые указатели
						if (fElem != null) {
							// обработка для файлов
							if (fElem.isFile()) {
								pp("               try unpack "+fElem.toString());
								//Если это zip архив, вытаскиваем его содержимое во временную папку
								if (unpack(fElem)==0){
									//Вытащить дайджесты из всех файлов из временной папки
									pp("here zip is "+fElem.getAbsolutePath());
									getDigestsFromFb2Directory(new File(FS_ZIP_TEMP_DIR));
									//Удалить временную папку со всеми файлами
								}
								//Если это fb2 файл, вытаскиваем из него дайджест
								pp("here fb2 is "+fElem.getAbsolutePath());
								getFb2Digest(fElem);
								}
							}
							// обработка для папок
							if (fElem.isDirectory()) {
								try {
									getFb2DigestList(fElem); 
								} catch (Exception ex) {
									log.severe("Recursion problem with addition to file list. Params: " + fElem.toString()
											+ "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
								}
							} // обр.папок
					} // for
				} // fpFiles
			} // fp
	}
	
	public void getDigestsFromFb2Directory(File fp)
	{
		File[] fpFiles = fp.listFiles();
		pp("Blbla "+fp.toString());
			// проверка входных параметров, что они не указывают null (папки нижнего
			// уровня без файлов содержат нулевые списки входящих файлов)
			if (fp != null) {
				if (fpFiles != null) {
					for (File fElem : fpFiles) {
						// проверка на нулевые указатели
						if (fElem != null) {
							// обработка для файлов
							if (fElem.isFile()) {
								//вытаскиваем дайджест из файла
								pp("here getDigestsFromFb2Directory "+fElem.getAbsolutePath());
								getFb2Digest(fElem);
								}
							}
							// обработка для папок
							if (fElem.isDirectory()) {
								try {
									getDigestsFromFb2Directory(fElem); 
								} catch (Exception ex) {
									log.severe("Recursion problem with addition to file list. Params: " + fElem.toString()
											+ "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
								}
							} // обр.папок
					} // for
				} // fpFiles
			} // fp
	}
	private  int lip=0;
    private  void pp( String ss){
    	if (ss!=null) {System.out.println(lip++ +") "+ss);}else { System.out.println(lip++);}}
      
	public  FSDescriptionBook getFb2Digest(File fp) {

		String fn = "";
		FSDescriptionBook fbook = new FSDescriptionBook();
		if (fp!=null){ 
		try {
				fn = fp.getCanonicalPath();
			
	            FileInputStream file = new FileInputStream(fp);
	                 
	            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	             
	            DocumentBuilder builder =  builderFactory.newDocumentBuilder();
	             
	            Document xmlDocument = builder.parse(file);
	            
	            XPath xPath =  XPathFactory.newInstance().newXPath();
	            
	            //*Заголовок*
	            String expression = "/FictionBook/description/title-info/book-title";
	            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                fbook.setFullTitle(nodeList.item(i).getFirstChild().getNodeValue());
	            }
	            
	            //*Аннотация*
	            expression = "/FictionBook/description/title-info/annotation/p";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            String s="";
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                s=s+" "+nodeList.item(i).getFirstChild().getNodeValue(); 
	            }
	            fbook.setFullAnnotation(s);
	            
	            //*Жанр*
	            expression = "/FictionBook/description/title-info/genre";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	            	fbook.addGenre(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	            
	            //*Автор фамилия*
	            expression = "/FictionBook/description/title-info/author/last-name";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	            	fbook.addAuthorsFamily(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	            
	            //*Автор имя*
	            expression = "/FictionBook/description/title-info/author/first-name";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                fbook.addAuthorsName(nodeList.item(i).getFirstChild().getNodeValue(),i); 
	            }
	            
	            //*Ключевые слова*
	            expression = "/FictionBook/description/title-info/keywords";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            s="";
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                s=s+nodeList.item(i).getFirstChild().getNodeValue(); 
	            }
	            fbook.setKeywords(s);
	            
	            //*Язык*
	            expression = "/FictionBook/description/title-info/lang";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                fbook.setLang(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	            
	            //*Язык оригинала*
	            expression = "/FictionBook/description/title-info/src-lang";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                fbook.setSrcLang(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	            
	            //*Закрываем данные в записи и выводим строкой*
	            fbook.prepareData(); 
	            System.out.println("** "+fbook.toCSVString());
	 	 
	        } catch (FileNotFoundException e) {
	            log.warning(fn+" Error: File Not Found. Msg: "+e.getMessage());
	        	//e.printStackTrace();
	        } catch (SAXException e) {
	        	log.warning(fn+" Error SAX. Msg: "+e.getMessage());
	            //e.printStackTrace();
	        } catch (IOException e) {
	        	log.warning(fn+" I/O Error. Msg: "+e.getMessage());
	            //e.printStackTrace();
	        } catch (ParserConfigurationException e) {
	        	log.warning(fn+" Parser Configuration Error. Msg: "+e.getMessage());
	            //e.printStackTrace();
	        } catch (XPathExpressionException e) {
	        	log.warning(fn+" XPath Error. Msg: "+e.getMessage());
	            //e.printStackTrace();
	        }       }
		return fbook;
	    }

	public static void getStructFb2(String fn) {
		 try {
	            FileInputStream file = new FileInputStream(new File("c://01_fs//employees.xml.txt"));
	                 
	            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	             
	            DocumentBuilder builder =  builderFactory.newDocumentBuilder();
	             
	            Document xmlDocument = builder.parse(file);
	 
	            XPath xPath =  XPathFactory.newInstance().newXPath();
	 
	            System.out.println("*************************");
	            String expression = "/Employees/Employee[@emplid='3333']/email";
	            System.out.println(expression);
	            String email = xPath.compile(expression).evaluate(xmlDocument);
	            System.out.println(email);
	 
	            System.out.println("*************************");
	            expression = "/Employees/Employee/firstname";
	            System.out.println(expression);
	            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	 
	            System.out.println("*************************");
	            expression = "/Employees/Employee[@type='admin']/firstname";
	            System.out.println(expression);
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	 
	            System.out.println("*************************");
	            expression = "/Employees/Employee[@emplid='2222']";
	            System.out.println(expression);
	            Node node = (Node) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
	            if(null != node) {
	                nodeList = node.getChildNodes();
	                for (int i = 0;null!=nodeList && i < nodeList.getLength(); i++) {
	                    Node nod = nodeList.item(i);
	                    if(nod.getNodeType() == Node.ELEMENT_NODE)
	                        System.out.println(nodeList.item(i).getNodeName() + " : " + nod.getFirstChild().getNodeValue()); 
	                }
	            }
	             
	            System.out.println("*************************");
	 
	            expression = "/Employees/Employee[age>40]/firstname";
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            System.out.println(expression);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	         
	            System.out.println("*************************");
	            expression = "/Employees/Employee[1]/firstname";
	            System.out.println(expression);
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	            System.out.println("*************************");
	            expression = "/Employees/Employee[position() <= 2]/firstname";
	            System.out.println(expression);
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	 
	            System.out.println("*************************");
	            expression = "/Employees/Employee[last()]/firstname";
	            System.out.println(expression);
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	 
	            System.out.println("*************************");
	 
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (SAXException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (ParserConfigurationException e) {
	            e.printStackTrace();
	        } catch (XPathExpressionException e) {
	            e.printStackTrace();
	        }       
	    }

}
