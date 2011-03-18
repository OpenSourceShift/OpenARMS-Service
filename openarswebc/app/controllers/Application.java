package controllers;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import play.mvc.Controller;
import Utility.RestClient;

import com.google.gson.Gson;

public class Application extends Controller {

    public static void index() {
        render();
    }
    
    public static void joinpoll(String id) throws JSONException {
    	JSONObject questionJSON = RestClient.getInstance().getQuestion();
       	String pollID = questionJSON.getString("pollID");
		String questionID = questionJSON.getString("questionID");
		String question = questionJSON.getString("question");
		JSONArray answersArray = questionJSON.getJSONArray("answers");
		//String multipleAllowed = questionJSON.getString("multipleAllowed");
		String duration = questionJSON.getString("duration");
			
		render(id, pollID, questionID, question, answersArray, duration);
	}
    
    public static void managepoll() {
        render();
    }
}