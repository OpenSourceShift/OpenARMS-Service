package api.responses;

import models.Poll;
import models.PollInstance;
import models.Vote;
import api.entities.PollInstanceJSON;
import api.entities.PollJSON;
import api.entities.VoteJSON;


public class UpdateVoteResponse extends Response {
	public VoteJSON vote;
	public UpdateVoteResponse() {
	}
	public UpdateVoteResponse(Vote v) {
		this(v.toJson());
	}
	public UpdateVoteResponse(VoteJSON json) {
		this.vote = json;
	}
}