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
 * @author ydubrovskiy
 *
 */
public class FileSorterStarter {

	// ��������� ���������� �������� ������
	private static Logger log = Logger.getLogger(FileSorterStarter.class.getName());

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {

		// ���� ��� ����� �� ���������� ���� �����
		String S;

		// �������������� �����

		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(new File("resources/logging.properties")));
		} catch (Exception e) {
			System.out.println(
					"Could not setup logger configuration from file resources/logging.properties: " + e.getMessage());
			e.printStackTrace();
		}

		// ��������� ���������
		long workTime = System.currentTimeMillis();
		FSScanFileCards crd = new FSScanFileCards();
		try {
			crd.addToFileCards(new File("."));
		} catch (Exception e) {
			log.warning("Cards did not add. Message: " + e.getMessage());
		}

		System.out.println("Working time is " + (System.currentTimeMillis() - workTime) + " ms.");

		// �������������� ���������� ��� ������ � �� ��� �������������� �
		// ��������� ������
		FSDeviceWinInfo dvc = new FSDeviceWinInfo();

		// ���������� ��������� � ����
		// ������� ����� ����
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
