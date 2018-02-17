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

	private static final boolean DEBUG_MODE = true;

	private static int count = 0;

	// CSV files
	private static Map<String, Integer> fetches = new HashMap<>();
	private static List<String[]> visits = new ArrayList<>();

	/** URLs in-site with repeats */
	private static List<String> urlsDiscoveredOK = new ArrayList<>();
	/** URLS outside with repeats */
	private static List<String> urlsDiscoveredNOK = new ArrayList<>();

	// Fetch Statistics
	private static int fetchesAttempted = 0;
	private static int fetchesSucceeded = 0;
	private static int fetchesFailedOrAborted = 0;

	// Outgoing URLs
	/** Unique URLS */
	private static Map<String, String> urlsOutUnique = new HashMap<>();
	private static int urlsOutTotalCount = 0;

	// Status Codes
	private static Map<String, Integer> sc = new HashMap<>();

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
		urlsDiscoveredOK.add(Controller.SEED_URL);
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

		if (!FILTERS.matcher(href).matches()) {

			// in-site
			if (href.startsWith(Controller.SEED_URL)) {
				urlsDiscoveredOK.add(href);
				return true;
			}

			// outside
			else {
				urlsDiscoveredNOK.add(href);
				return false;
			}

		} else
			return false;
	}

	@Override
	protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {

		String url = String.valueOf(webUrl).replaceAll(",", "_");
		// status_Code.put(statusCode, status_Code.containsKey(statusCode) ?
		// status_Code.get(statusCode) + 1 : 1);

		if (DEBUG_MODE) {
			System.out.printf("\n%5d: %s, Status: %2d", (count++), url, statusCode);
		}

		fetches.put(url, statusCode);

		// Update status code
		updateStatusCodeAndFetches(statusCode, statusDescription);
	}

	/**
	 * This function is called when a page is fetched and ready to be processed by
	 * your program.
	 */
	@Override
	public void visit(Page page) {

		String url = page.getWebURL().getURL().replaceAll(",", "_");

		// Update file size
		int fileSize = page.getContentData().length;
		updateFileSize(fileSize);

		// Update contentType
		String contentType = page.getContentType();
		updateContentType(contentType);

		// Check if fetch succeeded
		int statusCode = page.getStatusCode();
		if (statusCode >= 200 && statusCode < 300) {

			// URL, Size, Outlinks, contentType
			String visitParams[] = new String[4];

			visitParams[0] = url;

			visitParams[1] = fileSize + "";

			visitParams[2] = "0";
			if (page.getParseData() instanceof HtmlParseData) {
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();

				urlsOutTotalCount += links.size();

				for (WebURL w : links) {
					String outUrl = w.getURL();

					if (outUrl.startsWith(Controller.SEED_URL)) {
						if (!urlsOutUnique.containsKey(url)) {
							urlsOutUnique.put(url, "OK");
						}
					}

					else {
						if (!urlsOutUnique.containsKey(url)) {
							urlsOutUnique.put(url, "NOK");
						}
					}
				}

				visitParams[2] = links.size() + "";
			}

			visitParams[3] = contentType;

			if (DEBUG_MODE)
				System.out.printf(" (%sb, %s outgoing links, %s)", visitParams[1],
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

		if (fileSize < 1024)
			fs1k++;
		else if (fileSize < 10 * 1024)
			fs10k++;
		else if (fileSize < 100 * 1024)
			fs100k++;
		else if (fileSize < 1 * 1024 * 1024)
			fs1m++;
		else
			fsg1m++;

	}

	private void updateStatusCodeAndFetches(int statusCode, String description) {

		fetchesAttempted++;

		String status = statusCode + " " + description;

		if (sc.containsKey(status))
			sc.put(status, sc.get(status) + 1);
		else
			sc.put(status, 1);

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
			CSVUtils csvFetch = new CSVUtils("fetch_NBC_News.csv", false);
			for (Entry<String, Integer> e : SidrkCrawler.fetches.entrySet()) {
				csvFetch.printAsCSV(e.getKey(), (e.getValue() + ""));
			}
			csvFetch.closeOutputStream();

			CSVUtils csvVisit = new CSVUtils("visit_NBC_News.csv", false);
			for (String[] visitParams : visits) {
				csvVisit.printAsCSV(visitParams[0], visitParams[1], visitParams[2], visitParams[3]);
			}
			csvVisit.closeOutputStream();

			CSVUtils csvUrls = new CSVUtils("urls_NBC_News.csv", false);
			for (String u : urlsDiscoveredOK) {
				csvUrls.printAsCSV(u, "OK");
			}
			for (String u : urlsDiscoveredNOK) {
				csvUrls.printAsCSV(u, "N_OK");
			}
			csvUrls.closeOutputStream();

			// TODO Crawl Report
			CSVUtils report = new CSVUtils("CrawlReport_NBC_News.txt", false);
			report.println("Name: Siddhesh Rajiv Karekar");
			report.println("USC ID: " + 1234567890); // TODO change USC ID!
			report.println("News site crawled: " + Controller.SEED_URL);

			report.println("\nFetch Statistics\n================");
			report.println("# fetches attempted: " + fetchesAttempted);
			report.println("# fetches succeeded: " + fetchesSucceeded);
			report.println("# fetches failed or aborted: " + fetchesFailedOrAborted);

			report.println("\nOutgoing URLs:\n==============");
			report.println("Total URLs extracted: " + urlsOutTotalCount);

			int countUrlsOutUnique = urlsOutUnique.size();
			report.println("# unique URLs extracted: " + countUrlsOutUnique);

			int countUrlsOutUniqueWithin = 0, countUrlsOutUniqueOutside = 0;
			for (Entry<String, String> e : urlsOutUnique.entrySet()) {
				if (e.getValue().equals("OK"))
					countUrlsOutUniqueWithin++;
				else
					countUrlsOutUniqueOutside++;
			}
			report.println("# unique URLs within News Site: " + countUrlsOutUniqueWithin);
			report.println("# unique URLs outside News Site: " + countUrlsOutUniqueOutside);

			report.println("\nStatus Codes:\n=============");
			for (Entry<String, Integer> e : sc.entrySet()) {
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