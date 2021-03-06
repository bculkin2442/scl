package bjc.dicelang.scl;

import static bjc.dicelang.scl.Errors.ErrorKey.*;

import java.util.*;
import java.util.function.*;

import bjc.esodata.*;
import bjc.funcdata.*;
import bjc.utils.funcutils.*;

/**
 * Implements multiple interleaved parse streams, as well as a command language
 * for the streams.
 *
 * The idea for the interleaved streams came from the language Oozylbub &amp;
 * Murphy, but the command language was my own idea.
 *
 * @author Ben Culkin
 */
public class StreamEngine {
	/**
	 * Whether or not we're doing debugging.
	 */
	public final boolean debug = true;

	/* Our streams. */
	private Tape<ListEx<String>> streams;
	private ListEx<String> currStream;

	/* Saved streams */
	//private Map<String, IList<String>> savedStreams;

	/* Handler for SCL programs */
	private final StreamControlEngine scleng;

	private static MapEx<Character, Predicate<StreamEngine>> commands;

	static {
		commands = new FunctionalMap<>();

		commands.put('+', (eng) -> {
			eng.newStream();
			return true;
		});

		commands.put('>', (eng) -> eng.rightStream());
		commands.put('<', (eng) -> eng.leftStream());
		commands.put('-', (eng) -> eng.deleteStream());
		commands.put('M', (eng) -> eng.mergeStream());
		commands.put('L', (eng) -> {
			String[] arr = eng.currStream.toArray(new String[0]);

			if(eng.debug)
				System.out.printf("\tDEBUG: Executing '%s' as SCL program\n", eng.currStream);

			boolean succ = eng.scleng.runProgram(arr);

			return succ;
		});
	}

	/**
	 * Create a new stream engine.
	 *
	 */
	public StreamEngine() {
		//savedStreams = new HashMap<>();
		scleng = new StreamControlEngine(this);
	}

	/* Do pre-run (re)initialization. */
	private void init() {
		/* Reinitialize our list of streams. */
		streams = new SingleTape<>();

		/* Create an initial stream. */
		currStream = new FunctionalList<>();
		streams.insertBefore(currStream);
	}

	/**
	 * Process a possibly interleaved set of streams.
	 *
	 * @param toks
	 *        The raw token to read streams from.
	 *
	 * @param dest
	 *        The list to write the final stream to.
	 *
	 * @return Whether or not the streams were successfully processed.
	 */
	public boolean doStreams(final String[] toks, final ListEx<String> dest) {
		return doStreams(Arrays.asList(toks), dest);
	}

	/**
	 * Process a possibly interleaved set of streams.
	 *
	 * @param toks
	 *        The raw token to read streams from.
	 *
	 * @param dest
	 *        The list to write the final stream to.
	 *
	 * @return Whether or not the streams were successfully processed.
	 */
	public boolean doStreams(final Iterable<String> toks, final ListEx<String> dest) {
		/* Initialize per-run state. */
		init();

		/* Are we currently quoting things? */
		boolean quoteMode = false;

		/* Process each token. */
		for(final String tk : toks) {
			/* Process stream commands. */
			if(tk.startsWith("{@S") && !quoteMode) {
				if(tk.equals("{@SQ}")) {
					if(debug)
						System.out.println("\tDEBUG: Enabling quote mode\n");
					/* Start quoting. */
					quoteMode = true;
				} else if(!processCommand(tk)) {
					return false;
				}
			} else {
				if(tk.equals("{@SU}")) {
					if(debug)
						System.out.println("\tDEBUG: Disabling quote mode\n");
					/* Stop quoting. */
					quoteMode = false;
				} else if(tk.startsWith("\\") && tk.endsWith("{@SU}")) {
					/* Unquote quoted end. */
					currStream.add(tk.substring(1));
				} else {
					currStream.add(tk);
				}
			}
		}

		for(final String tk : currStream) {
			/* Collect tokens from the current stream. */
			dest.add(tk);
		}

		return true;
	}

	/** Create a new stream. */
	public void newStream() {
		streams.insertAfter(new FunctionalList<>());
	}

	/**
	 * Move to a stream to the right.
	 *
	 * @return Whether or not the move was successful.
	 */
	public boolean rightStream() {
		if(!streams.right()) {
			Errors.inst.printError(EK_STRM_NONEX, "right");
			return false;
		}

		currStream = streams.item();
		return true;
	}

	/**
	 * Move to a stream to the left.
	 *
	 * @return Whether or not the move was successful.
	 */
	public boolean leftStream() {
		if(!streams.left()) {
			Errors.inst.printError(EK_STRM_NONEX, "left");
			return false;
		}

		currStream = streams.item();
		return true;
	}

	/**
	 * Delete the current stream.
	 *
	 * @return Whether or not the delete succeeded.
	 */
	public boolean deleteStream() {
		if(streams.size() == 1) {
			Errors.inst.printError(EK_STRM_LAST);
			return false;
		}

		streams.remove();
		currStream = streams.item();

		return true;
	}

	/**
	 * Merge the current stream into the previous stream.
	 *
	 * @return Whether or not the merge succeded.
	 */
	public boolean mergeStream() {
		if(streams.size() == 1) {
			Errors.inst.printError(EK_STRM_LAST);
			return false;
		}

		final ListEx<String> stringLit = streams.remove();
		currStream = streams.item();

		final String merg = ListUtils.collapseTokens(stringLit, "");

		if(debug)
			System.out.printf("\tDEBUG: Merging string '%s'\n", merg);

		currStream.add(merg);

		return true;
	}

	/*
	 * Process an SCL command.
	 *
	 * These are single-character requests, but they can be chorded
	 * together.
	 *
	 * For example, the command {@SM} executes the M command, while the
	 * command {@SNS} would execute the N command, then the S command.
	 */
	private boolean processCommand(final String tk) {
		char[] comms = null;

		if(tk.length() > 5) {
			/* Pull off {@S and closing } */
			comms = tk.substring(3, tk.length() - 1).toCharArray();
		} else {
			/* Its a single char. command. */
			comms = new char[1];
			comms[0] = tk.charAt(3);
		}

		/* Process each command. */
		for(final char comm : comms) {
			boolean succ = commands.get(comm)
					.orElseGet(() -> (eng) -> {
						Errors.inst.printError(EK_STRM_INVCOM, tk);

						return false;
					}).test(this);

			if(!succ) return false;
		}

		return true;
	}
}
