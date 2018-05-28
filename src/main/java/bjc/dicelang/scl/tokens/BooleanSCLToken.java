package bjc.dicelang.scl.tokens;

/**
 * Represents a boolean token.
 * 
 * @author student
 *
 */
public class BooleanSCLToken extends SCLToken {
	/**
	 * The value of the token.
	 */
	public boolean boolVal;

	/**
	 * Create a new token.
	 * 
	 * @param val
	 *            The value of the token.
	 */
	public BooleanSCLToken(boolean val) {
		super(TokenType.BLIT);

		boolVal = val;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (boolVal ? 1231 : 1237);
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
		BooleanSCLToken other = (BooleanSCLToken) obj;
		if (boolVal != other.boolVal)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BooleanSCLToken [boolVal=" + boolVal + "]";
	}
}
