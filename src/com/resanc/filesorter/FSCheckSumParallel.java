/** Параллельное многопоточное вычисление контрольных сумм
 * 
 */
package com.resanc.filesorter;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Yury
 *
 */
public class FSCheckSumParallel implements Runnable{
    private ConcurrentLinkedQueue<FSFileCard>  fq, rq;
    
	/**
	 * @param fs
	 */
	public FSCheckSumParallel(ConcurrentLinkedQueue<FSFileCard> fq, ConcurrentLinkedQueue<FSFileCard> rq) {
		super();
		this.fq = fq;
		this.rq = rq;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
	//System.out.println(fs.getShortName()+" "+
		FSFileCard fs=fq.poll();
		if (fs!=null) {	fs.calculateCRC32(); rq.add(fs); 
		//--
		System.out.println(this.toString()+" file="+fs.getShortName()+" crc="+fs.getCheckSumCRC32());
		//--
		}	
	}
}
