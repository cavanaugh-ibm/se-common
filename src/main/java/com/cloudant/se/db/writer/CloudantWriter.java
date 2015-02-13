package com.cloudant.se.db.writer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.lightcouch.CouchDbException;
import org.lightcouch.DocumentConflictException;

import com.cloudant.client.api.Database;
import com.cloudant.se.Constants;
import com.cloudant.se.Constants.WriteCode;
import com.cloudant.se.db.exception.StructureException;
import com.google.gson.Gson;

/**
 * Callable which provides the base methods to insert/update/upsert a document into a cloudant database
 * 
 * @author IBM
 */
public abstract class CloudantWriter implements Callable<WriteCode> {
	protected static final Logger	log			= Logger.getLogger(CloudantWriter.class);

	protected Database				database	= null;
	protected Gson					gson		= new Gson();

	public CloudantWriter(Database database) {
		this.database = database;
	}

	protected WriteCode insert(Map<String, Object> map) {
		int i = 0;
		while (i < 30) {
			i++;
			WriteCode ic = _insert(map);
			switch (ic) {
				case TIMEOUT:
					break;
				case CONFLICT:
				case EXCEPTION:
				case INSERT:
				case MAX_ATTEMPTS:
				case SECURITY:
				case UPDATE:
					return ic;
			}
		}

		return WriteCode.MAX_ATTEMPTS;
	}

	private WriteCode _insert(Map<String, Object> map) {
		Object id = map.get("_id");
		try {
			log.debug("[id=" + id + "] - save - remote call");

			if (id == null || StringUtils.isBlank(id.toString())) {
				// Remove the magic string for generated IDs
				map.remove("_id");
			}
			database.save(map);
			return WriteCode.INSERT;
		} catch (DocumentConflictException e) {
			log.debug("[id=" + id + "] - insert - DocumentConflictException - conflict");
			return WriteCode.CONFLICT;
		} catch (CouchDbException e) {
			if (e.getCause() != null) {
				log.debug("[id=" + id + "] - insert - CouchDbException - " + e.getCause().getMessage());
				if (StringUtils.contains(e.getCause().getMessage(), "Connection timed out: connect")) {
					log.debug("[id=" + id + "] - insert - CouchDbException - timeout");
					return WriteCode.TIMEOUT;
				}
			}

			if (e.getMessage().contains("\"error\":\"forbidden\",\"reason\":\"_writer access is required for this request\"")) {
				log.fatal("Cloudant account does not have writer permissions");
				return WriteCode.SECURITY;
			}

			log.warn("[id=" + id + "] - insert - CouchDbException - unknown", e);
			return WriteCode.EXCEPTION;
		}
	}

	protected WriteCode update(Map<String, Object> map) {
		int i = 0;
		while (i < 30) {
			i++;
			WriteCode ic = _update(map);
			switch (ic) {
				case TIMEOUT:
				case CONFLICT:
					try {
						map = handleConflict(map);
					} catch (Exception e) {
						return WriteCode.EXCEPTION;
					}
					break;
				case EXCEPTION:
				case INSERT:
				case MAX_ATTEMPTS:
				case SECURITY:
				case UPDATE:
					return ic;
			}
		}

		return WriteCode.MAX_ATTEMPTS;
	}

	private WriteCode _update(Map<String, Object> map) throws CouchDbException, SecurityException {
		Object id = map.get("_id");
		try {
			log.debug("[id=" + id + "] - update - remote call");
			database.update(map);
			return WriteCode.UPDATE;
		} catch (DocumentConflictException e) {
			log.debug("[id=" + id + "] - update - DocumentConflictException - conflict");
			return WriteCode.CONFLICT;
		} catch (IllegalArgumentException e) {
			if (StringUtils.contains(e.getMessage(), "rev may not be null")) {
				log.debug("[id=" + id + "] - update - IllegalArgumentException - missing rev ");
				return WriteCode.EXCEPTION;
			}

			log.warn("[id=" + id + "] - update - IllegalArgumentException - unknown", e);
			return WriteCode.EXCEPTION;
		} catch (CouchDbException e) {
			if (e.getCause() != null) {
				log.debug("[id=" + id + "] - update - CouchDbException - " + e.getCause().getMessage());
				if (StringUtils.contains(e.getCause().getMessage(), "Connection timed out: connect")) {
					log.debug("[id=" + id + "] - update - CouchDbException - timeout");
					return WriteCode.TIMEOUT;
				}
			}

			if (e.getMessage().contains("\"error\":\"forbidden\",\"reason\":\"_writer access is required for this request\"")) {
				log.fatal("Cloudant account does not have writer permissions");
				return WriteCode.SECURITY;
			}

			log.warn("[id=" + id + "] - update - CouchDbException - unknown", e);
			return WriteCode.EXCEPTION;
		}
	}

	protected Map<String, Object> getFromCloudant(String id) throws JsonProcessingException, IOException {
		log.debug("[id=" + id + "] - read - call");
		InputStream is = database.find(id);
		Map<String, Object> map = new ObjectMapper().reader(Map.class).readValue(is);
		log.debug("[id=" + id + "] - read - success");

		return map;
	}

	protected abstract Map<String, Object> handleConflict(Map<String, Object> failed) throws StructureException, JsonProcessingException, IOException;

	protected WriteCode upsert(String id, Map<String, Object> map) {
		Map<String, Object> toUpsert = map;
		try {
			WriteCode insertCode = insert(toUpsert);
			switch (insertCode) {
				case INSERT:
					//
					// Insert worked, nothing else to do in this scenario
					log.debug("[id=" + id + "] - insert - succeeded");
					return WriteCode.INSERT;
				case CONFLICT:
					//
					// Conflict, get the old version, merge in our changes (adding)
					toUpsert = handleConflict(toUpsert);
					WriteCode updateCode = update(toUpsert);
					switch (updateCode) {
						case UPDATE:
							log.debug("[id=" + id + "] - update - succeeded");
							break;
						default:
							log.debug("[id=" + id + "] - update - failed - " + updateCode);
							break;
					}

					return updateCode;
				default:
					log.debug("[id=" + id + "] - insert - failed - " + insertCode);
					return insertCode;
			}
		} catch (Exception e) {
			log.warn("[id=" + id + "] - Unable to upsert a document due to exception - [" + gson.toJson(toUpsert) + "]", e);
			return WriteCode.EXCEPTION;
		}
	}
}
