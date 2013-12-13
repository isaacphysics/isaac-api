package uk.ac.cam.cl.dtg.clojure;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class Clojure {
	
	private Clojure() { }
	
	private static IFn generate = null;
	
	static {
		init();
	}
	
	private static void init()
	{
		Var REQUIRE = RT.var("clojure.core", "require");
		REQUIRE.invoke(Symbol.intern("rutherford.core"));
		
		Var g = RT.var("rutherford.interop", "generate");
		generate = g.fn();		
	}
	
	@SuppressWarnings("unchecked")
	public static <I> I generate(Class<I> i)
	{
		return (I)generate.invoke(i);
	}
}
