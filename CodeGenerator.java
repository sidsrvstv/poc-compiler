/**
 * Starter code for CodeGenerator.java used n the class project in COP5556 Programming Language Principles 
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


package cop5556sp18;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
import cop5556sp18.TypeChecker.SemanticException;
import cop5556sp18.CodeGenUtils;
import cop5556sp18.Scanner.Kind;

public class CodeGenerator implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */

	static final int Z = 255;

	private static final Object ACONSTNULL = null;

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;
	
	Integer slotNumber;
		
	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	final int defaultWidth;
	final int defaultHeight;
	// final boolean itf = false;
	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 * @param defaultWidth
	 *            default width of images
	 * @param defaultHeight
	 *            default height of images
	 */
	public CodeGenerator(boolean DEVEL, boolean GRADE, String sourceFileName,
			int defaultWidth, int defaultHeight) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
		this.slotNumber = 1;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO refactor and extend as necessary
		for (ASTNode node : block.decsOrStatements) {
			node.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitBooleanLiteral(
			ExpressionBooleanLiteral expressionBooleanLiteral, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		mv.visitLdcInsn(expressionBooleanLiteral.value);
		return null;
		
	}

	@Override 
	public Object visitDeclaration(Declaration declaration, Object arg)
			throws Exception {
		declaration.slot = slotNumber;
		Label decStart = new Label();
		mv.visitLabel(decStart);
		
		if(Types.getType(declaration.type) == Type.IMAGE) {
			if(declaration.height != null && declaration.width != null) {
				declaration.width.visit(this, arg);
				declaration.height.visit(this, arg);	
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "makeImage", RuntimeImageSupport.makeImageSig, false);
				mv.visitVarInsn(ASTORE, declaration.slot);
			} else {
				mv.visitLdcInsn(this.defaultWidth);
				mv.visitLdcInsn(this.defaultHeight);	
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "makeImage", RuntimeImageSupport.makeImageSig, false);
				mv.visitVarInsn(ASTORE, declaration.slot);		
			}
		} else {
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, declaration.slot);
		}
		slotNumber++;
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary,
			Object arg) throws Exception {
		

		Expression e0 = expressionBinary.leftExpression;
		Expression e1 = expressionBinary.rightExpression;
		e0.visit(this, arg);
		e1.visit(this, arg);
		
		Label elsePart = new Label();
		Label goToEnd = new Label();
		
		switch(expressionBinary.op) {
		case OP_PLUS: 
			if(e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitInsn(IADD);
			} else if (e0.type == Type.INTEGER && e1.type == Type.FLOAT) {
				mv.visitInsn(SWAP);
				mv.visitInsn(I2F);
				mv.visitInsn(FADD);
			} else if (e0.type == Type.FLOAT && e1.type == Type.INTEGER) {
				mv.visitInsn(I2F);
				mv.visitInsn(FADD);
			} else if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FADD);
			} 
			break;
		case OP_MINUS:
			if(e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitInsn(ISUB);
			} else if (e0.type == Type.INTEGER && e1.type == Type.FLOAT) {
				mv.visitInsn(SWAP);
				mv.visitInsn(I2F);
				mv.visitInsn(SWAP);
				mv.visitInsn(FSUB);				
			} else if (e0.type == Type.FLOAT && e1.type == Type.INTEGER) {
				mv.visitInsn(I2F);
				mv.visitInsn(FSUB);
			} else if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FSUB);		
			} 
			break;
		case OP_TIMES:
			if(e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitInsn(IMUL);
			} else if (e0.type == Type.INTEGER && e1.type == Type.FLOAT) {
				mv.visitInsn(SWAP);
				mv.visitInsn(I2F);
				mv.visitInsn(FMUL);
			} else if (e0.type == Type.FLOAT && e1.type == Type.INTEGER) {
				mv.visitInsn(I2F);
				mv.visitInsn(FMUL);
			} else if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FMUL);
			} 
			break;
		case OP_DIV:
			if(e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitInsn(IDIV);
			} else if (e0.type == Type.INTEGER && e1.type == Type.FLOAT) {
				mv.visitInsn(SWAP);
				mv.visitInsn(I2F);
				mv.visitInsn(SWAP);
				mv.visitInsn(FDIV);				
			} else if (e0.type == Type.FLOAT && e1.type == Type.INTEGER) {
				mv.visitInsn(I2F);
				mv.visitInsn(FDIV);
			} else if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FDIV);		
			} 
			break;
		case OP_POWER:
			// TO DO
			if(e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitInsn(POP);
				mv.visitInsn(POP);
				e0.visit(this, arg);
				mv.visitInsn(I2D);
				e1.visit(this, arg);				
				mv.visitInsn(I2D);				
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2I);
			} else if (e0.type == Type.INTEGER && e1.type == Type.FLOAT) {
				mv.visitInsn(POP);
				mv.visitInsn(POP);
				e0.visit(this, arg);
				mv.visitInsn(I2D);
				e1.visit(this, arg);				
				mv.visitInsn(F2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2F);
			} else if (e0.type == Type.FLOAT && e1.type == Type.INTEGER) {
				mv.visitInsn(POP);
				mv.visitInsn(POP);
				e0.visit(this, arg);
				mv.visitInsn(F2D);
				e1.visit(this, arg);				
				mv.visitInsn(I2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2F);
			} else if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(POP);
				mv.visitInsn(POP);
				e0.visit(this, arg);
				mv.visitInsn(F2D);
				e1.visit(this, arg);				
				mv.visitInsn(F2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2F);
			}
			
			break;
		
		case OP_EQ:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FCMPL); 
				mv.visitJumpInsn(IFEQ, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitJumpInsn(IF_ICMPEQ, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPEQ, elsePart);
				mv.visitLdcInsn(false);
			} 
			mv.visitJumpInsn(GOTO, goToEnd);
			mv.visitLabel(elsePart);
			mv.visitLdcInsn(true);
			mv.visitLabel(goToEnd);
			break;
		case OP_NEQ:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FCMPL); 
				mv.visitJumpInsn(IFNE, elsePart);
				mv.visitLdcInsn(false);
			}else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitJumpInsn(IF_ICMPNE, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPNE, elsePart);
				mv.visitLdcInsn(false);
			} 
			mv.visitJumpInsn(GOTO, goToEnd);
			mv.visitLabel(elsePart);
			mv.visitLdcInsn(true);
			mv.visitLabel(goToEnd);
			break;
		case OP_LE:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FCMPL); 
				mv.visitJumpInsn(IFLE, elsePart);
				mv.visitLdcInsn(false);
			}else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitJumpInsn(IF_ICMPLE, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPLE, elsePart);
				mv.visitLdcInsn(false);
			} 
			mv.visitJumpInsn(GOTO, goToEnd);
			mv.visitLabel(elsePart);
			mv.visitLdcInsn(true);
			mv.visitLabel(goToEnd);
			break;
		case OP_LT:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FCMPL); 
				mv.visitJumpInsn(IFLT, elsePart);
				mv.visitLdcInsn(false);
			}else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitJumpInsn(IF_ICMPLT, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPLT, elsePart);
				mv.visitLdcInsn(false);
			} 
			mv.visitJumpInsn(GOTO, goToEnd);
			mv.visitLabel(elsePart);
			mv.visitLdcInsn(true);
			mv.visitLabel(goToEnd);
			break;
		case OP_GT:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FCMPL); 
				mv.visitJumpInsn(IFGT, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitJumpInsn(IF_ICMPGT, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPGT, elsePart);
				mv.visitLdcInsn(false);
			} 
			mv.visitJumpInsn(GOTO, goToEnd);
			mv.visitLabel(elsePart);
			mv.visitLdcInsn(true);
			mv.visitLabel(goToEnd);
			break;
		case OP_GE:
			if (e0.type == Type.FLOAT && e1.type == Type.FLOAT) {
				mv.visitInsn(FCMPL); 
				mv.visitJumpInsn(IFGE, elsePart);
				mv.visitLdcInsn(false);
			}else if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitJumpInsn(IF_ICMPGE, elsePart);
				mv.visitLdcInsn(false);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitJumpInsn(IF_ICMPGE, elsePart);
				mv.visitLdcInsn(false);
			}
			mv.visitJumpInsn(GOTO, goToEnd);
			mv.visitLabel(elsePart);
			mv.visitLdcInsn(true);
			mv.visitLabel(goToEnd);
			break;
		case OP_AND: 
			if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {  //Implement
				mv.visitInsn(IAND);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitInsn(IAND);
			} 
			break;
		case OP_OR:
			if (e0.type == Type.INTEGER && e1.type == Type.INTEGER) {
				mv.visitInsn(IOR);
			} else if (e0.type == Type.BOOLEAN && e1.type == Type.BOOLEAN) {
				mv.visitInsn(IOR);
			} 
			break;
		
		case OP_MOD:			
			mv.visitInsn(IREM);
			break;
		
		}
		
		return null;
	}

	@Override
	public Object visitExpressionConditional(
			ExpressionConditional expressionConditional, Object arg)
			throws Exception {
		Label elsePart = new Label();
		Label goToEnd = new Label();
		expressionConditional.guard.visit(this, arg);
		mv.visitJumpInsn(IFNE, elsePart);
		expressionConditional.falseExpression.visit(this, arg);
		mv.visitJumpInsn(Opcodes.GOTO, goToEnd);
		mv.visitLabel(elsePart);
		expressionConditional.trueExpression.visit(this, arg);
		mv.visitLabel(goToEnd);
		return null;
	}

	@Override
	public Object visitExpressionFloatLiteral(
			ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {
		
		mv.visitLdcInsn(expressionFloatLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg expressionFunctionAppWithExpressionArg,
			Object arg) throws Exception {
		Expression e = expressionFunctionAppWithExpressionArg.e;
		e.visit(this, arg);
		Kind function = expressionFunctionAppWithExpressionArg.function;
		switch(function) {
		case KW_red:
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getRed", RuntimePixelOps.getRedSig, false);
			break;
		case KW_alpha: 
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getAlpha", RuntimePixelOps.getAlphaSig, false);
			break;
		case KW_green: 
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getGreen", RuntimePixelOps.getGreenSig, false);
			break;
		case KW_blue:
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getBlue", RuntimePixelOps.getBlueSig, false);
			break;
		case KW_sin: 
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			mv.visitInsn(D2F);
			break;
		case KW_cos: 
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			mv.visitInsn(D2F);		
			break;
		case KW_atan: 
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "atan", "(D)D", false);
			mv.visitInsn(D2F);	
			break;
		case KW_log:
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "log", "(D)D", false);
			mv.visitInsn(D2F);
			break;
		case KW_width:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className,"getWidth",RuntimeImageSupport.getWidthSig ,false);
			break;
		case KW_height:
			 mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "getHeight", RuntimeImageSupport.getHeightSig, false);
			break;	
		case KW_abs:
			if(e.type == Type.INTEGER) {
				mv.visitInsn(I2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
				mv.visitInsn(D2I);
			} else if (e.type == Type.FLOAT) {
				mv.visitInsn(F2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
				mv.visitInsn(D2F);
			} 
			break;	
		case KW_int:
			if (e.type == Type.FLOAT) {
				mv.visitInsn(F2I);
			} 
			break;			
		case KW_float:
			if(e.type == Type.INTEGER) {
				mv.visitInsn(I2F);
			}  
			break;		
		
		}
		return null;
		
	}

	@Override
	public Object visitExpressionFunctionAppWithPixel(
			ExpressionFunctionAppWithPixel expressionFunctionAppWithPixel,
			Object arg) throws Exception {
		Expression e0 = expressionFunctionAppWithPixel.e0; // r, x
		Expression e1 = expressionFunctionAppWithPixel.e1; // theta, y
		Kind name = expressionFunctionAppWithPixel.name;
		switch(name) {
		case KW_cart_x:
			e1.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false); // cos theta
			e0.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitInsn(DMUL); // r * cos theta
			mv.visitInsn(D2I);
			break;
		case KW_cart_y:
			e1.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			e0.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
			break;
		case KW_polar_a:
			e1.visit(this, arg);
			mv.visitInsn(I2D);
			e0.visit(this, arg);
			mv.visitInsn(I2D);			
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "atan2", "(DD)D", false); // atan2 y x
			mv.visitInsn(D2F);		
			break;
		case KW_polar_r:
			e0.visit(this, arg);
			mv.visitInsn(I2D);
			e1.visit(this, arg);
			mv.visitInsn(I2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "hypot", "(DD)D", false); // hyplot x y 
			mv.visitInsn(D2F);			
			break;
		
		}		
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent,
			Object arg) throws Exception {
		System.out.println(expressionIdent.dec.slot);
		
		switch(expressionIdent.type) {
			case INTEGER:
				mv.visitVarInsn(ILOAD, expressionIdent.dec.slot);
				break;
			case FLOAT:
				mv.visitVarInsn(FLOAD, expressionIdent.dec.slot);
				break;
			case BOOLEAN:
				mv.visitVarInsn(ILOAD, expressionIdent.dec.slot);
				break;
			case IMAGE:
				mv.visitVarInsn(ALOAD, expressionIdent.dec.slot);
				break;
			case FILE:
				mv.visitVarInsn(ALOAD, expressionIdent.dec.slot);
				break;
		}				
		return null;
	}

	@Override
	public Object visitExpressionIntegerLiteral(
			ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {
		// This one is all done!
		System.out.println("expressionIntegerLiteral visited");
		mv.visitLdcInsn(expressionIntegerLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel,
			Object arg) throws Exception {
		mv.visitVarInsn(ALOAD, expressionPixel.dec.slot);
		expressionPixel.pixelSelector.ex.visit(this, arg);
		expressionPixel.pixelSelector.ey.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "getPixel", RuntimeImageSupport.getPixelSig, false);
		return null;
	}

	@Override
	public Object visitExpressionPixelConstructor(
			ExpressionPixelConstructor expressionPixelConstructor, Object arg)
			throws Exception {
		Expression alpha = expressionPixelConstructor.alpha;
		Expression red = expressionPixelConstructor.red;
		Expression green = expressionPixelConstructor.green;
		Expression blue = expressionPixelConstructor.blue;
		alpha.visit(this, arg);
		red.visit(this, arg);
		green.visit(this, arg);
		blue.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "makePixel", RuntimePixelOps.makePixelSig, false);
		return null;
	}

	@Override
	public Object visitExpressionPredefinedName(
			ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {
		switch(expressionPredefinedName.name) {
		case KW_Z:
			mv.visitLdcInsn(Z);
			break;
		default:		
			mv.visitLdcInsn(this.defaultHeight);
			mv.visitLdcInsn(this.defaultWidth);
			break;
		}
		return null;
	}

	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary,
			Object arg) throws Exception {
		Expression e0 = expressionUnary.expression;
		e0.visit(this, arg);
		Kind op = expressionUnary.op;
		switch(op) {
			case OP_EXCLAMATION:
				if(e0.type == Type.INTEGER) {
					mv.visitInsn(ICONST_M1);
					mv.visitInsn(IXOR);
				} else if(e0.type == Type.BOOLEAN) {
					mv.visitInsn(ICONST_1);
					mv.visitInsn(IXOR);
				}
				break;
				
			case OP_MINUS:
				if(e0.type == Type.INTEGER) {
					mv.visitInsn(ICONST_M1); //try INEG
					mv.visitInsn(IMUL);
				} else if(e0.type == Type.FLOAT) {
					mv.visitInsn(FNEG);
				}
				break;
		
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg)
			throws Exception {
		if(lhsIdent.type == Type.IMAGE) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "deepCopy",RuntimeImageSupport.deepCopySig, false);
			mv.visitVarInsn(Opcodes.ASTORE, lhsIdent.dec.slot);
		} else if(lhsIdent.type == Type.BOOLEAN) {
			mv.visitVarInsn(Opcodes.ISTORE, lhsIdent.dec.slot);
		} else if(lhsIdent.type == Type.INTEGER) {
			mv.visitVarInsn(Opcodes.ISTORE, lhsIdent.dec.slot);
		} else if(lhsIdent.type == Type.FLOAT) {
			mv.visitVarInsn(Opcodes.FSTORE, lhsIdent.dec.slot);
		} else {
			mv.visitVarInsn(Opcodes.ASTORE, lhsIdent.dec.slot);
		}
		
		return null;
	}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg)
			throws Exception {
		mv.visitVarInsn(ALOAD, lhsPixel.dec.slot);
		PixelSelector ps = lhsPixel.pixelSelector;
		ps.ex.visit(this, arg);
		ps.ey.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "setPixel", RuntimeImageSupport.setPixelSig, false);
		return null;
	}

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg)
			throws Exception {
		mv.visitVarInsn(ALOAD,lhsSample.dec.slot);
		PixelSelector ps = lhsSample.pixelSelector;
		ps.visit(this, arg);
		switch (lhsSample.color) {
		case KW_alpha:
			mv.visitInsn(ICONST_0);
			break;
		case KW_red:
			mv.visitInsn(ICONST_1);
			break;
		case KW_green:
			mv.visitInsn(ICONST_2);
			break;
		case KW_blue:
			mv.visitInsn(ICONST_3);
			break;
		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "updatePixelColor", RuntimeImageSupport.updatePixelColorSig, false);

		return null;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg)
			throws Exception {
		Expression ex = pixelSelector.ex;
		Expression ey = pixelSelector.ey;
		if(ex.type == Type.FLOAT) {
			ey.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			ex.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
			
			ey.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			ex.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
		} else {
			ex.visit(this, arg);
			ey.visit(this, arg);
		}
	
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		// TODO refactor and extend as necessary
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// cw = new ClassWriter(0); //If the call to mv.visitMaxs(1, 1) crashes,
		// it is
		// sometime helpful to
		// temporarily run it without COMPUTE_FRAMES. You probably
		// won't get a completely correct classfile, but
		// you will be able to see the code that was
		// generated.
		className = program.progName;
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null,
				"java/lang/Object", null);
		cw.visitSource(sourceFileName, null);

		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main",
				"([Ljava/lang/String;)V", null, null);
		// initialize
		mv.visitCode();

		// add label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);

		CodeGenUtils.genLog(DEVEL, mv, "entering main");

		program.block.visit(this, arg);

		// generates code to add string to log
		CodeGenUtils.genLog(DEVEL, mv, "leaving main");

		// adds the required (by the JVM) return statement to main
		mv.visitInsn(RETURN);

		// adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart,
				mainEnd, 0);
		// Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the
		// constructor,
		// asm will calculate this itself and the parameters are ignored.
		// If you have trouble with failures in this routine, it may be useful
		// to temporarily change the parameter in the ClassWriter constructor
		// from COMPUTE_FRAMES to 0.
		// The generated classfile will not be correct, but you will at least be
		// able to see what is in it.
		mv.visitMaxs(0, 0);

		// terminate construction of main method
		mv.visitEnd();

		// terminate class construction
		cw.visitEnd();

		// generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign,
			Object arg) throws Exception {
		statementAssign.e.visit(this, arg);
		statementAssign.lhs.visit(this, arg);
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg)
			throws Exception {
		Label goToEnd = new Label();
		statementIf.guard.visit(this, arg);
		mv.visitJumpInsn(IFEQ, goToEnd);
		statementIf.b.visit(this, arg);
		mv.visitLabel(goToEnd);
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg)
			throws Exception {
		// first load the argument 
		mv.visitVarInsn(ALOAD, 0);
		statementInput.e.visit(this, arg);
		mv.visitInsn(AALOAD);
		
		Type type = Types.getType(statementInput.dec.type);
		
		switch(type) {
		case INTEGER:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitVarInsn(ISTORE, statementInput.dec.slot);
			break;
		case FLOAT:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", false);
			mv.visitVarInsn(FSTORE, statementInput.dec.slot);
			break;
		case BOOLEAN:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitVarInsn(ISTORE, statementInput.dec.slot);
			break;
		case IMAGE:
			if(statementInput.dec.height != null && statementInput.dec.width != null ) {
				statementInput.dec.width.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				statementInput.dec.height.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);				
			} else {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
			}
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "readImage",RuntimeImageSupport.readImageSig, false);
			mv.visitVarInsn(ASTORE, statementInput.dec.slot);
			break;
		case FILE:
			mv.visitVarInsn(ASTORE, statementInput.dec.slot);
			break;
		
		}
		
		return null;
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg)
			throws Exception {
		/**
		 * TODO refactor and complete implementation.
		 * 
		 * For integers, booleans, and floats, generate code to print to
		 * console. For images, generate code to display in a frame.
		 * 
		 * In all cases, invoke CodeGenUtils.genLogTOS(GRADE, mv, type); before
		 * consuming top of stack.
		 */
		statementShow.e.visit(this, arg);
		Type type = statementShow.e.getType();
		switch (type) {
			case INTEGER : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(I)V", false);
			}
				break;
			case BOOLEAN : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(Z)V", false);
				
			}
			break;
			// break; commented out because currently unreachable. You will need
			// it.
			case FLOAT : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(F)V", false);
				
			}
			break;
			// break; commented out because currently unreachable. You will need
			// it.
			case IMAGE : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className,
						"makeFrame", RuntimeImageSupport.makeFrameSig, false);
				mv.visitInsn(POP);
			}
			break;

		}
		return null;
	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg)
			throws Exception {
		
		statementSleep.duration.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toUnsignedLong", "(I)J", false);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread",
				"sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg)
			throws Exception {
		Label endOfLoop = new Label();
		Label stayHere = new Label();
		mv.visitJumpInsn(GOTO, endOfLoop);
		mv.visitLabel(stayHere);
		statementWhile.b.visit(this, arg);
		mv.visitLabel(endOfLoop);
		statementWhile.guard.visit(this, arg);
		mv.visitJumpInsn(IFNE, stayHere);
		return null;
		
	}

	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg)
			throws Exception {
		mv.visitVarInsn(ALOAD, statementWrite.sourceDec.slot);
		mv.visitVarInsn(ALOAD, statementWrite.destDec.slot);
		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "write", RuntimeImageSupport.writeSig,
				false);
		return null;
	}

}
