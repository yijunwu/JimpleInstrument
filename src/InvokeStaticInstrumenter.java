import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bgu.cs.util.Matcher;
import bgu.cs.util.Matcher.Case;
import bgu.cs.util.soot.CaseAssignLocal;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;

public class InvokeStaticInstrumenter extends BodyTransformer{

    /* some internal fields */
    static SootClass counterClass;
    static SootMethod init,finish,printer,method_finish,print_value,print_obj_value,toggle;
    static SootField name,value,objvalue;
	static Matcher<Unit> jimpleMatcher = new Matcher<>();

	//MyCounter Methods
    static {
      //classes
      counterClass    = Scene.v().loadClassAndSupport("MyCounter");
      printer = counterClass.getMethod("void printer(java.lang.String)");
      init   = counterClass.getMethod("void init(java.lang.String)");
      finish	  = counterClass.getMethod("void finish()");
      method_finish = counterClass.getMethod("void method_finish()");
      print_value = counterClass.getMethod("void printValue(int)");
      print_obj_value = counterClass.getMethod("void printObjValue(int)");
      toggle = counterClass.getMethod("void toggleDelta()");
      
      //fields
      name = counterClass.getFieldByName("name");
      value = counterClass.getFieldByName("value");
      objvalue = counterClass.getFieldByName("objvalue");

	  jimpleMatcher.addAllCasesFromPkg(bgu.cs.util.soot.JimpleCase.class.getPackage());
    }

    //every method instrumentation starts here
    protected void internalTransform(Body body, String phase, Map options) {
      SootMethod method = body.getMethod();
      
      //instrument the main's return call
	  Iterator stmtIt = body.getUnits().snapshotIterator();
	  Chain units = body.getUnits();
	  instrumentReturn(stmtIt,units ,body); 
      
	  //instrument method with @checker annotation
	  String tag = getMethodTag(method);
	  if(tag.contains("checker"))  
      {
    	  InitMethod(body, method);
      }
    }

	private String getMethodTag(SootMethod method) {
		VisibilityAnnotationTag tag = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
		if (tag != null) {
		   return tag.getAnnotations().get(0).getType();
		}
		return "";
	}

	private void InitMethod(Body body, SootMethod method) {
		  Chain units = body.getUnits();
		  Iterator stmtIt;
		  stmtIt = units.snapshotIterator();
		  //all the commands of the method
		  instrumentAnyCommand(units, stmtIt,body);

		  stmtIt = units.snapshotIterator();
		  //begining of the annotated method
		  instrumentBegin(stmtIt,units,body);

		  stmtIt = units.snapshotIterator();
		  //the ending of the method
		  instrumentMethodFinish(units, stmtIt,body);  
	}

	private void instrumentMethodFinish(Chain units, Iterator stmtIt, Body body) {
		while (stmtIt.hasNext()) {

		    Stmt stmt = (Stmt)stmtIt.next();
		    //if the statement is return or empty return this is a finish
		    if(stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt)
		    {
		    	 InvokeExpr incExpr= Jimple.v().newStaticInvokeExpr(method_finish.makeRef());
	 		     Stmt incStmt = Jimple.v().newInvokeStmt(incExpr); //call method_finish before you return
			     units.insertBefore(incStmt, stmt);
		    }
		}
	}

	private void instrumentAnyCommand(Chain units, Iterator stmtIt, Body body) {

		stmtIt.next();stmtIt.next();
		while (stmtIt.hasNext()) {

		    Stmt stmt = (Stmt)stmtIt.next();
		    Case<Unit> matchedCase = utilMatch(stmt);
		    String resulted = myMatcher.match(matchedCase);//romans pattern matching of commands
		    
		    if(resulted!="")
		    {
				if (matchedCase != null) {
					System.out.println(String.format("%s -> %s", stmt, matchedCase.getClass()));
				} else {
					System.out.println(String.format("%s -> ? (unmatched)", stmt));
					// throw new Error("Unable to match " + u);
				}

				//print command to spec file
				InvokeExpr incExpr= Jimple.v().newStaticInvokeExpr(printer.makeRef(),StringConstant.v(resulted+";//"));
			    Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
				
			    units.insertAfter(incStmt, stmt);
			    //print state -- this might be enabled only before and after method calls
			    PrintLocalValue(units,body,incStmt);
		    }
		  }
	}

	private Case<Unit> utilMatch(Stmt stmt) {
		try {
		Case<Unit> matchedCase = jimpleMatcher.match(stmt);
		return matchedCase;
		}
		catch(Exception e)
		{
			return null;
		}
	}


	private Stmt PrintParamValue(Chain units, Body body, Stmt incStmt)
	{
		StaticFieldRef jimple_name = Jimple.v().newStaticFieldRef(name.makeRef());
		StaticFieldRef jimple_value = Jimple.v().newStaticFieldRef(value.makeRef());
		StaticFieldRef jimple_obj_value = Jimple.v().newStaticFieldRef(objvalue.makeRef());
		int locnum=0;
		
		for(Local loc :body.getParameterLocals())
		{
			System.out.println(loc+" "+locnum);
			int islast = locnum==body.getParameterLocals().size()-1?1:0;
			if(loc.getType().toString().equals("int"))
					{	
						Stmt AssignStmt;
						AssignStmt = Jimple.v().newAssignStmt(jimple_value,loc);
				        units.insertAfter(AssignStmt, incStmt);
				        AssignStmt secondAssign = Jimple.v().newAssignStmt(
				                jimple_name,
				                StringConstant.v(loc.getName()));
				        units.insertAfter(secondAssign, AssignStmt);
						InvokeExpr printExpr= Jimple.v().newStaticInvokeExpr(print_value.makeRef(),IntConstant.v(islast));
						Stmt printStmt = Jimple.v().newInvokeStmt(printExpr);
						units.insertAfter(printStmt, secondAssign);
						incStmt = printStmt;
					}
			else
			{
				Stmt AssignStmt;
				AssignStmt = Jimple.v().newAssignStmt(jimple_obj_value,loc);
		        units.insertAfter(AssignStmt, incStmt);
		        AssignStmt secondAssign = Jimple.v().newAssignStmt(
		                jimple_name,
		                StringConstant.v(loc.getName()));
		        units.insertAfter(secondAssign, AssignStmt);
				InvokeExpr printExpr= Jimple.v().newStaticInvokeExpr(print_obj_value.makeRef(),IntConstant.v(islast));
				Stmt printStmt = Jimple.v().newInvokeStmt(printExpr);
				units.insertAfter(printStmt, secondAssign);
				incStmt = printStmt;
			}
			locnum++;
		}
		return incStmt;
		

	}
	private void PrintLocalValue(Chain units, Body body, Stmt incStmt)
	{
		StaticFieldRef jimple_name = Jimple.v().newStaticFieldRef(name.makeRef());
		StaticFieldRef jimple_value = Jimple.v().newStaticFieldRef(value.makeRef());
		StaticFieldRef jimple_obj_value = Jimple.v().newStaticFieldRef(objvalue.makeRef());
		int locnum=0;
		
		for(Local loc :body.getLocals())
		{
			System.out.println(loc+" "+locnum);
			int islast = locnum==body.getLocals().size()-1?1:0;
			if(loc.getType().toString().equals("int"))
					{	
						Stmt AssignStmt;
						AssignStmt = Jimple.v().newAssignStmt(jimple_value,loc);
				        units.insertAfter(AssignStmt, incStmt);
				       
				        AssignStmt secondAssign = Jimple.v().newAssignStmt(
				                jimple_name,
				                StringConstant.v(loc.getName()));
				        units.insertAfter(secondAssign, AssignStmt);
						InvokeExpr printExpr= Jimple.v().newStaticInvokeExpr(print_value.makeRef(),IntConstant.v(islast));
						Stmt printStmt = Jimple.v().newInvokeStmt(printExpr);
						units.insertAfter(printStmt, secondAssign);
						incStmt = printStmt;
					}
			else
			{
				Stmt AssignStmt;
				AssignStmt = Jimple.v().newAssignStmt(jimple_obj_value,loc);
		        //units.insertAfter(AssignStmt, incStmt);
		        
		        AssignStmt secondAssign = Jimple.v().newAssignStmt(
		                jimple_name,
		                StringConstant.v(loc.getName()));
		        units.insertAfter(secondAssign, incStmt);
		        
				InvokeExpr printExpr= Jimple.v().newStaticInvokeExpr(print_obj_value.makeRef(),IntConstant.v(islast));
				Stmt printStmt = Jimple.v().newInvokeStmt(printExpr);
				units.insertAfter(printStmt, secondAssign);
				incStmt = printStmt;
			}
			locnum++;
		}
		

	}
	
	private void instrumentBegin(Iterator stmtIt, Chain units, Body body) {
		// method prefix printing
		Stmt stmt = (Stmt) stmtIt.next();
		 stmt = (Stmt) stmtIt.next();

		
		String params ="(";
		int paramIndex=0;
		for(Type param : body.getMethod().getParameterTypes())
		{
			boolean mut=true;
			if(param.toString().contains("String"))
				mut=false;
			String mutable = mut?"mut":"immut";
			params+=mutable + " " +("i"+paramIndex++)+":"+param.toString()+",";
		}
		params=params.substring(0,params.length()-1);
		params=body.getMethod().getName()+" "+params+")";
		String ret = body.getMethod().getReturnType().toString();
		ret = ("(res:"+ret+"){");
		params+= " -> ";
		params+= ret;
		Stmt lstStmt;
		if(MainDriver.DELTA) {
			InvokeExpr incExpr= Jimple.v().newStaticInvokeExpr(toggle.makeRef());
			InvokeStmt incStmt2 = Jimple.v().newInvokeStmt(incExpr);
			units.insertAfter(incStmt2, stmt);
			incExpr = Jimple.v().newStaticInvokeExpr(init.makeRef(),StringConstant.v(params));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			units.insertAfter(incStmt, incStmt2);	
			lstStmt = incStmt;
		}
		else {
			StaticInvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(init.makeRef(),StringConstant.v(params));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			units.insertAfter(incStmt, stmt);	
			lstStmt = incStmt;
		}
		lstStmt = initLocals(units,body,lstStmt);

		Stmt aftStmt = PrintParamValue(units, body, lstStmt);

	}

	private Stmt initLocals(Chain units, Body body, Stmt aftStmt) {
		Stmt lstStmt=aftStmt;
		for(Local loc :body.getLocals())
		{
			if(!body.getParameterLocals().contains(loc)){
				if(loc.getType().toString().equals("int"))
				{	
					System.out.println("FUCKiing"+loc);

					Stmt AssignStmt = Jimple.v().newAssignStmt(loc,IntConstant.v(0));
					units.insertAfter(AssignStmt, lstStmt);
					lstStmt = AssignStmt;
				}
				else
				{
					System.out.println("FUCKobj"+loc);
					Stmt AssignStmt = Jimple.v().newAssignStmt(loc,NullConstant.v());
					units.insertAfter(AssignStmt, lstStmt);
					lstStmt = AssignStmt;
					
				}
				
			}
		}
		return lstStmt;
		
	}

	private void instrumentReturn(Iterator stmtIt, Chain units, Body body) {
		if(body.getMethod().getSubSignature().equals("void main(java.lang.String[])")){
			//this is the main
			while(stmtIt.hasNext())
			{
				Object stmt = stmtIt.next();
				if ((stmt instanceof ReturnStmt)
			              ||(stmt instanceof ReturnVoidStmt)) {
			            // 1. make invoke expression of MyCounter.report()
			            InvokeExpr reportExpr= Jimple.v().newStaticInvokeExpr(finish.makeRef());
		
			            // 2. then, make a invoke statement
			            Stmt reportStmt = Jimple.v().newInvokeStmt(reportExpr);
		
			            // 3. insert new statement into the chain
			            //    (we are mutating the unit chain).
			            units.insertBefore(reportStmt, stmt);
			          }
			 }
		}
	}
  }