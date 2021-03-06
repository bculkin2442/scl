package bjc.dicelang.scl.tokens;

import bjc.funcdata.ListEx;

/**
 * Represents a list of words.
 * 
 * @author student
 *
 */
public abstract class WordListSCLToken extends SCLToken {
	/**
	 * The list of words.
	 */
	public ListEx<SCLToken> tokenVals;

	/**
	 * Create a new word-list token.
	 * 
	 * @param isArray
	 *                Is this token an array.
	 * @param tokens
	 *                The tokens in the array.
	 */
	protected WordListSCLToken(boolean isArray, ListEx<SCLToken> tokens) {
		if (isArray) {
			type = TokenType.ARRAY;
		} else {
			type = TokenType.WORDS;
		}

		tokenVals = tokens;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((tokenVals == null) ? 0 : tokenVals.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WordListSCLToken other = (WordListSCLToken) obj;
		if (tokenVals == null) {
			if (other.tokenVals != null)
				return false;
		} else if (!tokenVals.equals(other.tokenVals))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WordsSCLToken [tokenVals=" + tokenVals + "]";
	}
}
