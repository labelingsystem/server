package info.labeling.v1.rest;

import de.i3mainz.ls.rdfutils.RDF;
import de.i3mainz.ls.rdfutils.Sesame2714;
import de.i3mainz.ls.identifier.UniqueIdentifier;
import de.i3mainz.ls.rdfutils.exceptions.ConfigException;
import de.i3mainz.ls.rdfutils.exceptions.Logging;
import de.i3mainz.ls.rdfutils.exceptions.RdfException;
import de.i3mainz.ls.rdfutils.exceptions.ResourceNotAvailableException;
import de.i3mainz.ls.rdfutils.exceptions.UniqueIdentifierException;
import info.labeling.v1.utils.Transformer;
import info.labeling.v1.utils.PropertiesLocal;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.jdom.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openrdf.query.BindingSet;

/**
 * REST API for Vocabularies
 *
 * @author Florian Thiery M.Sc.
 * @author i3mainz - Institute for Spatial Information and Surveying Technology
 * @version 17.06.2016
 */
@Path("/v1/vocabs")
public class VocabResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response getVocabularies(@HeaderParam("Accept") String acceptHeader) throws IOException, JDOMException, ConfigException, ParserConfigurationException, TransformerException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String query = rdf.getPREFIXSPARQL();
			query += "SELECT * WHERE { "
					+ "?s a ls:Vocabulary . "
					+ "?s ls:identifier ?identifier . "
					+ "}";
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> uris = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "s");
			List<String> ids = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "identifier");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			JSONObject out = new JSONObject();
			JSONArray outArray = new JSONArray();
			for (int i = 0; i < uris.size(); i++) {
				String item = "ls_voc";
				query = getVocabularySPARQL(item, ids.get(i));
				result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
				List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
				List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
				if (result.size() < 1) {
					throw new ResourceNotAvailableException();
				}
				for (int j = 0; j < predicates.size(); j++) {
					rdf.setModelTriple(item + ":" + ids.get(i), predicates.get(j), objects.get(j));
				}
				JSONObject outObject = Transformer.vocabulary_GET(rdf.getModel("RDF/JSON"), ids.get(i));
				outArray.add(outObject);
			}
			out.put("vocabs", outArray);
			return Response.ok(out).header("Content-Type", "application/json;charset=UTF-8").build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}")
	@Produces({"application/json;charset=UTF-8", "application/xml;charset=UTF-8", "application/rdf+xml;charset=UTF-8", "text/turtle;charset=UTF-8", "text/n3;charset=UTF-8", "application/ld+json;charset=UTF-8"})
	public Response getVocabulary(@PathParam("vocabulary") String vocabulary, @HeaderParam("Accept") String acceptHeader) throws IOException, JDOMException, RdfException, ParserConfigurationException, TransformerException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			if (acceptHeader.contains("application/json")) {
				String out = Transformer.vocabulary_GET(rdf.getModel("RDF/JSON"), vocabulary).toJSONString();
				return Response.ok(out).build();
			} else if (acceptHeader.contains("text/html")) {
				return Response.ok(rdf.getModel("RDF/JSON")).header("Content-Type", "application/json;charset=UTF-8").build();
			} else if (acceptHeader.contains("application/xml")) {
				return Response.ok(rdf.getModel("RDF/XML")).build();
			} else if (acceptHeader.contains("application/rdf+xml")) {
				return Response.ok(rdf.getModel("RDF/XML")).build();
			} else if (acceptHeader.contains("text/turtle")) {
				return Response.ok(rdf.getModel("Turtle")).build();
			} else if (acceptHeader.contains("text/n3")) {
				return Response.ok(rdf.getModel("N-Triples")).build();
			} else if (acceptHeader.contains("application/ld+json")) {
				return Response.ok(rdf.getModel("JSON-LD")).build();
			} else {
				return Response.ok(rdf.getModel("RDF/JSON")).header("Content-Type", "application/json;charset=UTF-8").build();
			}
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}.json")
	@Produces("application/json;charset=UTF-8")
	public Response getVocabulary_JSON(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, TransformerException, ParserConfigurationException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			String out = Transformer.vocabulary_GET(rdf.getModel("RDF/JSON"), vocabulary).toJSONString();
			return Response.ok(out).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}.xml")
	@Produces("application/xml;charset=UTF-8")
	public Response getVocabulary_XML(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, TransformerException, ParserConfigurationException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			String RDFoutput = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + rdf.getModel("RDF/XML");
			return Response.ok(RDFoutput).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}.rdf")
	@Produces("application/rdf+xml;charset=UTF-8")
	public Response getVocabularyRDF_XML(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, RdfException, ParserConfigurationException, TransformerException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			String RDFoutput = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + rdf.getModel("RDF/XML");
			return Response.ok(RDFoutput).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}.ttl")
	@Produces("text/turtle;charset=UTF-8")
	public Response getVocabularyRDF_Turtle(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, TransformerException, ParserConfigurationException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			return Response.ok(rdf.getModel("Turtle")).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}.n3")
	@Produces("text/n3;charset=UTF-8")
	public Response getVocabularyRDF_N3(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, TransformerException, ParserConfigurationException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			return Response.ok(rdf.getModel("N-Triples")).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}

	@GET
	@Path("/{vocabulary}.jsonrdf")
	@Produces("application/json;charset=UTF-8")
	public Response getVocabularyRDF_JSONRDF(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, TransformerException, ParserConfigurationException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			return Response.ok(rdf.getModel("RDF/JSON")).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}
	
	@GET
	@Path("/{vocabulary}.jsonld")
	@Produces("application/ld+json;charset=UTF-8")
	public Response getVocabularyRDF_JSONLD(@PathParam("vocabulary") String vocabulary) throws IOException, JDOMException, TransformerException, ParserConfigurationException {
		try {
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String item = "ls_voc";
			String query = getVocabularySPARQL(item, vocabulary);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + vocabulary, predicates.get(i), objects.get(i));
			}
			return Response.ok(rdf.getModel("JSON-LD")).build();
		} catch (Exception e) {
			if (e.toString().contains("ResourceNotAvailableException")) {
				return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
						.header("Content-Type", "application/json;charset=UTF-8").build();
			}
		}
	}
	
	@POST
	@Path("/user/{user}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response postVocabulary(@PathParam("user") String user, String json) throws IOException, JDOMException, ConfigException, ParserConfigurationException, TransformerException {
		try {
			// get variables
			String item = "ls_voc";
			String itemID = UniqueIdentifier.getUUID();
			// create triples
			json = Transformer.vocabulary_POST(json,itemID);
			String triples = createVocabularySPARQLUPDATE(item, itemID, user);
			// input triples
			Sesame2714.inputRDFfromRDFJSONString(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), json);
			Sesame2714.SPARQLupdate(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), triples);
			// get result als json
			RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
			String query = getVocabularySPARQL(item, itemID);
			List<BindingSet> result = Sesame2714.SPARQLquery(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), query);
			List<String> predicates = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "p");
			List<String> objects = Sesame2714.getValuesFromBindingSet_ORDEREDLIST(result, "o");
			if (result.size() < 1) {
				throw new ResourceNotAvailableException();
			}
			for (int i = 0; i < predicates.size(); i++) {
				rdf.setModelTriple(item + ":" + itemID, predicates.get(i), objects.get(i));
			}
			return Response.status(Response.Status.CREATED).entity(rdf.getModel("RDF/JSON")).header("Content-Type", "application/json;charset=UTF-8").build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
					.header("Content-Type", "application/json;charset=UTF-8").build();
		}
	}

	@PUT
	@Path("/{vocabulary}/user/{user}/type/{type}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response updateVocabulary(@PathParam("vocabulary") String vocabulary, @PathParam("user") String user, @PathParam("type") String type, String json)
			throws IOException, JDOMException, RdfException, ParserConfigurationException, TransformerException {
		try {
			String item = "ls_voc";
			Sesame2714.SPARQLupdate(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), putVocabularyREVISION(item, vocabulary, user, type));
			Sesame2714.SPARQLupdate(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), putVocabularySPARQLUPDATE(vocabulary));
			Sesame2714.inputRDFfromRDFJSONString(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), json);
			return Response.status(Response.Status.CREATED).entity(json).header("Content-Type", "application/json;charset=UTF-8").build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
					.header("Content-Type", "application/json;charset=UTF-8").build();
		}
	}

	@DELETE
	@Path("/{vocabulary}/user/{user}")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response deleteVocabulary(@PathParam("vocabulary") String vocabulary, @PathParam("user") String user) throws IOException, JDOMException, RdfException, ParserConfigurationException, TransformerException {
		try {
			String item = "ls_voc";
			Sesame2714.SPARQLupdate(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), deleteVocabularyREVISION(item, vocabulary, user));
			Sesame2714.SPARQLupdate(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), deleteVocabularySPARQLUPDATE(vocabulary));
			Sesame2714.SPARQLupdate(PropertiesLocal.getPropertyParam(PropertiesLocal.getREPOSITORY()), PropertiesLocal.getPropertyParam(PropertiesLocal.getSESAMESERVER()), deleteVocabularyStatusTypeSPARQLUPDATE(vocabulary));
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "info.labeling.v1.rest.VocabResource"))
					.header("Content-Type", "application/json;charset=UTF-8").build();
		}
	}

	private static String getVocabularySPARQL(String item, String itemid) throws ConfigException, IOException {
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String query = prefixes + "SELECT * WHERE { ";
		query += item + ":" + itemid + " ?p ?o. } ";
		query += "ORDER BY ASC(?p)";
		return query;
	}

	private static String createVocabularySPARQLUPDATE(String item, String itemid, String user) throws ConfigException, IOException, UniqueIdentifierException {
		String revID = UniqueIdentifier.getUUID();
		Calendar calender = Calendar.getInstance();
		Date date = calender.getTime();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		String dateiso = formatter.format(date);
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String triples = prefixes + "INSERT DATA { ";
		triples += item + ":" + itemid + " a ls:Vocabulary . ";
		triples += item + ":" + itemid + " a skos:ConceptScheme . ";
		triples += item + ":" + itemid + " ls:identifier \"" + itemid + "\"" + " . ";
		triples += item + ":" + itemid + " ls:sameAs "
				+ "<" + PropertiesLocal.getPropertyParam("ls_detailhtml")
				.replace("$host", PropertiesLocal.getPropertyParam("host"))
				.replace("$itemid", itemid).replace("$item", "vocabulary") + ">" + " . ";
		triples += item + ":" + itemid + " dc:creator \"" + user + "\"" + " . ";
		triples += item + ":" + itemid + " dct:creator ls_age:" + user + " . ";
		triples += item + ":" + itemid + " dc:contributor \"" + user + "\"" + " . ";
		triples += item + ":" + itemid + " dct:contributor ls_age:" + user + " . ";
		triples += item + ":" + itemid + " dct:date \"" + dateiso + "\"" + " . ";
		triples += item + ":" + itemid + " dct:identifier \"" + itemid + "\"" + " . ";
		triples += item + ":" + itemid + " dct:license <http://creativecommons.org/licenses/by/4.0/> . ";
		triples += item + ":" + itemid + " dc:created \"" + dateiso + "\"" + " . ";
		triples += item + ":" + itemid + " skos:changeNote ls_rev:" + revID + " . ";
		triples += "ls_rev" + ":" + revID + " a ls:Revision . ";
		triples += "ls_rev" + ":" + revID + " a prov:Activity . ";
		triples += "ls_rev" + ":" + revID + " a prov:Modify . ";
		triples += "ls_rev" + ":" + revID + " ls:identifier \"" + revID + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dc:creator \"" + user + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dct:creator ls_age:" + user + " . ";
		triples += "ls_rev" + ":" + revID + " dct:date \"" + dateiso + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dc:description \"" + "CreateRevision" + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dct:type ls:CreateRevision . ";
		triples += "ls_rev" + ":" + revID + " prov:startedAtTime \"" + dateiso + "\"" + " . ";
		triples += " }";
		return triples;
	}

	private static String putVocabularyREVISION(String item, String itemid, String user, String type) throws ConfigException, IOException, UniqueIdentifierException {
		String revID = UniqueIdentifier.getUUID();
		Calendar calender = Calendar.getInstance();
		Date date = calender.getTime();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		String dateiso = formatter.format(date);
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String triples = prefixes + "INSERT DATA { ";
		triples += item + ":" + itemid + " dc:modified \"" + dateiso + "\"" + " . ";
		triples += item + ":" + itemid + " skos:changeNote ls_rev:" + revID + " . ";
		triples += "ls_rev" + ":" + revID + " a ls:Revision . ";
		triples += "ls_rev" + ":" + revID + " a prov:Activity . ";
		triples += "ls_rev" + ":" + revID + " a prov:Modify . ";
		triples += "ls_rev" + ":" + revID + " ls:identifier \"" + revID + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dc:creator \"" + user + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dct:creator ls_age:" + user + " . ";
		triples += "ls_rev" + ":" + revID + " dct:date \"" + dateiso + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dc:description \"" + type + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dct:type ls:" + type + " . ";
		triples += "ls_rev" + ":" + revID + " prov:startedAtTime \"" + dateiso + "\"" + " . ";
		triples += " }";
		return triples;
	}
	
	private static String deleteVocabularyREVISION(String item, String itemid, String user) throws ConfigException, IOException, UniqueIdentifierException {
		String revID = UniqueIdentifier.getUUID();
		Calendar calender = Calendar.getInstance();
		Date date = calender.getTime();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		String dateiso = formatter.format(date);
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String triples = prefixes + "INSERT DATA { ";
		triples += item + ":" + itemid + " ls:hasStatusType ls:Deleted . ";
		triples += item + ":" + itemid + " skos:changeNote ls_rev:" + revID + " . ";
		triples += "ls_rev" + ":" + revID + " a ls:Revision . ";
		triples += "ls_rev" + ":" + revID + " a prov:Activity . ";
		triples += "ls_rev" + ":" + revID + " a prov:Modify . ";
		triples += "ls_rev" + ":" + revID + " ls:identifier \"" + revID + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dc:creator \"" + user + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dct:creator ls_age:" + user + " . ";
		triples += "ls_rev" + ":" + revID + " dct:date \"" + dateiso + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dc:description \"" + "DeleteRevision" + "\"" + " . ";
		triples += "ls_rev" + ":" + revID + " dct:type ls:" + "DeleteRevision" + " . ";
		triples += "ls_rev" + ":" + revID + " prov:startedAtTime \"" + dateiso + "\"" + " . ";
		triples += " }";
		return triples;
	}

	private static String putVocabularySPARQLUPDATE(String id) throws IOException {
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String update = prefixes
				+ "DELETE { ?vocabulary ?p ?o. } "
				+ "WHERE { "
				+ "?vocabulary ?p ?o. "
				+ "?vocabulary ls:identifier ?identifier. "
				+ "FILTER (?identifier=\"$identifier\") "
				+ "FILTER (?p IN (ls:belongsToProject,skos:hasTopConcept,dct:contributor,dc:contributor,dct:title,dct:description,ls:hasReleaseType,dcat:theme,ls:isRetcatsItem)) "
				+ "}";
		update = update.replace("$identifier", id);
		return update;
	}

	private static String deleteVocabularySPARQLUPDATE(String id) throws IOException {
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String update = prefixes
				+ "DELETE { ?vocabulary ?p ?o. } "
				+ "WHERE { "
				+ "?vocabulary ?p ?o. "
				+ "?vocabulary ls:identifier ?identifier. "
				+ "FILTER (?identifier=\"$identifier\") "
				+ "FILTER (?p IN (ls:belongsToProject,skos:hasTopConcept,dct:contributor,dc:contributor,dct:title,dct:description,ls:hasReleaseType,dcat:theme,ls:isRetcatsItem,ls:sameAs,dct:creator,dc:creator,dct:contributor,dc:contributor,dct:date,dct:identifier,dct:license,dc:created,dc:modified)) "
				+ "}";
		update = update.replace("$identifier", id);
		return update;
	}
	
	private static String deleteVocabularyStatusTypeSPARQLUPDATE(String id) throws IOException {
		RDF rdf = new RDF(PropertiesLocal.getPropertyParam("host"));
		String prefixes = rdf.getPREFIXSPARQL();
		String update = prefixes
				+ "DELETE { ?vocabulary ls:hasStatusType ls:Active. } "
				+ "WHERE { "
				+ "?vocabulary ls:hasStatusType ls:Active. "
				+ "?vocabulary ls:identifier ?identifier. "
				+ "FILTER (?identifier=\"$identifier\") "
				+ "}";
		update = update.replace("$identifier", id);
		return update;
	}

}
