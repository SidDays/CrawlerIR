package ir.crawler;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import deimos.common.CSVUtils;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class SidrkCrawler extends WebCrawler {

	// CSV files
	private static Map<String, Integer> fetches = new HashMap<>();
	private static List<String[]> visits = new ArrayList<>();
	private static List<String> urlsOK = new ArrayList<>();
	private static List<String> urlsNOK = new ArrayList<>();

	// Fetch Statistics
	private static int fetchesAttempted = 0;
	private static int fetchesSucceeded = 0;
	private static int fetchesFailedOrAborted = 0;

	// Outgoing URLs
	private static int urlsTotal = 0;
	private static int urlsUniqueExtracted = 0;
	private static int urlsUniqueWithin = 0;
	private static int urlsUniqueOutside = 0;

	// Status Codes
	private static Map<Integer, Integer> sc = new HashMap<>();

	// File Sizes
	private static int fs1k = 0; // < 1KB:
	private static int fs10k = 0; // 1KB ~ <10KB:
	private static int fs100k = 0; // 10KB ~ <100KB:
	private static int fs1m = 0; // 100KB ~ <1MB:
	private static int fsg1m = 0; // >= 1MB:

	// Content Types
	private static Map<String, Integer> ct = new HashMap<>();

	private final static Pattern FILTERS = Pattern
			.compile(".*(\\.(" + /* "gif|jpg|png|"+ */ "css|js|xml|rss|json|mp3|zip|gz))$");

	@Override
	public void onStart() {
		urlsOK.add(Controller.SEED_URL);
	}

	/**
	 * This method receives two parameters. The first parameter is the page in which
	 * we have discovered this new url and the second parameter is the new url. You
	 * should implement this function to specify whether the given url should be
	 * crawled or not (based on your crawling logic).<br>
	 * <br>
	 * In this case, we didn't need the referringPage parameter to make the
	 * decision.
	 */
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith(Controller.SEED_URL);
	}

	/**
	 * This function is called when a page is fetched and ready to be processed by
	 * your program.
	 */
	@Override
	public void visit(Page page) {

		String url = page.getWebURL().getURL();
		int statusCode = page.getStatusCode();
		System.out.println("\nURL: " + url + ", Status: " + statusCode);

		fetches.put(url, statusCode);

		// Update file size
		int fileSize = page.getContentData().length;
		updateFileSize(fileSize);

		// Update status code
		updateStatusCodeAndFetches(statusCode);

		// Update contentType
		String contentType = page.getContentType();
		updateContentType(contentType);

		// Check if fetch succeeded
		if (statusCode >= 200 && statusCode < 300) {

			// URL, Size, Outlinks, contentType
			String visitParams[] = new String[4];

			visitParams[0] = url;

			visitParams[1] = fileSize + "";

			visitParams[2] = "0";
			if (page.getParseData() instanceof HtmlParseData) {
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();

				for (WebURL w : links) {
					String outUrl = w.getURL();

					if (outUrl.startsWith(Controller.SEED_URL))
						urlsOK.add(url);
					else
						urlsNOK.add(url);
				}

				visitParams[2] = links.size() + "";
			}

			visitParams[3] = contentType;

			System.out.printf("Size: %s bytes, # of outgoing links: %s, Content-Type - %s\n", visitParams[1],
					visitParams[2], visitParams[3]);

			// Add it to the list
			visits.add(visitParams);
		}

	}

	private void updateContentType(String contentType) {

		if (contentType.startsWith("text/html"))
			contentType = "text/html";

		if (ct.containsKey(contentType))
			ct.put(contentType, ct.get(contentType) + 1);
		else
			ct.put(contentType, 1);
	}

	private void updateFileSize(int fileSize) {

		if (fileSize < 1000)
			fs1k++;
		else if (fileSize < 10 * 1000)
			fs10k++;
		else if (fileSize < 100 * 1000)
			fs100k++;
		else if (fileSize < 1 * 1000 * 1000)
			fs1m++;
		else
			fsg1m++;

	}

	private void updateStatusCodeAndFetches(int statusCode) {

		fetchesAttempted++;

		if (sc.containsKey(statusCode))
			sc.put(statusCode, sc.get(statusCode) + 1);
		else
			sc.put(statusCode, 1);

		if (statusCode >= 200 && statusCode < 300) {

			fetchesSucceeded++;

		} else {

			fetchesFailedOrAborted++;
		}
	}

	@Override
	public void onBeforeExit() {

		try {
			// Print the contents of every collection to the CSV file(s).
			CSVUtils csvFetch = new CSVUtils("fetch_NBCNews.csv", false);
			for (Entry<String, Integer> e : SidrkCrawler.fetches.entrySet()) {
				csvFetch.printAsCSV(e.getKey(), (e.getValue() + ""));
			}
			csvFetch.closeOutputStream();

			CSVUtils csvVisit = new CSVUtils("visit_NBCNews.csv", false);
			for (String[] visitParams : visits) {
				csvVisit.printAsCSV(visitParams[0], visitParams[1], visitParams[2], visitParams[3]);
			}
			csvVisit.closeOutputStream();

			CSVUtils csvUrls = new CSVUtils("urls_NBCNews.csv", false);
			for (String u : urlsOK) {
				csvUrls.printAsCSV(u, "OK");
			}
			for (String u : urlsNOK) {
				csvUrls.printAsCSV(u, "N_OK");
			}
			csvUrls.closeOutputStream();

			// TODO Crawl Report
			CSVUtils report = new CSVUtils("CrawlReport_NBCNews.txt", false);
			report.println("Name: Siddhesh Karekar");
			report.println("USC ID: " + 1234567890); // TODO change USC ID!
			report.println("News site crawled: " + Controller.SEED_URL);

			report.println("\nFetch Statistics\n================");
			report.println("# fetches attempted: " + fetchesAttempted);
			report.println("# fetches succeeded: " + fetchesSucceeded);
			report.println("# fetches failed or aborted: " + fetchesFailedOrAborted);

			report.println("\nOutgoing URLs:\n==============");
			report.println("Total URLs extracted: " + urlsTotal);
			report.println("# unique URLs extracted: " + urlsUniqueExtracted);
			report.println("# unique URLs within News Site: " + urlsUniqueWithin);
			report.println("# unique URLs outside News Site: " + urlsUniqueOutside);

			report.println("\nStatus Codes:\n=============");
			for (Entry<Integer, Integer> e : sc.entrySet()) {
				report.println(e.getKey() + ": " + e.getValue());
			}
			// report.println("200 OK: " + sc200);
			// report.println("301 Moved Permanently: " + sc301);
			// report.println("401 Unauthorized: " + sc401);
			// report.println("403 Forbidden: " + sc403);
			// report.println("404 Not Found: " + sc404);

			report.println("\nFile Sizes:\n===========");
			report.println("< 1KB: " + fs1k);
			report.println("1KB ~ <10KB: " + fs10k);
			report.println("10KB ~ <100KB: " + fs100k);
			report.println("100KB ~ <1MB: " + fs1m);
			report.println(">= 1MB: " + fsg1m);

			report.println("\nContent Types:\n==============");
			// report.println("text/html: " + ctHtml);
			// report.println("image/gif: " + ctGif);
			// report.println("image/jpeg: " + ctJpeg);
			// report.println("image/png: " + ctPng);
			// report.println("application/pdf: " + ctPdf);
			for (Entry<String, Integer> e : ct.entrySet()) {
				report.println(e.getKey() + ": " + e.getValue());
			}

			report.closeOutputStream();

		} catch (IOException e1) {

			e1.printStackTrace();
		}

	}
}