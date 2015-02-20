package com.github.fengtan.solrgui;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

// TODO test with Solr < 4.
public class SolrGUIServer {

	private URL url;
	private String name;
	private SolrServer server;
	private List<SolrGUIDocument> documents;
	private Set<ISolrGUIServerViewer> changeListeners = new HashSet<ISolrGUIServerViewer>();

	public SolrGUIServer(URL url, String name) {
		this.url = url;
		this.name = name;
		this.server = new HttpSolrServer(url.toExternalForm());
		refreshDocuments();
	}
	
	public URL getURL() {
		return url;
	}
	
	public String getName() {
		return name;
	}
	
	public void refreshDocuments() {
		documents = getAllDocuments();
	}
	
	// TODO cache ? use transactions ?
	private List<SolrGUIDocument> getAllDocuments() {
		SolrQuery query = new SolrQuery();
		query.set("q", "*:*");
		return getDocumentList(query);
	}
	
	private List<SolrGUIDocument> getDocumentList(SolrQuery query) {
		List<SolrGUIDocument> list = new ArrayList<SolrGUIDocument>();
		QueryResponse response;
		try {
			response = server.query(query);
		} catch (SolrServerException e) {
			// TODO log error
			return list;
		}
		for (SolrDocument document:response.getResults()) {
			list.add(new SolrGUIDocument(document));
		}
		return list;
	}

	/**
	 * Return the collection of documents
	 */
	public List<SolrGUIDocument> getDocuments() {
		return documents;
	}
	
	/**
	 * Add a new document to the collection of documents
	 */
	public void addDocument() {
		SolrGUIDocument document = new SolrGUIDocument(getFields());
		document.setChange(SolrGUIChange.ADDED);
		documents.add(documents.size(), document);
		for (ISolrGUIServerViewer viewer:changeListeners) {
			viewer.addDocument(document);
		}
	}

	/**
	 * @param document
	 */
	public void removeDocument(SolrGUIDocument document) {
		document.setChange(SolrGUIChange.DELETED);
		for (ISolrGUIServerViewer viewer:changeListeners) {
			viewer.updateDocument(document);
		}
	}

	/**
	 * @param document
	 */
	public void documentChanged(SolrGUIDocument document) {
		document.setChange(SolrGUIChange.UPDATED);
		for (ISolrGUIServerViewer viewer:changeListeners) {
			viewer.updateDocument(document);
		}
	}

	/**
	 * @param viewer
	 */
	public void removeChangeListener(ISolrGUIServerViewer viewer) {
		changeListeners.remove(viewer);
	}

	/**
	 * @param viewer
	 */
	public void addChangeListener(ISolrGUIServerViewer viewer) {
		changeListeners.add(viewer);
	}
	
	// TODO is there a better way to get the list of fields ? e.g. SolrQuery with only 1 document (not q *:*)
	// TODO what if there is no document in the server ?
	public String[] getFields() {
		LukeRequest request = new LukeRequest();
		try {
			LukeResponse response = request.process(server);
			Collection<FieldInfo> fieldsInfo = response.getFieldInfo().values();
			List<String> fields = new ArrayList<String>();
			for (FieldInfo fieldInfo:fieldsInfo) {
				fields.add(fieldInfo.getName());
			}
			return fields.toArray(new String[fields.size()]);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new String[]{};
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new String[]{};
		}
		/* TODO provide option to use this in case Luke handler is not available?
		Collection<String> fields = getAllDocuments().get(0).getFieldNames();
		return fields.toArray(new String[fields.size()]);
		*/
	}
	
	public void commit() {
		// TODO could use CollectionUtils.filter().
		// TODO graceful degradation if Luke handlers not provided by server
		// TODO clean up libraries
		SolrInputDocument input;
		for (SolrGUIDocument document:documents) {
			switch (document.getChange()) {
				case ADDED:
					input = ClientUtils.toSolrInputDocument(document.getDocument());
					try {
						// TODO use LukeRequest ?
						server.add(input); // Returned object seems to have no relevant information.
					} catch(SolrServerException e) {
						// TODO
						e.printStackTrace();
					} catch(IOException e) {
						// TODO
						e.printStackTrace();
					}
					break;
				case DELETED:
					// TODO use LukeRequest ?
					String id = document.getDocument().getFieldValue("id").toString(); // TODO what if no field named "id"
					try {
						server.deleteById(id);
					} catch (SolrServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case NONE:
					// Nothing to do.
					break;
				case UPDATED:
					// TODO use LukeRequest ?
					input = ClientUtils.toSolrInputDocument(document.getDocument());
					try {
						server.add(input); // Returned object seems to have no relevant information.
					} catch (SolrServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
			}
		}
		try {
			server.commit(); 
			refreshDocuments();  // Returned object seems to have no relevant information.
			// TODO popup to confirm commit is successful ?
			// TODO allow to revert a specific document
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO reload data
	}
	
	/*
	 * Release resources.
	 */
	public void dispose() {
		server.shutdown();
	}

}