package com.hs.gw.ext.jhomsLogAnalyzer.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.hs.gw.ext.jhomsLogAnalyzer.domain.Dispatch;
import com.hs.gw.ext.jhomsLogAnalyzer.domain.DispatchFactory;
import com.hs.gw.ext.jhomsLogAnalyzer.domain.Dispatch.Type;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AnalyzingServiceImpl implements AnalyzingService {

	public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";

	static SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
	static SimpleDateFormat writeFormat = new SimpleDateFormat("yyyyMMdd_HHmm");

	@Override
	public void process(String logPath, String nameLike) {
		// find file
		List<File> files = new ArrayList<>();
		if (StringUtils.isNotBlank(logPath)) {
			files.addAll(FileUtils.listFiles(new File(logPath), FileFilterUtils.prefixFileFilter(nameLike), null));
		}
		if (files.size() == 0) {
			log.error("log name info not exist");
			return;
		}
		
		// gathering dispatch
		List<Dispatch> dispatchList = new ArrayList<>();
		for (File file : files) {
			dispatchList.addAll(parseLogFileAndGetDispatch(file));
		}
		log.info("dispatch total count: {}", dispatchList.size());

		final String currentTime = writeFormat.format(new Date());
		
		// make timeline
		Map<String, AtomicInteger> timeMap = new TreeMap<>();
		for (Dispatch dispatch : dispatchList) {
			if (dispatch.getType() == Dispatch.Type.START) {
				String hhmm = dateFormat.format(dispatch.getDate());
				if (timeMap.containsKey(hhmm)) {
					timeMap.get(hhmm).incrementAndGet();
				} else {
					timeMap.put(hhmm, new AtomicInteger(1));
				}
			}
		}
		// print timeline
		File timetableFile = new File(logPath, "analyze_" + currentTime + "_" + nameLike + "_timeline.csv");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(timetableFile))) {
			writer.write(String.format("%s, %s, %s%n", "Date", "Time", "Count"));
			for (Entry<String, AtomicInteger> entry : timeMap.entrySet()) {
				String key = entry.getKey();
				AtomicInteger val = entry.getValue();
				log.debug("{}, {}", key, val.get());
				String[] split = StringUtils.split(key);
				writer.write(String.format("%s, %s, %s%n", split[0], split[1], val.get()));
			}
			writer.flush();
			log.info("write  {}", timetableFile);
		} catch (IOException e) {
			log.error("timetable write error " + e.getMessage(), e);
		}
		
		// make capi-count
		Map<String, List<Dispatch>> callMap = new TreeMap<>();
		for (Dispatch dispatch : dispatchList) {
			if (dispatch.getType() == Dispatch.Type.END) {
				String key = dispatch.getCapi();
				if (callMap.containsKey(key)) {
					callMap.get(key).add(dispatch);
				} else {
					List<Dispatch> list = new ArrayList<>();
					list.add(dispatch);
					callMap.put(key, list);
				}
			}
		}
		// print capi-count
		File calltableFile = new File(logPath, "analyze_" + currentTime + "_" + nameLike + "_capi-count.csv");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(calltableFile))) {
			writer.write(String.format("%s, %s, %s, %s, %s%n", "CAPI", "Count", "Avg laps", "Min laps", "Max laps"));
			for (Entry<String, List<Dispatch>> entry : callMap.entrySet()) {
				String key = entry.getKey();
				List<Dispatch> val = entry.getValue();
				log.debug("{}, {}", key, val.size());
				writer.write(String.format("%s, %s, %s, %s, %s%n", key, val.size(), calcAvgLaps(val), calcMinLaps(val), calcMaxLaps(val)));
			}
			writer.flush();
			log.info("write  {}", calltableFile);
		} catch (IOException e) {
			log.error("calltable write error " + e.getMessage(), e);
		}
		
		// make threadtable
		Map<String, List<Dispatch>> threadMap = new TreeMap<>();
		for (Dispatch dispatch : dispatchList) {
			if (dispatch.getType() == Dispatch.Type.END) {
				String key = dispatch.getThread();
				if (threadMap.containsKey(key)) {
					threadMap.get(key).add(dispatch);
				} else {
					List<Dispatch> list = new ArrayList<>();
					list.add(dispatch);
					threadMap.put(key, list);
				}
			}
		}
		// print threadtable
		File threadtableFile = new File(logPath, "analyze_" + currentTime + "_" + nameLike + "_thread.csv");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(threadtableFile))) {
			writer.write(String.format("%s, %s, %s, %s, %s%n", "Thread", "Count", "Avg laps", "Min laps", "Max laps"));
			for (Entry<String, List<Dispatch>> entry : threadMap.entrySet()) {
				String key = entry.getKey();
				List<Dispatch> val = entry.getValue();
				log.debug("{}, {}", key, val.size());
				writer.write(String.format("%s, %s, %s, %s, %s%n", key, val.size(), calcAvgLaps(val), calcMinLaps(val), calcMaxLaps(val)));
			}
			writer.flush();
			log.info("write  {}", threadtableFile);
		} catch (IOException e) {
			log.error("calltable write error " + e.getMessage(), e);
		}
		
		// capi long-laps 
		List<Dispatch> sortedList = dispatchList.stream().filter(d -> d.getType() == Type.END).sorted(Comparator.comparing(Dispatch::getLaps).reversed()).limit(100).collect(Collectors.toList());
		File longLapsFile = new File(logPath, "analyze_" + currentTime + "_" + nameLike + "_capi-longLaps.csv");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(longLapsFile))) {
			writer.write(String.format("%s, %s, %s, %s%n", "CAPI", "Laps", "file", "log"));
			for (Dispatch dispatch : sortedList) {
				log.debug("{}", dispatch.getLine());
				writer.write(String.format("%s, %s, %s, %s%n", dispatch.getCapi(), dispatch.getLaps(), dispatch.getFilename(), dispatch.getLine()));
			}
			writer.flush();
			log.info("write  {}", longLapsFile);
		} catch (IOException e) {
			log.error("long-laps write error " + e.getMessage(), e);
		}
		
	}

	private Object calcMaxLaps(List<Dispatch> val) {
		return val.stream().mapToLong(d -> d.getLaps()).max().getAsLong();
	}

	private Object calcMinLaps(List<Dispatch> val) {
		return val.stream().mapToLong(d -> d.getLaps()).min().getAsLong();
	}

	private long calcAvgLaps(List<Dispatch> val) {
		return (long) val.stream().mapToLong(d -> d.getLaps()).average().orElse(Double.NaN);
	}

	private List<Dispatch> parseLogFileAndGetDispatch(File file) {
		List<Dispatch> dispatchList = new ArrayList<>();
		try {
			log.info("read log: {}", file);
			List<String> readLines = FileUtils.readLines(file, Charset.defaultCharset());
			String filename = file.getName();
			for (String line : readLines) {
				Dispatch dispatch = DispatchFactory.get(line, filename);
				if (dispatch != null)
					dispatchList.add(dispatch);
			}
			log.info("{} lines, found dispatch {}", readLines.size(), dispatchList.size());
		} catch (IOException e) {
			throw new IllegalStateException("File read error", e);
		}
		
		return dispatchList;
	}

}
