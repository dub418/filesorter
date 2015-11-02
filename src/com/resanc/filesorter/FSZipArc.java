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

	/**
	 * Конструктор Создает временную папку для распаковки файлов
	 */
	// public FSZipArc() {
	// //TempDirectory = new File(FS_ZIP_TEMP_DIR);
	// try {
	// //TempDirectory.mkdir();
	// } catch (Exception e) {
	// log.severe("Cannot create temp directory " + FS_ZIP_TEMP_DIR + " Msg: " +
	// e.getMessage());
	// System.exit(0);
	// }
	// }

	// Папка, в которую распаковываются временные файлы из zip-архивов
	private static final String FS_ZIP_TEMP_DIR = "TempUnpackDir";
	private static final String FS_CSV_LOG_FILE = "fb2listlog.txt";
	private static final File csvFile = new File(FS_CSV_LOG_FILE);

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSZipArc.class.getName());

	public static final int FS_ZIP_ERR_ZIP = -1;
	public static final int FS_ZIP_ERR_FILE_IS_NOT_ZIP = -100;
	public static final int FS_ZIP_ERR_FILE_IS_NOT_OPEN = -200;
	public static final int FS_ZIP_ERR_IN_FILE_IS_NOT_OPEN = -300;
	public static final int FS_ZIP_ERR_OUT_FILE_IS_NOT_OPEN = -400;
	public static final int FS_ZIP_ERR_FILE_IS_NOT_WRITE = -500;

	/**
	 * Распаковка zip архива во временную папку
	 * 
	 * @param fpath
	 *            - файл с zip архивом
	 * @return 0 - успех, <0 - ошибка
	 */
	public int unpack(File fpath) {
		// пытаемся распаковать архив в эту временную папку
		ZipFile zip = null;
		// Открываем файл zip-архива
		try {
			zip = new ZipFile(fpath);
		} catch (ZipException e1) {
			// этот файл не zip - архив
			return FS_ZIP_ERR_FILE_IS_NOT_ZIP;
		} catch (IOException e1) {
			// этот файл не открывается
			return FS_ZIP_ERR_FILE_IS_NOT_OPEN;
		}
		// Проходим архив и извлекаем из него все имена файлов в список zfiles
		Enumeration entries = zip.entries();
		LinkedList<ZipEntry> zfiles = new LinkedList<ZipEntry>();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (!entry.isDirectory()) {
				zfiles.add(entry);
			}
		}
		// извлекаем файлы по списку во временную папку, анализируем и удаляем
		for (ZipEntry entry : zfiles) {
			//Открываем поток для чтения из архива
			InputStream in;
			try {
				in = zip.getInputStream(entry);
			} catch (IOException e) {
				try {
					zip.close();
				} catch (IOException e1) {
					return FS_ZIP_ERR_FILE_IS_NOT_OPEN;
				}
				return FS_ZIP_ERR_IN_FILE_IS_NOT_OPEN;
			}
			File tempFb2File = new File(FS_ZIP_TEMP_DIR + "/" + entry.getName());
			
			//Открываем поток для записи в файл во временной папке
			OutputStream out;
			try {
				out = new FileOutputStream(tempFb2File);
			} catch (FileNotFoundException e) {
				try {
					zip.close();
				} catch (IOException e1) {
					return FS_ZIP_ERR_FILE_IS_NOT_OPEN;
				}
				try {
					in.close();
				} catch (IOException e1) {
					return FS_ZIP_ERR_IN_FILE_IS_NOT_OPEN;
				}
				return FS_ZIP_ERR_OUT_FILE_IS_NOT_OPEN;
			}
			byte[] buffer = new byte[1024];
			int len;
			try {
				while ((len = in.read(buffer)) >= 0)
					out.write(buffer, 0, len);
			} catch (IOException e) {
				try {
					zip.close();
				} catch (IOException e1) {
					return FS_ZIP_ERR_FILE_IS_NOT_OPEN;
				}
				try {
					in.close();
				} catch (IOException e1) {
					return FS_ZIP_ERR_IN_FILE_IS_NOT_OPEN;
				}
				try {
					out.close();
				} catch (IOException e1) {
					return FS_ZIP_ERR_OUT_FILE_IS_NOT_OPEN;
				}
				return FS_ZIP_ERR_FILE_IS_NOT_WRITE;
			}
			
			try {
				in.close();
			} catch (IOException e) {
				return FS_ZIP_ERR_IN_FILE_IS_NOT_OPEN;
			}
			try {
				out.close();
			} catch (IOException e) {
				return FS_ZIP_ERR_OUT_FILE_IS_NOT_OPEN;
			}
			try {getFb2Digest(tempFb2File, fpath.toString());}
			catch(Exception e){e.printStackTrace();};
			tempFb2File.delete();
		}

		try {
			zip.close();
		} catch (IOException e) {
			return FS_ZIP_ERR_FILE_IS_NOT_OPEN;
		}
		return 0;
	}

	/**
	 * Просматривает указанную папку и подпапки, находит в них zip и fb2,
	 * управляет извлечением строк-дайджестов из них
	 * 
	 * @param fp
	 */
	public void getFb2DigestList(File fp) {
		// точка входа. Выполняет обход всех папок от указанной в вызове
		// корневой и,
		// 1. если находит архив zip, то вызывает метод распаковки и
		// метод сбора дайджестов по папке распаковки,
		// 2. если находит fb2, то вытаскивает его дайджест
		// pp("Начинаем обходить папку и искать в ней архивы и книги. Путь
		// начала: "+fp.toString());
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
							// pp(" try unpack "+fElem.toString());
							// Если это zip архив, вытаскиваем его содержимое во
							// временную папку, распаковываем и пишем в журнал
							// найденные дайджесты
							int js=unpack(fElem);
							if (js!=0){System.out.println(fElem.toString()+" "+js);}
							if (js == 0) {
								log.fine("Zip file unpacked and analyzed. Name=" + fElem.toString());
							} else {
								// Если это fb2 файл, вытаскиваем из него
								// дайджест
								getFb2Digest(fElem, "nozip");
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
					} // fElem-не null
				} // for
			} // fpFiles
		} // fp
	}

	private int lip = 0;

	private void pp(String ss) {
		if (ss != null) {
			System.out.println(lip++ + ") " + ss);
		} else {
			System.out.println(lip++);
		}
	}

	public static final int FSZ_AUTHOR = 1;
	public static final int FSZ_TITLE = 2;
	public static final int FSZ_GENRE = 3;
	public static final int FSZ_LANG = 4;
	public static final int FSZ_SRCLANG = 5;
	public static final int FSZ_ANNOTATION = 6;
	public static final int FSZ_KEYWORDS = 7;
	public static final int FSZ_ZERO = 0;

	public int toEnumConst(String s) {
		if (s.toLowerCase().trim().equals("author")) {
			return FSZ_AUTHOR;
		}
		if (s.toLowerCase().trim().equals("book-title")) {
			return FSZ_TITLE;
		}
		if (s.toLowerCase().trim().equals("genre")) {
			return FSZ_GENRE;
		}
		if (s.toLowerCase().trim().equals("lang")) {
			return FSZ_LANG;
		}
		if (s.toLowerCase().trim().equals("src-lang")) {
			return FSZ_SRCLANG;
		}
		if (s.toLowerCase().trim().equals("annotation")) {
			return FSZ_ANNOTATION;
		}
		if (s.toLowerCase().trim().equals("keywords")) {
			return FSZ_KEYWORDS;
		}
		return FSZ_ZERO;
	}

	public FSDescriptionBook getFb2Digest(File fp, String zipNam) {

		// String fn = "";
		FSDescriptionBook fbook = new FSDescriptionBook(zipNam, fp.getName());
//		FileInputStream file = null;
//		try {
//			file = new FileInputStream(fp);
//		} catch (Exception e) {
//			return fbook;
//			// pp("Файл не открылся. " + e.getLocalizedMessage());
//		}
		DocumentBuilderFactory builderFactory;
		DocumentBuilder builder = null;
		Document xmlDocument = null;
		XPath xPath = XPathFactory.newInstance().newXPath();
		boolean xmlFileReady = false;
		String expression;
		NodeList nodeList;
		String s = "";

		if (fp != null) {
			// открываем файл и готовим документы и компилятор XPath
//			try {
				// fn = fp.getCanonicalPath();
				// pp(" Строим дайджест для файла " + fp.toString());
//				try {
//					file = new FileInputStream(fp);
//				} catch (FileNotFoundException e1) {
//					return fbook;
//				}
				builderFactory = DocumentBuilderFactory.newInstance();
				try {
					builder = builderFactory.newDocumentBuilder();
				} catch (ParserConfigurationException e1) {
//					try {
//						file.close();
//					} catch (IOException e) {
//						return fbook;
//					}
					return fbook;
				}
				try {
					xmlDocument = builder.parse(fp);
				} catch (SAXException e1) {
					log.warning(fp.toString() + " Error SAX. Msg: " + e1.getMessage());
//					try {
//						file.close();
//					} catch (IOException e) {
//						return fbook;
//					}
					return fbook;
				} catch (IOException e1) {
					log.warning(fp.toString() + " I/O Error. Msg: " + e1.getMessage());
//					try {
//						file.close();
//					} catch (IOException e) {
//						return fbook;
//					}
					return fbook;
				}
				// xPath = XPathFactory.newInstance().newXPath();
				xmlFileReady = true;
//			} catch (FileNotFoundException e) {
//				log.warning(fp.toString() + " Error: File Not Found. Msg: " + e.getMessage());
//			} catch (SAXException e) {
//				log.warning(fp.toString() + " Error SAX. Msg: " + e.getMessage());
//			} catch (IOException e) {
//				log.warning(fp.toString() + " I/O Error. Msg: " + e.getMessage());
//			} catch (ParserConfigurationException e) {
//				log.warning(fp.toString() + " Parser Configuration Error. Msg: " + e.getMessage());
//				// } catch (XPathExpressionException e) {
//				// log.warning(fn + " XPath Error. Msg: " + e.getMessage());
//			}

			// xmlDocument.cl
//			try {
//				file.close();
//			} catch (IOException e) {
//			return fbook;}
			//	// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
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

		System.out.println("fbook ** " + fbook.toCSVString());
		FileOutputStream out;
		try {
			out = new FileOutputStream(csvFile, true);
			String s1 = fbook.toCSVString().trim() + "\r\n";
			out.write(s1.getBytes());
			out.close();
		} catch (IOException e) {
			log.warning("Cannot write to file " + csvFile.toString() + " Msg: " + e.getMessage());
		}

		return fbook;
	}

}
