package de.uni_stuttgart.iaas.bpel.model.utilities.exceptions;

/**
 * AmbiguousPropertyForLinkException shows up when the given property of a link
 * matches to multiple links in process.
 * 
 * @since Feb 12, 2012
 * @author Daojun Cui
 */
public class AmbiguousPropertyForLinkException extends Exception {

	private static final long serialVersionUID = 2405793722331749822L;

	public AmbiguousPropertyForLinkException() {
		super();
	}

	public AmbiguousPropertyForLinkException(String message, Throwable cause) {
		super(message, cause);
	}

	public AmbiguousPropertyForLinkException(String message) {
		super(message);
	}

	public AmbiguousPropertyForLinkException(Throwable cause) {
		super(cause);
	}

}
