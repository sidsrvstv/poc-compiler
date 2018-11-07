package cop5556sp18;

import java.util.*;
import cop5556sp18.Types.*;

import cop5556sp18.AST.*;
public class SymbolTable {

    public SymbolTable() {
    }
    
    public void enterScope() {
    		current_scope = next_scope;
    		scope_stack.add(new Integer(next_scope++));
    }

    public void leaveScope(){
		if(scope_stack.size() > 0){
			scope_stack.remove(scope_stack.size() -1 );

			if(scope_stack.size() > 0) current_scope = scope_stack.get(scope_stack.size() -1 );
			else current_scope = 0;
		} else {
		    current_scope = 0;
		}
    }
    
    public void put(String ident, Declaration d){
    	if(! symbolTable.containsKey(ident) ){
    	    HashMap<Integer, Declaration> m = new HashMap<>();
    	    m.put(current_scope, d);
    	    symbolTable.put(ident, m);
    	} else {
    	    HashMap<Integer, Declaration> m = symbolTable.get(ident);
    	    m.put(current_scope, d);
    	    symbolTable.replace(ident, m);
    	}
	System.out.println("Entering ident " + ident + " with scopekey :" + current_scope);
    }
    /* lookup method shall tell whether the identifier is declared in current scope*/
    public boolean currentlookup(String ident){
	if(symbolTable.containsKey(ident)){
	    HashMap<Integer, Declaration> m = symbolTable.get(ident);
	    if(m.containsKey(current_scope)){
	    		return true;
	    }
	}
	return false;
    }
    /* get method shall return the type of the innermost identifier in scope */
    public Declaration lookup(String ident){
    	System.out.println("fetching "+ ident);
    	
	if(symbolTable.containsKey(ident)){
		System.out.println("table contains ident");
	    Integer innerMost = -1;
	    HashMap<Integer, Declaration> m = symbolTable.get(ident);
	    
	    System.out.println("size: " + scope_stack.size());
	    for(int i = scope_stack.size() -1 ; i>=0; i-- ){
	    		System.out.println(scope_stack.get(i));
			if(m.containsKey(scope_stack.get(i))){
			    innerMost = scope_stack.get(i);
			    break;
			}
	    }
	    return m.get(innerMost);
	}
	return null;
    }
    
    private int next_scope = 0;
    private int current_scope = 0;

    private HashMap<String, HashMap<Integer, Declaration>> symbolTable = new HashMap<>();

    private ArrayList<Integer> scope_stack = new ArrayList<Integer>();

}
