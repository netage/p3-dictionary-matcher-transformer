package eu.fusepool.p3.transformer.dictionarymatcher.tests;

import eu.fusepool.p3.transformer.TransformerException;
import eu.fusepool.p3.transformer.dictionarymatcher.cache.Cache;
import eu.fusepool.p3.transformer.dictionarymatcher.impl.Annotation;
import eu.fusepool.p3.transformer.dictionarymatcher.impl.DictionaryAnnotator;
import eu.fusepool.p3.transformer.dictionarymatcher.impl.DictionaryStore;
import eu.fusepool.p3.transformer.dictionarymatcher.impl.Extractor;
import eu.fusepool.p3.transformer.dictionarymatcher.impl.Reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for Dictionary Matcher Transformer
 */
public class TransformerTest {

	final private String taxonomy = "NASA.subjects.skos.rdf";
	final private String documentUri = "http://nasa.test.com/article";


	final private String testText = "The International Space Station (ISS) combines NASA's Space Station Freedom "
			+ "project with the Soviet/Russian Mir-2 station, the European Columbus station, and the Japanese KibĹŤ "
			+ "laboratory module. NASA originally planned in the 1980s to develop Freedom alone, but US budget "
			+ "constraints led to the merger of these projects into a single multi-national program in 1993, "
			+ "managed by NASA, the Russian Federal Space Agency (RKA), the Japan Aerospace Exploration Agency (JAXA), "
			+ "the European Space Agency (ESA), and the Canadian Space Agency (CSA). The station consists of pressurized "
			+ "modules, external trusses, solar arrays and other components, which have been launched by Russian Proton "
			+ "and Soyuz rockets, and the US Space Shuttles. It is currently being assembled in Low Earth Orbit. The on-orbit "
			+ "assembly began in 1998, the completion of the US Orbital Segment occurred in 2011 and the completion of the "
			+ "Russian Orbital Segment is expected by 2016. The ownership and use of the space station is established "
			+ "in intergovernmental treaties and agreements which divide the station into two areas and allow Russia to "
			+ "retain full ownership of the Russian Orbital Segment (with the exception of Zarya), with the US Orbital Segment "
			+ "allocated between the other international partners.";

	@Test
	public void loadDictionary() throws FileNotFoundException {
		Object globalLock = new Object();
		Model tempModel = ModelFactory.createDefaultModel();

		boolean caseSensitivity = false;
		DictionaryStore dictionaryStore = null;
		String stemmingLanguage = "english";

		Object lock;
		long start, end;
		String cache = "";

		synchronized (globalLock) {
			lock = Cache.register(taxonomy);
		}

		synchronized (lock) {
			start = System.currentTimeMillis();

			File initialFile = new File("src/test/resources/"+this.taxonomy);
			InputStream inputStream = new FileInputStream(initialFile);
			
			Model m = ModelFactory.createDefaultModel();
			m.read(inputStream, "RDF/XML");
			dictionaryStore = Reader.readDictionary(m);					
			Assert.assertTrue(dictionaryStore.getSize() == 3046);
		}
	}

	@Test
	public void getAnnotations() throws IOException {
		Object globalLock = new Object();
		DictionaryAnnotator dictionaryAnnotator = null;
		Model tempModel = ModelFactory.createDefaultModel();

		boolean caseSensitivity = false;
		DictionaryStore dictionaryStore = null;
		String stemmingLanguage = "english";

		Object lock;
		long start, end;
		String cache = "";

		synchronized (globalLock) {
			lock = Cache.register(taxonomy);
		}

		synchronized (lock) {
			start = System.currentTimeMillis();

			if (!Cache.containsTaxonomy(taxonomy)) {
				File initialFile = new File("src/test/resources/"+this.taxonomy);
				InputStream inputStream = new FileInputStream(initialFile);
				
				Model m = ModelFactory.createDefaultModel();
				m.read(inputStream, "RDF/XML");
				dictionaryStore = Reader.readDictionary(m);	

				// process taxonomy
				dictionaryAnnotator = new DictionaryAnnotator(dictionaryStore, stemmingLanguage, caseSensitivity, 0);

				// add it to cache
				Cache.setTaxonomy(taxonomy, dictionaryAnnotator);
			} else {
				cache = "(CACHED) ";
				// get it from cache
				dictionaryAnnotator = Cache.getTaxonomy(taxonomy);
			}
		}

		String data = this.testText;
		String documentURI = this.documentUri;

		if (StringUtils.isNotBlank(data)) {
			int i = 1;
			// create extractor instance
			Extractor extractor = new Extractor(dictionaryAnnotator);
			// create output from annotations
			for (Annotation e : extractor.getEntities(data)) {
				// create selector URI
				String selector = documentURI + "#char=" + e.getBegin() + "," + e.getEnd();
				// create annotation-body URI
				String annotationBody = documentURI + "#annotation-body" + i;
				// create annotation URI
				String annotation = documentURI + "#annotation" + i;
				// create sp-resource URI
				String spResource = documentURI + "#sp-resource" + i;

				// Linked Entity Annotation (body)
				tempModel.createResource(annotationBody)
				.addProperty(RDF.type, tempModel.createResource("http://vocab.fusepool.info/fam#LinkedEntity"));

				if (e.getAltLabel() != null) {
					tempModel.createResource(annotationBody).addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#label-type"), tempModel.createLiteral("altLabel"));
					tempModel.createResource(annotationBody).addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#entity_label"), tempModel.createLiteral(e.getAltLabel()));
				} else {
					tempModel.createResource(annotationBody).addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#label_type"), tempModel.createLiteral("prefLabel"));
					tempModel.createResource(annotationBody).addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#entity_label"), tempModel.createLiteral(e.getPrefLabel()));
				}

				tempModel.createResource(annotationBody)
				.addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#entity_reference"), tempModel.createResource(e.getUri()))
				.addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#entity_mention"), tempModel.createLiteral(e.getLabel()))
				.addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#extracted_from"), tempModel.createResource(documentURI))
				.addProperty(tempModel.createProperty("http://vocab.fusepool.info/fam#selector"), tempModel.createResource(selector));

				// oa:Annotation
				tempModel.createResource(annotation)
				.addProperty(RDF.type, tempModel.createResource("http://www.w3.org/ns/oa#Annotation"))
				.addProperty(tempModel.createProperty("http://www.w3.org/ns/oa#hasBody"), tempModel.createResource(annotationBody))
				.addProperty(tempModel.createProperty("http://www.w3.org/ns/oa#hasTarget"), tempModel.createResource(spResource))
				.addProperty(tempModel.createProperty("http://www.w3.org/ns/oa#annotatedBy"), tempModel.createResource("p3-dictionary-matcher-transformer"))
				.addProperty(tempModel.createProperty("http://www.w3.org/ns/oa#annotatedAt"), tempModel.createLiteral(e.getTimestamp()));

				// oa:SpecificResource
				tempModel.createResource(spResource)
				.addProperty(RDF.type, tempModel.createResource("http://www.w3.org/ns/oa#SpecificResource"))
				.addProperty(tempModel.createProperty("http://www.w3.org/ns/oa#hasSource"), tempModel.createResource(annotationBody))
				.addProperty(tempModel.createProperty("http://www.w3.org/ns/oa#hasSelector"), tempModel.createResource(selector));

				// NIF selector
				tempModel.createResource(selector)
				.addProperty(RDF.type, tempModel.createResource("http://vocab.fusepool.info/fam#NifSelector"))
				.addProperty(RDF.type, tempModel.createResource("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#String"))
				.addProperty(tempModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#beginIndex"), tempModel.createTypedLiteral(e.getBegin()))
				.addProperty(tempModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex"), tempModel.createTypedLiteral(e.getEnd())); 

				i++;
			}
		} else {
			throw new TransformerException(HttpServletResponse.SC_BAD_REQUEST, "ERROR: Input text was not provided!");
		}
		Assert.assertTrue(tempModel.size() == 133);
	}


}
