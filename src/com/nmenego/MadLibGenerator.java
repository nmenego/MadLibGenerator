package com.nmenego;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * MadLib Generator
 * 
 * Create a story by replacing the predefined word tokens in a story file and
 * replace them with words in a JSON file.
 * 
 * @version 1.0
 * @author nicomartin.enego 2015/07/29
 *
 */
public class MadLibGenerator {

	// class fields
	private final String jsonPath; // path to JSON file
	private final String storyPath; // path to story text file
	private final String outPath; // path to output text
	private final Map<String, List<String>> words;

	// error codes
	private static final int ERR_PARAM_COUNT = 1;
	private static final int ERR_FILE_NOT_FOUND = 2;
	private static final int ERR_JSON_PARSE = 3;
	private static final int ERR_OTHER = 10;

	// JSON constants
	private static final String JSON_WORD = "word";
	private static final String JSON_TYPE = "type";

	// Regex for tokens inside the story file
	private static final String TOKEN_REGEX = "\\[(.*?)\\]";

	// For when no words are found matching the type given
	private static final String NO_WORD_FOUND = "XXX";

	/**
	 * Program invoker.
	 * 
	 * @param args
	 *            - command line arguments
	 */
	public static void main(String[] args) {
		// check inputs.
		if (args == null || args.length != 3) {
			System.err.println("Invalid parameter count.");
			System.exit(ERR_PARAM_COUNT);

		} else {
			MadLibGenerator gen = new MadLibGenerator(args[0], args[1], args[2]);
			gen.generate();
		}
	}

	/**
	 * Constructor
	 * 
	 * @param jsonPath
	 *            - path to JSON file
	 * @param storyPath
	 *            - path to story file
	 * @param outPath
	 *            - path to output file
	 */
	public MadLibGenerator(String jsonPath, String storyPath, String outPath) {
		this.jsonPath = jsonPath;
		this.storyPath = storyPath;
		this.outPath = outPath;
		this.words = new HashMap<String, List<String>>();
	}

	/**
	 * Main method to perform generation of madlib.
	 */
	public void generate() {

		try {
			// parse JSON using Jackson
			parseJson();
			// create the story!
			createStory();
		} catch (FileNotFoundException e) {
			System.err.format("File not found %s!", jsonPath);
			System.exit(ERR_FILE_NOT_FOUND);
		} catch (JsonParseException e) {
			System.err.format("Unable to parse JSON: %s", jsonPath);
			System.err.format("Reason: %s", e.getMessage());
			System.exit(ERR_JSON_PARSE);
		} catch (Exception e) {
			System.err.println("Generic error: " + e.getMessage());
			System.exit(ERR_OTHER);
		}

		System.out.format("Output file: %s", outPath);
	}

	/**
	 * Plot random words from the dictionary to corresponding types written
	 * inside [] in the story.
	 * 
	 * @throws IOException
	 */
	private void createStory() throws IOException {
		// Buffered reader/writer to handle big-file cases
		BufferedReader reader = new BufferedReader(new FileReader(storyPath));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outPath));
		// Create pattern to match.
		Pattern p = Pattern.compile(TOKEN_REGEX);

		String type, word;
		String line = reader.readLine();
		while (line != null) {
			StringBuffer sb = new StringBuffer();
			Matcher m = p.matcher(line);

			while (m.find()) {
				// token = m.group(0); [number]
				type = m.group(1);
				word = getRandomWordByType(type);
				m.appendReplacement(sb, word);
			}
			// copy end of input sequence
			m.appendTail(sb);

			// write new string.
			writer.write(sb.toString());
			writer.newLine();

			// read next line.
			line = reader.readLine();
		}

		reader.close();
		writer.close();

	}

	/**
	 * Provides a random word for the given type from the dictionary.
	 * 
	 * @param type
	 *            - type to search
	 * @return random word for given type
	 */
	private String getRandomWordByType(String type) {
		List<String> wordList = words.get(type);
		String randomWord;

		if (wordList == null) {
			randomWord = NO_WORD_FOUND;
		} else {
			Random rand = new Random();
			int randomIndex = rand.nextInt(wordList.size());
			randomWord = wordList.get(randomIndex);
		}

		return randomWord;
	}

	/**
	 * Parse JSON file then store it in local dictionary.
	 * 
	 * @throws IOException
	 */
	private void parseJson() throws IOException {
		FileReader file = new FileReader(jsonPath);
		JsonFactory jfactory = new JsonFactory();
		JsonParser parser = jfactory.createParser(file);
		String word, type;

		// we assume that JSON is an array of words.
		JsonToken currentToken = parser.nextToken();
		while (currentToken != null
				&& !JsonToken.END_ARRAY.equals(currentToken)) {

			if (JSON_WORD.equals(parser.getCurrentName())) {
				// get word value
				parser.nextToken();
				word = parser.getText();

				// process type
				parser.nextToken();
				if (JSON_TYPE.equals(parser.getCurrentName())) {
					// get type value
					parser.nextToken();
					type = parser.getText();

					// get words belonging to same type
					List<String> wordList = words.get(type);
					if (wordList == null) {
						wordList = new ArrayList<String>();
					}

					wordList.add(word);
					// store values in our dictionary
					// System.out.println(type + " >> " + word);
					words.put(type, wordList);

				} else {
					// every word should have a type!!!
					throw new JsonParseException(
							"No matching type for <word>.",
							parser.getCurrentLocation());
				}

			}

			// get next token
			currentToken = parser.nextToken();
		}
	}

	/**
	 * @return the jsonPath
	 */
	public String getJsonPath() {
		return jsonPath;
	}

	/**
	 * @return the storyPath
	 */
	public String getStoryPath() {
		return storyPath;
	}

	/**
	 * @return the outPath
	 */
	public String getOutPath() {
		return outPath;
	}

	/**
	 * @return the words
	 */
	public Map<String, List<String>> getWords() {
		return words;
	}
}
