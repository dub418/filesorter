/**
 * Пакет работы с файлами домашнего архива
 */
package com.resanc.filesorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * База данных SQLite, в которой хранится и обрабатывается информация о файлах
 * домашнего архива Логика работы класса такая: В программе создается единый
 * класс, который держит соединение с БД SQLite, предоставляет методы записи
 * картотеки в БД, формирования таблицы дубликатов в БД, внесения приоритетных
 * путей хранения для дубликатов. Для загрузки данных из других файлов sqlite БД
 * предусмотрен отдельный метод.
 * 
 * @author Yury
 * 
 * 
 */

/**
 * @author Yury
 *
 */
/**
 * @author Yury
 *
 */
/**
 * @author Yury
 *
 */
public class FSSQLDatabase {

	private static boolean dbDebug = true;// режим отладочных сообщений на
											// консоль
	private static String dbFilename = "filesorterbase.s3db";
	private static String dbLibGeneratorName = "FSSQLDatabase_ver_1";
	private static String dbFilenameWithoutExt = "fsdatafile";
	private static String dbJDBCClassname = "org.sqlite.JDBC";
	private static String dbJDBCname = "jdbc:sqlite:";
	public static String dbMainTableName = "Files1";// имя основной таблицы для
													// списка файлов
	private static Connection dbMainConn;
	private static Statement dbMainStmt;
	private static PreparedStatement dbMainPStmt;
	private static ResultSet dbMainRes;

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSSQLDatabase.class.getName());
	private static boolean dbIsConnected = false;

	private FSExtList extL; // выгружаем значения в файл
	private	FSFileExtList extL1; // возвращаем значения из файла

	/**
	 * Отладочный метод для демонстрации меток
	 * 
	 * @param l
	 */
	private void p(long l) {
		if (dbDebug) {
			System.out.println("Stage " + l);
		} else {
			System.out.print(".");
		}
	}

	public static String getDbLibGeneratorName() {
		return dbLibGeneratorName;
	}

	/**
	 * Внутренний класс для хранения и сериализации в json списка расширений и
	 * приоритетных путей хранения файлов с этими расширениями. Используется при
	 * выгрузке файла настройки приоритетных расширений
	 * 
	 * @author Yury
	 *
	 */
	private class FSExtList {

		/**
		 * @return the generator
		 */
		public String getGenerator() {
			return generator;
		}

		/**
		 * @param generator
		 *            the generator to set
		 */
		public void setGenerator(String generator) {
			this.generator = generator;
		}

		/**
		 * @return the version
		 */
		public Long getVersion() {
			return version;
		}

		/**
		 * @param version
		 *            the version to set
		 */
		public void setVersion(Long version) {
			this.version = version;
		}

		private String generator = "FSSQLDatabase";
		private long version = 1L;
		public ArrayList<FSFileExtensionPath> extList;

		public FSExtList() {
			// super();
			this.extList = new ArrayList<FSFileExtensionPath>();
		}

		public ArrayList<FSFileExtensionPath> getExtList() {
			return extList;
		}

		public void setExtList(ArrayList<FSFileExtensionPath> extList) {
			this.extList = extList;
		}

	}// FSExtList

	/**
	 * При создании объекта устанавливается соединение с БД или возвращается
	 * ошибка SQL. Connect, созданный конструктором, используется всеми методами
	 * класса.
	 * 
	 * @param fn
	 *            - имя файла базы данных SQLite
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public FSSQLDatabase(String fn) throws ClassNotFoundException, SQLException {
		if ((fn != null) && (fn != "")) {
			dbFilename = fn;
		}
		Class.forName(dbJDBCClassname);
		dbMainConn = DriverManager.getConnection(dbJDBCname + dbFilename);
		// создаем таблицу файлов, если ее нет, все хранение ставим в режим
		// опер.памяти, чтоб быстрее

		dbMainConn.setAutoCommit(false);// открываем транзакцию
		dbMainStmt = dbMainConn.createStatement();
		dbMainStmt.execute("PRAGMA journal_mode=MEMORY");// перенос файла
															// транзакций и
															// временного буфера
															// в память
		dbMainStmt.execute("PRAGMA temp_store=MEMORY");
		// создаем основную таблицу хранения списка файлов
		dbMainStmt.execute("CREATE TABLE if not exists [" + dbMainTableName + "] ("
				+ "[FileID] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + "[ShortName] VARCHAR(255)  NOT NULL,"
				+ "[FullPath] VARCHAR(255)  NOT NULL," + "[FileSize] BIGINT NOT NULL,"
				+ "[CheckSumCRC32] BIGINT NOT NULL," + "[isCRC32Calculated] BOOLEAN DEFAULT 'false' NOT NULL,"
				+ "[dtLastModification] DATETIME  NULL,"
				+ "[dtCreateRecord] TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," + "[ErrStatus] INTEGER  NULL,"
				+ "[DeviceSerial] VARCHAR(25) NULL," + "[DeviceComment] VARCHAR(100)  NULL,"
				+ "[DiscLabel] VARCHAR(35)  NULL," + "[FileExt] VARCHAR(35)  NULL" + ");");
		p(1);
		// создаем вспомогательную таблицу, куда переносятся файлы процедурой
		// дедупликации списка файлов (удаления повторяющихся строк таблицы,
		// возникших при повтроном сканировании и присоединении баз из файлов)
		dbMainStmt.execute("CREATE TABLE if not exists [FilesTemp] (" + "[ShortName] VARCHAR(255)  NOT NULL,"
				+ "[FullPath] VARCHAR(255)  NOT NULL," + "[FileSize] BIGINT NOT NULL,"
				+ "[CheckSumCRC32] BIGINT NOT NULL," + "[isCRC32Calculated] BOOLEAN DEFAULT 'false' NOT NULL,"
				+ "[dtLastModification] DATETIME  NULL," + "[ErrStatus] INTEGER  NULL,"
				+ "[DeviceSerial] VARCHAR(25) NULL," + "[DeviceComment] VARCHAR(100)  NULL,"
				+ "[DiscLabel] VARCHAR(35)  NULL," + "[FileExt] VARCHAR(35)  NULL" + ");");
		p(2);
		dbMainStmt.execute("CREATE INDEX if not exists [SearchFileSizeCRC32] ON [" + dbMainTableName + "]("
				+ "[FileSize]  DESC," + "[isCRC32Calculated]  DESC" + ");");
		// создаем вспомогательную таблицу, в которой собираются дубли файлов на
		// носителях, по ней впоследствии формируется пакет bat на удаление
		// файлов
		p(3);
		dbMainStmt.execute("CREATE TABLE IF NOT EXISTS [doubles] (" + "[CheckSum] BIGINT NOT NULL,"
				+ "[FileSize] BIGINT NOT NULL," + "[Cnt] BIGINT NOT NULL)");
		// создаем вспомогательную таблицу, в которой собирается статистика
		// размещения файлов в папках в разрезе расширений файлов
		p(4);
		dbMainStmt.execute("CREATE TABLE if not exists [StatExt] (" + "[FileExt] VARCHAR(255) NULL,"
				+ "[FullPath] VARCHAR(255)  NOT NULL, [CNT] BIGINT NULL);");
		// создаем вспомогательную таблицу, в которой определяются приоритетные
		// пути размещения файлов в папках в разрезе расширений файлов для
		// определения, какое из найденных мест хранения файла следует оставить,
		// а какое - удалить в bat файле
		p(5);
		dbMainStmt.execute("CREATE TABLE if not exists [PrefExt] (" + "[FileExt] VARCHAR(255) NULL,"
				+ "[FullPath] VARCHAR(255)  NOT NULL, [CNT] BIGINT NULL);");
		// коммитим транзакцию создания таблиц
		p(6);
		dbMainConn.commit();
		// выставляем признак для всех методов класса, что БД открыта для работы
		dbIsConnected = true;
		p(7);
		log.info("Database connects and tables creates Ok in " + dbFilename);
	}// constructor

	/**
	 * Возвращает расширение файла из заданной строки его полного пути. В
	 * результате расширению предшествует точка. Если расширения нет,
	 * возвращается строка нулевой длины, не null.
	 * 
	 * @param str
	 * @return
	 */
	public static String getExt(String str) {
		String sufix = "";
		try {
			Pattern p = Pattern.compile("\\.\\w+$");
			Matcher matcher;
			matcher = p.matcher(str);
			matcher.find();
			sufix = matcher.group();
			if (sufix == null) {
				sufix = "";
			}
		} catch (Exception e) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("No dot in filename so no EXT for str=[" + str + "] sufix=[" + sufix + "] Message:"
						+ e.getMessage());
			}
		}
		return sufix.toLowerCase();
	}// getExt

	/**
	 * Закрывает основную базу данных класса, если она открыта: connect,
	 * statments, result
	 * 
	 * @throws SQLException
	 */
	public void closeDB() throws SQLException {
		if (dbIsConnected) {
			if (dbMainRes != null) {
				dbMainRes.close();
			}
			if (dbMainPStmt != null) {
				dbMainPStmt.close();
			}
			if (dbMainStmt != null) {
				dbMainStmt.close();
			}
			if (dbMainConn != null) {
				dbMainConn.close();
			}
			dbIsConnected = false;
			log.info("DB closed Ok!");
		}
	}// closeDB

	/**
	 * Записывает картотеку crd с устройством dvc в основную базу данных, перед
	 * записью база не очищается. По существу, формирует и исполняет INSERT в
	 * базу.
	 * 
	 * @param crd
	 * @param dvc
	 * @throws SQLException
	 */
	public void writeToDB(FSScanFileCards crd, FSDeviceWinInfo dvc) throws SQLException {
		if ((dbIsConnected) && (crd != null) && (dvc != null)) {
			String sp = "\"";
			String req = "INSERT INTO " + sp + dbMainTableName + sp + "(" + sp + "ShortName" + sp + ", " + sp
					+ "FullPath" + sp + ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp
					+ "isCRC32Calculated" + sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", "
					+ sp + "DeviceComment" + sp + ", " + sp + "DiscLabel" + sp + ", " + sp + "FileExt" + sp + ", " + sp
					+ "dtLastModification" + sp + ") VALUES("
					+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, strftime('%Y-%m-%d %H:%M:%S', ? ,'unixepoch'))";
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepares follow request {" + req + "}");
			}
			dbMainPStmt = dbMainConn.prepareStatement(req);
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepared request {" + req + "} has prepared in DB ok!");
			}
			long ll = 0;
			for (FSFileCard fil : crd.getScanFileCards()) {
				dbMainPStmt.setString(1, fil.getShortName());
				dbMainPStmt.setString(2, fil.getFullPath());
				dbMainPStmt.setLong(3, fil.getFileSize());
				dbMainPStmt.setLong(4, fil.getCheckSumCRC32());
				dbMainPStmt.setBoolean(5, fil.isCheckSumCalculated());
				dbMainPStmt.setInt(6, fil.getErrStatus());
				dbMainPStmt.setString(7, dvc.getSerial());
				dbMainPStmt.setString(8, dvc.getHostName());
				dbMainPStmt.setString(9, fil.getDiscLabel());
				dbMainPStmt.setString(10, FSSQLDatabase.getExt(fil.getShortName()));
				dbMainPStmt.setLong(11, fil.getLastModification() / 1000L);
				try {
					dbMainPStmt.executeUpdate();
				} catch (Exception e) {
					log.severe("ERROR UPDATE! " + e.getMessage() + " | " + e.getLocalizedMessage());
				}
				if (log.isLoggable(Level.FINE)) {
					log.fine(ll++ + "." + FSSQLDatabase.getExt(fil.getShortName()) + ". prepared request for {"
							+ fil.toString(fil.TO_FULL_STRING_FORM) + "} has worked ok!");
				}
			}
			try {
				dbMainConn.commit();
			} catch (Exception e) {
				log.severe("ERROR COMMIT! " + e.getMessage() + " | " + e.getLocalizedMessage());
			}

		} // if
		else {
			log.warning(
					"Application can not write file cards to database because database connection disable or null params for call.");
		}
	}// writeToDB

	/**
	 * Считывает приоритетные пути и расширения из JSON-файла и записывает их в
	 * таблицу БД prefExt
	 * 
	 * @param fname
	 * @param mode
	 * @throws SQLException
	 * @throws IOException
	 */
	public void writeExtensionsDB(String fname, String mode) throws SQLException, IOException {
		p(8);
		// FSFileExtList extL1;
		// System.out.println("I'm here fn=[" + fname + "] mode=[" + mode);
		if ((dbIsConnected)) {
			if ((mode != null) && (mode.equals("-ADD") || mode.equals("-NEW"))) {
				p(9);
				String sp = "\"";
				if (mode.equals("-NEW")) {// очищаем таблицу, если указан
											// параметр NEW
					p(10);
					dbMainStmt.execute("DELETE FROM " + sp + "prefExt" + sp + ";");
					dbMainConn.commit();
					log.fine("Table prefExt was deleted all records.");
				}
				// заполняем массив из файла json
				// сериализуем из json
				ObjectMapper mapper = new ObjectMapper();
				// создаем файл для загрузки
				p(11);
				File fpj = null;
				try {
					if ((fname == null) || (fname.equals(""))) {
						fpj = new File(dbFilenameWithoutExt + ".json");
					} else {
						if (getExt(fname).equals("")) {
							fpj = new File(fname + ".json");
						} else {
							fpj = new File(fname);
						}
					} // fname
					System.out.println(fpj.getAbsolutePath());
				} catch (Exception e) {
					log.severe("General Error with read file for serialization to DB from JSON. Message: "
							+ e.getMessage());
				}

				if (fpj != null) {
					try {
						extL1 = mapper.readValue(fpj, FSFileExtList.class);
						p(12);
						System.out.println(extL1.toString());
						// System.out.println(extL.extList.toString());
					} catch (JsonParseException e) {
						log.severe("Json Parsing Error with read file for serialization to DB from JSON. Message: "
								+ e.getMessage());
					} catch (JsonGenerationException e) {
						log.severe("Json Generation Error with read file for serialization to DB from JSON. Message: "
								+ e.getMessage());
					} catch (JsonMappingException e) {
						log.severe("Json Mapping Error with read file for serialization to DB from JSON. Message: "
								+ e.getMessage());
					}
				} // read from json
					// вставляем записи в таблицу БД
				String req = "INSERT INTO " + sp + "prefExt" + sp + "(" + sp + "FileExt" + sp + ", " + sp + "FullPath"
						+ sp + ", " + sp + "CNT" + sp + ") VALUES(" + "?, ?, ?)";
				if (log.isLoggable(Level.FINE)) {
					log.fine("prepares follow request {" + req + "}");
				}
				dbMainPStmt = dbMainConn.prepareStatement(req);
				if (log.isLoggable(Level.FINE)) {
					log.fine("prepared request {" + req + "} has prepared in DB ok!");
				}
				long ll = 0;
				for (FSFileExt elm : extL1.extList) {
					dbMainPStmt.setString(1, elm.getExt());
					dbMainPStmt.setString(2, elm.getPath());
					dbMainPStmt.setLong(3, elm.getPriorityLevel());
					ll++;
					try {
						dbMainPStmt.executeUpdate();
						p(13);
					} catch (Exception e) {
						log.severe("ERROR UPDATE! " + e.getMessage() + " | " + e.getLocalizedMessage());
					}
					if (log.isLoggable(Level.FINE)) {
						log.fine(ll++ + "." + elm.toString() + ". prepared request has worked ok!");
					}
				} // for

				try {
					dbMainConn.commit();
					p(14);
				} catch (Exception e) {
					log.severe("ERROR COMMIT! " + e.getMessage() + " | " + e.getLocalizedMessage());
				}
			} // if add or new mode
			else {
				if (mode == null) {
					log.warning("Invalid mode option is null");
				} else {
					log.warning("Invalid mode option is " + mode);
				}
			}
		} // if db
		else {
			log.warning("Application can not write StatExt table because database connection disable.");
		}
	}// writeExtentionsDB

	/**
	 * Формирует статистику использования путей для хранения файлов в разрезе
	 * расширения. Записывает ее в основную БД в таблицу StatExt в виде
	 * расширение-путь-сколько раз использовано (более 1), выгружает таблицу в
	 * json файл.
	 * 
	 * @throws SQLException
	 * @throws JsonGenerationException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public void readExtensionsDB() throws SQLException, JsonGenerationException, JsonProcessingException, IOException {
		if ((dbIsConnected)) {
			String sp = "\"";
			dbMainStmt.execute("DELETE FROM " + sp + "StatExt" + sp + ";");
			String req = "INSERT INTO " + sp + "StatExt" + sp + "(" + sp + "FullPath" + sp + ", " + sp + "FileExt" + sp
					+ ", " + sp + "CNT" + sp + ") "
					+ "SELECT FullPath, FileExt, count(FileExt) as Cnt from Files1 where FileExt<>" + sp + sp
					+ " group by FullPath,FileExt having cnt>1 order by FileExt asc, Cnt desc";
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepared follow request for extentions {" + req + "}");
			}
			dbMainPStmt = dbMainConn.prepareStatement(req);
			dbMainPStmt.executeUpdate();
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepared request for extentions {" + req + "} has worked ok!");
			}

			dbMainConn.commit();
			// получаем запросом из таблицы список расширений и выгружаем в json
			dbMainRes = dbMainStmt.executeQuery("SELECT * FROM [StatExt]");
			dbMainConn.commit();

			// заносим в массив все расширения из запроса к БД
			extL = new FSExtList();
			while (dbMainRes.next()) {
				String fileExt = dbMainRes.getString("FileExt");
				String fullPath = dbMainRes.getString("FullPath");
				long cnt = dbMainRes.getLong("CNT");
				extL.extList.add(new FSFileExtensionPath(fileExt, fullPath, cnt));
			} // while
				// сериализуем в json
			ObjectMapper mapper = new ObjectMapper();
			// создаем файл для выгрузки
			try {
				File fpj = new File(dbFilenameWithoutExt + ".json");
				mapper.writerWithDefaultPrettyPrinter().writeValue(fpj, extL);
			} catch (Exception e) {
				log.severe("Error with serialization to JSON file. Message " + e.getMessage());
			}
		} // if db
		else {
			log.warning("Application can not write StatExt table because database connection disable.");
		}

	}// readExtentionsDB

	/**
	 * Переносит в FilesTemp из основной таблицы файлов дедуплицированный список
	 * файлов. Этот список используется для поиска повторных хранений файла и
	 * создания bat-файла удаления копий
	 * 
	 * @throws SQLException
	 */
	public void dedupDB() throws SQLException {
		if ((dbIsConnected)) {
			String sp = "\"";
			dbMainStmt.execute("DELETE FROM " + sp + "FilesTemp" + sp + ";");
			String req = "INSERT INTO " + sp + "FilesTemp" + sp + "(" + sp + "ShortName" + sp + ", " + sp + "FullPath"
					+ sp + ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp
					+ "isCRC32Calculated" + sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", "
					+ sp + "DeviceComment" + sp + ", " + sp + "DiscLabel" + sp + ", " + sp + "dtLastModification" + sp
					+ ", " + sp + "FileExt" + sp + ") " + "SELECT DISTINCT " + sp + "ShortName" + sp + ", " + sp
					+ "FullPath" + sp + ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp
					+ "isCRC32Calculated" + sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", "
					+ sp + "DeviceComment" + sp + ", " + sp + "DiscLabel" + sp + ", " + sp + "dtLastmodification" + sp
					+ ", " + sp + "FileExt" + sp + " from " + sp + dbMainTableName + sp;// --
			// +
			// ";";
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepared follow request {" + req + "}");
			}
			dbMainPStmt = dbMainConn.prepareStatement(req);
			dbMainPStmt.executeUpdate();
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepared request for {" + req + "} has worked ok!");
			}

			dbMainConn.commit();
		} // if
		else {
			log.warning("Application can not deduplicate records because database connection disable.");
		}

	}// dedup

	/**
	 * Присоединяет к основной таблице БД сведения из такой же БД, расположенной
	 * в файле fnDif. Проверки дублирования или еще каких-то проверок при
	 * присоединении не выполняется
	 * 
	 * @param fnDif
	 * @return
	 * @throws SQLException
	 */
	public long mergeFromDB(String fnDif) throws SQLException {
		if ((dbIsConnected)) {
			long cnt = 0;
			if (fnDif.equals(null) || fnDif.equals("")) {
				log.warning("Can not merge with null or empty file name of DB.");
			} else {
				Connection connDif = null;
				try {
					Class.forName("org.sqlite.JDBC");
					connDif = DriverManager.getConnection("jdbc:sqlite:" + fnDif);
				} catch (ClassNotFoundException ex) {
					log.severe("Class not found jdbc:sqlite: with message: " + ex.getMessage());
					System.out.println("Class not found jdbc:sqlite: with message: " + ex.getMessage());
					System.exit(0);
				} catch (SQLException ex) {
					log.severe("Can not connect to jdbc:sqlite: with message: " + ex.getMessage());
					System.out.println("Class not connect to jdbc:sqlite: with message: " + ex.getMessage());
					System.exit(0);
				}
				String req = "";
				String sp = "\"";
				Statement statmtDif = null;
				ResultSet resSetDif = null;

				try {
					connDif.setAutoCommit(false);// открываем вторую базу и
													// читаем все файлы из
													// таблицы
					statmtDif = connDif.createStatement();
					statmtDif.execute("PRAGMA journal_mode=MEMORY");// перенос
																	// файла
																	// транзакций
																	// и
																	// временного
																	// буфера в
																	// память
					statmtDif.execute("PRAGMA temp_store=MEMORY");
					resSetDif = statmtDif.executeQuery("SELECT * FROM [" + dbMainTableName + "]");
					connDif.commit();
				} catch (SQLException ex) {
					log.severe("Select * from " + dbMainTableName + ". Error with message: " + ex.getMessage());
					if (connDif != null) {
						connDif.close();
					}
					closeDB();
					System.out.println("Error select * from " + dbMainTableName);
					System.exit(0);
				}
				// готовим запрос на вставку в prepare insert
				try {
					req = "INSERT INTO " + sp + dbMainTableName + sp + "(" + sp + "ShortName" + sp + ", " + sp
							+ "FullPath" + sp + ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", "
							+ sp + "isCRC32Calculated" + sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial"
							+ sp + ", " + sp + "DeviceComment" + sp + ", " + sp + "DiscLabel" + sp + ", " + sp
							+ "dtLastModification" + sp + ", " + sp + "dtCreateRecord" + sp + ", " + sp + "FileExt" + sp
							+ ") VALUES(" + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
					dbMainPStmt = dbMainConn.prepareStatement(req);
					if (log.isLoggable(Level.FINE)) {
						log.fine("prepared request {" + req + "} sent.");
					}
				} catch (SQLException ex) {
					log.warning("Prepared request {" + req + "} error with message: " + ex.getMessage());
					if (connDif != null) {
						connDif.close();
					}
					closeDB();
					System.out.println("Error prepare request.");
					System.exit(0);
				}
				// добавляем строки для вставки
				try {
					while (resSetDif.next()) {
						// long fileID = resSetDif.getLong("FileID");
						String shortName = resSetDif.getString("ShortName");
						String fullPath = resSetDif.getString("FullPath");
						long fileSize = resSetDif.getLong("FileSize");
						long checkSumCRC32 = resSetDif.getLong("CheckSumCRC32");
						boolean isCRC32Calculated = resSetDif.getBoolean("isCRC32Calculated");
						String dtLastModification = resSetDif.getString("dtLastModification");
						String dtCreateRecord = resSetDif.getString("dtCreateRecord");
						int errStatus = resSetDif.getInt("ErrStatus");
						String deviceSerial = resSetDif.getString("DeviceSerial");
						String deviceComment = resSetDif.getString("DeviceComment");
						String discLabel = resSetDif.getString("DiscLabel");
						String fileExt = getExt(shortName);// ;resSetDif.getString("DiscLabel");

						dbMainPStmt.setString(1, shortName);
						dbMainPStmt.setString(2, fullPath);
						dbMainPStmt.setLong(3, fileSize);
						dbMainPStmt.setLong(4, checkSumCRC32);
						dbMainPStmt.setBoolean(5, isCRC32Calculated);
						dbMainPStmt.setInt(6, errStatus);
						dbMainPStmt.setString(7, deviceSerial);
						dbMainPStmt.setString(8, deviceComment);
						dbMainPStmt.setString(9, discLabel);
						dbMainPStmt.setString(10, dtLastModification);
						dbMainPStmt.setString(11, dtCreateRecord);
						dbMainPStmt.setString(12, fileExt);
						dbMainPStmt.executeUpdate();
						cnt++;
						if (log.isLoggable(Level.FINE)) {
							log.fine(cnt + ". Prepared request for {" + fullPath + "} has worked ok!");
						}
					}
					dbMainConn.commit();
				} catch (SQLException ex) {
					log.warning("Prepared request {" + req + "} stopped with message: " + ex.getMessage());
					if (connDif != null) {
						connDif.close();
					}
					closeDB();
					System.out.println("Error insert " + cnt + " lines into prepare request.");
					System.exit(0);
				}

				try {
					connDif.close();
					statmtDif.close();
					resSetDif.close();
				} catch (SQLException ex) {
					log.warning("Can not close merged DB with message: " + ex.getMessage());
				}
			}
			return cnt;
		}
		return -1;
	}// readFromDB

	/**
	 * Генерирует bat файл, содержащий строки для удаления дублированных файлов
	 * 
	 * @param fnBase
	 * @param req
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public long genBatScript(String fnBase) throws SQLException, IOException {
		long ll = -1;
		File fp1 = new File(dbFilenameWithoutExt + ".bat");
		FileWriter fw = new FileWriter(fp1, true);
		extL1 = new FSFileExtList();
		extL1.extList = new ArrayList<FSFileExt>();
		String sp = "\"";
		p(8);
		if ((dbIsConnected)) {
			// Загружаем из таблицы БД предпочтительные пути для расширений
			// файлов
			ll = 0;
			String req = "";
			dbMainRes = dbMainStmt.executeQuery("SELECT * FROM [prefExt]");
			dbMainConn.commit();
			p(9);
			while (dbMainRes.next()) {
				String fileExt = dbMainRes.getString("FileExt");
				String fullPath = dbMainRes.getString("FullPath");
				long cnt = dbMainRes.getLong("CNT");
				FSFileExt fex = new FSFileExt();
				fex.setExt(fileExt);
				fex.setPath(fullPath);
				fex.setPriorityLevel(cnt);
				extL1.extList.add(fex);
				log.fine(ll++ + ". Read from DB " + fex.toString());
			} // while

			p(10);
			// Заполняем таблицу дубликатов
			req = "DELETE FROM [doubles]";
			try {
				dbMainPStmt.executeUpdate(req);
				dbMainConn.commit();
			} catch (SQLException ex) {
				log.warning(
						"Error in request " + req + " Message: " + ex.getMessage() + " SQLstate:" + ex.getSQLState());
			}
			
			req="INSERT INTO [doubles] ([CheckSum],[FileSize],[Cnt])"+ 
	   			"	SELECT  CheckSumCRC32 as CheckSum, Filesize as FileSize, count(shortname) as Cnt from filesTemp group by CheckSum,filesize having cnt>1 order by cnt desc";
			try {
				dbMainPStmt.executeUpdate(req);
				dbMainConn.commit();
			} catch (SQLException ex) {
				log.warning(
						"Error in request " + req + " Message: " + ex.getMessage() + " SQLstate:" + ex.getSQLState());
			}
			
			// Запрашиваем данные дубликатов из таблицы БД
			req = "SELECT * FROM [doubles] WHERE FileSize>(-1) ORDER BY FileSize DESC";
			try {
				dbMainRes = dbMainPStmt.executeQuery(req);
				dbMainConn.commit();
			} catch (SQLException ex) {
				log.warning(
						"Error in request " + req + " Message: " + ex.getMessage() + " SQLstate:" + ex.getSQLState());
			} catch (Exception ex) {
				log.warning("Error in request " + req + " Message: " + ex.getMessage());
			}

			p(11);
			// Открываем файл скрипта удаления дублей
			try {
				if (!((fnBase == null) || (fnBase.equals("")))) {
					fp1 = new File(fnBase + ".bat");
				}
				fw = new FileWriter(fp1, false);
				fw.write("REM ===========================================================================\r\n");
				fw.write("REM       This script was generated automatically by " + dbLibGeneratorName + "\r\n");
				fw.write("REM   !!! All external copyes of your files will be deleted by this script!!!\r\n");
				fw.write("REM ===========================================================================\r\n");
			} catch (IOException e) {
				log.severe("File cannot created " + fp1.getCanonicalPath() + " Message: " + e.getMessage());
			}

			p(12);
			// заполняем файл bat-скрипта
			ResultSet res = null;
			ll = 0;
			while (dbMainRes.next()) {
				long countFile = dbMainRes.getLong("Cnt");
				long fileSize = dbMainRes.getLong("FileSize");
				long checkSum = dbMainRes.getLong("CheckSum");
				
				req = "SELECT * FROM filesTemp WHERE FileSize=" + fileSize + " AND CheckSumCRC32=" + checkSum
						+ " Order by ShortName DESC";
				try {
					res = dbMainPStmt.executeQuery(req);
					dbMainConn.commit();
				} catch (SQLException ex) {
					log.severe("Error in request " + req + " Message: " + ex.getMessage() + " SQLstate:"
							+ ex.getSQLState());

					// Создаем переменные, в которые помещаем строку результата
					// запроса БД
					String shortName = res.getString("ShortName");
					String fullPath = res.getString("FullPath");
					long fileSize1 = res.getLong("FileSize");
					long checkSumCRC32 = res.getLong("CheckSumCRC32");
					String dtLastModification = res.getString("dtLastModification");
					String deviceSerial = res.getString("DeviceSerial");
					String deviceComment = res.getString("DeviceComment");

					String cmdPrefix = "DEL";
					String firstPrefix = "REM";
					String cmdLine = "";
					String sp1 = "; ";
					String sp2 = "\"";

					ll++;

					cmdLine = cmdPrefix + " " + sp2 + fullPath + sp2 + " :: " + " Size=" + fileSize1 + sp1 + "CRC32="
							+ checkSumCRC32 + sp1 + "Modifyed=" + dtLastModification + sp1 + "Name=" + shortName + sp1
							+ " on (" + deviceComment + " SN." + deviceSerial + ") #";
					try {
						fw.write(cmdLine + "\r\n");
					} catch (FileNotFoundException e) {
						log.severe("Error BAT file cannot write. File not exist. Message: " + e.getMessage());
					} catch (IOException e) {
						log.severe("Error BAT file cannot write. File not available. Message: " + e.getMessage());
					}

					System.out.println("    " + ll + ") " + cmdLine);
				} // while write to fw
				p(13);
				
			}
			fw.close();
		} // if db
		else {
			log.warning("Application can not write bat file because database connection disable.");
		} // else db
		p(100 + ll);
		return ll;
	}// genBatScript(fnBase)
}

/*------------------------всяческие примеры кода по ходу разработки-----
 * 
 * 
 * 
 * updateSales = con.prepareStatement(updateString); updateTotal =
 * con.prepareStatement(updateStatement);
 * 
 * for (Map.Entry<String, Integer> e : salesForWeek.entrySet()) {
 * updateSales.setInt(1, e.getValue().intValue()); updateSales.setString(2,
 * e.getKey()); updateSales.executeUpdate(); updateTotal.setInt(1,
 * e.getValue().intValue()); updateTotal.setString(2, e.getKey());
 * updateTotal.executeUpdate(); con.commit(); } statmtBase.execute(
 * "PRAGMA journal_mode=MEMORY");//перенос файла транзакций и временного
 * буфера в память statmtBase.execute("PRAGMA temp_store=MEMORY");
 * 
 * 
 * /*
 * 
 * try { req = "INSERT INTO "+sp+"Files"+sp+"("+ sp+"ShortName"+sp+", "
 * +sp+"FullPath"+sp+", "+sp+"FileSize"+sp+", "+sp+"CheckSumCRC32"+sp+", " +
 * sp+"isCRC32Calculated"+sp+", "+sp+"dtLastModification"+sp+", "
 * +sp+"ErrStatus"+sp+", " + sp+"DeviceSerial"+sp+", "
 * +sp+"DeviceComment"+sp+ ") VALUES("+ sp+mf.getShortName()+sp+", "
 * +sp+mf.getFullPath()+sp+", " +sp+mf.getFileSize()+sp+", "
 * +sp+mf.getCheckSumCRC32()+ sp+", " +sp+mf.isCheckSumCalculated()+sp+", "
 * +sp+ ds +sp+ ", " +sp+mf.getErrStatus()+sp+", "+sp+ sserial+sp+", "
 * +sp+shost+sp+");";
 * 
 * statmt.execute(req); //кириллицу пишет в UTF-8, как по умолчанию хранит в
 * JAVA и в SQLite //System.out.println(ds+" "+req); } catch (SQLException
 * ex) { MyErrorMSG.dlg(errPrefix+"005",
 * " Не удалось добавить запись в БД.\r\n" +req+"\r\n",ex,
 * MyErrorMSG.getDefaultOutMode()); try { conn.close(); statmt.close();
 * //resSet.close(); } catch (SQLException ex1) {
 * MyErrorMSG.dlg(errPrefix+"007",
 * "Не удалось закрыть соединение с БД после ошибки.",ex1,
 * MyErrorMSG.getDefaultOutMode()); } System.exit(0); } //надо проверить,
 * чтобы писать в UTF8, возможны глюки с кириллицей //System.out.println(
 * "saveToDB " +fil.fullPath); conn.WriteDBRecord(fil, sHost, sSerial);
 * saved++; } //for (MyFile fil : this.filesList) {
 * //conn.WriteDBRecord(fil, sHost, sSerial); // saved++; }//for
 *
 * 
 * FileWriter fw = null; try { fw = new
 * FileWriter(dbFilenameWithoutExt+".json", true); } catch (IOException e)
 * {log.severe("Can't open or create JSON file ["+dbFilenameWithoutExt+
 * ".json] with message "+e.getMessage());} if (fw!=null) //Writer writer =
 * new StringWriter(); //FSFileExtensionPath fp = new
 * FSFileExtensionPath(fileExt, fullPath, cnt);
 * 
 * //mapper. //DateFormat customDateFormat = new SimpleDateFormat(
 * "yyyy/dd/MM, HH:mm:ss"); //mapper.setDateFormat(customDateFormat);
 * 
 * //Writer writer = new StringWriter(); // //mapper.writeValue(fw, fp);
 * //System.out.println(//fileExt+" "+fullPath+" "+cnt+" wr="+ //
 * writer.toString());
 *
 * // select distinct substr(shortname,instr(shortname,".")) as s, //
 * count(shortname) from filestemp where length(s)<25 group by s; // select
 * distinct substr(fullpath,1,instr(fullpath,shortname)-2) as // subdir ,
 * substr(shortname,instr(shortname,".")) as ext, // count(shortname) from
 * filestemp where length(ext)<25 group by subdir; // select fullpath,
 * fileext, count(fileext) as cnt from files1 group by // fullpath,fileext
 * order by cnt desc;
 *
 */