package models;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.Model;
import api.entities.ChoiceJSON;
import api.entities.Jsonable;
import api.entities.PollInstanceJSON;
import api.entities.PollJSON;

/**
 * Model class for a poll question. This is related to answer one-to-many
 * @author OpenARS Server API team
 */
@Entity
public class Poll extends Model implements Jsonable {
	private static final long serialVersionUID = 5276961463864101032L;
	
	/**
	 * The set of characters to use when generating admin keys.
	 */
	private static final String ADMINKEY_CHARSET = "0123456789abcdefghijklmnopqrstuvwxyz";
	/**
	 * The token of the poll (formerly known as pollID).
	 */
    @Required
    @Unique
    public String token;
    /**
     * The admin key, this is used to gain administrative access to the poll.
     */
    @ManyToOne
    @Required
    public User admin;
    
    /**
     * The question that the poll states, ex. "What is 2+2?"
     */
    @Column(columnDefinition="TEXT")
    public String question;
    /**
     * A human understandable reference that the teacher will use to
     * remember the relation of the poll, i.e. a course number, subject or alike.
     */
    public String reference;
    /**
     * Is it allowed to add multiple answers?
     */
    @Required
    public Boolean multipleAllowed;
    /**
     * Login required to vote?
     */
    @Required
    public Boolean loginRequired;
    /**
     * All the possible choices associated with the poll.
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "poll", fetch = FetchType.EAGER)
    public List<Choice> choices;
    /**
     * All the concrete instances of the poll.
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "poll")
    public List<PollInstance> instances;
    
    /**
     * Constructs a new Poll object.
     * This does not save it to the database, use the save method to do this.
     * 
     * @param token
     * @param question Text of the question
     * @param multipleAllowed whether there are multiple options allowed or not
                            v.count++;
     * @param email e-mail address of the poll creator
     */
    public Poll(String token, String question, Boolean multipleAllowed, Boolean loginRequired) {
        this.token = token;
        this.question = question;
        this.multipleAllowed = multipleAllowed;
        this.loginRequired = loginRequired;
    }
    
    /**
     * Constructor Copy for Polls, this method creates a Poll object 
     * with a new token and the same fields as the toCopy object.
     * @param token  -- New token for the new Poll
     * @param toCopy -- Poll object to be copied.
     */
    public Poll (String token, Poll toCopy) {
    	this.token = token;
    	this.multipleAllowed = toCopy.multipleAllowed;
        this.loginRequired = toCopy.loginRequired;
    	this.question = toCopy.question;
    	this.reference = toCopy.reference;
    	
    	this.choices = new LinkedList<Choice>();
		for(Choice c: toCopy.choices) {
			this.choices.add(Choice.copy(c));
		}
		// Update the references.
		for (Choice c : this.choices) {
					c.poll = this;
		}
    }

    /**
     * Activates the question for provided number of seconds.
     * If the question is in active state, it changes the activation duration,
     * otherwise it creates new voting round.
     * @param duration number of seconds to activate the question for
     * @return activated Question object - does not have to be used
     */
    public Poll activateFor(Date startDateTime, Date endDateTime) {
        if (isActive()) {
            PollInstance latestInstance = getLatestInstance();
            latestInstance.startDateTime = startDateTime;
            latestInstance.endDateTime = endDateTime;
            latestInstance.save();
        } else {
            new PollInstance(startDateTime, endDateTime, this).save();
        }
        return this;
    }

    /**
     * Gets latest voting round if it exists or null otherwise.
     * TODO: Implement this in a faster way, using a hybernate query.
     * @return PollInstance The latest instance of the poll (based on endDateTime)
     */
    @Deprecated
    public PollInstance getLatestInstance() {
        if (instances.isEmpty()) {
            return null;
        }
        Collections.sort(instances);
        int lastIndex = instances.size() - 1;
        return instances.get(lastIndex);
    }

    /**
     * Generates random string of alphanumerical characters.
     * @param length int length of generated string
     * @return String generated string
     */
    /*public void generateAdminKey(int length) {
        Random rand = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int pos = rand.nextInt(ADMINKEY_CHARSET.length());
            sb.append(ADMINKEY_CHARSET.charAt(pos));
        }
        adminKey = sb.toString();
    }*/

    /**
     * Gets array of answers as strings.
     * @return
     */
    public String[] getChoicesArray() {
        String[] array = new String[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            array[i] = choices.get(i).text;
        }
        return array;
    }

    /**
     * Can be used to determine if the question is activated or not
     * @return boolean activation status
     */
    public boolean isActive() {
        return timeRemaining() > 0;
    }

    /**
     * Returns remaining time for which the question is activated. This value
     * should be sent to the clients so that they can set the countdown. It is
     * also used by bethod isActive() to determine the activation state.
     * @return
     */
    public int timeRemaining() {
        PollInstance lastRound = getLatestInstance();
        if (lastRound == null) {
            return 0;
        }

        Date endTime = lastRound.endDateTime;
        Date currentTime = new Date(System.currentTimeMillis());

        int difference = (int) Math.ceil((endTime.getTime() - currentTime.getTime()) / 1000);
        return (difference > 0) ? difference : 0;
    }

    /**
     * Returns true when there has not been any voting done yet
     * @return true when there is no voting round
     */
    public boolean isFresh() {
        return getLatestInstance() == null;
    }

    /**
     * Gets vote counts as an array of integers. Used for statistics.
     * @return int[] array of vote counts / results
     */
    @Deprecated
    public int[] getVoteCounts() {
        int index = 0;
        int[] votes = new int[choices.size()];

        if (isFresh()) {
            return votes;
        }

        for (Choice choice : choices) {
            List<Vote> votesList = choice.latestVotes();
            if (votesList.isEmpty()) {
                votes[index] = 0;
            } else {
            	// FIXME: A new vote should be created instead of counting up the value.
                //votes[index] = votesList.get(0).count;
            }
            index++;
        }
        return votes;
    }
    
    /**
     * Turn this Poll into a PollJSON
     * @return A PollJSON object that represents this poll.
     */
    public PollJSON toJson() {
    	return toJson(this);
    }

    /**
     * Turn a Poll into a PollJSON
     * @param p the poll
     * @return A PollJSON object that represents the poll.
     */
    public static PollJSON toJson(Poll p) {
    	PollJSON result = new PollJSON();
    	result.id = p.id;
    	result.token = p.token;
    	result.reference = p.reference;
    	result.question = p.question;
    	result.multipleAllowed = p.multipleAllowed;
    	result.loginRequired = p.loginRequired;
    	if (p.admin != null) {
    		result.admin = p.admin.id;
    	}
    	result.choices = new LinkedList<ChoiceJSON>();
		for(Choice c: p.choices) {
			result.choices.add(c.toJson());
		}
    	result.pollinstances = new LinkedList<PollInstanceJSON>();
    	if(p.instances != null) {
			for(PollInstance pi: p.instances) {
				result.pollinstances.add(pi.toJson(false));
			}
    	}
		return result;
    }
    
    /**
     * Turn a Poll into a PollJSON
     * @return PollJSON A PollJSON object that represents this poll.
     */
    public static Poll fromJson(PollJSON json) {
    	Poll result = new Poll(json.token, json.question, json.multipleAllowed, json.loginRequired);
    	result.id = json.id;
    	result.token = json.token;
    	result.reference = json.reference;
    	result.question = json.question;
    	
    	result.choices = new LinkedList<Choice>();

		// Update the choices
    	if (json.choices != null) {
    		for (ChoiceJSON c: json.choices) {
    			result.choices.add(Choice.fromJson(c));	
    		}
    		// Update the references.
    		for (Choice c: result.choices) {
    			c.poll = result;
    		}
    	}
		// TODO: Check if we need to do this with other collections on the Poll as well.
		
		return result;
    }
    
	public String toString() {
		return "(#"+id+" / "+token+") \""+question+"\" (by #"+admin.id+" "+admin.name+")";
	}
}
