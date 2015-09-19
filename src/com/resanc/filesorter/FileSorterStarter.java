/**
 * 
 */
package com.resanc.filesorter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author yury.dubrovskiy
 *
 */
public class FileSorterStarter {

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FileSorterStarter.class.getName());
	public static String version = "0.02 / 2015-09-19";

	private static void prn(final String s) {
		System.out.println(s);
	}

	private static void hlpScreen(int i) {
		prn("=============================================================================");
		prn("                        File Sorter for home file archives                   ");
		prn("                        Version: " + version);
		if (i == 1) {prn("ERROR! INCORRECT PARAMETER(S) IN COMMAND LINE!");}
		prn("Start format:   Java -jar FileSorterStarter -<Param> <Path>                  ");
		prn("                                                                             ");
		prn("                <Param> - command parameter one of follow:                   ");
		prn("                    SCAN - scaning of path directory;                        ");
		prn("                    MERGE - merging path database file to main database file;");
		prn("                    DEDUP - delete copies of rows from main database file;   ");
		prn("                                                                             ");
		prn("=============================================================================");
	}

	private static void startStringWrite(final String args[]) {
		if (args.length > 1) {
			System.out.println("Program started with params [" + args[0] + "] [" + args[1] + "]+ Ver." + version);
			log.info("============================================================\r\n"
					+ "Program started with params [" + args[0] + "] [" + args[1] + "]+ Ver." + version + "\r\n"
					+ "==================================================================");
		} else {
			System.out.println("Program started with params [" + args[0] + "] Ver." + version);
			log.info("============================================================\r\n"
					+ "Program started with params [" + args[0] + "] Ver." + version + "\r\n"
					+ "==================================================================");
		}
	}

	public static void main(String[] args)
			throws IOException, ClassNotFoundException, SQLException, InterruptedException {
		// инициализируем логер
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(new File("resources/logging.properties")));
		} catch (Exception e) {
			System.out.println(
					"Program Exit with Error: Could not setup logger configuration from file resources/logging.properties: "
							+ e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		long viparam = 0; // визуальный показатель, сколько строк обработала
							// команда
		FSMetrics metr = new FSMetrics();// временная метрика
		FSMetrics totl = new FSMetrics(); // полное время выполнения программы
		FSScanFileCards crd = new FSScanFileCards(false);

		// обрабатываем параметры запуска. Если пусто, то сообщаем список
		// параметров запуска
		switch (args.length) {
		case 1: // service function without path of help screen
			startStringWrite(args);
			if (args[0].toUpperCase().trim().equals("-DEDUP")) {
				// удаляем из базы дублирующие строки
				// Создаем новую базу
				FSSQLDatabase dbs = null;
				try {
					dbs = new FSSQLDatabase("database.s3db");
					try {
						dbs.dedupDB();
					} catch (Exception e) {
						log.warning("Cannot deduplicate in DB. Message: " + e.getMessage());
					}
				} catch (Exception e) {
					log.warning("Cannot open connect to DB. Message: " + e.getMessage());
				} finally {
					if (dbs != null) {
						dbs.closeDB();
					}
				}
				metr.getTime("Deduplicate database file", 0, "");
			} // -----------dedup-------------
			else {
				hlpScreen(1);
				System.exit(0);
			}
			break;
		case 2: // service function with path params
			startStringWrite(args);
			if (args[0].toUpperCase().trim().equals("-SCAN")) {
				try {
					crd.addToFileCards(new File(args[1]));
				} catch (Exception e) {
					log.severe("Error: File cards did not add to card list. Message: " + e.getMessage());
				}
				System.out.println("Card size=" + crd.getCounterFiles());
				if (crd.isAsync()) {
					crd.getAsyncCheckSum();
				}
				metr.getTime("Loading cards to memory", crd.getCounterBytes(), "bytes");
				// инициализируем для записи в БД идентификатора и SN устройства
				FSDeviceWinInfo dvc = new FSDeviceWinInfo();
				// записываем картотеку в базу
				// Создаем новую базу
				FSSQLDatabase dbs = null;
				try {
					dbs = new FSSQLDatabase("database.s3db");
					try {
						dbs.writeToDB(crd, dvc);
					} catch (Exception e) {
						log.warning("Cannot write cards to DB. Message: " + e.getMessage());
					}
				} catch (Exception e) {
					log.warning("Cannot open connect to DB. Message: " + e.getMessage());
				} finally {
					if (dbs != null) {
						dbs.closeDB();
					}
				}
				metr.getTime("Writing cards to database file", crd.getCounterFiles(), "files");
				viparam = crd.getCounterFiles();
			} // -----------scan-------------
			else if (args[0].toUpperCase().trim().equals("-MERGE")) {
				// удаляем из базы дублирующие строки
				// Создаем новую базу
				FSSQLDatabase dbs = null;
				long li = 0;
				try {
					dbs = new FSSQLDatabase("database.s3db");
					try {
						li = dbs.mergeFromDB(args[1]);
					} catch (Exception e) {
						log.warning("Cannot merge in DB. Message: " + e.getMessage());
					}
				} catch (Exception e) {
					log.warning("Cannot open connect to main DB for merging. Message: " + e.getMessage());
				} finally {
					if (dbs != null) {
						dbs.closeDB();
					}
				}
				metr.getTime("Data merged " + li + "rows to database file from " + args[1], li, "rows");
				viparam = li;
			} // ---------merge-------
			else {
				hlpScreen(1);
				System.exit(0);
			}
			break;
		default:
			hlpScreen(0);
			System.exit(0); // Show Help Screen
		}
		metr.getTime(null, 0, "");
		if (args.length == 1) {
			System.out.println("Application finished with params: [" + args[0] + "] "
					+ totl.getTime("total time", 1, "program") + " msec.");
		}
		if (args.length == 2) {
			System.out.println("Application finished with params: [" + args[0] + "] [" + args[1] + "] "
					+ totl.getTime("total time", 1, "program") + " msec." + viparam + " files.");
		}

	}
}
