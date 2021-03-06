package v1.rest;

import exceptions.Logging;
import v1.utils.config.ConfigProperties;
import link.labeling.retcat.classes.SuggestionItem;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import v1.utils.similarity.Similarity;

@Path("/search")
public class SearchResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public Response getResultsLabelingSystem(
			@HeaderParam("Accept-Encoding") String acceptEncoding,
			@QueryParam("query") String searchword,
			@QueryParam("vocab") String vocabulary,
			@QueryParam("lookup") String fields,
			@QueryParam("maxhits") String maxhits,
			@QueryParam("draft") String draft) {
		try {
			String OUTSTRING = "";
			int limit = 20;
			if (maxhits != null) {
				limit = Integer.parseInt(maxhits);
			}
			String url = ConfigProperties.getPropertyParam("api") + "/v1/sparql";
			String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> PREFIX ls: <http://labeling.link/docs/ls/core#> PREFIX dc: <http://purl.org/dc/elements/1.1/> "
					+ "SELECT ?Subject ?id ?prefLabels ?scopeNote ?BroaderPreferredTerm ?BroaderPreferred ?NarrowerPreferredTerm ?NarrowerPreferred ?schemeTitle ?scheme WHERE { "
					//+ "SELECT ?Subject ?id ?prefLabel WHERE { "
					+ "?Subject dc:identifier ?id . "
					+ "?Subject skos:inScheme ?scheme . "
					+ "?scheme dc:title ?schemeTitle . "
					+ "?Subject skos:prefLabel ?prefLabel . "
					+ "?Subject skos:prefLabel ?prefLabels . "
					+ "?Subject ls:thumbnail ?preferredLabel . "
					+ "OPTIONAL { ?Subject skos:scopeNote ?scopeNote . } "
					+ "OPTIONAL {?Subject skos:broader ?BroaderPreferred . ?BroaderPreferred ls:thumbnail ?BroaderPreferredTerm.} "
					+ "OPTIONAL {?Subject skos:narrower ?NarrowerPreferred . ?NarrowerPreferred ls:thumbnail ?NarrowerPreferredTerm .} ";
			if (fields != null) {
				if (fields.contains("prefLabel")) {
					sparql += "FILTER(regex(?prefLabel, '" + searchword + "', 'i')) ";
				} else if (fields.contains("scopeNote")) {
					sparql += "FILTER(regex(?scopeNote, '" + searchword + "', 'i')) ";
				} else {
					sparql += "FILTER(regex(?prefLabel, '" + searchword + "', 'i') || regex(?scopeNote, '" + searchword + "', 'i')) ";
				}
			} else {
				sparql += "FILTER(regex(?prefLabel, '" + searchword + "', 'i') || regex(?scopeNote, '" + searchword + "', 'i')) ";
			}
			if (vocabulary != null) {
				sparql += "FILTER(?scheme=<http://" + ConfigProperties.getPropertyParam("host") + "/item/vocabulary/" + vocabulary + ">) ";
			}
			if (draft == null) {
				sparql += "?scheme ls:hasReleaseType ls:Public . ";
			}
			sparql += "} ORDER BY ASC(?preferredLabel)";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Accept", "application/sparql-results+json");
			String urlParameters = "query=" + sparql;
			//byte[] bytes = urlParameters.getBytes(StandardCharsets.UTF_8);
			//urlParameters = new String(bytes, StandardCharsets.ISO_8859_1);
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF8"));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// init output
			JSONArray outArray = new JSONArray();
			// parse SPARQL results json
			JSONObject jsonObject = (JSONObject) new JSONParser().parse(response.toString());
			JSONObject resultsObject = (JSONObject) jsonObject.get("results");
			JSONArray bindingsArray = (JSONArray) resultsObject.get("bindings");
			// create unique list of ids
			HashSet<String> uris = new HashSet<String>();
			for (Object element : bindingsArray) {
				JSONObject tmpElement = (JSONObject) element;
				JSONObject subject = (JSONObject) tmpElement.get("Subject");
				String subjectValue = (String) subject.get("value");
				uris.add(subjectValue);
			}
			// create list of autosuggest objects
			Map<String, SuggestionItem> autosuggests = new HashMap<String, SuggestionItem>();
			for (String element : uris) {
				autosuggests.put(element, new SuggestionItem(element));
			}
			// fill objects
			for (Object element : bindingsArray) {
				JSONObject tmpElement = (JSONObject) element;
				// get Subject
				JSONObject subject = (JSONObject) tmpElement.get("Subject");
				String subjectValue = (String) subject.get("value");
				// for every subject value get object from list and write values in it
				SuggestionItem tmpAutosuggest = autosuggests.get(subjectValue);
				// get id
				JSONObject idObject = (JSONObject) tmpElement.get("id");
				String idValue = (String) idObject.get("value");
				tmpAutosuggest.setId(idValue);
				// get Label
				JSONObject labelObject = (JSONObject) tmpElement.get("prefLabels");
				String labelValue = (String) labelObject.get("value");
				String labelLang = (String) labelObject.get("xml:lang");
				tmpAutosuggest.setLabel(labelValue + "@" + labelLang);
				// get Scheme
				JSONObject schemeObject = (JSONObject) tmpElement.get("schemeTitle");
				String schemeValue = (String) schemeObject.get("value");
				String schemeLang = (String) schemeObject.get("xml:lang");
				tmpAutosuggest.setSchemeTitle(schemeValue + "@" + schemeLang);
				JSONObject schemeURIObject = (JSONObject) tmpElement.get("scheme");
				String schemeURIValue = (String) schemeURIObject.get("value");
				tmpAutosuggest.setSchemeURI(schemeURIValue);
				// get scopeNote
				JSONObject scopeNoteObject = (JSONObject) tmpElement.get("scopeNote");
				if (scopeNoteObject != null) {
					String scopeNoteValue = (String) scopeNoteObject.get("value");
					String scopeNoteLang = (String) scopeNoteObject.get("xml:lang");
					tmpAutosuggest.setDescription(scopeNoteValue + "@" + scopeNoteLang);
				}
				// get broader
				String broaderVL = "";
				String broaderURI = "";
				JSONObject broaderObject = (JSONObject) tmpElement.get("BroaderPreferredTerm");
				if (broaderObject != null) {
					String broaderValue = (String) broaderObject.get("value");
					String broaderLang = (String) broaderObject.get("xml:lang");
					broaderVL = broaderValue.replace("<", "").replace(">", "") + "@" + broaderLang.replace("<", "").replace(">", "");
				}
				JSONObject broaderURIObject = (JSONObject) tmpElement.get("BroaderPreferred");
				if (broaderURIObject != null) {
					broaderURI = (String) broaderURIObject.get("value");
				}
				if (!broaderURI.equals("")) {
					HashMap<String, String> hstmpBroader = new HashMap<String, String>();
					hstmpBroader.put(broaderURI, broaderVL);
					tmpAutosuggest.setBroaderTerm(hstmpBroader);
				}
				// get narrower
				String narrowerVL = "";
				String narrowerURI = "";
				JSONObject narrowerObject = (JSONObject) tmpElement.get("NarrowerPreferredTerm");
				if (narrowerObject != null) {
					String narrowerValue = (String) narrowerObject.get("value");
					String narrowerLang = (String) narrowerObject.get("xml:lang");
					narrowerVL = narrowerValue.replace("<", "").replace(">", "") + "@" + narrowerLang.replace("<", "").replace(">", "");
				}
				JSONObject narrowerURIObject = (JSONObject) tmpElement.get("NarrowerPreferred");
				if (narrowerURIObject != null) {
					narrowerURI = (String) narrowerURIObject.get("value");
				}
				if (!narrowerURI.equals("")) {
					HashMap<String, String> hstmpNarrower = new HashMap<String, String>();
					hstmpNarrower.put(narrowerURI, narrowerVL);
					tmpAutosuggest.setNarrowerTerm(hstmpNarrower);
				}
			}
			// fill output json
			int i = 0;
			for (Map.Entry<String, SuggestionItem> entry : autosuggests.entrySet()) {
				if (i < limit) {
					JSONObject similarityObject = new JSONObject();
					SuggestionItem tmpAS = entry.getValue();
					JSONObject suggestionObject = new JSONObject();
					// url
					suggestionObject.put("uri", tmpAS.getURL());
					suggestionObject.put("id", tmpAS.getId());
					// prefLabels
					JSONArray prefLabelArray = new JSONArray();
					List<Double> prefLabelSimilarity = new ArrayList();
					for (String item : tmpAS.getLabels()) {
						String[] splitItem = item.split("@");
						JSONObject jo = new JSONObject();
						jo.put("label", splitItem[0]);
						jo.put("lang", splitItem[1]);
						prefLabelArray.add(jo);
						prefLabelSimilarity.add(Similarity.NormalizedLevenshtein(searchword, splitItem[0]));
					}
					suggestionObject.put("prefLabels", prefLabelArray);
					similarityObject.put("label", Collections.min(prefLabelSimilarity));
					// scheme
					String[] splitVocabItem = tmpAS.getSchemeTitle().split("@");
					if (splitVocabItem.length > 1) {
						JSONObject joVoc = new JSONObject();
						joVoc.put("label", splitVocabItem[0]);
						joVoc.put("lang", splitVocabItem[1]);
						joVoc.put("uri", tmpAS.getSchemeURI());
						suggestionObject.put("vocab", joVoc);
					}
					// scopeNote
					for (String item : tmpAS.getDescriptions()) {
						String[] splitItem = item.split("@");
						JSONObject jo = new JSONObject();
						jo.put("note", splitItem[0]);
						jo.put("lang", splitItem[1]);
						suggestionObject.put("scopeNote", jo);
						similarityObject.put("scopeNote", Similarity.NormalizedLevenshtein(searchword, splitItem[0]));
					}
					// broader
					Set broaderTerms = tmpAS.getBroaderTerms();
					JSONArray broaderArrayNew = new JSONArray();
					if (broaderTerms.size() > 0) {
						for (Object element : broaderTerms) {
							Map hm = (Map) element;
							Iterator entries = hm.entrySet().iterator();
							while (entries.hasNext()) {
								Map.Entry thisEntry = (Map.Entry) entries.next();
								String key = (String) thisEntry.getKey();
								String value = (String) thisEntry.getValue();
								JSONObject broaderObjectTmp = new JSONObject();
								broaderObjectTmp.put("uri", key);
								broaderObjectTmp.put("label", value);
								broaderArrayNew.add(broaderObjectTmp);
							}
						}
					}
					suggestionObject.put("broaderTerms", broaderArrayNew);
					// narrower
					Set narrowerTerms = tmpAS.getNarrowerTerms();
					JSONArray narrowerArrayNew = new JSONArray();
					if (narrowerTerms.size() > 0) {
						for (Object element : narrowerTerms) {
							Map hm = (Map) element;
							Iterator entries = hm.entrySet().iterator();
							while (entries.hasNext()) {
								Map.Entry thisEntry = (Map.Entry) entries.next();
								String key = (String) thisEntry.getKey();
								String value = (String) thisEntry.getValue();
								JSONObject narrowerObjectTmp = new JSONObject();
								narrowerObjectTmp.put("uri", key);
								narrowerObjectTmp.put("label", value);
								narrowerArrayNew.add(narrowerObjectTmp);
							}
						}
					}
					suggestionObject.put("narrowerTerms", narrowerArrayNew);
					// add similarity
					suggestionObject.put("similarityNormalizedLevenshtein", similarityObject);
					// add information to output
					outArray.add(suggestionObject);
				}
				i++;
			}
			OUTSTRING = outArray.toString();
			if (acceptEncoding.contains("gzip")) {
				// set outputstream
				final String OUTSTRING_FINAL = OUTSTRING;
				StreamingOutput stream;
				stream = new StreamingOutput() {
					@Override
					public void write(OutputStream output) throws IOException, WebApplicationException {
						try {
							output = GZIP(OUTSTRING_FINAL, output);
						} catch (Exception e) {
							System.out.println(e.toString());
						}
					}
				};
				return Response.ok(stream).header("Content-Type", "application/json;charset=UTF-8").header("Content-Encoding", "gzip").build();
			} else {
				return Response.ok(OUTSTRING).header("Content-Type", "application/json;charset=UTF-8").build();
			}
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.SearchResource"))
					.header("Content-Type", "application/json;charset=UTF-8").build();
		}
	}

	private static OutputStream GZIP(String input, OutputStream baos) throws IOException {
		try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
			gzos.write(input.getBytes("UTF-8"));
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return baos;
	}

}
