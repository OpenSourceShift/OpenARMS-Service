package api.requests;

import api.responses.EmptyResponse;
import api.responses.Response;

/**
 * A request for the service: Deletes a vote
 */
public class DeleteVoteRequest extends Request {
	public Long vote_id;
	public DeleteVoteRequest (Long l) {
		this.vote_id = l;
	}
	@Override
	public String getURL() {
		return "/vote/" + vote_id;
	}
	@Override
	public Class<? extends Response> getExpectedResponseClass() {
		return EmptyResponse.class;
	}
	@Override
	public Method getHttpMethod() {
		return Method.DELETE;
	}
}
