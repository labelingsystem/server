package v1.rest;

import exceptions.Logging;
import exceptions.WaybacklinkException;
import v1.utils.config.ConfigProperties;
import v1.utils.retcat.RetcatItems;
import v1.utils.db.SQlite;
import v1.utils.retcat.SuggestionItem;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import v1.utils.retcat.RetcatItem;
import v1.utils.retcat.Retcat_ChronOntology;
import v1.utils.retcat.Retcat_Dbpedia;
import v1.utils.retcat.Retcat_Fao;
import v1.utils.retcat.Retcat_Finto;
import v1.utils.retcat.Retcat_GeoNames;
import v1.utils.retcat.Retcat_Getty;
import v1.utils.retcat.Retcat_HTML;
import v1.utils.retcat.Retcat_HeritageData;
import v1.utils.retcat.Retcat_LabelingSystem;
import v1.utils.retcat.Retcat_Pleiades;
import v1.utils.retcat.Retcat_Unesco;

@Path("/retcat")
public class RetcatResource {

    private static final int LIMIT = 20;

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getRetcatList() {
        try {
            JSONArray outArray = new JSONArray();
            // add items
            for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                JSONObject tmpRETCAT = new JSONObject();
                tmpRETCAT.put("name", item.getName());
                tmpRETCAT.put("description", item.getDescription());
                tmpRETCAT.put("queryURL", item.getQueryURL());
                tmpRETCAT.put("labelURL", item.getLabelURL());
                tmpRETCAT.put("prefix", item.getPrefix());
                tmpRETCAT.put("group", item.getGroup());
                tmpRETCAT.put("type", item.getType());
                tmpRETCAT.put("language", item.getLanguage());
                tmpRETCAT.put("quality", item.getQuality());
                outArray.add(tmpRETCAT);
            }
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/{retcat}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getRetcatDetails(@PathParam("retcat") String retcat) {
        try {
            JSONArray outArray = new JSONArray();
            for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                if (item.getName().equals(retcat)) {
                    JSONObject tmpRETCAT = new JSONObject();
                    tmpRETCAT.put("name", item.getName());
                    tmpRETCAT.put("description", item.getDescription());
                    tmpRETCAT.put("queryURL", item.getQueryURL());
                    tmpRETCAT.put("labelURL", item.getLabelURL());
                    tmpRETCAT.put("prefix", item.getPrefix());
                    tmpRETCAT.put("group", item.getGroup());
                    tmpRETCAT.put("type", item.getType());
                    tmpRETCAT.put("language", item.getLanguage());
                    tmpRETCAT.put("quality", item.getQuality());
                    outArray.add(tmpRETCAT);
                }
            }
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/qualities")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getRetcatQualities() {
        try {
            JSONArray outArray = new JSONArray();
            HashSet<String> qualityList = new HashSet();
            for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                qualityList.add(item.getQuality());
            }
            for (String item : qualityList) {
                outArray.add(item);
            }
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/groups")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getRetcatGroups() {
        try {
            JSONArray outArray = new JSONArray();
            HashSet<String> groupList = new HashSet();
            for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                groupList.add(item.getGroup());
            }
            for (String item : groupList) {
                outArray.add(item);
            }
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/vocabulary/{vocabulary}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getRetcatListByVocabulary(@PathParam("vocabulary") String vocabulary) {
        try {
            // output json
            JSONArray outArray = new JSONArray();
            // get retcat items
            String newRetcatString = SQlite.getRetcatByVocabulary(vocabulary);
            if (newRetcatString != null) {
                String[] retcatItems = newRetcatString.split(",");
                // set data
                for (String vocItem : retcatItems) {
                    for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                        if (item.getName().equals(vocItem)) {
                            JSONObject tmpRETCAT = new JSONObject();
                            tmpRETCAT.put("name", item.getName());
                            tmpRETCAT.put("description", item.getDescription());
                            tmpRETCAT.put("queryURL", item.getQueryURL());
                            tmpRETCAT.put("labelURL", item.getLabelURL());
                            tmpRETCAT.put("prefix", item.getPrefix());
                            tmpRETCAT.put("group", item.getGroup());
                            tmpRETCAT.put("type", item.getType());
                            tmpRETCAT.put("language", item.getLanguage());
                            tmpRETCAT.put("quality", item.getQuality());
                            outArray.add(tmpRETCAT);
                        }
                    }
                }
            }
            // add own vocabulary
            JSONObject tmpRETCAT = new JSONObject();
            tmpRETCAT.put("name", "this." + vocabulary);
            tmpRETCAT.put("description", "this vocabulary");
            outArray.add(tmpRETCAT);
            // output
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @POST
    @Path("/vocabulary/{vocabulary}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response setRetcatForVocabulary(@PathParam("vocabulary") String vocabulary, String json) {
        try {
            // db string
            String retcatString = "";
            // parse json
            JSONArray retcatArray = (JSONArray) new JSONParser().parse(json);
            for (Object itemObject : retcatArray) {
                JSONObject tmpObject = (JSONObject) itemObject;
                retcatString += tmpObject.get("name") + ",";
            }
            retcatString = retcatString.substring(0, retcatString.length() - 1);
            // sqlite db action
            SQlite.deleteRetcatEntry(vocabulary);
            SQlite.insertRetcatString(vocabulary, retcatString);
            String newRetcatString = SQlite.getRetcatByVocabulary(vocabulary);
            String[] retcatItems = newRetcatString.split(",");
            // output json
            JSONArray outArray = new JSONArray();
            // set data
            for (String vocItem : retcatItems) {
                for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                    if (item.getName().equals(vocItem)) {
                        JSONObject tmpRETCAT = new JSONObject();
                        tmpRETCAT.put("name", item.getName());
                        tmpRETCAT.put("description", item.getDescription());
                        tmpRETCAT.put("queryURL", item.getQueryURL());
                        tmpRETCAT.put("labelURL", item.getLabelURL());
                        tmpRETCAT.put("prefix", item.getPrefix());
                        tmpRETCAT.put("group", item.getGroup());
                        tmpRETCAT.put("type", item.getType());
                        tmpRETCAT.put("language", item.getLanguage());
                        tmpRETCAT.put("quality", item.getQuality());
                        outArray.add(tmpRETCAT);
                    }
                }
            }
            // add own vocabulary
            JSONObject tmpRETCAT = new JSONObject();
            tmpRETCAT.put("name", "this." + vocabulary);
            tmpRETCAT.put("description", "this vocabulary");
            outArray.add(tmpRETCAT);
            // output
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @PUT
    @Path("/vocabulary/{vocabulary}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response updateRetcatForVocabulary(@PathParam("vocabulary") String vocabulary, String json) {
        try {
            // db string
            String retcatString = "";
            // parse json
            JSONArray retcatArray = (JSONArray) new JSONParser().parse(json);
            for (Object itemObject : retcatArray) {
                JSONObject tmpObject = (JSONObject) itemObject;
                retcatString += tmpObject.get("name") + ",";
            }
            retcatString = retcatString.substring(0, retcatString.length() - 1);
            // sqlite db action
            SQlite.deleteRetcatEntry(vocabulary);
            SQlite.insertRetcatString(vocabulary, retcatString);
            String newRetcatString = SQlite.getRetcatByVocabulary(vocabulary);
            String[] retcatItems = newRetcatString.split(",");
            // output json
            JSONArray outArray = new JSONArray();
            // set data
            for (String vocItem : retcatItems) {
                for (RetcatItem item : RetcatItems.getAllRetcatItems()) {
                    if (item.getName().equals(vocItem)) {
                        JSONObject tmpRETCAT = new JSONObject();
                        tmpRETCAT.put("name", item.getName());
                        tmpRETCAT.put("description", item.getDescription());
                        tmpRETCAT.put("queryURL", item.getQueryURL());
                        tmpRETCAT.put("labelURL", item.getLabelURL());
                        tmpRETCAT.put("prefix", item.getPrefix());
                        tmpRETCAT.put("group", item.getGroup());
                        tmpRETCAT.put("type", item.getType());
                        tmpRETCAT.put("language", item.getLanguage());
                        tmpRETCAT.put("quality", item.getQuality());
                        outArray.add(tmpRETCAT);
                    }
                }
            }
            // add own vocabulary
            JSONObject tmpRETCAT = new JSONObject();
            tmpRETCAT.put("name", "this." + vocabulary);
            tmpRETCAT.put("description", "this vocabulary");
            outArray.add(tmpRETCAT);
            // output
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/waybacklink")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getWaybackLink(@QueryParam("url") String url) {
        try {
            // check if url is item in retcat
            List<RetcatItem> retcatlist = RetcatItems.getAllRetcatItems();
            for (RetcatItem item : retcatlist) {
                if (url.contains(item.getPrefix())) {
                    throw new WaybacklinkException("item match in reference thesaurus catalog");
                }
            }
            // get waybacklink
            URL obj = new URL(ConfigProperties.getPropertyParam("waybackapi").replace("$url", url));
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            String urlParameters = "";
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
            JSONObject jsonOut = new JSONObject();
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(response.toString());
            JSONObject resultsObject = (JSONObject) jsonObject.get("archived_snapshots");
            JSONObject resultsObject2 = (JSONObject) resultsObject.get("closest");
            String wburl = null;
            try {
                wburl = (String) resultsObject2.get("url");
            } catch (Exception e) {
                throw new WaybacklinkException("no wayback url available");
            }
            jsonOut.put("url", wburl);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/labelingsystem")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsLabelingSystem(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_LabelingSystem.queryAll(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/labelingsystem/{vocabulary}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsLabelingSystemVocabulary(@QueryParam("query") String searchword, @PathParam("vocabulary") String vocabulary) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_LabelingSystem.queryVocab(searchword, vocabulary);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/labelingsystem")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoLabelingSystem(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_LabelingSystem.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/getty/aat")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsGettyAAT(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Getty.queryAAT(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/getty/tgn")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsGettyTGN(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Getty.queryTGN(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/getty/ulan")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsGettyULAN(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Getty.queryULAN(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/getty")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoGetty(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_Getty.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/heritagedata/historicengland")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsHeritagedataHE(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_HeritageData.queryHE(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/heritagedata/rcahms")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsHeritagedataRCAHMS(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_HeritageData.queryRCAHMS(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/heritagedata/rcahmw")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsHeritagedataRCAHMW(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_HeritageData.queryRCAHMW(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/heritagedata")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoHeritageData(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_HeritageData.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/chronontology")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsCHRONONTOLOGY(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_ChronOntology.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/chronontology")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoChronontology(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_ChronOntology.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/pelagiospleiadesplaces")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsPELAGIOS(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Pleiades.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/pelagiospleiadesplaces")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoPelagios(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_Pleiades.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/skosmos/finto")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoSkosmosFinto(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_Finto.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/skosmos/finto")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsSkosmosFINTO(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Finto.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/skosmos/fao")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsSkosmosFAO(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Fao.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/skosmos/fao")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoSkosmosFao(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_Fao.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/skosmos/unesco")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsSkosmosUNESCO(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Unesco.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/skosmos/unesco")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoSkosmosUnesco(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_Unesco.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/dbpedia")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsDBPEDIA(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_Dbpedia.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/dbpedia")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoDBpedia(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_Dbpedia.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/geonames")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsGEONAMES(@QueryParam("query") String searchword) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_GeoNames.query(searchword);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/geonames")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoGeoNames(@QueryParam("url") String url) {
        try {
            JSONObject jsonOut = Retcat_GeoNames.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/query/html")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getQueryResultsHTML(@QueryParam("query") String url) {
        try {
            Map<String, SuggestionItem> autosuggests = Retcat_HTML.query(url);
            JSONArray outArray = new JSONArray();
            outArray = fillOutputJSONforQuery(autosuggests);
            return Response.ok(outArray).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    @GET
    @Path("/info/html")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getInfoExtern(@QueryParam("url") String url, @QueryParam("type") String type) {
        try {
            JSONObject jsonOut = Retcat_HTML.info(url);
            return Response.ok(jsonOut).header("Content-Type", "application/json;charset=UTF-8").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Logging.getMessageJSON(e, "v1.rest.RetcatResource"))
                    .header("Content-Type", "application/json;charset=UTF-8").build();
        }
    }

    private JSONArray fillOutputJSONforQuery(Map<String, SuggestionItem> autosuggests) {
        JSONArray outArray = new JSONArray();
        int i = 0;
        for (Map.Entry<String, SuggestionItem> entry : autosuggests.entrySet()) {
            if (i < LIMIT) {
                SuggestionItem tmpAS = entry.getValue();
                JSONObject suggestionObject = new JSONObject();
                // url
                suggestionObject.put("uri", tmpAS.getURL());
                // labels
                suggestionObject.put("label", tmpAS.getLabels().iterator().next());
                // scheme
                suggestionObject.put("scheme", tmpAS.getSchemeTitle());
                // descriptions
                if (tmpAS.getDescriptions().size() > 0) {
                    suggestionObject.put("description", tmpAS.getDescriptions().iterator().next());
                } else {
                    suggestionObject.put("description", "");
                }
                // language
                suggestionObject.put("lang", tmpAS.getLanguage());
                // type
                suggestionObject.put("type", tmpAS.getType());
                // group
                suggestionObject.put("group", tmpAS.getGroup());
                // quality
                suggestionObject.put("quality", tmpAS.getQuality());
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
                // narrrower
                Set narrrowerTerms = tmpAS.getNarrowerTerms();
                JSONArray narrrowerArrayNew = new JSONArray();
                if (narrrowerTerms.size() > 0) {
                    for (Object element : narrrowerTerms) {
                        Map hm = (Map) element;
                        Iterator entries = hm.entrySet().iterator();
                        while (entries.hasNext()) {
                            Map.Entry thisEntry = (Map.Entry) entries.next();
                            String key = (String) thisEntry.getKey();
                            String value = (String) thisEntry.getValue();
                            JSONObject narrrowerObjectTmp = new JSONObject();
                            narrrowerObjectTmp.put("uri", key);
                            narrrowerObjectTmp.put("label", value);
                            narrrowerArrayNew.add(narrrowerObjectTmp);
                        }
                    }
                }
                suggestionObject.put("narrrowerTerms", narrrowerArrayNew);
                // add information to output array
                outArray.add(suggestionObject);
                i++;
            }
        }
        return outArray;
    }

    public static int getLimit() {
        return LIMIT;
    }

}