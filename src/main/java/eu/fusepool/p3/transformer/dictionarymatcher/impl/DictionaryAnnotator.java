package eu.fusepool.p3.transformer.dictionarymatcher.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;
import opennlp.tools.util.Span;
import org.apache.commons.lang.StringUtils;
import org.arabidopsis.ahocorasick.AhoCorasick;

/**
 *
 * @author Gábor Reményi
 */
public class DictionaryAnnotator {
	// contains the search tree

	public final AhoCorasick tree;
	// OpenNLP tokenizer class
	private List<ProcessedText> processedTerms;
	public DictionaryStore dictionary;
	public final DictionaryStore originalDictionary;
	public final DictionaryStore processedDictionary;
	public boolean caseSensitive;
	public int caseSensitiveLength;
	public String stemmingLanguage;
	public ALGORITHM stemmingAlgorithm;
	public Boolean stemming;
	private final Map<String, ALGORITHM> languages;

	/**
	 * Initializes the dictionary annotator by reading the dictionary and building the search tree which is the soul of
	 * the Aho-Corasic algorithm.
	 *
	 * @param _dictionary
	 * @param _stemmingLanguage
	 * @param _caseSensitive
	 * @param _caseSensitiveLength
	 */
	public DictionaryAnnotator(DictionaryStore _dictionary, String _stemmingLanguage, boolean _caseSensitive, int _caseSensitiveLength) {

		dictionary = _dictionary;
		stemmingLanguage = _stemmingLanguage;
		caseSensitive = _caseSensitive;
		caseSensitiveLength = _caseSensitiveLength;

		// loading opennlp tokenizer model
		Tokenizer tokenizer = null;
		try {
			String tokenizeLanguageFilename = "/en-token.bin";
			if(stemmingLanguage.equals("dutch")){
				tokenizeLanguageFilename = "/nl-token.bin";
			}
			InputStream inputStream = this.getClass().getResourceAsStream(tokenizeLanguageFilename);
			TokenizerModel modelTok = new TokenizerModel(inputStream);
			tokenizer = new TokenizerME(modelTok);
		} catch (IOException e) {
			System.err.println("Error while loading tokenizer model: " + e.getMessage());
			throw new RuntimeException(e);
		}

		// if no stemming language configuration is provided set stemming language to None
		if (StringUtils.isBlank(stemmingLanguage)) {
			stemmingLanguage = "none";
		}
		// create a mapping between the language and the name of the class
		// responsible for the stemming of the current language
		languages = new HashMap<>();
		languages.put("none", null);
		languages.put("danish", SnowballStemmer.ALGORITHM.DANISH);
		languages.put("dutch", SnowballStemmer.ALGORITHM.DUTCH);
		languages.put("english", SnowballStemmer.ALGORITHM.ENGLISH);
		languages.put("finnish", SnowballStemmer.ALGORITHM.FINNISH);
		languages.put("french", SnowballStemmer.ALGORITHM.FRENCH);
		languages.put("german", SnowballStemmer.ALGORITHM.GERMAN);
		languages.put("hungarian", SnowballStemmer.ALGORITHM.HUNGARIAN);
		languages.put("italian", SnowballStemmer.ALGORITHM.ITALIAN);
		languages.put("norwegian", SnowballStemmer.ALGORITHM.NORWEGIAN);
		languages.put("english2", SnowballStemmer.ALGORITHM.PORTER);
		languages.put("portuguese", SnowballStemmer.ALGORITHM.PORTUGUESE);
		languages.put("romanian", SnowballStemmer.ALGORITHM.ROMANIAN);
		languages.put("russian", SnowballStemmer.ALGORITHM.RUSSIAN);
		languages.put("spanish", SnowballStemmer.ALGORITHM.SPANISH);
		languages.put("swedish", SnowballStemmer.ALGORITHM.SWEDISH);
		languages.put("turkish", SnowballStemmer.ALGORITHM.TURKISH);

		originalDictionary = new DictionaryStore();
		processedDictionary = new DictionaryStore();

		stemming = false;
		if (stemmingLanguage != null) {
			if (languages.get(stemmingLanguage) != null) {
				stemmingAlgorithm = languages.get(stemmingLanguage);
				stemming = true;
			}
		}

		// read labels from the input dictionary
		String[] terms = readDictionary();

		// tokenize terms in the dictionary
		tokenizeTerms(tokenizer, terms);

		tree = new AhoCorasick();

		// if stemming language was set, perform stemming of terms in the dictionary
		if (stemming) {
			stemTerms();
			// add each term to the seachtree
			for (ProcessedText e : processedTerms) {
				tree.add(e.stemmedText, e.stemmedText);
			}
		} else {
			// add each term to the seachtree
			for (ProcessedText e : processedTerms) {
				tree.add(e.tokenizedText, e.tokenizedText);
			}
		}

		// create search trie
		tree.prepare();
	}

	/**
	 * Creates the dictionary from the HashMap which contains label-URI pairs.
	 *
	 * @param input The original dictionary as a HashMap (label-URI pairs)
	 * @return
	 */
	private String[] readDictionary() {
		String[] labels = new String[dictionary.keywords.size()];
		Set set = dictionary.keywords.entrySet();
		Iterator iterator = set.iterator();

		int index = 0;
		String label;
		Concept concept;
		while (iterator.hasNext()) {
			Map.Entry me = (Map.Entry) iterator.next();
			label = (String) me.getKey();
			concept = (Concept) me.getValue();
			labels[index] = label;
			originalDictionary.addElement(label, concept);
			index++;
		}
		return labels;
	}

	/**
	 * Tokenizes the all the entities in the dictionary and returns the tokenized entities. If caseSensitive is true and
	 * caseSensitiveLength > 0 all tokens whose length is equal or bigger than the caseSensitiveLength are converted to
	 * lowercase. If caseSensitive is true and caseSensitiveLength = 0 no conversion is applied. If caseSensitive is
	 * false all tokens are converted to lowercase.
	 *
	 * @param tokenizer
	 * @param originalTerms
	 */
	public void tokenizeTerms(Tokenizer tokenizer, String[] originalTerms) {
		StringBuilder sb;
		Span[] spans;
		String[] terms = originalTerms;

		processedTerms = new ArrayList<>();
		ProcessedText tokText;
		for (int i = 0; i < originalTerms.length; i++) {
			tokText = new ProcessedText(originalTerms[i]);

			spans = tokenizer.tokenizePos(terms[i]);
			sb = new StringBuilder();

			Token t;
			String word;
			int position = 1;
			int begin, end;
			sb.append(" ");

			for (Span span : spans) {
				word = terms[i].substring(span.getStart(), span.getEnd());

				if (caseSensitive) {
					if (caseSensitiveLength > 0) {
						if (word.length() > caseSensitiveLength) {
							word = word.toLowerCase();
						}
					}
				} else {
					word = word.toLowerCase();
				}

				t = new Token(word);
				t.setOriginalBegin(span.getStart());
				t.setOriginalEnd(span.getEnd());

				begin = position + 1;
				t.setBegin(begin);

				end = begin + word.length();
				t.setEnd(end);

				position = end;

				tokText.addToken(t);

				sb.append(word);
				sb.append(" ");
			}

			tokText.setTokenizedText(sb.toString());
			processedTerms.add(tokText);
		}
	}

	/**
	 * The function is responsible for the stemming of each entity in the dictionary based on the stemming language
	 * defined in the constructor.
	 */
	private void stemTerms() {
		int offset, overallOffset = 0;
		String name, word;
		Concept concept;
		StringBuilder sb;

		Stemmer stemmer = new SnowballStemmer(stemmingAlgorithm);
		for (ProcessedText pt : processedTerms) {
			sb = new StringBuilder();
			sb.append(" ");
			for (Token token : pt.tokens) {
				word = stemmer.stem(token.text).toString();

				offset = token.text.length() - word.length();

				token.begin -= overallOffset;
				overallOffset += offset;
				token.end -= overallOffset;

				sb.append(word);
				sb.append(" ");

				token.stem = word;
			}
			name = sb.toString();
			concept = originalDictionary.getConcept(pt.originalText);
			pt.setStemmedText(name);

			processedDictionary.addElement(name.substring(1, name.length() - 1), concept);
		}
	} 
}
