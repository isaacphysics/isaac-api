package uk.ac.cam.cl.dtg.teaching;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import clojure.lang.Keyword;

public class Clojure {
	
	private Clojure() { }
	
	private static IFn generate = null;
	
	static {
		init();
	}
	
	private static void init()
	{
		Var REQUIRE = RT.var("clojure.core", "require");
		REQUIRE.invoke(Symbol.intern("rutherford.core"), Keyword.intern("reload"));
		
		Var g = RT.var("rutherford.core", "generate");
		generate = g.fn();		
	}
	
	@SuppressWarnings("unchecked")
	protected static <I> I generate(Class<I> i)
	{
		return (I)generate.invoke(i);
	}	
	
	@SuppressWarnings("unchecked")
	protected static <I> I generate(Class<I> i, boolean reload)
	{
		if (reload)
			init();
		return (I)generate.invoke(i);
	}

}
