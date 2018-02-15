package deimos.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * For the CSV outputs that we use in our paper.
 * 
 * @author Siddhesh Karekar
 */
public class CSVUtils {

	/** Required for opening the file. */
	private FileWriter fileWriter;
	private PrintWriter writer;

	/**
	 * Must include trailing slash!
	 */
	private static final String DIR_STATS = "data/crawl/";

	/**
	 * Specify whether it should be appended to (true) or overwritten (false). Not
	 * used afterwards
	 */
	private boolean appendMode;

	/**
	 * Create/use a file in the DIR_STATS, usually a CSV file.
	 * 
	 * @param filename
	 *            The name (include the extension!)
	 * @param appendMode
	 *            Specify whether it should be appended to (true) or overwritten
	 *            (false). Default append mode is true
	 * @throws FileNotFoundException
	 *             Usually if the output file can't be opened.
	 */
	public CSVUtils(String filename, boolean appendMode) throws FileNotFoundException, IOException {
		this.appendMode = appendMode;

		String fullPath = DIR_STATS + filename;

		fileWriter = new FileWriter(new File(fullPath), this.appendMode);
		writer = new PrintWriter(fileWriter);

	}

	/**
	 * Create/use a file in the DIR_STATS, usually a CSV file, opening it in append
	 * mode.
	 * 
	 * @param filename
	 *            The name (include the extension!)
	 * @throws FileNotFoundException
	 *             Usually if the output file can't be opened.
	 */
	public CSVUtils(String filename) throws FileNotFoundException, IOException {
		this(filename, true);
	}

	/**
	 * Closes all required resources after output, and allows the output to appear
	 * in the file. MUST BE EXPLICITLY CALLED! Can't depend upon garbage collection.
	 */
	public void closeOutputStream() {
		try {
			if (fileWriter != null)
				fileWriter.close();
			if (writer != null) {
				writer.flush();
				writer.close();
			}
			// System.out.println("Closed!");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/** Write a new line to it. */
	public void println(String newLine) {
		writer.println(newLine);
	}

	/**
	 * Returns a CSV row string containing each of the input parameter strings. Uses
	 * toCSVSafeString to escape CSV-unsafe chars.
	 * 
	 * @param strings
	 *            an array of Strings e.g. Appl,e, Ball, "Poop"
	 * @return "Appl_e","Ball","_Poop_"
	 */
	public static String toCSV(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			String current = toCSVSafeString(strings[i]);
			sb.append("\"" + current + "\"");
			if (i != strings.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * Replaces CSV-incompatible characters, such as double quotes and commas, by
	 * underscores.
	 * 
	 * @param input
	 * @return
	 */
	public static String toCSVSafeString(String input) {
		return input.replace(",", "_").replace("\"", "_");
	}

	/**
	 * Prints a CSV row string containing each of the input parameter strings.
	 * Escapes double quotes with underscores.
	 * 
	 * @param strings
	 *            an array of Strings e.g. (Apple, Ball, "Poop") is saved in the CSV
	 *            as "Apple", "Ball", "_Poop_"
	 */
	public void printAsCSV(String... strings) {
		String lineCSV = toCSV(strings);
		writer.println(lineCSV);

		// System.out.println(lineCSV);
	}

	/*public static void main(String args[]) {

		CSVUtils csv;
		try {
			csv = new CSVUtils("test.csv", false);
			csv.printAsCSV("Hello", "World");
			csv.closeOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}*/

}
