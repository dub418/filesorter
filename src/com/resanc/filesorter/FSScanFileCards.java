/**
 * 
 */
package com.resanc.filesorter;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Картотека файлов, хранимая в оперативной памяти. Используется для сбора
 * данных при сканировании файлов на диске
 * 
 * @author Yury
 *
 */
public class FSScanFileCards {

	// переменные данных картотеки файлов
	private ArrayList<FSFileCard> filesList; // список всех найденных файлов
	private ConcurrentLinkedQueue<FSFileCard> filesQueue; // очередь всех
															// найденных файлов
	private ConcurrentLinkedQueue<FSFileCard> filesQueueRes; // очередь всех
																// найденных
																// файлов после
																// обработки
	private boolean asyncMode = false; // синхронный (по умолчанию) или
										// асинхронный режим работы
	private FSDiscLabels labels;
	private long counterDirectoties = 0; // счетчика папок
	private long counterFiles = 0; // счетчик файлов
	private long counterBytes = 0; // счетчик размера в байтах всей картотеке

	// переменные настройки и статуса картотеки файлов
	private static boolean setCheckSumCalculateON = true; // при заполнении
															// считать CRC32
	private static boolean setCheckSumCalculateOFF = false; // при заполнении не
															// считать CRC32
	private static boolean setAsyncCheckSumCalc = true; // при заполнении
														// считать CRC32
														// асинхронно
	private static boolean setSyncCheckSumCalc = false; // при заполнении
														// считать CRC32
														// синхронно

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSScanFileCards.class.getName());

	/**
	 * @return counterDirectoties++
	 */
	private long addCounterDirectories() {
		return this.counterDirectoties++;
	}

	/**
	 * @return counterFiles++
	 */
	private long addCounterFiles() {
		return this.counterFiles++;
	}

	/**
	 * @param l
	 * @return counterByte
	 */
	private long addCounterBytes(long l) {
		this.counterBytes = this.counterBytes + l;
		return this.counterBytes;
	}

	/**
	 * @return counterDirectoties
	 */
	private long getCounterDirectories() {
		return this.counterDirectoties;
	}

	/**
	 * @return counterFiles
	 */
	public long getCounterFiles() {
		return this.counterFiles;
	}

	/**
	 * @return counterBytes
	 */
	public long getCounterBytes() {
		return this.counterBytes;
	}

	public boolean isAsync(){return asyncMode;}
	/**
	 * Создает новую картотеку файлов для сканирования
	 */
	public FSScanFileCards() {
		filesList = new ArrayList<FSFileCard>();
		labels = new FSDiscLabels();
	}

	public FSScanFileCards(boolean async) {
		if (async) {
			filesQueue = new ConcurrentLinkedQueue<FSFileCard>();
		} else {
			filesList = new ArrayList<FSFileCard>();
		}
		this.asyncMode = async;
		labels = new FSDiscLabels();
	}

	/**
	 * @return filesList
	 */
	public ArrayList<FSFileCard> getScanFileCards() {
		return filesList;
	};

	public ConcurrentLinkedQueue<FSFileCard> getScanFileCardsQueue() {
		return filesQueue;
	};

	/**
	 * Преобразование в строку для вывода
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FSScanFileCards [filesList=" + filesList + ", counterDirectoties=" + counterDirectoties
				+ ", counterFiles=" + counterFiles + ", getCounterDirectories()=" + getCounterDirectories()
				+ ", getCounterFiles()=" + getCounterFiles() + ", getFileCardsSize()=" + getFileCardsSize() + "]";
	}

	public long getFileCardsSize() {
		if (asyncMode) {
			return filesQueue.size();
		} else {
			return filesList.size();
		}
	}

	public void getAsyncCheckSum() throws InterruptedException {
		if (asyncMode) {
			ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			filesList = new ArrayList<FSFileCard>();
			filesQueueRes= new ConcurrentLinkedQueue<FSFileCard> ();
			int i=0; int f=0; int r=0;//--
			while (!filesQueue.isEmpty()) {
				FSCheckSumParallel pr = new FSCheckSumParallel(filesQueue, filesQueueRes);
				//--
				f=filesQueue.size(); r=filesQueueRes.size(); System.out.println(i++ + ". sz(in)=" + f +" sz(ou)=" + r + " total="+(f+r));
				//--
				pool.execute(pr);
				Thread.sleep(1);
			}
			filesList.addAll(filesQueueRes);
			pool.shutdown();
		}
	}

	/**
	 * Добавление сведений о файле в картотеку последовательным обходом дерева
	 * файлов (рекурсия)
	 * 
	 * @param fp
	 *            начало обхода
	 */
	public void addToFileCards(File fp) throws Exception {
		File[] fpFiles = fp.listFiles();
		// long lfiles=0;
		// long ldirs=0;

		// проверка входных параметров, что они не указывают null (папки нижнего
		// уровня без файлов содержат нулевые списки входящих файлов)
		if (fp != null) {
			if (fpFiles != null) {
				for (File fElem : fpFiles) {
					// проверка на нулевые указатели
					if (fElem != null) {
						// обработка для файлов
						boolean isCalcCRC = this.setCheckSumCalculateON;
						if (asyncMode) {
							isCalcCRC = this.setCheckSumCalculateOFF;
						}
						if (fElem.isFile()) {
							FSFileCard rec = new FSFileCard(fElem.getCanonicalPath(), fElem.getName(), fElem.length(),
									fElem.lastModified(), isCalcCRC, this.labels);
							if (asyncMode) {
								if (!filesQueue.add(rec)) {
									log.warning("Application can not add new record into file queue for "
											+ fElem.getCanonicalPath());
								}
							} else {
								if (!filesList.add(rec)) {
									log.warning("Application can not add new record into file list for "
											+ fElem.getCanonicalPath());
								}
							}
							addCounterBytes(rec.getFileSize());
							addCounterFiles();
							if (log.isLoggable(Level.FINE)) {
								log.fine(getCounterFiles() + " point adds file to cards: path="
										+ fElem.getCanonicalPath() + " last=" + fElem.lastModified());
							}
						}

						// обработка для папок
						if (fElem.isDirectory()) {
							addCounterDirectories();
							if (log.isLoggable(Level.FINE)) {
								log.fine(getCounterDirectories() + " folders adds. Now size of file list is "
										+ this.filesList.size() + " on " + fElem.getCanonicalPath());
							}
							try {
								addToFileCards(fElem);
							} catch (Exception ex) {
								log.severe("Recursion problem with addition to file list. Params: " + "FoldersCount = "
										+ getCounterDirectories() + ", FilesCount = " + getCounterFiles()
										+ "\r\nPath = " + fElem.getPath() + "\r\n File = " + fElem.toString()
										+ "\r\n Trace = " + ex.toString() + " ErrorMsg = " + ex.getMessage());
								throw ex;
							}
						} // обр.папок
					} // не null
				} // for
			} // fpFiles
		} // fp
	}// addToFileCards
}
