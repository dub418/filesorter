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
	
	public void getFb2FilesDigest(File fp)
	{
		File[] fpFiles = fp.listFiles();
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
								pp("here "+fElem.getAbsolutePath());
								getFb2Digest(fElem);
								}
							}
							// обработка для папок
							if (fElem.isDirectory()) {
								try {
									getFb2FilesDigest(fElem); 
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
	public  void getFb2Digest(File fp) {
		pp(" xxx "+fp.getAbsolutePath());
		if (fp!=null){ 
		try {
	            FileInputStream file = new FileInputStream(fp);
	                 
	            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	             
	            DocumentBuilder builder =  builderFactory.newDocumentBuilder();
	             
	            Document xmlDocument = builder.parse(file);
	 
	            XPath xPath =  XPathFactory.newInstance().newXPath();
	 
	            System.out.println("*1************************");
	            String expression = "/FictionBook[@xmlns='http://www.gribuser.ru/xml/fictionbook/2.0']/description";
	            System.out.println(expression);
	            String email1 = xPath.compile(expression).evaluate(xmlDocument);
	            System.out.println(email1);
	            
	            System.out.println("*************************");
	            expression = "/Employees/Employee[@emplid='3333']/email";
	            System.out.println(expression);
	            String email = xPath.compile(expression).evaluate(xmlDocument);
	            System.out.println(email);
	 
	            System.out.println("*************************");
	            expression = "/FictionBook/description/title-info/book-title";
	            System.out.println(expression);
	            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }

	            System.out.println("*************************");
	            expression = "/FictionBook/description/title-info/genre";
	            System.out.println(expression);
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
	            for (int i = 0; i < nodeList.getLength(); i++) {
	                System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
	            }
	            
	            System.out.println("*************************");
	            expression = "/FictionBook/description/title-info/annotation";
	            System.out.println(expression);
	            nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
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
	        }       }
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
