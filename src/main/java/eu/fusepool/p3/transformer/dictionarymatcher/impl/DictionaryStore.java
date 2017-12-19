package eu.fusepool.p3.transformer.dictionarymatcher.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

/**
 * This class represents the dictionary using a HashMap to store the entity - URI pairs for fast retrieval.
 *
 * @author Gábor Reményi
 */
public class DictionaryStore {

    // HashMap representation of the keywords and matching URIs
    Map<String, Concept> keywords;
    Map<String, String> prefLabels;

    /**
     * Simple constructor.
     */
    public DictionaryStore() {
        keywords = new HashMap<>();
        prefLabels = new HashMap<>();
    }

    /**
     * Add new element to the dictionary, label is transformed to lower case.
     *
     * @param label
     * @param concept
     */
    public void addElement(String label, Concept concept) {
        //concept.labelText = concept.labelText.toLowerCase();
        keywords.put(label.toLowerCase(), concept);
    }

    /**
     * Add new element to the dictionary without any change.
     *
     * @param labelText
     * @param preflabel
     * @param uri
     */
    public void addOriginalElement(String labelText, Property preflabel, String uri) {
        Concept concept;

        if (keywords.containsKey(labelText)) {
            concept = keywords.get(labelText);
        } else {
            concept = new Concept(labelText, preflabel, uri);
        }

        keywords.put(labelText, concept);

        if (concept.isPrefLabel()) {
            prefLabels.put(uri, labelText);
        }
    }

    /**
     * Add new element to the dictionary.
     *
     * @param labelText
     * @param labelType
     * @param type
     * @param uri
     */
    public void addOriginalElement(String labelText, RDFNode labelType, String uri, String type) {
        Concept concept;

        if (keywords.containsKey(labelText)) {
            concept = keywords.get(labelText);
        } else {
            concept = new Concept(labelText, labelType, uri, type);
        }

        keywords.put(labelText, concept);

        if (concept.isPrefLabel()) {
            prefLabels.put(uri, labelText);
        }
    }

    /**
     * Get the URI of the matching entity, label is transformed to lower case.
     *
     * @param label
     * @return
     */
    public String getURI(String label) {
        return keywords.get(label.toLowerCase()).uri;
    }

    /**
     * Get the URI of the matching entity.
     *
     * @param label
     * @return
     */
    public String getOriginalURI(String label) {
        return keywords.get(label.toLowerCase()).uri;
    }

    /**
     * Get the type of the matching entity.
     *
     * @param label
     * @return
     */
    public String getType(String label) {
        return keywords.get(label).type;
    }

    /**
     * Get concept object
     *
     * @param label
     * @return
     */
    public Concept getConcept(String label) {
        return keywords.get(label.toLowerCase());
    }

    /**
     * Get prefLabel by URI
     *
     * @param uri
     * @return
     */
    public String getPrefLabel(String uri) {
        return prefLabels.get(uri);
    }

    public int getSize() {
        return keywords.size();
    }

    @Override
    public String toString() {
        String result = "";

        Set set = keywords.entrySet();
        Iterator iterator = set.iterator();

        int index = 0;
        String label;
        Concept concept;
        while (iterator.hasNext()) {
            Map.Entry me = (Map.Entry) iterator.next();
            label = (String) me.getKey();
            concept = (Concept) me.getValue();
            if (concept != null) {
                result += "\t\"" + label + "\", " + concept.toString() + "\r\n";
            } else {
                result += "\t\"" + label + "\", null\r\n";
            }
            index++;
        }

        return "DictionaryStore{\r\n" + result + '}';
    }

}
