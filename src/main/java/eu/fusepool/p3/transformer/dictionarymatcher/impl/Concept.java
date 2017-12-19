package eu.fusepool.p3.transformer.dictionarymatcher.impl;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.SKOS;

/**
 * This object represents a single triple in a SKOS taxonomy
 *
 * @author Gábor Reményi
 */
public class Concept {

    public String labelText;
    public RDFNode labelType;
    public String uri;
    public String type;

    public Concept() {
    }

    public Concept(String labelText, Property preflabel, String uri) {
        this.labelText = labelText;
        this.labelType = preflabel;
        this.uri = uri;
    }

    public Concept(String labelText, RDFNode labelType2, String uri, String type) {
        this.labelText = labelText;
        this.labelType = labelType2;
        this.uri = uri;
        this.type = type;
    }

    public Boolean isPrefLabel() {
        return SKOS.prefLabel.equals(labelType);
    }

    public Boolean isAltLabel() {
        return SKOS.altLabel.equals(labelType);
    }

    @Override
    public String toString() {
        return "Concept{" + "labelText=" + labelText + ", labelType=" + labelType + ", uri=" + uri + ", type=" + type + '}';
    }

}
