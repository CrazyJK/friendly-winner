package com.hs.gw.ext.jhomsLogAnalyzer.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.hs.gw.ext.jhomsLogAnalyzer.domain.Dispatch.Type;

public class DispatchFactory {

	public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

	static SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

	public static Dispatch get(String line, String filename) {
		// 2017-08-28 16:01:04,333 INFO main0 STARTS DISPATCHING: #5503
		// [0]        [1]          [2]  [3]   [4]    [5]          [6]
		// 2017-08-28 16:01:04,393 INFO main2 END DISPATCHING: #4660 (laps: 17 ms)
		// [0]        [1]          [2]  [3]   [4] [5]          [6]   [7]    [8]
		
		if (StringUtils.isBlank(line))
			return null;
		
		String[] split = StringUtils.split(line);
		
		if (split.length < 7)
			return null;
		
		Date date = null;
		String thread = null;
		Type type = null;
		String capi = null;
		long laps = 0;
		
		String dateText = split[0] + " " + split[1];
		try {
			date = dateFormat.parse(dateText);
		} catch (ParseException e) {
			return null;
		}
		
		thread = split[3];
		
		if ("STARTS".equals(split[4])) {
			type = Type.START;
		} else if ("END".equals(split[4])) {
			type = Type.END;
		} else {
			return null;
		}
		
		if (!split[6].startsWith("#"))
			return null;
		
		capi = split[6];

		if (type == Type.END && split.length > 8)
			try {
				laps = Long.parseLong(split[8]);
			} catch (NumberFormatException e) {
					return null;
			}
		
		return new Dispatch(date, thread, type, capi, laps, line, filename);
	}

}
