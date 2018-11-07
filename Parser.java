package cop5556sp18;
/* *
 * Initial code for SimpleParser for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Spring 2018.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Spring 2018 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2018
 */


import static  cop5556sp18.Scanner.Kind.*;
import static cop5556sp18.Scanner.Kind.BOOLEAN_LITERAL;
import static cop5556sp18.Scanner.Kind.COMMA;
import static cop5556sp18.Scanner.Kind.DOT;
import static cop5556sp18.Scanner.Kind.EOF;
import static cop5556sp18.Scanner.Kind.IDENTIFIER;
import static cop5556sp18.Scanner.Kind.INTEGER_LITERAL;
import static cop5556sp18.Scanner.Kind.FLOAT_LITERAL;
import static cop5556sp18.Scanner.Kind.KW_Z;
import static cop5556sp18.Scanner.Kind.KW_abs;
import static cop5556sp18.Scanner.Kind.KW_alpha;
import static cop5556sp18.Scanner.Kind.KW_atan;
import static cop5556sp18.Scanner.Kind.KW_blue;
import static cop5556sp18.Scanner.Kind.KW_boolean;
import static cop5556sp18.Scanner.Kind.KW_cart_x;
import static cop5556sp18.Scanner.Kind.KW_cart_y;
import static cop5556sp18.Scanner.Kind.KW_cos;
import static cop5556sp18.Scanner.Kind.KW_default_height;
import static cop5556sp18.Scanner.Kind.KW_default_width;
import static cop5556sp18.Scanner.Kind.KW_filename;
import static cop5556sp18.Scanner.Kind.KW_float;
import static cop5556sp18.Scanner.Kind.KW_from;
import static cop5556sp18.Scanner.Kind.KW_green;
import static cop5556sp18.Scanner.Kind.KW_height;
import static cop5556sp18.Scanner.Kind.KW_if;
import static cop5556sp18.Scanner.Kind.KW_image;
import static cop5556sp18.Scanner.Kind.KW_input;
import static cop5556sp18.Scanner.Kind.KW_int;
import static cop5556sp18.Scanner.Kind.KW_log;
import static cop5556sp18.Scanner.Kind.KW_polar_a;
import static cop5556sp18.Scanner.Kind.KW_polar_r;
import static cop5556sp18.Scanner.Kind.KW_red;
import static cop5556sp18.Scanner.Kind.KW_show;
import static cop5556sp18.Scanner.Kind.KW_sin;
import static cop5556sp18.Scanner.Kind.KW_to;
import static cop5556sp18.Scanner.Kind.KW_while;
import static cop5556sp18.Scanner.Kind.KW_width;
import static cop5556sp18.Scanner.Kind.KW_write;
import static cop5556sp18.Scanner.Kind.LBRACE;
import static cop5556sp18.Scanner.Kind.LPAREN;
import static cop5556sp18.Scanner.Kind.LSQUARE;
import static cop5556sp18.Scanner.Kind.OP_AND;
import static cop5556sp18.Scanner.Kind.OP_AT;
import static cop5556sp18.Scanner.Kind.OP_EXCLAMATION;
import static cop5556sp18.Scanner.Kind.OP_COLON;
import static cop5556sp18.Scanner.Kind.OP_DIV;
import static cop5556sp18.Scanner.Kind.OP_EQ;
import static cop5556sp18.Scanner.Kind.OP_GE;
import static cop5556sp18.Scanner.Kind.OP_GT;
import static cop5556sp18.Scanner.Kind.OP_LE;
import static cop5556sp18.Scanner.Kind.OP_LT;
import static cop5556sp18.Scanner.Kind.OP_MINUS;
import static cop5556sp18.Scanner.Kind.OP_MOD;
import static cop5556sp18.Scanner.Kind.OP_NEQ;
import static cop5556sp18.Scanner.Kind.OP_OR;
import static cop5556sp18.Scanner.Kind.OP_PLUS;
import static cop5556sp18.Scanner.Kind.OP_POWER;
import static cop5556sp18.Scanner.Kind.OP_QUESTION;
import static cop5556sp18.Scanner.Kind.OP_TIMES;
import static cop5556sp18.Scanner.Kind.RBRACE;
import static cop5556sp18.Scanner.Kind.RPAREN;
import static cop5556sp18.Scanner.Kind.RSQUARE;
import static cop5556sp18.Scanner.Kind.SEMI;

import java.util.ArrayList;
import java.util.Arrays;

import cop5556sp18.Scanner.Kind;
import cop5556sp18.Scanner.Token;
import cop5556sp18.AST.ASTNode;
import cop5556sp18.AST.Block;
import cop5556sp18.AST.Declaration;
import cop5556sp18.AST.Expression;
import cop5556sp18.AST.ExpressionBinary;
import cop5556sp18.AST.ExpressionBooleanLiteral;
import cop5556sp18.AST.ExpressionConditional;
import cop5556sp18.AST.ExpressionFloatLiteral;
import cop5556sp18.AST.ExpressionFunctionAppWithExpressionArg;
import cop5556sp18.AST.ExpressionFunctionAppWithPixel;
import cop5556sp18.AST.ExpressionIdent;
import cop5556sp18.AST.ExpressionIntegerLiteral;
import cop5556sp18.AST.ExpressionPixelConstructor;
import cop5556sp18.AST.ExpressionPixel;
import cop5556sp18.AST.ExpressionPredefinedName;
import cop5556sp18.AST.ExpressionUnary;
import cop5556sp18.AST.StatementInput;
import cop5556sp18.AST.StatementShow;
import cop5556sp18.AST.StatementSleep;
import cop5556sp18.AST.StatementWhile;
import cop5556sp18.AST.LHS;
import cop5556sp18.AST.LHSIdent;
import cop5556sp18.AST.LHSPixel;
import cop5556sp18.AST.LHSSample;
import cop5556sp18.AST.PixelSelector;
import cop5556sp18.AST.Program;
import cop5556sp18.AST.Statement;
import cop5556sp18.AST.StatementAssign;
import cop5556sp18.AST.StatementIf;
import cop5556sp18.AST.StatementWrite;


public class Parser {
	
    @SuppressWarnings("serial")
    public static class SyntaxException extends Exception {
	Token t;

	public SyntaxException(Token t, String message) {
	    super(message);
	    this.t = t;
	}

    }

    void error(Kind... expectedKinds) throws SyntaxException {
	String kinds = Arrays.toString(expectedKinds);
	String message;
	if(expectedKinds.length == 1) {
	    message = "Expected " + kinds + " at " + t.line() + ":" + t.posInLine();
	} else {
	    message = "Expected one of" + kinds + " at " + t.line() + ":" + t.posInLine();
	}
	throw new SyntaxException(t, message);
    }

    void error(Token t, String m) throws SyntaxException {
	String message = m + " at " + t.line() + ":" + t.posInLine();
	throw new SyntaxException(t, message);
    }


    Scanner scanner;
    Token t;

    Parser(Scanner scanner) {
	this.scanner = scanner;
	t = scanner.nextToken();
    }


    public Program parse() throws SyntaxException {
	Program p = program();
	matchEOF();
	return p;
    }

    /*
     * Program ::= Identifier Block
     */
    Program program() throws SyntaxException {
	Token first = t;
	Token progName = match(IDENTIFIER);
	Block block = block();
	return new Program (first, progName, block);
    }
	
	
    /*
     * Block ::=  { (  (Declaration | Statement) ; )* }
     */
	
    Kind[] firstDec = { KW_int, KW_boolean, KW_image, KW_float, KW_filename };

    // NoTe: added first Kind arrays
    //input, write, while, show, IDENTIFIER, red, green, blue, alpha, sleep, if
    Kind[] firstStatement = { KW_input ,KW_write, KW_while, KW_show, KW_sleep, KW_if ,IDENTIFIER ,KW_red ,KW_green ,KW_blue ,KW_alpha  };
    Kind[] firstType = { KW_int ,KW_float ,KW_boolean ,KW_image ,KW_filename  };
    Kind[] firstLHS = { IDENTIFIER ,KW_red ,KW_green ,KW_blue ,KW_alpha  };
    Kind[] firstPixelSelector = { LSQUARE  };
    Kind[] firstColor = { KW_red ,KW_green ,KW_blue ,KW_alpha  };
    Kind[] firstPrimary = { INTEGER_LITERAL ,BOOLEAN_LITERAL ,FLOAT_LITERAL ,LPAREN ,IDENTIFIER ,KW_sin ,KW_cos ,KW_atan ,KW_abs ,KW_log ,KW_cart_x ,KW_cart_y ,KW_polar_a ,KW_polar_r ,KW_int ,KW_float ,KW_width ,KW_height ,KW_red ,KW_green ,KW_blue ,KW_alpha ,KW_Z ,KW_default_height ,KW_default_width ,LPIXEL  };
    Kind[] firstFunctionName = { KW_sin ,KW_cos ,KW_atan ,KW_abs ,KW_log ,KW_cart_x ,KW_cart_y ,KW_polar_a ,KW_polar_r ,KW_int ,KW_float ,KW_width ,KW_height ,KW_red ,KW_green ,KW_blue ,KW_alpha  };
    Kind[] firstFunctionApplication = { KW_sin ,KW_cos ,KW_atan ,KW_abs ,KW_log ,KW_cart_x ,KW_cart_y ,KW_polar_a ,KW_polar_r ,KW_int ,KW_float ,KW_width ,KW_height ,KW_red ,KW_green ,KW_blue ,KW_alpha  };
    Kind[] firstPredefinedName = { KW_Z ,KW_default_height ,KW_default_width  };

    
    //  Block ::=  { (  (Declaration | Statement) ; )* }
    Block block() throws SyntaxException {
	Token first = t;
	match(LBRACE);

	ArrayList<ASTNode> decsAndStatements = new ArrayList<ASTNode>();
	while (isKind(firstDec)|isKind(firstStatement)) {
	    if (isKind(firstDec)) {
		Declaration dec = declaration();
		decsAndStatements.add(dec);
	    } else if (isKind(firstStatement)) {
		Statement s = statement();
		decsAndStatements.add(s);
	    }
	    match(SEMI);
	}
	match(RBRACE);
	return new Block(first, decsAndStatements);
    }
	

    // Declaration ::= Type IDENTIFIER | image IDENTIFIER [ Expression , Expression ]
    Declaration declaration() throws SyntaxException {
	Kind orig = t.kind;
	Token first = t;
	Token type = consume();
	Token name = match(IDENTIFIER);
	Token temp = t;
	Kind forMatch = LSQUARE;
	Kind forCondition = KW_image;
	Expression width = null;
	Expression height = null;
	if(orig == forCondition && temp.kind == forMatch){
	    match(LSQUARE);
	    width = expression();
	    match(COMMA);
	    height = expression();
	    match(RSQUARE);
	}
	return new Declaration(first, type, name, width, height);
    }


    // public void type() throws SyntaxException {
    // 	if(isKind(firstType)){
    // 	    consume();
    // 	} else {
    // 	    throw new SyntaxException(t,"Wrong Type specified");
    // 	}
    // }

    // NoTe: added functions
    // Statement ::= StatementInput | StatementWrite | StatementAssignment | StatementWhile | StatementIf | StatementShow | StatementSleep	
    Statement statement() throws SyntaxException {
	if(isKind(KW_input)){
	    return StatementInput();
	} else if (isKind(KW_write)){
	    return StatementWrite();
	} else if (isKind(firstLHS)){
	    return StatementAssignment();
	} else if (isKind(KW_while)){
	    return StatementWhile();
	} else if (isKind(KW_show)){
	    return StatementShow();
	} else if (isKind(KW_sleep)){
	    return StatementSleep();
	} else if (isKind(KW_if)){
	    return StatementIf();
	} else {
		 error(t, "Illegal expression");
		 return null;
	}
    }

    // StatementInput ::= input IDENTIFIER from @ Expression
    Statement StatementInput() throws SyntaxException {
	Token first = t;
	consume();
	Token destName = match(IDENTIFIER);
	match(KW_from);
	match(OP_AT);
	Expression e = expression();
	return new StatementInput(first, destName, e);
    }

    // StatementWrite ::= write IDENTIFIER to IDENTIFIER
    Statement StatementWrite() throws SyntaxException {
	Token first = t;	
	consume();
	Token sourceName = match(IDENTIFIER);
	match(KW_to);
	Token destName = match(IDENTIFIER);
	return new StatementWrite(first, sourceName, destName);
    }

    // StatementAssignment ::=  LHS := Expression
    Statement StatementAssignment() throws SyntaxException {
	Token first = t;
	LHS lhs = lhs();
	match(OP_ASSIGN);
	Expression e = expression();
	return new StatementAssign(first, lhs, e);
    }

    // StatementWhile ::=  while (Expression ) Block
    Statement StatementWhile() throws SyntaxException {
	Token first = t;	
	consume();
	match(LPAREN);
	Expression e = expression();
	match(RPAREN);
	Block b = block();
	return new StatementWhile(first, e, b);
    }

    // StatementIf ::=  if ( Expression ) Block 
    Statement StatementIf() throws SyntaxException {
	Token first = t;	
	consume();
	match(LPAREN);
	Expression e = expression();
	match(RPAREN);
	Block b = block();
	return new StatementIf(first, e, b);
    }

    // StatementShow ::=  show Expression
    Statement StatementShow() throws SyntaxException {
	Token first = t;	
	consume(); 
	Expression e = expression();
	return new StatementShow(first, e);
    }

    // StatementSleep ::=  sleep Expression
    Statement StatementSleep() throws SyntaxException {
	Token first = t;	
	consume();
	Expression e = expression();
	return new StatementSleep(first, e);
    }

    // LHS ::=  IDENTIFIER | IDENTIFIER PixelSelector | Color ( IDENTIFIER PixelSelector )
    LHS lhs() throws SyntaxException {
	Token first = t;
	if(isKind(IDENTIFIER)){
	    Token name = consume();
	    Token temp = t;
	    Kind forMatch = LSQUARE;
	    if(temp.kind == forMatch){
		PixelSelector pixel = PixelSelector();
		return new LHSPixel(first, name, pixel);
	    }
	    return new LHSIdent(first, name);
	} 
	Token color = consume();
	match(LPAREN);
	Token name = match(IDENTIFIER);
	PixelSelector selector = PixelSelector();
	match(RPAREN);
	return new LHSSample(first, name, selector, color);
    }

    // // Color ::= red | green | blue | alpha
    // public void Color() throws SyntaxException {
    // 	if(isKind(firstColor)){
    // 	    consume();
    // 	} else {
    // 	    throw new SyntaxException(t,"Wrong Color type specified");
    // 	}
    // }

    // PixelSelector ::= [ Expression , Expression ]
    PixelSelector PixelSelector() throws SyntaxException {
	Token first = t;
	consume();
	Expression e0 = expression();
	match(COMMA);
	Expression e1 = expression();
	match(RSQUARE);
	PixelSelector pixel = new PixelSelector(first,e0,e1);
	return pixel;
    }

    // Expression ::=  OrExpression  ?  Expression  :  Expression |   OrExpression
    Expression expression() throws SyntaxException {
	Token first = t;
	Expression e0 = OrExpression();
	Token temp = t;
	Kind forMatch = OP_QUESTION;
	if(temp.kind == forMatch){
	    match(OP_QUESTION);
	    Expression e1 = expression();
	    match(OP_COLON);
	    Expression e2 = expression();
	    e0 = new ExpressionConditional(first, e0, e1, e2);
	}
	return e0;
    }

    // OrExpression  ::=  AndExpression   (  |  AndExpression ) *
    Expression OrExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = AndExpression();
	while(isKind(OP_OR)) {
	    Token op = consume();
	    Expression e1 = AndExpression();
	    e0 = new ExpressionBinary(first, e0, op, e1);
	}
	return e0;
    }

    // AndExpression ::=  EqExpression ( & EqExpression )*
    Expression AndExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = EqExpression();
	while(isKind(OP_AND)) {
	    Token op = consume();
	    Expression e1 = EqExpression();
	    e0 = new ExpressionBinary(first, e0, op, e1);
	}
	return e0;
    }

    // EqExpression ::=  RelExpression  (  (== | != )  RelExpression )*
    Expression EqExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = RelExpression();
	while(isKind(OP_EQ) | isKind(OP_NEQ)){
	    Token op = consume();
	    Expression e1 = RelExpression();
	    e0 = new ExpressionBinary(first, e0,op,e1);
	}
	return e0;
    }


    // RelExpression ::= AddExpression (  (<  | > |  <=  | >= )   AddExpression)*
    Expression RelExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = AddExpression();
	while(isKind(OP_LT) | isKind(OP_GT) | isKind(OP_LE) | isKind(OP_GE) ){
	    Token op = consume();
	    Expression e1 = AddExpression();
	    e0 = new ExpressionBinary(first, e0, op, e1);
	}
	return e0;
    }


    // AddExpression ::= MultExpression   (  ( + | - ) MultExpression )*
    Expression AddExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = MultExpression();
	while(isKind(OP_PLUS) |  isKind(OP_MINUS) ) {
	    Token op = consume();
	    Expression e1 = MultExpression();
	    e0 = new ExpressionBinary(first,e0,op,e1);
	}
	return e0;

    }

    // MultExpression := PowerExpression ( ( * | /  | % ) PowerExpression )*
    Expression MultExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = PowerExpression();
	while(isKind(OP_TIMES) | isKind(OP_DIV) | isKind(OP_MOD) ){
	    Token op = consume();
	    Expression e1 = PowerExpression();
	    e0 = new ExpressionBinary(first,e0,op,e1);
	}
	return e0;
    }

    // PowerExpression := UnaryExpression  (** PowerExpression | ε)
    Expression PowerExpression() throws SyntaxException {
	Token first = t;
	Expression e0 = UnaryExpression();
	Token temp = t;
	Kind forMatch = OP_POWER;
	if(temp.kind == forMatch){
	    Token op = consume();
	    Expression e1 = PowerExpression();
	    return new ExpressionBinary(first, e0, op, e1);
	}
	return e0;
    }

    // UnaryExpression ::= + UnaryExpression | - UnaryExpression | UnaryExpressionNotPlusMinus
    Expression UnaryExpression() throws SyntaxException {
	Token first = t;
	if (isKind(OP_PLUS)) {  
	    Token op = consume();
	    Expression e = UnaryExpression();
	    //	    return UnaryExpression();
	    return new ExpressionUnary(first, op, e);
	}
	else if (isKind(OP_MINUS)){
	    Token op = consume();
	    Expression e = UnaryExpression();
	    return new ExpressionUnary(first, op, e);
	}
	else {
	    return UnaryExpressionNotPlusMinus();
	}
    }

    // UnaryExpressionNotPlusMinus ::=  ! UnaryExpression  | Primary 
    Expression UnaryExpressionNotPlusMinus() throws SyntaxException {
	Token first = t;
	if (isKind(OP_EXCLAMATION)) {
	    Token op = consume();
	    Expression e = UnaryExpression();
	    return new ExpressionUnary(first,op,e);
	} else 	{
	    return Primary(); //errors will be reported by primary()
	}
    }

    // Primary ::= INTEGER_LITERAL | BOOLEAN_LITERAL | FLOAT_LITERAL | 
    //                 ( Expression ) | FunctionApplication  | IDENTIFIER | PixelExpression | 
    //                  PredefinedName | PixelConstructor
    protected Expression Primary() throws SyntaxException {
	Token first = t;
	if(isKind(INTEGER_LITERAL)){
	    Token intLit = consume();
	    return new ExpressionIntegerLiteral(first, intLit);

	} else if (isKind(BOOLEAN_LITERAL)){
	    Token booleanLit = consume();			
	    return new ExpressionBooleanLiteral(first, booleanLit);

	} else if (isKind(FLOAT_LITERAL)){
	    Token floatLit = consume();
	    return new ExpressionFloatLiteral(first, floatLit);

	} else if (isKind(LPAREN)){
	    consume();
	    Expression e = expression();
	    match(RPAREN);
	    return e;

	} else if (isKind(firstFunctionApplication)){
	    Expression e= FunctionApplication();
	    return e;
	} else if (isKind(IDENTIFIER)){
	    Token name = consume();
	    Token temp = t;   // PixelExpression ::= IDENTIFIER PixelSelector
	    Kind forMatch = LSQUARE;
	    if(temp.kind == forMatch){
		PixelSelector selector = PixelSelector();
		return new ExpressionPixel(first, name, selector);
	    }
	    return new ExpressionIdent(first, name);
	} else if (isKind(firstPredefinedName)){
	    Token t = consume();
	    return new ExpressionPredefinedName(first,t);

	} else if (isKind(LPIXEL)){
	    return PixelConstructor();
	} else {
	    error(t, "Illegal expression");
	    return null;
	}
    }

    // PixelConstructor ::=  <<  Expression , Expression , Expression , Expression  >> 
    Expression PixelConstructor() throws SyntaxException {
	Token first = t;
	consume();
	Expression alpha = expression();
	match(COMMA);
	Expression red = expression();
	match(COMMA);
	Expression green = expression();
	match(COMMA);
	Expression blue = expression();
	match(RPIXEL);
	return new ExpressionPixelConstructor(first, alpha, red, green, blue);
    }

    // PixelExpression ::= IDENTIFIER PixelSelector
    public void PixelExpression() throws SyntaxException {
	match(IDENTIFIER);
	PixelSelector();
    }

    // FunctionApplication ::= FunctionName ( Expression )  | FunctionName  [ Expression , Expression ] 
    Expression FunctionApplication() throws SyntaxException {
	Token first = t;
	Token name = match(firstFunctionName);
	if (isKind(LPAREN)) {
	    consume();
	    Expression e = expression();
	    match(RPAREN);
	    return new ExpressionFunctionAppWithExpressionArg(first, name, e);
	}
	if (isKind(LSQUARE)) {
	    consume();
	    Expression e0 = expression();
	    match(COMMA);
	    Expression e1 = expression();
	    match(RSQUARE);
	    return new ExpressionFunctionAppWithPixel(first, name, e0, e1);
	}
	error(t, "bug in parser");
	return null;
    }

    // // FunctionName ::= sin | cos | atan | abs | log | cart_x | cart_y | polar_a | polar_r 
    // // 	    int | float | width | height | Color
    public void FunctionName() throws SyntaxException {
    	if(isKind(firstFunctionName)){
    	    consume();
    	} else {
    	    throw new SyntaxException(t,"Wrong FunctionName Type specified");
    	}

    }

    // // PredefinedName ::= Z | default_height | default_width
    // public void PredefinedName() throws SyntaxException {
    // 	if(isKind(KW_Z)){
    // 	    match(KW_Z);
    // 	} else if (isKind(KW_default_height)){
    // 	    match(KW_default_height);
    // 	} else if (isKind(KW_default_width)){
    // 	    match(KW_default_width);
    // 	}

    // }

    // NoTe: end of added functions


    
    
    protected boolean isKind(Kind kind) {
	return t.kind == kind;
    }

    protected boolean isKind(Kind... kinds) {
	for (Kind k : kinds) {
	    if (k == t.kind)
		return true;
	}
	return false;
    }


    /**
     * Precondition: kind != EOF
     * 
     * @param kind
     * @return
     * @throws SyntaxException
     */
    Token match(Kind kind) throws SyntaxException {
	Token tmp = t;
	if (isKind(kind)) {
	    consume();
	    return tmp;
	}
	error(kind);
	return null;
    }

    Token match(Kind... kinds) throws SyntaxException {
	Token tmp = t;
	if (isKind(kinds)) {
	    consume();
	    return tmp;
	}
	StringBuilder sb = new StringBuilder();
	for (Kind kind1 : kinds) {
	    sb.append(kind1).append(kind1).append(" ");
	}
	error(kinds);
	return null; // unreachable
    }

    Token consume() throws SyntaxException {
	Token tmp = t;
	if (isKind( EOF)) {
	    error(t, "attempting to consume EOF");
	}
	t = scanner.nextToken();
	return tmp;
    }


    /**
     * Only for check at end of program. Does not "consume" EOF so no attempt to get
     * nonexistent next Token.
     * 
     * @return
     * @throws SyntaxException
     */
    Token matchEOF() throws SyntaxException {
	if (isKind(EOF)) {
	    return t;
	}
	error(EOF);
	return null;
    }
	

}

