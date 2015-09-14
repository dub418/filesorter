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
 * ��������� ������, �������� � ����������� ������. ������������ ��� �����
 * ������ ��� ������������ ������ �� �����
 * 
 * @author Yury
 *
 */
public class FSScanFileCards {

	// ���������� ������ ��������� ������
	private ArrayList<FSFileCard> filesList; // ������ ���� ��������� ������
	private ConcurrentLinkedQueue<FSFileCard> filesQueue; // ������� ����
															// ��������� ������
	private ConcurrentLinkedQueue<FSFileCard> filesQueueRes; // ������� ����
																// ���������
																// ������ �����
																// ���������
	private boolean asyncMode = false; // ���������� (�� ���������) ���
										// ����������� ����� ������
	private FSDiscLabels labels;
	private long counterDirectoties = 0; // �������� �����
	private long counterFiles = 0; // ������� ������
	private long counterBytes = 0; // ������� ������� � ������ ���� ���������

	// ���������� ��������� � ������� ��������� ������
	private static boolean setCheckSumCalculateON = true; // ��� ����������
															// ������� CRC32
	private static boolean setCheckSumCalculateOFF = false; // ��� ���������� ��
															// ������� CRC32
	private static boolean setAsyncCheckSumCalc = true; // ��� ����������
														// ������� CRC32
														// ����������
	private static boolean setSyncCheckSumCalc = false; // ��� ����������
														// ������� CRC32
														// ���������

	// ��������� ���������� �������� ������
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
	 * ������� ����� ��������� ������ ��� ������������
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
	 * �������������� � ������ ��� ������
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
	 * ���������� �������� � ����� � ��������� ���������������� ������� ������
	 * ������ (��������)
	 * 
	 * @param fp
	 *            ������ ������
	 */
	public void addToFileCards(File fp) throws Exception {
		File[] fpFiles = fp.listFiles();
		// long lfiles=0;
		// long ldirs=0;

		// �������� ������� ����������, ��� ��� �� ��������� null (����� �������
		// ������ ��� ������ �������� ������� ������ �������� ������)
		if (fp != null) {
			if (fpFiles != null) {
				for (File fElem : fpFiles) {
					// �������� �� ������� ���������
					if (fElem != null) {
						// ��������� ��� ������
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

						// ��������� ��� �����
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
						} // ���.�����
					} // �� null
				} // for
			} // fpFiles
		} // fp
	}// addToFileCards
}
