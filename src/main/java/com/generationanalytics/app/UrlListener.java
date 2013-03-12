package com.generationanalytics.app;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
//import javax.servlet.annotation.WebServlet;

import com.mongodb.*;
import com.mongodb.util.JSON;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.*;

//@WebServlet("/UrlListener")
public class UrlListener extends HttpServlet {

    public BasicDBObject clickFromArticle(String code, String userId, DB db, HttpServletResponse response)
    throws MongoException, JSONException, IOException
    {
	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	Date curDate = new Date();
	
	DBCollection source = db.getCollection("articles");

	// Look up code to get corresponding object
	BasicDBObject query = new BasicDBObject("c", code);
	DBCursor cursor = source.find(query);
	
	// Throw exception if there are multiple matches or if there are no matches
	// TODO: If there are 0 matches, handle error differently
	if (cursor.count() > 1)
	{ throw new MongoException("Multiple article matches for one URL"); }
	DBObject result = cursor.next();
	cursor.close();
		
	// Use the code to get the item URL and articleId
	JSONObject item = new JSONObject(JSON.serialize(result));
	String[] parts = item.getString("_id").split("\"");
	String articleId = parts[3];

	// Create a new click object to record the user's click
	BasicDBObject click = new BasicDBObject("uid", userId).	
		append("aid", articleId).
		append("ct", dateFormat.format(curDate));

	// Redirect to the URL
	response.sendRedirect(response.encodeRedirectURL(item.getString("url")));

	return click;
    }

    public BasicDBObject clickFromWebpage(String code, String userId, DB db, HttpServletResponse response)
    throws MongoException, JSONException, IOException
    {
	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	Date curDate = new Date();
	
	DBCollection source = db.getCollection("webpages");

	// Look up code to get corresponding object
	BasicDBObject query = new BasicDBObject("c", code);
	DBCursor cursor = source.find(query);
	
	// Throw exception if there are multiple matches or if there are no matches
	// TODO: If there are 0 matches, handle error differently
	if (cursor.count() > 1)
	{ throw new MongoException("Multiple webpage matches for one URL"); }
	DBObject result = cursor.next();
	cursor.close();
		
	// Use the code to get the item URL and webpageId
	JSONObject item = new JSONObject(JSON.serialize(result));
	String[] parts = item.getString("_id").split("\"");
	String webpageId = parts[3];
	
	// Create a new click object to record the user's click
	BasicDBObject click = new BasicDBObject("uid", userId).	
		append("wid", webpageId).
		append("ct", dateFormat.format(curDate));

	// Redirect to the URL
	response.sendRedirect(response.encodeRedirectURL(item.getString("url")));

	return click;
    }
    
    public BasicDBObject clickFromAd(String codeAsUrl, String userId, DB db, HttpServletResponse response)
    throws MongoException, JSONException, IOException
    {
	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	Date curDate = new Date();

	// TODO: Store ad objects in a collection?
	
	// Create a new click object to record the user's click
	BasicDBObject click = new BasicDBObject("uid", userId).	
		append("did", codeAsUrl).
		append("ct", dateFormat.format(curDate));

	// Redirect to the URL
	response.sendRedirect(response.encodeRedirectURL(codeAsUrl));

	return click;
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
 	String code = request.getParameter("code");
	String content_type = request.getParameter("content");
	String userId = request.getParameter("uid");
	BasicDBObject click;

	try
	{
		MongoClient mongoClient = new MongoClient();
		DB db = mongoClient.getDB("db");	
		DBCollection clickTracker = db.getCollection("user_clicks");

		if (content_type.equals("a"))
		{ click = clickFromArticle(code, userId, db, response); }
		else if (content_type.equals("w"))
		{ click = clickFromWebpage(code, userId, db, response); }
		else if (content_type.equals("d"))
		{ click = clickFromAd(code, userId, db, response); }
		else
		{
			System.out.println("ERROR: Bad content type " + content_type);
			throw new ServletException();
		}
		
		clickTracker.insert(click);
		
	}
	catch (MongoException e)
	{ e.printStackTrace(); }

	catch (ServletException e)
	{ e.printStackTrace(); }

	catch (JSONException e)
	{ e.printStackTrace(); }

	catch (IOException e)
	{ 
		e.printStackTrace();
		response.sendRedirect(response.encodeRedirectURL("http://www.biffle.co"));
	}

    }
}

