package com.hs.gw.ext.jhomsLogAnalyzer.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Dispatch {

	public enum Type {
		START, END;
	}
	
	Date date;
	String thread;
	Type type;
	String capi;
	long laps;
	String line;
	String filename;

}
