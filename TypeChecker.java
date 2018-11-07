package cop5556sp18;


import java.util.*;



import cop5556sp18.Scanner.Kind;
import cop5556sp18.Scanner.Token;
import cop5556sp18.SymbolTable;
import cop5556sp18.Types.Type;
import cop5556sp18.AST.ASTNode;
import cop5556sp18.AST.ASTVisitor;
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
import cop5556sp18.AST.ExpressionPixel;
import cop5556sp18.AST.ExpressionPixelConstructor;
import cop5556sp18.AST.ExpressionPredefinedName;
import cop5556sp18.AST.ExpressionUnary;
import cop5556sp18.AST.LHS;
import cop5556sp18.AST.LHSIdent;
import cop5556sp18.AST.LHSPixel;
import cop5556sp18.AST.LHSSample;
import cop5556sp18.AST.PixelSelector;
import cop5556sp18.AST.Program;
import cop5556sp18.AST.StatementAssign;
import cop5556sp18.AST.StatementIf;
import cop5556sp18.AST.StatementInput;
import cop5556sp18.AST.StatementShow;
import cop5556sp18.AST.StatementSleep;
import cop5556sp18.AST.StatementWhile;
import cop5556sp18.AST.StatementWrite;

public class TypeChecker implements ASTVisitor {

	SymbolTable st;
	TypeChecker() {
		st = new SymbolTable();
	}

	@SuppressWarnings("serial")
	public static class SemanticException extends Exception {
		Token t;

		public SemanticException(Token t, String message) {
			super(message);
			this.t = t;
		}
	}

	
	
	// Name is only used for naming the output file. 
	// Visit the child block to type check program.
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		program.block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO Auto-generated method stub
	    st.enterScope();
	    List<ASTNode> decsOrStatements = block.decsOrStatements;
	    for(int i = 0; i < decsOrStatements.size(); i++) {
		decsOrStatements.get(i).visit(this, arg);
	    }
	    st.leaveScope();
	    return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg) throws Exception {
		// TODO Auto-generated method stub
		String ident = declaration.name;
		if(st.currentlookup(ident)) {
			throw new SemanticException(null, "Re declared in same scope");
		}
		Expression e0 = declaration.height;
		Expression e1 = declaration.width;
		
		if(e0!=null || e1!=null) {
			if( Types.getType(declaration.type) != Type.IMAGE) {
				throw new SemanticException(null, "Expecected type image");
			}
			if(e0 == null || e1 == null ) {
				throw new SemanticException(null, "Both height and width need to be specified");
			}
			e0.visit(this, arg);
			e1.visit(this, arg);
			if(e0.type != Type.INTEGER || e1.type != Type.INTEGER) {
				throw new SemanticException(null, "Integer expected");
			}
			
		}
		st.put(ident, declaration);
		return null;
	//	throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg) throws Exception {
		// TODO Auto-generated method stub
		String sourceName = statementWrite.sourceName;
		String destName = statementWrite.destName;
		Declaration sourceDec = st.lookup(sourceName);
		Declaration destDec = st.lookup(destName);
		statementWrite.sourceDec = sourceDec;
		statementWrite.destDec = destDec;
		if(sourceDec == null || destDec == null) {
			throw new SemanticException(null, "source and destination not specified");
		}
		if(Types.getType(sourceDec.type) != Type.IMAGE || Types.getType(destDec.type) != Type.FILE) {
			throw new SemanticException(null, "incompatible types");
		}		
		return null;

	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws Exception {
		// TODO Auto-generated method stub
		String ident = statementInput.destName;
		Expression e = statementInput.e;
		e.visit(this, arg);
		Declaration dec = st.lookup(ident);
		if(dec == null) {
			throw new SemanticException(null, "undeclared");
		}
		if(e.type != Type.INTEGER) {
			throw new SemanticException(null, "int expected");
		}
		statementInput.dec = dec;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression ex = pixelSelector.ex;
		Expression ey = pixelSelector.ey;
		ex.visit(this, arg);
		ey.visit(this, arg);
		if(ex.type != ey.type) {
			throw new SemanticException(null, "ex ey same type expected");
		}
		if(ex.type != Type.FLOAT && ex.type != Type.INTEGER) {
			throw new SemanticException(null, "int or float expected");
		}
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionConditional(ExpressionConditional expressionConditional, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression e0 = expressionConditional.guard;
		Expression e1 = expressionConditional.trueExpression;
		Expression e2 = expressionConditional.falseExpression;
		e0.visit(this, arg);
		e1.visit(this, arg);
		e2.visit(this, arg);
		if(e0.type != Type.BOOLEAN) {
			throw new SemanticException(null, "bool expected");
		}
		if (e1.type != e2.type) {
			throw new SemanticException(null, "mismatched types");
		}
		expressionConditional.type = e1.type;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression e0 = expressionBinary.leftExpression;
		Expression e1 = expressionBinary.rightExpression;
		Kind op = expressionBinary.op;
		e0.visit(this, arg);
		e1.visit(this, arg);
		
		switch(op) {
		case OP_PLUS: case OP_MINUS: case OP_TIMES: case OP_DIV: case OP_POWER:
			if(e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				expressionBinary.type = Type.INTEGER;
			} else if (e0.type == Type.INTEGER && e1.type == Type.FLOAT) {
				expressionBinary.type = Type.FLOAT;
			} else if (e0.type == Type.FLOAT && e1.type == Type.INTEGER) {
				expressionBinary.type = Type.FLOAT;
			}else if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				expressionBinary.type = Type.FLOAT;
			} else {
				throw new SemanticException(null, "Incompatible types");
			}
			break;
		case OP_EQ: case OP_NEQ: case OP_LE: case OP_LT: case OP_GE: case OP_GT:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				expressionBinary.type = Type.BOOLEAN;
			}else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				expressionBinary.type = Type.BOOLEAN;
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				expressionBinary.type = Type.BOOLEAN;
			} else {
				throw new SemanticException(null, "Incompatible types");
			}
			break;
			
		case OP_AND: case OP_OR:
			if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				expressionBinary.type = Type.INTEGER;
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				expressionBinary.type = Type.BOOLEAN;
			} else {
				throw new SemanticException(null, "Incompatible types");
			}
			break;
		
		case OP_MOD:
			if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				expressionBinary.type = Type.INTEGER;
			} else {
				throw new SemanticException(null, "Incompatible types");
			}
			break;
		
		default:
			throw new SemanticException(null, "unknown operator");
		}
		
		
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression e = expressionUnary.expression;
		e.visit(this, arg);
		expressionUnary.type = e.type;
		return null;
//		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionIntegerLiteral(ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		expressionIntegerLiteral.type = Type.INTEGER;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitBooleanLiteral(ExpressionBooleanLiteral expressionBooleanLiteral, Object arg) throws Exception {
		// TODO Auto-generated method stub
		expressionBooleanLiteral.type = Type.BOOLEAN;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionPredefinedName(ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		expressionPredefinedName.type = Type.INTEGER;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionFloatLiteral(ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		expressionFloatLiteral.type = Type.FLOAT;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg expressionFunctionAppWithExpressionArg, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Expression e = expressionFunctionAppWithExpressionArg.e;
		e.visit(this, arg);
		Kind function = expressionFunctionAppWithExpressionArg.function;
		switch(function) {
		case KW_red: case KW_alpha: case KW_green: case KW_blue:
			if(e.type == Type.INTEGER) {
				expressionFunctionAppWithExpressionArg.type = Type.INTEGER;
			} else {
				throw new SemanticException(null, "int expected");
			}
			break;
		case KW_sin: case KW_cos: case KW_atan: case KW_log:
			if(e.type == Type.FLOAT) {
				expressionFunctionAppWithExpressionArg.type = Type.FLOAT;
			} else {
				throw new SemanticException(null, "float expected");
			}
			break;
		case KW_width: case KW_height:
			if(e.type == Type.IMAGE) {
				expressionFunctionAppWithExpressionArg.type = Type.INTEGER;
			} else {
				throw new SemanticException(null, "type image expected");
			}
			break;	
		case KW_abs:
			if(e.type == Type.INTEGER) {
				expressionFunctionAppWithExpressionArg.type = Type.INTEGER;
			} else if (e.type == Type.FLOAT) {
				expressionFunctionAppWithExpressionArg.type = Type.FLOAT;
			} else {
				throw new SemanticException(null, "type image expected");
			}
			break;	
		case KW_int:
			if(e.type == Type.INTEGER) {
				expressionFunctionAppWithExpressionArg.type = Type.INTEGER;
			} else if (e.type == Type.FLOAT) {
				expressionFunctionAppWithExpressionArg.type = Type.INTEGER;
			} else {
				throw new SemanticException(null, "int or float expected");
			}
			break;			
		case KW_float:
			if(e.type == Type.INTEGER) {
				expressionFunctionAppWithExpressionArg.type = Type.FLOAT;
			} else if (e.type == Type.FLOAT) {
				expressionFunctionAppWithExpressionArg.type = Type.FLOAT;
			} else {
				throw new SemanticException(null, "int or float expected");
			}
			break;		
		default:
			throw new SemanticException(null, "unknown function");
		}
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionFunctionAppWithPixel(ExpressionFunctionAppWithPixel expressionFunctionAppWithPixel,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression e0 = expressionFunctionAppWithPixel.e0;
		Expression e1 = expressionFunctionAppWithPixel.e1;
		e0.visit(this, arg);
		e1.visit(this, arg);
		Kind name = expressionFunctionAppWithPixel.name;
		if(name == Kind.KW_cart_x || name == Kind.KW_cart_y) {
			if(e0.type != Type.FLOAT || e1.type != Type.FLOAT) {
				throw new SemanticException(null, "Type float expected");
			}
			expressionFunctionAppWithPixel.type = Type.INTEGER;
		}
		if(name == Kind.KW_polar_a || name == Kind.KW_polar_r) {
			if(e0.type != Type.INTEGER || e1.type != Type.INTEGER) {
				throw new SemanticException(null, "Type int expected");
			}
			expressionFunctionAppWithPixel.type = Type.FLOAT;		
		}
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionPixelConstructor(ExpressionPixelConstructor expressionPixelConstructor, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Expression alpha = expressionPixelConstructor.alpha;
		Expression red = expressionPixelConstructor.red;
		Expression green = expressionPixelConstructor.green;
		Expression blue = expressionPixelConstructor.blue;
		alpha.visit(this, arg);
		red.visit(this, arg);
		green.visit(this, arg);
		blue.visit(this, arg);
		if (alpha.type != Type.INTEGER || red.type != Type.INTEGER || green.type != Type.INTEGER || blue.type != Type.INTEGER) {
			throw new SemanticException(null, "Integer expected");
		}
		expressionPixelConstructor.type = Type.INTEGER;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {
		// TODO Auto-generated method stub
		LHS lhs = statementAssign.lhs;
		Expression e = statementAssign.e;
		lhs.visit(this, arg);
		e.visit(this, arg);
		if(lhs.type != e.type) {
			throw new SemanticException(null, "Unequal types");
		}
		
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression e = statementShow.e;
		e.visit(this, arg);
		if(e.type != Type.INTEGER && e.type != Type.BOOLEAN && e.type != Type.FLOAT && e.type != Type.IMAGE) {
			throw new SemanticException(null, "Uncompatible type");
		}
		
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel, Object arg) throws Exception {
		// TODO Auto-generated method stub
		String ident = expressionPixel.name;
		PixelSelector ps = expressionPixel.pixelSelector;
		ps.visit(this, arg);
		Declaration dec = st.lookup(ident);
		expressionPixel.dec = dec;
		if(dec == null) {
			throw new SemanticException(null, "Undeclared");
		}
		if( Types.getType(dec.type) != Type.IMAGE) {
			throw new SemanticException(null, "Image required");
		}
		expressionPixel.type = Type.INTEGER;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Declaration dec = st.lookup(expressionIdent.name);
		expressionIdent.dec = dec;
		if(dec == null) {
			throw new SemanticException(null, "Undeclared");
		}
		expressionIdent.type = Types.getType(dec.type);
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Declaration dec = st.lookup(lhsSample.name);
		lhsSample.dec = dec;
		PixelSelector ps = lhsSample.pixelSelector;
		ps.visit(this, arg);
		if(dec == null) {
			throw new SemanticException(null, "Undeclared");
		}
		if(Types.getType(dec.type)!=Type.IMAGE) {
			throw new SemanticException(null, "type image expected");
		}
		lhsSample.type = Type.INTEGER;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Declaration dec = st.lookup(lhsPixel.name);
		lhsPixel.dec = dec;
		PixelSelector ps = lhsPixel.pixelSelector;
		ps.visit(this, arg);
		if(dec == null) {
			throw new SemanticException(null, "Undeclared");
		}
		if(Types.getType(dec.type)!=Type.IMAGE) {
			throw new SemanticException(null, "type image expected");
		}
		lhsPixel.type = Type.INTEGER;
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Declaration dec = st.lookup(lhsIdent.name);
		lhsIdent.dec = dec;
		if(dec == null) {
			throw new SemanticException(null, "Undeclared");
		}
		lhsIdent.type = Types.getType(dec.type);
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression guard = statementIf.guard;
		Block b = statementIf.b;
		guard.visit(this, arg);
		b.visit(this, arg);
		if(guard.type != Type.BOOLEAN) {
			throw new SemanticException(null, "bool expected");
		}
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression guard = statementWhile.guard;
		Block b = statementWhile.b;
		guard.visit(this, arg);
		b.visit(this, arg);
		if(guard.type != Type.BOOLEAN) {
			throw new SemanticException(null, "bool expected");
		}
		return null;
		//throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression duration = statementSleep.duration;
		duration.visit(this, arg);
		if(duration.type != Type.INTEGER) {
			throw new SemanticException(null, "int expected");
		}
		
		return null;
		//throw new UnsupportedOperationException();
	}


}
