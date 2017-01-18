package eu.fusepool.p3.transformer.dictionarymatcher.impl;

import java.io.InputStream;
import java.util.Iterator;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SKOS04;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;

/**
 *
 * @author Gabor
 */
public class Reader {

    public static DictionaryStore readDictionary(InputStream inputStream, String contentType) {
        Triple triple;
        Resource resource;
        String label, lang;

        DictionaryStore dictionary = new DictionaryStore();
        MGraph graph = new IndexedMGraph();
        Parser.getInstance().parse(graph, inputStream, contentType);
        Iterator<Triple> typeTriples = graph.filter(null, RDF.type, SKOS04.Concept);
        while (typeTriples.hasNext()) {
            NonLiteral s = typeTriples.next().getSubject();
            if (s instanceof UriRef) {
                UriRef concept = (UriRef) s;

                // getting prefLabels
                Iterator<Triple> prefTriples = graph.filter(concept, SKOS04.prefLabel, null);
                while (prefTriples.hasNext()) {
                    triple = prefTriples.next();
                    resource = triple.getObject();
                    if (resource instanceof PlainLiteral) {
                        label = ((PlainLiteral) resource).getLexicalForm();
                        lang = ((PlainLiteral) resource).getLanguage() == null ? null : ((PlainLiteral) resource).getLanguage().toString();
                    } else if (resource instanceof TypedLiteral && ((TypedLiteral) resource).getDataType().equals(XSD.string)) {
                        label = ((Literal) resource).getLexicalForm();
                        lang = null;
                    } else {
                        label = null;
                        lang = null;
                    }

                    if (StringUtils.isNotBlank(label) && StringUtils.isNotBlank(concept.getUnicodeString())) {
                        dictionary.addOriginalElement(label, SKOS04.prefLabel, concept.getUnicodeString());
                    }
                }

                // getting altLabels
                Iterator<Triple> altTriples = graph.filter(concept, SKOS04.altLabel, null);
                while (altTriples.hasNext()) {
                    triple = altTriples.next();
                    resource = triple.getObject();
                    if (resource instanceof PlainLiteral) {
                        label = ((PlainLiteral) resource).getLexicalForm();
                        lang = ((PlainLiteral) resource).getLanguage() == null ? null : ((PlainLiteral) resource).getLanguage().toString();
                    } else if (resource instanceof TypedLiteral && ((TypedLiteral) resource).getDataType().equals(XSD.string)) {
                        label = ((Literal) resource).getLexicalForm();
                        lang = null;
                    } else {
                        label = null;
                        lang = null;
                    }

                    if (StringUtils.isNotBlank(label) && StringUtils.isNotBlank(concept.getUnicodeString())) {
                        dictionary.addOriginalElement(label, SKOS04.altLabel, concept.getUnicodeString());
                    }
                }

            }
        }
        return dictionary;
    }
}
