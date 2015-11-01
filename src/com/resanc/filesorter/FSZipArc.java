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
import java.util.ArrayList;
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
	// ��������� ���������� �������� ������
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
		//System.out.println("unpack started...");
		int res=-1;
		File ftmp = new File(FS_ZIP_TEMP_DIR);
		ftmp.mkdir();
		unpackTempDirectory = ftmp;
		
		//try {
			//System.out.println("ftmp="+ftmp.getCanonicalPath());
		//} catch (IOException e1) {
			// TODO Auto-generated catch block
		//	e1.printStackTrace();
		//}
		//�������� ����������� ����� � ��� ��������� �����
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
		System.out.println("   ����������� ���� "+fpath.toString()+" � ����� "+ftmp.toString());
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

	/**������������� ��������� ����� � ��������, ������� � ��� zip � fb2, 
	 * ��������� ����������� �����-���������� �� ���
	 * @param fp
	 */
	public void getFb2DigestList(File fp)
	{
		// ����� �����. ��������� ����� ���� ����� �� ��������� � ������
		// �������� �, 
		// 1. ���� ������� ����� zip, �� �������� �����  ���������� �
		// ����� ����� ���������� �� ����� ����������, 
		// 2. ���� ������� fb2, �� ����������� ��� ��������
		pp("�������� �������� ����� � ������ � ��� ������ � �����. ���� ������: "+fp.toString());
		File[] fpFiles = fp.listFiles();
			// �������� ������� ����������, ��� ��� �� ��������� null (����� �������
			// ������ ��� ������ �������� ������� ������ �������� ������)
			if (fp != null) {
				if (fpFiles != null) {
					for (File fElem : fpFiles) {
						// �������� �� ������� ���������
						if (fElem != null) {
							// ��������� ��� ������
							if (fElem.isFile()) {
								//pp("               try unpack "+fElem.toString());
								//���� ��� zip �����, ����������� ��� ���������� �� ��������� �����
								if (unpack(fElem)==0){
									//pp(" I FIND IT!!! here zip is "+fElem.getAbsolutePath());
									//����������� ��������� �� ���� ������ �� ��������� �����, � ���� ����� �������
									try {
										getDigestsFromFb2Directory(new File(FS_ZIP_TEMP_DIR), true, fElem.getCanonicalPath());
									} catch (IOException e) {
										log.severe("Error with getting digests from directory "+fElem.toString()+" Msg:"+e.getMessage());
									}
								}
								//���� ��� fb2 ����, ����������� �� ���� ��������, �� �� �������
								pp("Oh-Oh-Oh!!! here fb2 is "+fElem.getAbsolutePath());
								getFb2Digest(fElem, false,"nozip");
								}
							
							// ��������� ��� �����
							if (fElem.isDirectory()) {
								try {
									getFb2DigestList(fElem); 
								} catch (Exception ex) {
									log.severe("Recursion problem with addition to file list. Params: " + fElem.toString()
											+ "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
								}
							} // ���.�����
					}//fElem-�� null
					} // for
				} // fpFiles
			} // fp
	}
	
	public void getDigestsFromFb2Directory(File fp, boolean filesDeleted, String zipName)
	{
		File[] fpFiles = fp.listFiles();
		pp("  �������� ��� ����� ���������� �� ����� "+fp.toString());
		pp("Fd="+filesDeleted+"; zipf="+zipName);
			// �������� ������� ����������, ��� ��� �� ��������� null (����� �������
			// ������ ��� ������ �������� ������� ������ �������� ������)
			if (fp != null) {
				if (fpFiles != null) {
					for (File fElem : fpFiles) {
						// �������� �� ������� ���������
						if (fElem != null) {
							// ��������� ��� ������
							if (fElem.isFile()) {
								//����������� �������� �� �����
								pp("       �������� ������ � ���������� ������ here getDigestsFromFb2Directory "+fElem.getAbsolutePath());
								getFb2Digest(fElem, filesDeleted,zipName);
								}
							}
							// ��������� ��� �����
							if (fElem.isDirectory()) {
								try {
									getDigestsFromFb2Directory(fElem,filesDeleted, zipName); 
								} catch (Exception ex) {
									log.severe("Recursion problem with addition to file list. Params: " + fElem.toString()
											+ "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
								}
							} // ���.�����
					} // for
				} // fpFiles
			} // fp
	}
	
	private  int lip=0;
    private  void pp( String ss){
    	if (ss!=null) {System.out.println(lip++ +") "+ss);}else { System.out.println(lip++);}}
    
	/*public String[] getByXPath(String expression, XPath xPath, Document xmlDocument) {
		ArrayList<String> res;
		res = new ArrayList<String>();
		pp("inner ds 1");
		if (expression != null) {
			// *�������� ������ �� ��������� �� xPath-�*
			NodeList nodeList;
			pp("inner ds 10");
			try {
				nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
				pp("inner ds 100");
				for (int i = 0; i < nodeList.getLength(); i++) {
					res.add(nodeList.item(i).getFirstChild().getNodeValue());
					pp("inner ds 1000");	
				}
			} catch (XPathExpressionException e) {
				//���� ������
				pp("inner ds 10 000");
			} catch (Exception e) {
			//���� ������
			pp("inner ds 10 000 EX");
		}
			pp("inner ds 100 000 ");
		}
		pp("inner ds 1 000 000 res="+res.toString()+"ressize="+res.size()+" null?="+(res==null));
		return (String[]) res.toArray();
	}*/
    
    public static final int FSZ_AUTHOR=1;
    public static final int FSZ_TITLE=2;
    public static final int FSZ_GENRE=3;
    public static final int FSZ_LANG=4;
    public static final int FSZ_SRCLANG=5;
    public static final int FSZ_ANNOTATION=6;
    public static final int FSZ_KEYWORDS=7;
    public static final int FSZ_ZERO=0;
    
    public int toEnumConst(String s)
    {
    	if (s.toLowerCase().trim().equals("author")) {return FSZ_AUTHOR;}
    	if (s.toLowerCase().trim().equals("book-title")) {return FSZ_TITLE;}
    	if (s.toLowerCase().trim().equals("genre")) {return FSZ_GENRE;}
    	if (s.toLowerCase().trim().equals("lang")) {return FSZ_LANG;}
    	if (s.toLowerCase().trim().equals("src-lang")) {return FSZ_SRCLANG;}
    	if (s.toLowerCase().trim().equals("annotation")) {return FSZ_ANNOTATION;}
    	if (s.toLowerCase().trim().equals("keywords")) {return FSZ_KEYWORDS;}
    	return FSZ_ZERO;
    }
    
	public FSDescriptionBook getFb2Digest(File fp, boolean deleteFile, String zipNam) {

		String fn = "";
		FSDescriptionBook fbook = new FSDescriptionBook(zipNam,fp.getName());
		FileInputStream file=null;
		try {
		file = new FileInputStream(fp);
		} catch (Exception e){pp("���� �� ��������. "+e.getLocalizedMessage());}
		DocumentBuilderFactory builderFactory;
		DocumentBuilder builder=null;
		Document xmlDocument=null;
		XPath xPath = XPathFactory.newInstance().newXPath();
		boolean xmlFileReady = false;
		String expression;
		NodeList nodeList;
		String s="";

		if (fp != null) {
			//��������� ���� � ������� ��������� � ���������� XPath
			try {
				fn = fp.getCanonicalPath();
				pp("           ������ �������� ��� ����� fn="+fn);
				file = new FileInputStream(fp);
				builderFactory = DocumentBuilderFactory.newInstance();
				builder = builderFactory.newDocumentBuilder();
				xmlDocument = builder.parse(file);
				//xPath = XPathFactory.newInstance().newXPath();
				xmlFileReady=true;
			} catch (FileNotFoundException e) {
				log.warning(fn + " Error: File Not Found. Msg: " + e.getMessage());
			} catch (SAXException e) {
				log.warning(fn + " Error SAX. Msg: " + e.getMessage());
			} catch (IOException e) {
				log.warning(fn + " I/O Error. Msg: " + e.getMessage());
			} catch (ParserConfigurationException e) {
				log.warning(fn + " Parser Configuration Error. Msg: " + e.getMessage());
//			} catch (XPathExpressionException e) {
//				log.warning(fn + " XPath Error. Msg: " + e.getMessage());
			}
		}//if fp!=null ��������� ��� ������������� � ���������� �� xPath
			
		if (xmlFileReady) {
			xmlDocument.getDocumentElement().normalize();
		    NodeList nodeDescription = xmlDocument.getElementsByTagName("title-info");
		    if (nodeDescription.getLength()>0){
		    NodeList nodesUnderDescription = nodeDescription.item(0).getChildNodes();
		    int iii= nodesUnderDescription.getLength();
		    for (int j=0;j<iii;j++)
		    	switch (toEnumConst((nodesUnderDescription.item(j).getNodeName().toLowerCase().trim()))){
		    	case FSZ_AUTHOR:
		    		String author=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		    		fbook.addAuthor(author);
		    		System.out.println("Author="+author);
		    		break;
		        case FSZ_TITLE: 
		        	String title=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		    		fbook.setFullTitle(title);
		        	System.out.println("Title="+title);
		    		break;
		        case FSZ_GENRE: 
		        	String genre=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		        	fbook.addGenre(genre);
		    		System.out.println("Genre="+genre);
		    		break;
		        case FSZ_LANG: 
		        	String lang=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		        	fbook.setLang(lang);
		    		System.out.println("Lang="+lang);
		    		break;
		        case FSZ_SRCLANG: 
		        	String srclang=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		        	fbook.setSrcLang(srclang);
		        	System.out.println("SrcLang="+srclang);
		        	break;
		        case FSZ_ANNOTATION: 
		        	String annotation=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		        	fbook.setFullAnnotation(annotation);
		        	System.out.println("Annotation="+annotation);
		        	break;
		        case FSZ_KEYWORDS: 
		        	String keywords=nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim().replaceAll(" +", " ");
		        	fbook.setKeywords(keywords);
		        	System.out.println("KeyWords"+keywords);break;
		    }
		    //System.out.println("Total no of tags : " + iii);
		    }
		    }
		// *��������� ������ � ������ � ������� �������*
		fbook.prepareData();
		System.out.println("** " + fbook.toCSVString());

		// *���� ������� �������� �����, �� ������� ����
		if (deleteFile) {
			if (!fp.delete()) {
				log.warning("Error to delete temporary file " + fn);
			}
			else {pp("Deleted "+fn);}
		}
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
