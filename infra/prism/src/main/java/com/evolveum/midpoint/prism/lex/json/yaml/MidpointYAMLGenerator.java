package com.evolveum.midpoint.prism.lex.json.yaml;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.events.ImplicitTuple;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.io.IOException;
import java.io.Writer;

public class MidpointYAMLGenerator extends YAMLGenerator {

	public MidpointYAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures, ObjectCodec codec,
			Writer out, DumperOptions.Version version) throws IOException {
		super(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
	}

	/**
	 * Brutal hack, as default behavior has lead to the following:
	 *  - !<http://midpoint.evolveum.com/xml/ns/public/model/scripting-3/SearchExpressionType>
	 *    !<http://midpoint.evolveum.com/xml/ns/public/model/scripting-3/SearchExpressionType> '@element': "http://midpoint.evolveum.com/xml/ns/public/model/scripting-3#search"
	 *
	 * (so we need to explicitly reset typeId after writing it)
	 */
	public void resetTypeId() {
		_typeId = null;
	}

	@Override
	protected ScalarEvent _scalarEvent(String value, Character style) {
		if (value.contains("\n")) {
			style = Character.valueOf('|');
		}

		ImplicitTuple implicit;
		String yamlTag = _typeId;
		if (yamlTag != null) {
			_typeId = null;
			implicit = new ImplicitTuple(false, false);			// we want to always preserve the tags (if they are present)
		} else {
			implicit = new ImplicitTuple(true, true);
		}
		String anchor = _objectId;
		if (anchor != null) {
			_objectId = null;
		}
		return new ScalarEvent(anchor, yamlTag, implicit, value, null, null, style);
	}

}
