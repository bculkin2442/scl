package bjc.dicelang.scl;

import java.util.Arrays;

/**
 * Repository for error messages.
 *
 *
 * @author EVE
 */
/*
 * @TODO 10/08/17 Ben Culkin :ErrorRefactor
 * 
 * This way of handling error messages is not easy to deal with. Something else
 * needs to be done, but I'm not sure what at the moment.
 *
 * ADDENDA: 5/28/18 Ben Culkin
 * 	The error messages were moved into ErrorKey, as well as checking how
 * 	many arguments they expect. This is better than it was, but still mildly
 * 	annoying.
 *
 */
public class Errors {
	/**
	 * The types of error/warning message.
	 *
	 * Error messages are marked by starting with EK, warnings start with WK
	 *
	 * @author EVE
	 *
	 */
	public static enum ErrorKey {
		/* Stream Errors */
		/**
		 * Attempted to switch to a non-existant stream
		 */
		EK_STRM_NONEX("\tERROR: Fell off the stream list, attempting to move %s", 1),
		/**
		 * Can't delete the last stream
		 */
		EK_STRM_LAST("\tERROR: Cannot delete last remaining stream", 0),
		/**
		 * Unknown stream command
		 */
		EK_STRM_INVCOM("\tERROR: Unknown stream control command %s\n", 1),

		/* SCL Warnings */
		/**
		 * Word execution failed.
		 */
		WK_SCL_WRDFAIL("\tWARNING: Execution of word %s failed\n", 1),

		/* SCL Errors */
		/**
		 * Unknown SCL token
		 */
		EK_SCL_INVTOKEN("\tERROR: Unknown SCL token %s\n", 1),
		/**
		 * Mismatched quote in SCL command
		 */
		EK_SCL_MMQUOTE("\tERROR: Mismatched delimiter %s in SCL command\n", 1),
		/**
		 * Stack underflow in SCL command
		 */
		EK_SCL_SUNDERFLOW("\tERROR: Not enough items in stack for word %s (need at least %d)\n", 2),
		/**
		 * Unknown word in SCL command
		 */
		EK_SCL_UNWORD("\tERROR: Unknown word %s\n", 1),
		/**
		 * Invalid argument to SCL command
		 */
		EK_SCL_INVARG("\tERROR: Invalid argument to SCL command\n", 0);

		/**
		 * The message of the error.
		 */
		public final String msg;
		/**
		 * The number of arguments to the error.
		 */
		public final int argc;

		private ErrorKey(String message, int argcount) {
			msg = message;

			argc = argcount;
		}
	}
	/**
	 * The mode for the type of error messages to print out.
	 *
	 * @author EVE
	 *
	 */
	public static enum ErrorMode {
		/**
		 * Output error messages for wizards.
		 */
		WIZARD,
		/**
		 * Output error messages for developers.
		 */
		DEV
	}

	private ErrorMode mode;

	/**
	 * Print an error.
	 *
	 * @param key
	 *        The key of the error.
	 *
	 * @param args
	 *        The arguments for the error.
	 */
	public void printError(final ErrorKey key, final Object... args) {
		switch(mode) {
		case WIZARD:
			System.out.printf("\t? %d %s\n", key.ordinal(), Arrays.deepToString(args));
			break;
		case DEV:
			devError(key, args);
			break;
		default:
			System.out.printf("\tERROR ERROR: Unknown error mode %s\n", mode);
		}
	}

	private static void devError(final ErrorKey key, final Object[] args) {
		if(args.length != key.argc) {
			System.out.printf("\tERROR ERROR: Incorrect # of format arguments (got %d, expected %d)\n", args.length, key.argc);

			return;
		}

		System.out.printf(key.msg, args);
	}

	/**
	 * The instance of the errors.
	 */
	public final static Errors inst;

	static {
		inst = new Errors();

		inst.mode = ErrorMode.DEV;
	}
}
