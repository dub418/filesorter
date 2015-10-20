/**
 * 
 */
package com.resanc.filesorter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author Yury
 *
 */
public class FSZipArc {
	public File directory; //folder for result files
	
	 public static void pack(File directory, String to) throws IOException {
		    URI base = directory.toURI();
		    Deque<File> queue = new LinkedList<File>();
		    queue.push(directory);
		    OutputStream out = new FileOutputStream(new File(to));
		    Closeable res = out;

		    try {
		      ZipOutputStream zout = new ZipOutputStream(out);
		      res = zout;
		      while (!queue.isEmpty()) {
		        directory = queue.pop();
		        for (File child : directory.listFiles()) {
		          String name = base.relativize(child.toURI()).getPath();
		          if (child.isDirectory()) {
		            queue.push(child);
		            name = name.endsWith("/") ? name : name + "/";
		            zout.putNextEntry(new ZipEntry(name));
		          } else {
		            zout.putNextEntry(new ZipEntry(name));
		            InputStream in = new FileInputStream(child);
		            try {
		              byte[] buffer = new byte[1024];
		              while (true) {
		                int readCount = in.read(buffer);
		                if (readCount < 0) {
		                  break;
		                }
		                zout.write(buffer, 0, readCount);
		              }
		            } finally {
		              in.close();
		            }
		            zout.closeEntry();
		          }
		        }
		      }
		    } finally {
		      res.close();
		    }
		  }

	 public static void unpack(String path, String dir_to) throws IOException {
		    ZipFile zip = new ZipFile(path);
		    Enumeration entries = zip.entries();
		    LinkedList<ZipEntry> zfiles = new LinkedList<ZipEntry>();
		    while (entries.hasMoreElements()) {
		      ZipEntry entry = (ZipEntry) entries.nextElement();
		      if (entry.isDirectory()) {
		        new File(dir_to+"/"+entry.getName()).mkdir();
		      } else {
		        zfiles.add(entry);
		      }
		    }
		    for (ZipEntry entry : zfiles) {
		      InputStream in = zip.getInputStream(entry);
		      OutputStream out = new FileOutputStream(dir_to+"/"+entry.getName());
		      byte[] buffer = new byte[1024];
		      int len;
		      while ((len = in.read(buffer)) >= 0)
		        out.write(buffer, 0, len);
		      in.close();
		      out.close();
		      }
		    zip.close();
		  }
	 
}
