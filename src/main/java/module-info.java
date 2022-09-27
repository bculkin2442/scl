/**
 * Stream Control Language.
 * 
 * A FORTH-inspired language for manipulating text streams.
 * @author bjculkin
 *
 */
module dicelang.scl {
	exports bjc.dicelang.sclv2;
	exports bjc.dicelang.scl.tokens;
	exports bjc.dicelang.scl;

	requires bjc.utils;
	requires esodata;
}