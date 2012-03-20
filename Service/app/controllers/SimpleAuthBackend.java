package controllers;

import java.util.List;

import api.entities.UserJSON;
import api.helpers.GsonHelper;
import api.responses.CreateUserResponse;

import notifiers.MailNotifier;

import models.SimpleUserAuthBinding;
import models.User;
import play.*;
import play.mvc.*;
import play.mvc.Http.*;

/**
 * Controller which specifies simple authentication method.
 * @author OpenARMS Service team
 */
public class SimpleAuthBackend extends AuthBackend {
	/**
	 * Method that authenticates the user to access the system.
	 * @return true if user authenticated and false otherwise
	 */
	public static User authenticate(User user) {
	    // Find correct user in the DB
	    User u = null;
	    u = (User)User.find("name",user.name).first();
	    if (u != null) {
	    	Logger.debug("authenticate() found user: %s", u.toString());
	    	SimpleUserAuthBinding auth = (SimpleUserAuthBinding)u.userAuth;
	    	Logger.debug("Got password: %s", ((SimpleUserAuthBinding)user.userAuth).password);
	    	String s = auth.authenticate(((SimpleUserAuthBinding)user.userAuth).password);
	    	u.secret = s;
	    	u.save();
	    	Logger.debug("Got secret: %s Saved secret: %s", s, user.secret);
		}
	    return u; 
	}
	
	/**
	 * Method that resets the password of the user and sends it to user via email.
	 */
	@Override
	public void resetPassword() {
		User user = null;
		Header header = Http.Request.current().headers.get("passreset");
		if (header != null) {
			user = (User)User.find("name", Http.Request.current().user).fetch().get(0);
			if (user != null) {
				((SimpleUserAuthBinding)user.userAuth).generatePassword();
				MailNotifier.sendPassword(user);
			}
		}
	}
	
	
}
