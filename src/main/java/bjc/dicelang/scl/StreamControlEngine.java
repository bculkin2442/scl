package bjc.dicelang.scl;

import static bjc.dicelang.scl.Errors.ErrorKey.EK_SCL_INVARG;
import static bjc.dicelang.scl.Errors.ErrorKey.EK_SCL_MMQUOTE;
import static bjc.dicelang.scl.Errors.ErrorKey.EK_SCL_SUNDERFLOW;
import static bjc.dicelang.scl.Errors.ErrorKey.EK_SCL_UNWORD;
import static bjc.dicelang.scl.Errors.ErrorKey.WK_SCL_WRDFAIL;
import static bjc.dicelang.scl.tokens.TokenType.ARRAY;
import static bjc.dicelang.scl.tokens.TokenType.ILIT;
import static bjc.dicelang.scl.tokens.TokenType.SYMBOL;
import static bjc.dicelang.scl.tokens.TokenType.WORDS;
import static bjc.dicelang.scl.tokens.WordType.NDROP;
import static bjc.dicelang.scl.tokens.WordType.NNIP;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bjc.dicelang.scl.tokens.ArraySCLToken;
import bjc.dicelang.scl.tokens.BooleanSCLToken;
import bjc.dicelang.scl.tokens.IntSCLToken;
import bjc.dicelang.scl.tokens.SCLToken;
import bjc.dicelang.scl.tokens.StringLitSCLToken;
import bjc.dicelang.scl.tokens.SymbolSCLToken;
import bjc.dicelang.scl.tokens.WordListSCLToken;
import bjc.dicelang.scl.tokens.WordSCLToken;
import bjc.dicelang.scl.tokens.WordsSCLToken;
import bjc.esodata.SimpleStack;
import bjc.esodata.Stack;
import bjc.funcdata.FunctionalList;
import bjc.funcdata.IList;
import bjc.utils.parserutils.TokenUtils;

/*
 * @TODO 10/08/17 Ben Culkin :SCLReorg
 * 
 * This is a large enough class that it should maybe be split into subclasses.
 */

/**
 * Runs a Stream Control Language (SCL) program.
 *
 * SCL is a stack-based concatenative language based mostly off of Postscript
 * and Factor, with inspiration from various other languages.
 *
 * @author Ben Culkin
 */
public class StreamControlEngine {
	/**
	 * Are we debugging or not?
	 */
	public final boolean debug = true;

	/* The stream engine we're hooked to. */
	private final StreamEngine eng;

	/* The current stack state. */
	private final Stack<SCLToken> curStack;

	/* Map of user defined words. */
	private final Map<String, SCLToken> words;

	/**
	 * Create a new stream control engine.
	 *
	 * @param engine
	 *            The engine to control.
	 */
	public StreamControlEngine(final StreamEngine engine) {
		eng = engine;

		words = new HashMap<>();
		curStack = new SimpleStack<>();
	}

	/**
	 * Run a SCL program.
	 *
	 * @param tokens
	 *            The program to run.
	 *
	 * @return Whether the program executed successfully.
	 */
	public boolean runProgram(final String[] tokens) {
		return runProgram(Arrays.asList(tokens).iterator());
	}

	/**
	 * Run a SCL program.
	 *
	 * @param tokens
	 *            The program to run.
	 *
	 * @return Whether the program executed successfully.
	 */
	public boolean runProgram(final Iterator<String> tokens) {
		while(tokens.hasNext()) {
			/* Tokenize each token. */
			final String token = tokens.next();
			final SCLToken tok = SCLToken.tokenizeString(token);

			if (tok == null) return false;

			/* Handle token types. */
			switch (tok.type) {
			case SQUOTE: {
				/* Handle single-quotes. */
				boolean succ = handleSingleQuote(tokens);
				if(!succ) return false;

				break;
			}
			case OBRACKET: {
				/* Handle delimited brackets. */
				boolean succ = handleDelim(tokens, "]");
				if (!succ) return false;

				break;
			}
			case OBRACE: {
				/* Handle delimited braces. */
				boolean succ = handleDelim(tokens, "}");
				if (!succ)
					return false;
				final SCLToken brak = curStack.pop();
				curStack.push(new ArraySCLToken(((WordListSCLToken) brak).tokenVals));
				break;
			}
			case WORD: {
				/* Handle built-in words. */
				if (!handleWord((WordSCLToken) tok)) {
					Errors.inst.printError(WK_SCL_WRDFAIL, tok);
				}
				break;
			}
			default:
				/* Put it onto the stack. */
				curStack.push(tok);
				break;
			}
		}

		return true;
	}

	private boolean handleWord(final WordSCLToken tk) {
		boolean succ = true;

		/* Handle each type of word. */
		/*
		 * @TODO 5/29/18 Ben Culkin :SCLWordDict
		 *
		 * This should probably use something other than a switch statement.
		 */
		switch (tk.wordVal) {
		case NEWSTREAM:
			eng.newStream();
			break;
		case LEFTSTREAM:
			succ = eng.leftStream();
			if (!succ) {
				return false;
			}
			break;
		case RIGHTSTREAM:
			succ = eng.rightStream();
			if (!succ) {
				return false;
			}
			break;
		case DELETESTREAM:
			succ = eng.deleteStream();
			if (!succ) {
				return false;
			}
			break;
		case MERGESTREAM:
			succ = eng.mergeStream();
			if (!succ) {
				return false;
			}
			break;
		case MAKEARRAY:
			succ = makeArray();
			if (!succ) {
				return false;
			}
			break;
		case MAKEEXEC:
			succ = toggleExec(true);
			if (!succ) {
				return false;
			}
			break;
		case MAKEUNEXEC:
			succ = toggleExec(false);
			if (!succ) {
				return false;
			}
			break;
		case STACKCOUNT:
			curStack.push(new IntSCLToken(curStack.size()));
			break;
		case STACKEMPTY:
			curStack.push(new BooleanSCLToken(curStack.isEmpty()));
			break;
		case DROP:
			if (curStack.size() == 0) {
				Errors.inst.printError(EK_SCL_SUNDERFLOW, tk.toString(), 1);

				return false;
			}
			curStack.drop();
			break;
		case NDROP:
			succ = handleNDrop();
			if (!succ) {
				return false;
			}
			break;
		case NIP:
			if (curStack.size() < 2) {
				Errors.inst.printError(EK_SCL_SUNDERFLOW, tk.toString(), 2);
				return false;
			}
			curStack.nip();
			break;
		case NNIP:
			succ = handleNNip();
			if (!succ) {
				return false;
			}
			break;
		case DEFINE:
			succ = handleDefine();
			if (!succ) {
				return false;
			}
			break;
		default:
			Errors.inst.printError(EK_SCL_UNWORD, tk.toString());
			return false;
		}

		return true;
	}

	private boolean handleDefine() {
		if (curStack.size() < 2) {
			Errors.inst.printError(EK_SCL_SUNDERFLOW, "def", 2);
			return false;
		}

		SCLToken name = curStack.pop();
		if (name.type != SYMBOL) {
			Errors.inst.printError(EK_SCL_INVARG, name.type.toString());
			return false;
		}
		String nam = ((SymbolSCLToken) name).stringVal;

		SCLToken def = curStack.pop();
		if (name.type != WORDS) {
			Errors.inst.printError(EK_SCL_INVARG, def.type.toString());
			return false;
		}

		words.put(nam, def);
		return false;
	}

	/* Handle nipping a specified number of items. */
	private boolean handleNNip() {
		final SCLToken num = curStack.pop();

		if (num.type != ILIT) {
			Errors.inst.printError(EK_SCL_INVARG, num.type.toString());
			return false;
		}

		final int n = (int) ((IntSCLToken) num).intVal;

		if (curStack.size() < n) {
			Errors.inst.printError(EK_SCL_SUNDERFLOW, NNIP.toString(), n);
			return false;
		}

		curStack.nip(n);
		return true;
	}

	/* Handle dropping a specified number of items. */
	private boolean handleNDrop() {
		final SCLToken num = curStack.pop();

		if (num.type != ILIT) {
			Errors.inst.printError(EK_SCL_INVARG, num.type.toString());
			return false;
		}

		final int n = (int) ((IntSCLToken) num).intVal;

		if (curStack.size() < n) {
			Errors.inst.printError(EK_SCL_SUNDERFLOW, NDROP.toString(), n);
			return false;
		}

		curStack.drop(n);
		return true;
	}

	/* Handle toggling the executable flag on an array. */
	private boolean toggleExec(final boolean exec) {
		final SCLToken top = curStack.top();

		if (exec) {
			if (top.type != ARRAY) {
				Errors.inst.printError(EK_SCL_INVARG, top.toString());
				return false;
			}

			top.type = WORDS;
		} else {
			if (top.type != WORDS) {
				Errors.inst.printError(EK_SCL_INVARG, top.toString());
				return false;
			}

			top.type = ARRAY;
		}

		return true;
	}

	/* Handle creating an array. */
	private boolean makeArray() {
		final SCLToken num = curStack.pop();

		if (num.type != ILIT) {
			Errors.inst.printError(EK_SCL_INVARG, num.type.toString());
		}

		final IList<SCLToken> arr = new FunctionalList<>();

		for (int i = 0; i < ((IntSCLToken) num).intVal; i++) {
			arr.add(curStack.pop());
		}

		curStack.push(new ArraySCLToken(arr));

		return true;
	}

	/* Handle a delimited series of tokens. */
	private boolean handleDelim(final Iterator<String> tokens, final String delim) {
		final IList<SCLToken> toks = new FunctionalList<>();

		if (!tokens.hasNext()) {
			Errors.inst.printError(EK_SCL_MMQUOTE, delim);

			return false;
		}

		String tok = tokens.next();

		while (!tok.equals(delim)) {
			final SCLToken ntok = SCLToken.tokenizeString(tok);

			switch (ntok.type) {
			case SQUOTE: {
				boolean succ = handleSingleQuote(tokens);
				if (!succ) return false;

				toks.add(curStack.pop());
				break;
			}
			case OBRACKET: {
				boolean succ = handleDelim(tokens, "]");
				if (!succ) return false;
				toks.add(curStack.pop());
				break;
			}
			case OBRACE: {
				boolean succ = handleDelim(tokens, "}");
				if (!succ) return false;

				final SCLToken brak = curStack.pop();
				toks.add(new ArraySCLToken(((WordListSCLToken) brak).tokenVals));
				break;
			}
			default:
				toks.add(ntok);
			}

			if (!tokens.hasNext()) {
				Errors.inst.printError(EK_SCL_MMQUOTE, delim);

				return false;
			}

			tok = tokens.next();
		}

		/* Skip the closing delimiter */
		tokens.next();

		/*
		 * @NOTE
		 *
		 * Instead of being hardcoded, this should be a parameter.
		 */
		curStack.push(new WordsSCLToken(toks));

		return true;
	}

	/* Handle a single-quoted string. */
	private boolean handleSingleQuote(final Iterator<String> tokens) {
		final StringBuilder sb = new StringBuilder();

		if (!tokens.hasNext()) {
			Errors.inst.printError(EK_SCL_MMQUOTE, "'");

			return false;
		}

		String tok = tokens.next();

		while (!tok.equals("'")) {
			if (tok.matches("\\\\+'")) {
				/* Handle escaped quotes. */
				sb.append(tok.substring(1));
			} else {
				sb.append(tok);
			}

			/* Move to the next token */
			if (!tokens.hasNext()) {
				Errors.inst.printError(EK_SCL_MMQUOTE, "'");

				return false;
			}

			tok = tokens.next();
		}

		/*
		 * Skip the single quote
		 */
		//tokens.next();

		String strang = TokenUtils.descapeString(sb.toString());

		curStack.push(new StringLitSCLToken(strang));

		return true;
	}
}
