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
	
	// Statistics
	// private static Map<String, Integer> contentTypes = new HashMap<>();

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
		
		// Check if fetch succeeded		
		if(statusCode >= 200 && statusCode < 300) {
			
			// URL, Size, Outlinks, contentType
			String visitParams[] = new String[4];
			
			visitParams[0] = url;
			
			visitParams[1] = page.getContentData().length + "";
			
			visitParams[2] = "0";
			if (page.getParseData() instanceof HtmlParseData) {
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();
				
				for(WebURL w : links) {
					String outUrl = w.getURL();
					
					if(outUrl.startsWith(Controller.SEED_URL))
						urlsOK.add(url);
					else
						urlsNOK.add(url);
				}
				
				visitParams[2] = links.size() + "";
			}
			
			visitParams[3] = page.getContentType().substring(0, page.getContentType().indexOf(";"));
			
			System.out.printf("Size: %s bytes, # of outgoing links: %s, Content-Type - %s\n",visitParams[1], visitParams[2], visitParams[3]);
			
			// Add it to the list
			visits.add(visitParams);
		}

		
	}

	@Override
	public void onBeforeExit() {

		try {
			// TODO Print the contents of every collection to the CSV file(s).
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
			for(String u : urlsOK) {
				csvUrls.printAsCSV(u, "OK");
			}
			for(String u : urlsNOK) {
				csvUrls.printAsCSV(u, "N_OK");
			}
			
			csvUrls.closeOutputStream();
			
		} catch (IOException e1) {
			
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
}