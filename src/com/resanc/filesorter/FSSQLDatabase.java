/**
 * 
 */
package com.resanc.filesorter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


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
public class FSSQLDatabase {

	private static String dbFilename = "filesorterbase.s3db";
	private static String dbJDBCClassname = "org.sqlite.JDBC";
	private static String dbJDBCname = "jdbc:sqlite:";
	private static Connection dbMainConn;
	private static Statement dbMainStmt;
	private static PreparedStatement dbMainPStmt;
	private static ResultSet dbMainRes;

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSSQLDatabase.class.getName());
	private static boolean dbIsConnected = false;

	/**
	 * При создании объекта устанавливается соединение с БД или возвращается
	 * ошибка SQL
	 * 
	 * @param fn
	 *            имя файла базы данных SQLite
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
		dbMainStmt.execute("CREATE TABLE if not exists [Files] ("
				+ "[FileID] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + "[ShortName] VARCHAR(255)  NOT NULL,"
				+ "[FullPath] VARCHAR(255)  NOT NULL," + "[FileSize] BIGINT NOT NULL,"
				+ "[CheckSumCRC32] BIGINT NOT NULL," + "[isCRC32Calculated] BOOLEAN DEFAULT 'false' NOT NULL,"
				+ "[dtLastModification] DATETIME  NULL,"
				+ "[dtCreateRecord] TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," + "[ErrStatus] INTEGER  NULL,"
				+ "[DeviceSerial] VARCHAR(25) NULL," + "[DeviceComment] VARCHAR(100)  NULL,"
				+ "[DiscLabel] VARCHAR(35)  NULL" + ");");
		dbMainStmt.execute("CREATE TABLE if not exists [FilesTemp] (" + "[ShortName] VARCHAR(255)  NOT NULL,"
				+ "[FullPath] VARCHAR(255)  NOT NULL," + "[FileSize] BIGINT NOT NULL,"
				+ "[CheckSumCRC32] BIGINT NOT NULL," + "[isCRC32Calculated] BOOLEAN DEFAULT 'false' NOT NULL,"
				+ "[dtLastModification] DATETIME  NULL," + "[ErrStatus] INTEGER  NULL,"
				+ "[DeviceSerial] VARCHAR(25) NULL," + "[DeviceComment] VARCHAR(100)  NULL,"
				+ "[DiscLabel] VARCHAR(35)  NULL" + ");");
		dbMainStmt.execute("CREATE INDEX if not exists [SearchFileSizeCRC32] ON [Files](" + "[FileSize]  DESC,"
				+ "[isCRC32Calculated]  DESC" + ");");
		dbMainStmt.execute("CREATE TABLE IF NOT EXISTS [doubles] (" + "[CheckSum] BIGINT NOT NULL,"
				+ "[FileSize] BIGINT NOT NULL," + "[Cnt] BIGINT NOT NULL)");
		dbMainConn.commit();// коммитим транзакцию создания таблиц
		dbIsConnected = true;
		log.info("Database connects and tables creates Ok in " + dbFilename);
	}// constructor

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

	public void writeToDB(FSScanFileCards crd, FSDeviceWinInfo dvc) throws SQLException {
		if ((dbIsConnected) && (crd != null) && (dvc != null)) {
			String sp = "\"";
			String req = "INSERT INTO " + sp + "Files" + sp + "(" + sp + "ShortName" + sp + ", " + sp + "FullPath" + sp
					+ ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp + "isCRC32Calculated"
					+ sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", " + sp + "DeviceComment"
					+ sp + ", " + sp + "DiscLabel" + sp + ", " + sp + "dtLastModification" + sp + ") VALUES("
					+ "?, ?, ?, ?, ?, ?, ?, ?, ?, strftime('%Y-%m-%d %H:%M:%S', ? ,'unixepoch'))";
			dbMainPStmt = dbMainConn.prepareStatement(req);
			if (log.isLoggable(Level.FINE)) {
				log.fine("prepared request {" + req + "} has worked ok!");
			}

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
				dbMainPStmt.setLong(10, fil.getLastModification() / 1000L);
				dbMainPStmt.executeUpdate();
				if (log.isLoggable(Level.FINE)) {
					log.fine("prepared request for {" + fil.toString(fil.TO_FULL_STRING_FORM) + "} has worked ok!");
				}
			}
			dbMainConn.commit();
		} // if
		else {
			log.warning(
					"Application can not write file cards to database because database connection disable or null params for call.");
		}
	}// writeToDB

	public void dedupDB() throws SQLException {
		if ((dbIsConnected)) {
			String sp = "\"";
			dbMainStmt.execute("DELETE FROM " + sp + "FilesTemp" + sp + ";");
			String req = "INSERT INTO " + sp + "FilesTemp" + sp + "(" + sp + "ShortName" + sp + ", " + sp + "FullPath"
					+ sp + ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp
					+ "isCRC32Calculated" + sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", "
					+ sp + "DeviceComment" + sp + ", " + sp + "DiscLabel" + sp + ", " + sp + "dtLastModification" + sp
					+ ") " + "SELECT DISTINCT " + sp + "ShortName" + sp + ", " + sp + "FullPath" + sp + ", " + sp
					+ "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp + "isCRC32Calculated" + sp + ", "
					+ sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", " + sp + "DeviceComment" + sp + ", "
					+ sp + "DiscLabel" + sp + ", " + sp + "dtLastmodification" + sp + " from " + sp + "FILES" + sp;// --
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

	public long mergeFromDB(String fnDif) throws SQLException {
		if ((dbIsConnected)) {
			long cnt=0;
	    	if (fnDif.equals(null)||fnDif.equals("")){
	    		log.warning("Can not merge with null or empty file neme of DB.");}
           else {
        	  Connection connDif = null;
    	      try { Class.forName("org.sqlite.JDBC");
	       		   connDif = DriverManager.getConnection("jdbc:sqlite:"+fnDif);       		   
	    	   } catch (ClassNotFoundException ex) {
	    		   log.severe("Class not found jdbc:sqlite: with message: "+ex.getMessage());
	    		   System.out.println("Class not found jdbc:sqlite: with message: "+ex.getMessage()); System.exit(0);
	           } catch (SQLException ex) {
	        	   log.severe("Can not connect to jdbc:sqlite: with message: "+ex.getMessage());
	        	   System.out.println("Class not connect to jdbc:sqlite: with message: "+ex.getMessage()); System.exit(0);
	           }
	    	   String req    = "";
	    	   String sp="\"";
	    	   Statement statmtDif = null;
	    	   ResultSet resSetDif = null;
	          
	    	   try { connDif.setAutoCommit(false);//открываем вторую базу и читаем все файлы из таблицы	
	    	   		statmtDif = connDif.createStatement();
	       		   	statmtDif.execute("PRAGMA journal_mode=MEMORY");//перенос файла транзакций и временного буфера в память
		       		statmtDif.execute("PRAGMA temp_store=MEMORY");
	       		   	resSetDif = statmtDif.executeQuery("SELECT * FROM files");
	       		   	connDif.commit();}
	       	 catch (SQLException ex) {
	       		   		log.severe("Select * from files. Error with message: "+ex.getMessage());
	       		   		if (connDif!=null){connDif.close();}
	       		   		closeDB();
	       		   		System.out.println("Error select * from files."); System.exit(0);}
	       		   	//готовим запрос на вставку в prepare insert
	       	try{
	  	   	req = "INSERT INTO " + sp + "Files" + sp + "(" + sp + "ShortName" + sp + ", " + sp + "FullPath" + sp
							+ ", " + sp + "FileSize" + sp + ", " + sp + "CheckSumCRC32" + sp + ", " + sp + "isCRC32Calculated"
							+ sp + ", " + sp + "ErrStatus" + sp + ", " + sp + "DeviceSerial" + sp + ", " + sp + "DeviceComment"
							+ sp + ", " + sp + "DiscLabel" + sp + ", " + sp + "dtLastModification" 
							+ sp + ", " + sp + "dtCreateRecord"+ sp + ") VALUES("
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
					dbMainPStmt = dbMainConn.prepareStatement(req);
					if (log.isLoggable(Level.FINE)) {
						log.fine("prepared request {" + req + "} sent.");
					}}
	       		   	catch (SQLException ex) {
	       		   		log.warning("Prepared request {"+req+"} error with message: "+ex.getMessage());
	       		   		if (connDif!=null){connDif.close();}
	       		   		closeDB();
	       		   	System.out.println("Error prepare request.");System.exit(0);
	       		   	}
	       		   	//добавляем строки для вставки
	       try {	
	       		   		while(resSetDif.next())
	       		   		{			            
			            //long 	fileID 			= 	resSetDif.getLong("FileID");
			            String 	shortName 		= 	resSetDif.getString("ShortName");
			            String 	fullPath 		= 	resSetDif.getString("FullPath");
			            long	fileSize 		=	resSetDif.getLong("FileSize");
			            long	checkSumCRC32 	=	resSetDif.getLong("CheckSumCRC32");
        				boolean isCRC32Calculated = resSetDif.getBoolean("isCRC32Calculated");
        				String 	dtLastModification=	resSetDif.getString("dtLastModification");
        				String	dtCreateRecord 	=	resSetDif.getString("dtCreateRecord");
        				int		errStatus		=	resSetDif.getInt("ErrStatus");
        				String 	deviceSerial	=	resSetDif.getString("DeviceSerial");
        				String 	deviceComment 	=	resSetDif.getString("DeviceComment");
        				String 	discLabel 		=	resSetDif.getString("DiscLabel");
        			            
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
        				dbMainPStmt.executeUpdate(); cnt++;
        				if (log.isLoggable(Level.FINE)) {
        							log.fine(cnt+". Prepared request for {" + fullPath + "} has worked ok!");	}
        				}
        				dbMainConn.commit();       		    	           		    	            		    	            		    	    
        		    	} catch (SQLException ex)
        		    	    {
        		    	 	   log.warning("Prepared request {"+req+"} stopped with message: "+ex.getMessage());
        		    	 	  if (connDif!=null){connDif.close();}
      	       		   		closeDB();System.out.println("Error insert "+cnt+" lines into prepare request.");
      	       		   		System.exit(0);
        		    	    }
	    	    
	           try {
	        	  connDif.close();
	        	  statmtDif.close();
		          resSetDif.close();
	           } catch (SQLException ex) {
	        	   log.warning("Can not close merged DB with message: "+ex.getMessage());	       	   } 
    	   }
	return cnt;
		}
		return -1;
}// readFromDB

/*
 * примеры
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
 * "PRAGMA journal_mode=MEMORY");//перенос файла транзакций и временного буфера
 * в память statmtBase.execute("PRAGMA temp_store=MEMORY");
 * 
 * 
 * /*
 * 
 * try { req = "INSERT INTO "+sp+"Files"+sp+"("+ sp+"ShortName"+sp+", "
 * +sp+"FullPath"+sp+", "+sp+"FileSize"+sp+", "+sp+"CheckSumCRC32"+sp+", " +
 * sp+"isCRC32Calculated"+sp+", "+sp+"dtLastModification"+sp+", "
 * +sp+"ErrStatus"+sp+", " + sp+"DeviceSerial"+sp+", " +sp+"DeviceComment"+sp+
 * ") VALUES("+ sp+mf.getShortName()+sp+", " +sp+mf.getFullPath()+sp+", "
 * +sp+mf.getFileSize()+sp+", " +sp+mf.getCheckSumCRC32()+ sp+", "
 * +sp+mf.isCheckSumCalculated()+sp+", " +sp+ ds +sp+ ", "
 * +sp+mf.getErrStatus()+sp+", "+sp+ sserial+sp+", " +sp+shost+sp+");";
 * 
 * statmt.execute(req); //кириллицу пишет в UTF-8, как по умолчанию хранит в
 * JAVA и в SQLite //System.out.println(ds+" "+req); } catch (SQLException ex) {
 * MyErrorMSG.dlg(errPrefix+"005", " Не удалось добавить запись в БД.\r\n"
 * +req+"\r\n",ex, MyErrorMSG.getDefaultOutMode()); try { conn.close();
 * statmt.close(); //resSet.close(); } catch (SQLException ex1) {
 * MyErrorMSG.dlg(errPrefix+"007",
 * "Не удалось закрыть соединение с БД после ошибки.",ex1,
 * MyErrorMSG.getDefaultOutMode()); } System.exit(0); } //надо проверить, чтобы
 * писать в UTF8, возможны глюки с кириллицей //System.out.println( "saveToDB "
 * +fil.fullPath); conn.WriteDBRecord(fil, sHost, sSerial); saved++; } //for
 * (MyFile fil : this.filesList) { //conn.WriteDBRecord(fil, sHost, sSerial); //
 * saved++; }//for
 */
}
