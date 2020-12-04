package bjc.dicelang.sclv2;

import java.util.*;

import bjc.esodata.*;

/**
 * Engine for running SCL.
 * 
 * Not threadsafe.
 * 
 * @author Ben Culkin
 */
public class SCLEngine
{
	private TapeLibrary<Deque<String>> streams;
	private Deque<String>              currentStream;

	private Map<String, Deque<String>> streamLibrary;

	private boolean inQuoteMode;

	/**
	 * Create a new SCL engine.
	 */
	public SCLEngine()
	{
		reinit();
	}

	/**
	 * Reinitialize this engine, as if it was newly constructed.
	 */
	public void reinit()
	{
		streams = new TapeLibrary<>();
		streams.setAllowAutoCreation(true);
		streams.mountTape("default");

		streamLibrary = new HashMap<>();

		currentStream = new ArrayDeque<>();
		streams.item(currentStream);

		inQuoteMode = false;
	}

	/**
	 * Run the engine on a given set of input.
	 * 
	 * @param input
	 *              The source of input to use.
	 * 
	 * @return The stream that was active at the end of the input.
	 */
	public Deque<String> run(Iterator<String> input)
	{
		while (input.hasNext())
		{
			String token = input.next();

			if (inQuoteMode)
			{

				if      (token.equalsIgnoreCase("{@SQ}")) inQuoteMode = true;
				else if (token.equalsIgnoreCase("{@SU}")) inQuoteMode = false;
				else                                      currentStream.add(token);
			} else
			{
				if (token.startsWith("{@S") && token.endsWith("}"))
				{
					String[] commands = token.substring(3, token.length()).split(";");
					for (String command : commands) {
						switch (command)
						{
						default:
							// Unknown command; need to handle it
						}
					}
				} else
				{					
					currentStream.add(token);
				}
			}
		}

		return currentStream;
	}
}