package com.resanc.filesorter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Logger;

public class FSDeviceWinInfo {

	private static String DeviceName;
	private static String Serial;

	// служебные внутренние свойства класса
	private static Logger log = Logger.getLogger(FSDeviceWinInfo.class.getName());

	public FSDeviceWinInfo() {	FSDeviceWinInfo.setParams("bios", "serialnumber"); 
								log.info("Device: "+DeviceName+" SN."+Serial);}
	public static String getHostName() { return DeviceName; }
	public String getSerial() {	return Serial; }

	private static void setParams(String stype, String snumber)
	{
		Scanner sc = null;
		String property = "none";
		String serial = "noserial";
		try {
			Process process = Runtime.getRuntime().exec(new String[] { "wmic", stype, "get", snumber });
			process.getOutputStream().close();
			sc = new Scanner(process.getInputStream());
			property = sc.next();
			serial = sc.next();
		} catch (Exception ex) { log.warning("Problem with getting serial number of device. Message: " + ex.getMessage());
		} finally {	sc.close(); }
		Serial = serial;
		
		InetAddress ip;
		try {
			String computername = InetAddress.getLocalHost().getCanonicalHostName();
			DeviceName = computername;
		} catch (UnknownHostException ex) {	log.warning("Problem with getting host computer name. Message: "+ ex.getMessage());}
	}//setParams
}


// System.out.println("Current host : "+computername);

/*
 * ip = InetAddress.getLocalHost(); System.out.println(
 * "Current IP address : " + ip.getHostAddress());
 * 
 * NetworkInterface network = NetworkInterface.getByInetAddress(ip);
 * 
 * byte[] mac = network.getHardwareAddress();
 * 
 * System.out.print("Current MAC address : ");
 * 
 * StringBuilder sb = new StringBuilder(); for (int i = 0; i <
 * mac.length; i++) { sb.append(String.format("%02X%s", mac[i], (i <
 * mac.length - 1) ? "-" : ""));
 * 
 * } System.out.println(sb.toString());
 */
//---------------------------------
// wmic command for diskdrive id: wmic DISKDRIVE GET SerialNumber
// wmic command for cpu id : wmic cpu get ProcessorId
/*
 * Process process = Runtime.getRuntime().exec(new String[] { "wmic",
 * "bios", "get", "serialnumber" }); process.getOutputStream().close();
 * Scanner sc = new Scanner(process.getInputStream()); String property =
 * sc.next(); String serial = sc.next();
 */
// return MyDeviceWin.getParamWin("DISKDRIVE", "serialnumber",
// MyDeviceWin.ONLY_SERIAL)+"\n"
// +MyDeviceWin.getParamWin("cpu", "processorid",
// MyDeviceWin.ONLY_SERIAL)+"\n"
// +MyDeviceWin.getParamWin("bios", "serialnumber",
// MyDeviceWin.ONLY_SERIAL)+"\n";

