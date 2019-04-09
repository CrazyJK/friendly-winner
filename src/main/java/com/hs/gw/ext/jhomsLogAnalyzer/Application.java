package com.hs.gw.ext.jhomsLogAnalyzer;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.hs.gw.ext.jhomsLogAnalyzer.service.AnalyzingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class Application implements ApplicationRunner {

	@Autowired
	private ApplicationContext appContext;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/*
	 * jhoms-dispatch-log-analyzer \ 
	 * --log.path="/Users/namjk/Downloads/logs" \
	 * --name.like=dispatch
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("JhomsLogAnalyzer started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));
		log.debug("NonOptionArgs: {}", args.getNonOptionArgs());
		log.debug("OptionNames: {}", args.getOptionNames());

		for (String name : args.getOptionNames()) {
			log.info("arg {} =  {}", name, args.getOptionValues(name));
		}

		if (validArgument(args, "log.path") && validArgument(args, "name.like")) {
			String logPath  = args.getOptionValues("log.path").get(0);
			String nameLike = args.getOptionValues("name.like").get(0);
			AnalyzingService bean = appContext.getBean(AnalyzingService.class);
			bean.process(logPath, nameLike);
		} else {
			log.error("arguments[log.path, name.like] must be not empty");
		}
	}

	private boolean validArgument(ApplicationArguments args, String name) {
		return args.containsOption(name) && args.getOptionValues(name).size() > 0 && StringUtils.isNotBlank(args.getOptionValues(name).get(0));
	}

}
