package edu.ncsu.store.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class WikipediaExample {

	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }

	  public static JsonObject readJsonFromUrl(String url) throws IOException {
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      JsonParser jp = new JsonParser();
	      JsonElement element = jp.parse(jsonText);
	      JsonObject json = element.getAsJsonObject();
	      return json;
	    } finally {
	      is.close();
	    }
	  }

	 
	private static List<String> getArticleRevisions(JsonObject response) {
		
		List<String> revisionIdList = new LinkedList<>();
		JsonObject query = response.getAsJsonObject("query");
		JsonObject pages = query.getAsJsonObject("pages");
		JsonObject pageNo = pages.getAsJsonObject("17100774");
		JsonArray revisions = pageNo.getAsJsonArray("revisions");
		for(JsonElement revision : revisions){
			JsonObject revisionObject = revision.getAsJsonObject();
			
			JsonPrimitive revid = revisionObject.getAsJsonPrimitive("revid");
			
			revisionIdList.add(revid.getAsString());
		}
		return revisionIdList;
	}

	private static long getTimeStamp(String date){
		String pattern = "yyyy-MM-dd'T'HH:mm:ssZ";
		DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
		
		Long timestamp = dtf.parseMillis(date);
		System.out.println(timestamp);
		return timestamp;
	}
	private static void extractContentAndTimestamp(JsonObject response) throws Exception {
		
		ImmutableStore store = new ImmutableStore();
		JsonObject query = response.getAsJsonObject("query");
		JsonObject pages = query.getAsJsonObject("pages");
		JsonObject pageNo = pages.getAsJsonObject("17100774");
		JsonArray revisions = pageNo.getAsJsonArray("revisions");
		for(JsonElement revision : revisions){
			JsonObject revisionObject = revision.getAsJsonObject();
			
			JsonPrimitive content = revisionObject.getAsJsonPrimitive("*");
			JsonPrimitive timestamp = revisionObject.getAsJsonPrimitive("timestamp");
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			String formattedDate = timestamp.toString().substring(1, timestamp.toString().length() - 1);
			long insertionTimestamp = getTimeStamp(formattedDate);
			
			store.put("Berkeley_Levett", content.toString().getBytes(), insertionTimestamp);
		}
	}
	
	private static void populateDB(List<String> revisionIdList) throws Exception {
		Collections.reverse(revisionIdList);
		for(String revisionId : revisionIdList){
			String url = "https://en.wikipedia.org/w/api.php?action=query&titles=Berkeley_Levett&format=json&prop=revisions&rvlimit=max&rvprop=ids|size|content|timestamp&rvstartid=" + revisionId + "&rvendid=" + revisionId;
			try {
				JsonObject response = readJsonFromUrl(url);
				extractContentAndTimestamp(response);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	

	public static void main(String[] args) throws Exception{
		List<String> revisionIdList = new LinkedList<>();
		JsonObject response;
			response = readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&titles=Berkeley_Levett&format=json&prop=revisions&rvlimit=max&rvprop=ids|size|timestamp");
			revisionIdList = getArticleRevisions(response);
			populateDB(revisionIdList);
		
		
	}

	

}
