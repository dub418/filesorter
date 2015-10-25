/**
 * 
 */
package com.resanc.filesorter;

import java.util.ArrayList;

/**
 * @author Yury
 *
 */
class FSLauthor {
	String Family;//фамилия
	String Otc;//отчество, не используется пока
	String Name;//имя

	FSLauthor() {
		Family = "";
		Otc = "";
		Name = "";
	}
}

class FSLgenre {
	String genre;
	String fullgenre;
}

public class FSDescriptionBook {
	private String fullTitle;
	private String fullAnnotation;
	private String fullAuthor;
	private String fullGenre;
	private String lang;
	private String keywords;
	private String srcLang;
	private ArrayList<FSLauthor> author;
	private ArrayList<FSLgenre> genre;

	public FSDescriptionBook() {
		fullTitle = "";
		fullAnnotation = "";
		fullAuthor = "";
		fullGenre = "";
		lang = "";
		srcLang = "";
		keywords = "";
		author = new ArrayList<FSLauthor>();
		genre = new ArrayList<FSLgenre>();
	}

	public String getFullTitle() {
		return fullTitle;
	}

	public void setFullTitle(String fullTitle) {
		this.fullTitle = fullTitle;
	}

	public String getFullAnnotation() {
		return fullAnnotation;
	}

	public void setFullAnnotation(String fullAnnotation) {
		this.fullAnnotation = fullAnnotation;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String lang) {
		this.keywords = lang;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getSrcLang() {
		return srcLang;
	}

	public void setSrcLang(String srcLang) {
		this.srcLang = srcLang;
	}

	public void prepareData()
	{
		String s="";
		for (FSLauthor elm:author)
		{
			if (s.length()>0){s=s+", ";}
			s=s+elm.Family+" "+elm.Name;
		}
		this.fullAuthor=s;
		s="";
		for (FSLgenre elm:genre)
		{
			if (s.length()>0){s=s+", ";}
			s=s+elm.fullgenre;
		}
		this.fullGenre=s;
	}
	
	public String toCSVString()
	{
		String sp="#";
		return 	this.fullTitle+sp+
				this.fullAuthor+sp+
				this.keywords+sp+
				this.fullGenre+sp+
				this.lang+sp+
				this.srcLang+sp+
				this.fullAnnotation;
	}
	
	public void addGenre(String s) {
		FSLgenre genrl = new FSLgenre();
		genrl.genre = s;
		genrl.fullgenre="";
		FSGenreClassificator gc=new FSGenreClassificator();
		int i=0;
		for (String elm:gc.genreLabel)
		{
			//System.out.println("s="+s+"; elm="+elm+"; i="+i);
			if (elm.equals(s)){
				genrl.fullgenre=gc.genreName[i];
				break;
			}
		i++;
		}
		this.genre.add(genrl);
	}

	public void addAuthorsFamily(String s) {
		FSLauthor auth = new FSLauthor();
		auth.Family = s;
		this.author.add(auth);
	}

	public void addAuthorsName(String s, int i) {
		if (i>=0 && i<=this.author.size())		{
			FSLauthor auth=new FSLauthor();
			auth=author.get(i);
			auth.Name=s;		
			author.set(i, auth);
		}
	}
}
