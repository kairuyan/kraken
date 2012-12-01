package org.krakenapps.logdb.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.krakenapps.confdb.Config;
import org.krakenapps.confdb.ConfigCollection;
import org.krakenapps.confdb.ConfigDatabase;
import org.krakenapps.confdb.ConfigService;
import org.krakenapps.confdb.Predicates;
import org.krakenapps.logdb.CsvLookupRegistry;
import org.krakenapps.logdb.LookupHandler;
import org.krakenapps.logdb.LookupHandlerRegistry;

import au.com.bytecode.opencsv.CSVReader;

/**
 * If you need per-user csv lookup management, implement or use other service
 * component. This csv lookup service provides only global configuration.
 * 
 * @author xeraph
 * 
 */
@Component(name = "logdb-csv-lookup-registry")
@Provides
public class CsvLookupRegistryImpl implements CsvLookupRegistry {

	@Requires
	private ConfigService conf;

	@Requires
	private LookupHandlerRegistry lookup;

	@Validate
	public void start() {

	}

	@Invalidate
	public void stop() {

	}

	@Override
	public Set<File> getCsvFiles() {
		ConfigDatabase db = conf.ensureDatabase("kraken-logdb");
		ConfigCollection col = db.ensureCollection("csv_lookups");

		Set<File> files = new HashSet<File>();
		for (Object o : col.findAll().getDocuments()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) o;
			files.add(new File((String) m.get("path")));
		}

		return files;
	}

	@Override
	public void registerCsvFile(File f) throws IOException {
		if (f == null)
			throw new IllegalArgumentException("csv path should not be null");

		if (!f.exists())
			throw new IllegalStateException("csv path doesn't exist: " + f.getAbsolutePath());

		if (!f.isFile())
			throw new IllegalStateException("csv path is not file: " + f.getAbsolutePath());

		if (!f.canRead())
			throw new IllegalStateException("cannot read csv file, check read permission: " + f.getAbsolutePath());

		ConfigDatabase db = conf.ensureDatabase("kraken-logdb");
		ConfigCollection col = db.ensureCollection("csv_lookups");

		// check duplicate
		Config c = col.findOne(Predicates.field("path", f.getAbsolutePath()));
		if (c != null)
			throw new IllegalStateException("csv path already exists: " + f.getAbsolutePath());

		// add
		Map<String, Object> doc = new HashMap<String, Object>();
		doc.put("path", f.getAbsolutePath());
		col.add(doc);

		// add to lookup handler service
		lookup.addLookupHandler(getLookupName(f), new CsvLookupHandler(f));
	}

	@Override
	public void unregisterCsvFile(File f) {
		if (f == null)
			throw new IllegalArgumentException("csv path should not be null");

		ConfigDatabase db = conf.ensureDatabase("kraken-logdb");
		ConfigCollection col = db.ensureCollection("csv_lookups");

		// check existence
		Config c = col.findOne(Predicates.field("path", f.getAbsolutePath()));
		if (c == null)
			throw new IllegalStateException("not registered path: " + f.getAbsolutePath());

		c.remove();

		// remove from lookup handler service
		lookup.removeLookupHandler(getLookupName(f));
	}

	private String getLookupName(File f) {
		return "csv$" + f.getName() + "$" + f.hashCode();
	}

	private class CsvLookupHandler implements LookupHandler {
		private String keyFieldName;
		private String valueFieldName;
		private Map<String, String> mappings = new HashMap<String, String>();

		public CsvLookupHandler(File f) throws IOException {
			CSVReader reader = new CSVReader(new FileReader(f));

			String[] nextLine = reader.readNext();
			if (nextLine == null)
				throw new IllegalStateException("header columns not found");

			if (nextLine.length < 2)
				throw new IllegalStateException("not enough columns (should be 2 or more)");

			keyFieldName = nextLine[0];
			valueFieldName = nextLine[1];

			while ((nextLine = reader.readNext()) != null) {
				mappings.put(nextLine[0], nextLine[1]);
			}
		}

		@Override
		public Object lookup(String srcField, String dstField, Object value) {
			if (!srcField.equals(keyFieldName))
				return null;

			if (!dstField.equals(valueFieldName))
				return null;

			return mappings.get(value.toString());
		}
	}
}
