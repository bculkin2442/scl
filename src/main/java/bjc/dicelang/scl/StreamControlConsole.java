package bjc.dicelang.scl;

import java.util.Scanner;

import bjc.funcdata.FunctionalList;
import bjc.funcdata.ListEx;

/**
 * Implement a SCL REPL
 *
 * @author Ben Culkin
 */
public class StreamControlConsole {
	/*
	 * @TODO 10/08/17 :SCLArgs
	 * 
	 * Do something useful with the CLI args.
	 *
	 */
	/**
	 * Main method
	 *
	 * @param args
	 *        Unused CLI args.
	 */
	public static void main(String[] args) {
		/*
		 * Initialize vars.
		 * 
		 */
		StreamEngine sengine = new StreamEngine();
		StreamControlEngine sclengine = new StreamControlEngine(sengine);
		Scanner scn = new Scanner(System.in);

		/* Get input from the user. */
		System.out.print("Enter a SCL command string (blank to exit): ");

		/* Process it. */
		while(scn.hasNextLine()) {
			String ln = scn.nextLine().trim();

			if(ln.equals("")) {
				/* Ignore empty lines. */
				break;
			}

			/* Break the token into strings. */
			ListEx<String> res = new FunctionalList<>();
			String[] tokens = ln.split(" ");

			/* Run the stream engine on the tokens. */
			boolean succ = sengine.doStreams(tokens, res);
			if(!succ) {
				System.out.printf("\tERROR: Stream engine failed for line '%s'\n", ln);
				continue;
			}

			if(sengine.debug)
				System.out.printf("\tDEBUG: Streamed tokens: { %s }\n", res);

			/* Run the command through SCL. */
			succ = sclengine.runProgram(res.iterator());
			if(!succ) {
				System.out.printf("\tERROR: SCL engine failed for line '%s'\n", ln);
				continue;
			}

			/* Prompt again. */
			System.out.print("Command string executed succesfully.\n\n");
			System.out.print("Enter a SCL command string (blank to exit): ");
		}

		scn.close();
	}
}
