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
		File fl = new File("resources/here.txt");
		File flog = new File("resources/logging.properties");
		
		InputStream ilog = new FileInputStream(flog);
				
		fl.createNewFile();
		PrintWriter out = new PrintWriter(fl.getAbsoluteFile());
		out.print("text abs="+fl.getAbsolutePath()+" cnc="+fl.getAbsolutePath()+" log="+log);
        out.close();
            
		try {
			LogManager.getLogManager()
					.readConfiguration(ilog);
							//FileSorterStarter.class.getResourceAsStream("resources/logging.properties"));
		} catch (Exception e) {
			System.out.println("Could not setup logger configuration: " + e.getMessage()); e.printStackTrace();
		}

		// ��������� ���������
		long workTime = System.currentTimeMillis();
		FSScanFileCards crd = new FSScanFileCards();
		try {
			crd.addToFileCards(new File("."));
			// System.out.println( crd.toString());
		} catch (Exception e) {
			System.out.println("Cards not added. Message: "+e.getMessage());
			e.printStackTrace();
		}
		;
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

			System.out.println("Hello Would!");

			System.out.println("Correction Would hello!");

		}

	}
}
