package com.resanc.filesorter;

import java.io.File;

public class FSTextFile {
	
public enum TYPE {
	ANY, //произвольный
	STANDARD, //стандарт
	MANUAL, //руководство пользователя
	LEGAL, PAYMENT, TECHNICAL, ART, LANGUAGE, EDUCATION, IT}	
private String ext;
private String shortName;
private String fullPath;
private String textIndex;
private TYPE type;
private String autor;
private String publisher;
private String ISDN;

public String typeToString(int i)
{String typ="no type";
switch (i){
case 1:
case 2:
}
	return typ;
	}
}
