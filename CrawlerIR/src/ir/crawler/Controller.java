package ir.crawler;

import java.io.IOException;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {

	public static final String SEED_URL = "https://www.nbcnews.com";
	private static final String CRAWL_STORAGE_FOLDER = "data/crawl";
	private static final String USER_AGENT = "SidrkCrawler";
	private static final int MAX_PAGES = 1;
	private static final int MAX_DEPTH = 16;
	private static final int POLITENESS_DELAY = 500;
	private static final int NUMBER_OF_CRAWLERS = 7;
	private static final boolean INCLUDE_BINARY = true;

	public static void main(String[] args) throws Exception {

		try {

			CrawlConfig config = new CrawlConfig();
			config.setCrawlStorageFolder(CRAWL_STORAGE_FOLDER);
			
			/*
			 * Politeness – It is really important to crawl politely, and not disrupt the
			 * services provided by the server. By default, crawler4j waits for at least
			 * 200ms between requests, but you might want to increase this duration.
			 */
			config.setPolitenessDelay(POLITENESS_DELAY);
			
			config.setMaxDepthOfCrawling(MAX_DEPTH);
			
			config.setIncludeBinaryContentInCrawling(INCLUDE_BINARY);

			config.setMaxPagesToFetch(MAX_PAGES);

			/*
			 * User Agent – This is part of the politeness policy as well, identifying
			 * yourself to the server you are crawling.
			 */

			config.setUserAgentString(USER_AGENT);
			
			/* Set timeout factors */
			// config.setThreadMonitoringDelaySeconds(1);

			/*
			 * Instantiate the controller for this crawl.
			 */
			PageFetcher pageFetcher = new PageFetcher(config);

			RobotstxtConfig robotstxtConfig = new RobotstxtConfig();

			RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

			CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

			/*
			 * For each crawl, you need to add some seed urls. These are the first URLs that
			 * are fetched and then the crawler starts following links which are found in
			 * these pages
			 */
			controller.addSeed(SEED_URL);

			/*
			 * Start the crawl. This is a blocking operation, meaning that your code will
			 * reach the line after this only when crawling is finished.
			 */
			controller.start(SidrkCrawler.class, NUMBER_OF_CRAWLERS);
			
			System.out.println("Bye!");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}