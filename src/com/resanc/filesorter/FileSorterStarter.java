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

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

		String S;

		// инициализируем логер

		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(new File("resources/logging.properties")));
		} catch (Exception e) {
			System.out.println("Error: Could not setup logger configuration from file resources/logging.properties: " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}

		// ищем все файлы по указанному пути папки и заполняем картотеку
		long workTime = System.currentTimeMillis();//засекаем время исполнения
		FSScanFileCards crd = new FSScanFileCards();
		try {
			crd.addToFileCards(new File("."));
		} catch (Exception e) {
			log.severe("Error: File cards did not add to card list. Message: " + e.getMessage());
		}
		log.fine("Working time is " + (System.currentTimeMillis() - workTime) + " ms.");//показываем время исполнения и скорость

		// инициализируем устройство для записи в БД его идентификатора и
		// серийного номера
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
	}
}
