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
import java.nio.file.Files;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
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
 * Мусорка кода. используется для временного хранения интересного и
 * неотлаженного, а также не используемого кода. Компилироваться не должен и не
 * будет.
 * 
 * @author Yury
 *
 */
public class FSMusorkaCode {
	
	/**Упаковка в zip архив. Не тестировалась.
	 * @param directory
	 * @param to
	 * @throws IOException
	 */
	public void pack(File directory, String to) throws IOException {
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

	/**Распаковка zip архива. Не тестировалась.
	 * @param path
	 * @param dir_to
	 * @throws IOException
	 */
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

	public FSDescriptionBook getFb2Digest(File fp, boolean deleteFile, String zipNam) {

		String fn = "";
		FSDescriptionBook fbook = new FSDescriptionBook(zipNam, fp.getName());
		FileInputStream file = null;
/*		try {
			file = new FileInputStream(fp);
		} catch (Exception e) {
			pp("Файл не открылся. " + e.getLocalizedMessage());
		}
		DocumentBuilderFactory builderFactory;
		DocumentBuilder builder = null;
		Document xmlDocument = null;
		XPath xPath = XPathFactory.newInstance().newXPath();
		boolean xmlFileReady = false;
		String expression;
		NodeList nodeList;
		String s = "";

		if (deleteFile) {
			fp.deleteOnExit();
			pp("TEST! DEL " + fp.delete() + " wr=" + fp.canWrite() + " dl=" + " " + fp.toString());
			return fbook;
		}

		if (fp != null) {
			// открываем файл и готовим документы и компилятор XPath
			try {
				fn = fp.getCanonicalPath();
				pp("           Строим дайджест для файла fn=" + fn);
				file = new FileInputStream(fp);
				builderFactory = DocumentBuilderFactory.newInstance();
				builder = builderFactory.newDocumentBuilder();
				xmlDocument = builder.parse(file);
				// xPath = XPathFactory.newInstance().newXPath();
				xmlFileReady = true;
			} catch (FileNotFoundException e) {
				log.warning(fn + " Error: File Not Found. Msg: " + e.getMessage());
			} catch (SAXException e) {
				log.warning(fn + " Error SAX. Msg: " + e.getMessage());
			} catch (IOException e) {
				log.warning(fn + " I/O Error. Msg: " + e.getMessage());
			} catch (ParserConfigurationException e) {
				log.warning(fn + " Parser Configuration Error. Msg: " + e.getMessage());
				// } catch (XPathExpressionException e) {
				// log.warning(fn + " XPath Error. Msg: " + e.getMessage());
			}

			// xmlDocument.cl
			try {
				file.close();
				fp.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} // if fp!=null завершили все приготовления к доставанию по xPath

		if (xmlFileReady) {
			xmlDocument.getDocumentElement().normalize();
			NodeList nodeDescription = xmlDocument.getElementsByTagName("title-info");
			if (nodeDescription.getLength() > 0) {
				NodeList nodesUnderDescription = nodeDescription.item(0).getChildNodes();
				int iii = nodesUnderDescription.getLength();
				for (int j = 0; j < iii; j++)
					switch (toEnumConst((nodesUnderDescription.item(j).getNodeName().toLowerCase().trim()))) {
					case FSZ_AUTHOR:
						String author = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "")
								.trim().replaceAll(" +", " ");
						fbook.addAuthor(author);
						// System.out.println("Author="+author);
						break;
					case FSZ_TITLE:
						String title = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "")
								.trim().replaceAll(" +", " ");
						fbook.setFullTitle(title);
						// System.out.println("Title="+title);
						break;
					case FSZ_GENRE:
						String genre = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "")
								.trim().replaceAll(" +", " ");
						fbook.addGenre(genre);
						// System.out.println("Genre="+genre);
						break;
					case FSZ_LANG:
						String lang = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "").trim()
								.replaceAll(" +", " ");
						fbook.setLang(lang);
						// System.out.println("Lang="+lang);
						break;
					case FSZ_SRCLANG:
						String srclang = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "")
								.trim().replaceAll(" +", " ");
						fbook.setSrcLang(srclang);
						// System.out.println("SrcLang="+srclang);
						break;
					case FSZ_ANNOTATION:
						String annotation = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "")
								.trim().replaceAll(" +", " ");
						fbook.setFullAnnotation(annotation);
						// System.out.println("Annotation="+annotation);
						break;
					case FSZ_KEYWORDS:
						String keywords = nodesUnderDescription.item(j).getTextContent().replaceAll("\\p{Cntrl}", "")
								.trim().replaceAll(" +", " ");
						fbook.setKeywords(keywords);
						// System.out.println("KeyWords"+keywords);
						break;
					}
				// System.out.println("Total no of tags : " + iii);
			}
		}
		// *Закрываем данные в записи и выводим строкой*
		fbook.prepareData();
		System.out.println("** " + fbook.toCSVString());

		// *Если признак удаления файла, то удаляем файл
		if (deleteFile) {

			// if (!fp.delete())
			for (int i1 = 0; i1 < 10; i1++) {
				try {
					Files.deleteIfExists(fp.toPath());
					pp("FILE DELETED");
					break;
				} catch (IOException e1) {
					if (i1 < 10) {
						log.warning("MSG:" + e1.getMessage() + " Error to delete temporary file " + fn + " "
								+ fp.getName());
					} else {
						try {
							wait(77);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			// else {pp("Deleted "+fn);}
		}
*/		return fbook;
	}

	public void getDigestsFromFb2Directory(File fp, boolean filesDeleted, String zipName) {
	/*	File[] fpFiles = fp.listFiles();
		// pp(" Проходим для сбора дайждестов по папке "+fp.toString());
		// pp("Fd="+filesDeleted+"; zipf="+zipName);
		// проверка входных параметров, что они не указывают null (папки нижнего
		// уровня без файлов содержат нулевые списки входящих файлов)
		if (fp != null) {
			if (fpFiles != null) {
				for (File fElem : fpFiles) {
					// проверка на нулевые указатели
					if (fElem != null) {
						// обработка для файлов
						if (fElem.isFile()) {
							// вытаскиваем дайджест из файла
							// pp(" Вызываем анализ и извлечение данных here
							// getDigestsFromFb2Directory
							// "+fElem.getAbsolutePath());
							getFb2Digest(fElem, zipName);
						}
					}
					// обработка для папок
					if (fElem.isDirectory()) {
						try {
							getDigestsFromFb2Directory(fElem, filesDeleted, zipName);
						} catch (Exception ex) {
							log.severe("Recursion problem with addition to file list. Params: " + fElem.toString()
									+ "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
						}
					} // обр.папок
				} // for
			} // fpFiles
		} // fp
*/	}

	public static void getStructFb2(String fn) {
		try {
			FileInputStream file = new FileInputStream(new File("c://01_fs//employees.xml.txt"));

			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder builder = builderFactory.newDocumentBuilder();

			Document xmlDocument = builder.parse(file);

			XPath xPath = XPathFactory.newInstance().newXPath();

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
			if (null != node) {
				nodeList = node.getChildNodes();
				for (int i = 0; null != nodeList && i < nodeList.getLength(); i++) {
					Node nod = nodeList.item(i);
					if (nod.getNodeType() == Node.ELEMENT_NODE)
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
