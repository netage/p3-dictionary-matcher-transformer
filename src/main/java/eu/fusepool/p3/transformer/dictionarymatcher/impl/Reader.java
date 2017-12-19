package eu.fusepool.p3.transformer.dictionarymatcher.impl;

import java.io.InputStream;
import java.util.Iterator;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.*;

/**
 *
 * @author Gabor
 */
public class Reader {

	public static DictionaryStore readDictionary(InputStream inputStream, String contentType) {
		Triple triple;
		String label, lang;

		DictionaryStore dictionary = new DictionaryStore();
		Model model = ModelFactory.createDefaultModel();
		model.read(inputStream, null);

		ResIterator typeTriples = model.listSubjectsWithProperty(RDF.type, SKOS.Concept);
		while (typeTriples.hasNext()) {
			RDFNode s = typeTriples.next();
			// getting prefLabels
			Iterator<RDFNode> prefTriples = model.listObjectsOfProperty(s.asResource(), SKOS.prefLabel);
			while (prefTriples.hasNext()) {
				RDFNode object = prefTriples.next();
				if (object.isLiteral()) {
					label = object.asLiteral().getLexicalForm();
					lang = object.asLiteral().getLanguage() == null ? null : object.asLiteral().getLanguage().toString();
				} else if (object.asLiteral().getDatatypeURI().equals(XSD.xstring)) {
					label = object.asLiteral().getLexicalForm();
					lang = null;
				} else {
					label = null;
					lang = null;
				}

				if (StringUtils.isNotBlank(label) && StringUtils.isNotBlank(object.asLiteral().getString())) {
					dictionary.addOriginalElement(label, SKOS.prefLabel, object.asLiteral().getString());
				}
			}

			// getting altLabels
			Iterator<RDFNode> altTriples = model.listObjectsOfProperty(s.asResource(), SKOS.altLabel);
			while (altTriples.hasNext()) {
				RDFNode object = altTriples.next();
				if (object.isLiteral()) {
					label = object.asLiteral().getLexicalForm();
					lang = object.asLiteral().getLanguage() == null ? null : object.asLiteral().getLanguage().toString();
				} else if (object.asLiteral().getDatatypeURI().equals(XSD.xstring)) {
					label = object.asLiteral().getLexicalForm();
					lang = null;
				} else {
					label = null;
					lang = null;
				}

				if (StringUtils.isNotBlank(label) && StringUtils.isNotBlank(object.asLiteral().getString())) {
					dictionary.addOriginalElement(label, SKOS.altLabel, object.asLiteral().getString());
				}
			}
		}
		return dictionary;
	}
}
