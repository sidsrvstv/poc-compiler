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

import cop5556sp18.CodeGenUtils;

public class CodeGenerator implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */

	static final int Z = 255;

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	final int defaultWidth; // Check if 0
	final int defaultHeight; // Check if 0

	public static int slotNumber = 1; // Check if 0

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
	public CodeGenerator(boolean DEVEL, boolean GRADE, String sourceFileName, int defaultWidth, int defaultHeight) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
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
	public Object visitBooleanLiteral(ExpressionBooleanLiteral expressionBooleanLiteral, Object arg) throws Exception {
		mv.visitLdcInsn(expressionBooleanLiteral.value);
		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg) throws Exception {
		declaration.setSlot(slotNumber);
		// Label decStart = new Label();
		// mv.visitLabel(decStart);
		if (Types.getType(declaration.type) == Type.IMAGE) {
			if (declaration.width != null && declaration.height != null) {
				declaration.width.visit(this, arg);
				declaration.height.visit(this, arg);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "makeImage",
						RuntimeImageSupport.makeImageSig, false);
				mv.visitVarInsn(Opcodes.ASTORE, declaration.getSlot());
			} else if (declaration.width == null && declaration.height == null) {
				mv.visitLdcInsn(this.defaultWidth);
				mv.visitLdcInsn(this.defaultHeight);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "makeImage",
						RuntimeImageSupport.makeImageSig, false);
				mv.visitVarInsn(Opcodes.ASTORE, declaration.getSlot());
			}
		} else {
			mv.visitInsn(Opcodes.ACONST_NULL); // Check LdcInsn vs Insn
			mv.visitVarInsn(Opcodes.ASTORE, declaration.getSlot());
		}
		slotNumber++;
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws Exception {
		Label setTrue = new Label();
		Label endTrue = new Label();
		expressionBinary.leftExpression.visit(this, arg);
		expressionBinary.rightExpression.visit(this, arg);
		Type t1 = expressionBinary.leftExpression.type;
		Type t2 = expressionBinary.rightExpression.type;

		if (t1.equals(Type.INTEGER) && t2.equals(Type.INTEGER)) {
			switch (expressionBinary.op) { // Check for &, |
			case OP_PLUS:
				mv.visitInsn(Opcodes.IADD);
				break;
			case OP_MINUS:
				mv.visitInsn(Opcodes.ISUB);
				break;
			case OP_TIMES:
				mv.visitInsn(Opcodes.IMUL);
				break;
			case OP_DIV:
				mv.visitInsn(Opcodes.IDIV);
				break;
			case OP_AND:
				mv.visitInsn(Opcodes.IAND);
				break;
			case OP_OR:
				mv.visitInsn(Opcodes.IOR);
				break;
			case OP_MOD:
				mv.visitInsn(Opcodes.IREM);
				break;
			case OP_POWER:
				mv.visitInsn(Opcodes.POP);
				mv.visitInsn(Opcodes.I2D);
				expressionBinary.rightExpression.visit(this, arg);
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(Opcodes.D2I);
				break;
			case OP_NEQ:
				mv.visitJumpInsn(IF_ICMPNE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_EQ:
				mv.visitJumpInsn(IF_ICMPEQ, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_LE:
				mv.visitJumpInsn(IF_ICMPLE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_LT:
				mv.visitJumpInsn(IF_ICMPLT, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_GE:
				mv.visitJumpInsn(IF_ICMPGE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_GT:
				mv.visitJumpInsn(IF_ICMPGT, setTrue);
				mv.visitLdcInsn(false);
				break;
			default:
				break;
			}
			mv.visitJumpInsn(Opcodes.GOTO, endTrue);
			mv.visitLabel(setTrue);
			mv.visitLdcInsn(true);
			mv.visitLabel(endTrue);
		} else if (t1.equals(Type.FLOAT) && t2.equals(Type.FLOAT)) {
			switch (expressionBinary.op) { // Check for &, |
			case OP_PLUS:
				mv.visitInsn(Opcodes.FADD);
				break;
			case OP_MINUS:
				mv.visitInsn(Opcodes.FSUB);
				break;
			case OP_TIMES:
				mv.visitInsn(Opcodes.FMUL);
				break;
			case OP_DIV:
				mv.visitInsn(Opcodes.FDIV);
				break;
			case OP_POWER:
				mv.visitInsn(Opcodes.POP);
				mv.visitInsn(Opcodes.F2D);
				expressionBinary.rightExpression.visit(this, arg);
				mv.visitInsn(Opcodes.F2D);// for power
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(Opcodes.D2F);
				break;
			case OP_NEQ: // Check for working in test cases
				mv.visitInsn(Opcodes.FCMPL); //FCMPL IFEQ
				mv.visitJumpInsn(Opcodes.IFNE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_EQ:
				mv.visitInsn(Opcodes.FCMPL); //FCMPL IFNE
				mv.visitJumpInsn(Opcodes.IFEQ, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_LE:
				mv.visitInsn(Opcodes.FCMPG); //FCMPG IFGT
				mv.visitJumpInsn(Opcodes.IFLE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_LT:
				mv.visitInsn(Opcodes.FCMPG); //FCMPG IFGE
				mv.visitJumpInsn(Opcodes.IFLT, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_GE:
				mv.visitInsn(Opcodes.FCMPL); //FCMPL IFLT
				mv.visitJumpInsn(Opcodes.IFGE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_GT:
				mv.visitInsn(Opcodes.FCMPL); //FCMPL IFLE
				mv.visitJumpInsn(Opcodes.IFGT, setTrue);
				mv.visitLdcInsn(false);
				break;
			default:
				break;
			}
			mv.visitJumpInsn(Opcodes.GOTO, endTrue);
			mv.visitLabel(setTrue);
			mv.visitLdcInsn(true);
			mv.visitLabel(endTrue);
		} else if ((t1.equals(Type.FLOAT) && t2.equals(Type.INTEGER))
				|| (t1.equals(Type.INTEGER) && t2.equals(Type.FLOAT))) {
			switch (expressionBinary.op) {
			case OP_PLUS:
				if (t1.equals(Type.INTEGER)) {
					mv.visitInsn(Opcodes.POP);
					mv.visitInsn(Opcodes.I2F);
					expressionBinary.rightExpression.visit(this, arg);
					mv.visitInsn(Opcodes.FADD);
				} else if (t1.equals(Type.FLOAT)) {
					mv.visitInsn(Opcodes.I2F);
					mv.visitInsn(Opcodes.FADD);
				}
				break;
			case OP_MINUS:
				if (t1.equals(Type.INTEGER)) {
					mv.visitInsn(Opcodes.POP);
					mv.visitInsn(Opcodes.I2F);
					expressionBinary.rightExpression.visit(this, arg);
					mv.visitInsn(Opcodes.FSUB);
				} else if (t1.equals(Type.FLOAT)) {
					mv.visitInsn(Opcodes.I2F);
					mv.visitInsn(Opcodes.FSUB);
				}
				break;
			case OP_TIMES:
				if (t1.equals(Type.INTEGER)) {
					mv.visitInsn(Opcodes.POP);
					mv.visitInsn(Opcodes.I2F);
					expressionBinary.rightExpression.visit(this, arg);
					mv.visitInsn(Opcodes.FMUL);
				} else if (t1.equals(Type.FLOAT)) {
					mv.visitInsn(Opcodes.I2F);
					mv.visitInsn(Opcodes.FMUL);
				}
				break;
			case OP_DIV:
				if (t1.equals(Type.INTEGER)) {
					mv.visitInsn(Opcodes.POP);
					mv.visitInsn(Opcodes.I2F);
					expressionBinary.rightExpression.visit(this, arg);
					mv.visitInsn(Opcodes.FDIV);
				} else if (t1.equals(Type.FLOAT)) {
					mv.visitInsn(Opcodes.I2F);
					mv.visitInsn(Opcodes.FDIV);
				}
				break;
			case OP_POWER:
				if (t1.equals(Type.FLOAT)) {
					mv.visitInsn(Opcodes.POP);
					mv.visitInsn(Opcodes.F2D);
					expressionBinary.rightExpression.visit(this, arg);
					mv.visitInsn(Opcodes.I2D);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
					mv.visitInsn(Opcodes.D2F);
				} else if (t1.equals(Type.INTEGER)) {
					mv.visitInsn(Opcodes.POP);
					mv.visitInsn(Opcodes.I2D);
					expressionBinary.rightExpression.visit(this, arg);
					mv.visitInsn(Opcodes.F2D);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
					mv.visitInsn(Opcodes.D2F);
				}
			default:
				break;
			}
			mv.visitJumpInsn(Opcodes.GOTO, endTrue);
			mv.visitLabel(setTrue);
			mv.visitLdcInsn(true);
			mv.visitLabel(endTrue);
		}

		else if (t1.equals(Type.BOOLEAN) && t2.equals(Type.BOOLEAN)) { // Add relational operators
			switch (expressionBinary.op) {
			case OP_AND:
				mv.visitInsn(Opcodes.IAND);
				break;
			case OP_OR:
				mv.visitInsn(Opcodes.IOR);
				break;
			case OP_NEQ:
				mv.visitJumpInsn(IF_ICMPNE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_EQ:
				mv.visitJumpInsn(IF_ICMPEQ, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_LE:
				mv.visitJumpInsn(IF_ICMPLE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_LT:
				mv.visitJumpInsn(IF_ICMPLT, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_GE:
				mv.visitJumpInsn(IF_ICMPGE, setTrue);
				mv.visitLdcInsn(false);
				break;
			case OP_GT:
				mv.visitJumpInsn(IF_ICMPGT, setTrue);
				mv.visitLdcInsn(false);
				break;
			}
			mv.visitJumpInsn(Opcodes.GOTO, endTrue);
			mv.visitLabel(setTrue);
			mv.visitLdcInsn(true);
			mv.visitLabel(endTrue);
		}
		return null; // Add all other relational operators for next assignment
	}

	@Override
	public Object visitExpressionConditional(ExpressionConditional expressionConditional, Object arg) throws Exception { // Next
		// assignment
		Label setTrue = new Label();
		Label endTrue = new Label();
		expressionConditional.guard.visit(this, arg);
		mv.visitJumpInsn(Opcodes.IFNE, setTrue);
		expressionConditional.falseExpression.visit(this, arg);
		mv.visitJumpInsn(Opcodes.GOTO, endTrue);
		mv.visitLabel(setTrue);
		expressionConditional.trueExpression.visit(this, arg);
		mv.visitLabel(endTrue);
		return null;
	}

	@Override
	public Object visitExpressionFloatLiteral(ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {
		mv.visitLdcInsn(expressionFloatLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg expressionFunctionAppWithExpressionArg, Object arg)
			throws Exception {
		expressionFunctionAppWithExpressionArg.e.visit(this, arg);
		Type t1 = expressionFunctionAppWithExpressionArg.e.type;
		switch (expressionFunctionAppWithExpressionArg.function) {
		case KW_sin:
			mv.visitInsn(Opcodes.F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			mv.visitInsn(Opcodes.D2F);
			break;
		case KW_cos:
			mv.visitInsn(Opcodes.F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			mv.visitInsn(Opcodes.D2F);
			break;
		case KW_atan:
			mv.visitInsn(Opcodes.F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "atan", "(D)D", false);
			mv.visitInsn(Opcodes.D2F);
			break;
		case KW_log:
			mv.visitInsn(Opcodes.F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "log", "(D)D", false);
			mv.visitInsn(Opcodes.D2F);
			break;

		case KW_abs:
			if (t1.equals(Type.FLOAT)) {
				mv.visitInsn(Opcodes.F2D);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
				mv.visitInsn(Opcodes.D2F);
			} else if (t1.equals(Type.INTEGER)) {
				mv.visitInsn(Opcodes.I2D);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
				mv.visitInsn(Opcodes.D2I);
			}
			break;
		case KW_red:
			if (t1.equals(Type.INTEGER)) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getRed", RuntimePixelOps.getRedSig,
						false);// Check
				// parameters
				// again

			}
			break;
		case KW_blue:
			if (t1.equals(Type.INTEGER)) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getBlue",
						RuntimePixelOps.getBlueSig, // Check parameters again
						false);
			}
			break;
		case KW_green:
			if (t1.equals(Type.INTEGER)) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getGreen",
						RuntimePixelOps.getGreenSig, // Check parameters again
						false);
			}
			break;
		case KW_alpha:
			if (t1.equals(Type.INTEGER)) {
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getAlpha",
						RuntimePixelOps.getAlphaSig, // Check parameters again
						false);
			}
			break;
		case KW_int:
			if (t1.equals(Type.INTEGER)) {
				// Do nothing
			} else if (t1.equals(Type.FLOAT)) {
				mv.visitInsn(Opcodes.F2I);
			}
			break;
		case KW_float:
			if (t1.equals(Type.FLOAT)) {
				// Do nothing
			} else if (t1.equals(Type.INTEGER)) {
				mv.visitInsn(Opcodes.I2F);
			}
			break;
		case KW_width:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "getWidth",
					RuntimeImageSupport.getWidthSig, // Check parameters again
					false);
			break;
		case KW_height:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "getHeight",
					RuntimeImageSupport.getHeightSig, // Check parameters again
					false);
			break;
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithPixel(ExpressionFunctionAppWithPixel expressionFunctionAppWithPixel,
			Object arg) throws Exception { //dont round. cast to int for safety
		if (expressionFunctionAppWithPixel.name == Scanner.Kind.KW_cart_x) {
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(Opcodes.F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(Opcodes.F2D);
			mv.visitInsn(Opcodes.DMUL);
			mv.visitInsn(Opcodes.D2I);
		} else if (expressionFunctionAppWithPixel.name == Scanner.Kind.KW_cart_y) {
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(Opcodes.F2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(Opcodes.F2D);
			mv.visitInsn(Opcodes.DMUL);
			mv.visitInsn(Opcodes.D2I);
		} else if (expressionFunctionAppWithPixel.name == Scanner.Kind.KW_polar_a) {
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(Opcodes.I2D);
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(Opcodes.I2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "atan2", "(DD)D", false); //check if atan2 works
			mv.visitInsn(Opcodes.D2F);
		} else if (expressionFunctionAppWithPixel.name == Scanner.Kind.KW_polar_r) {
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(Opcodes.I2D);
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(Opcodes.I2D);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "hypot", "(DD)D", false); 
			mv.visitInsn(Opcodes.D2F);
		}
		return null;

	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws Exception {
		if (Types.getType(expressionIdent.dec.type).equals(Type.BOOLEAN)
				|| Types.getType(expressionIdent.dec.type).equals(Type.INTEGER)) {
			mv.visitVarInsn(Opcodes.ILOAD, expressionIdent.dec.getSlot());
		} else if (Types.getType(expressionIdent.dec.type).equals(Type.FLOAT)) {
			mv.visitVarInsn(Opcodes.FLOAD, expressionIdent.dec.getSlot());
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, expressionIdent.dec.getSlot());
		}
		return null;
	}

	@Override
	public Object visitExpressionIntegerLiteral(ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {
		// This one is all done!
		mv.visitLdcInsn(expressionIntegerLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel, Object arg) throws Exception {
		mv.visitVarInsn(Opcodes.ALOAD, expressionPixel.dec.getSlot());
		expressionPixel.pixelSelector.ex.visit(this, arg);
		expressionPixel.pixelSelector.ey.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "getPixel",
				RuntimeImageSupport.getPixelSig, false);
		// mv.visitVarInsn(Opcodes.ASTORE, expressionPixel.dec.getSlot());
		return null;
	}

	@Override
	public Object visitExpressionPixelConstructor(ExpressionPixelConstructor expressionPixelConstructor, Object arg)
			throws Exception {
		expressionPixelConstructor.alpha.visit(this, arg);
		expressionPixelConstructor.red.visit(this, arg);
		expressionPixelConstructor.green.visit(this, arg);
		expressionPixelConstructor.blue.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "makePixel", RuntimePixelOps.makePixelSig,
				false);
		return null;
	}

	@Override
	public Object visitExpressionPredefinedName(ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {
		if (expressionPredefinedName.name == Scanner.Kind.KW_Z) {
			mv.visitLdcInsn(Z);
		} else if (expressionPredefinedName.name == Scanner.Kind.KW_default_height) {
			mv.visitLdcInsn(this.defaultHeight);
		} else if (expressionPredefinedName.name == Scanner.Kind.KW_default_width) {
			mv.visitLdcInsn(this.defaultWidth);
		}
		return null;
	}

	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary, Object arg) throws Exception {
		expressionUnary.expression.visit(this, arg);
		if (expressionUnary.op == Scanner.Kind.OP_MINUS) {
			if (expressionUnary.expression.type == Type.INTEGER) {
				mv.visitInsn(Opcodes.INEG);
			} else if (expressionUnary.expression.type == Type.FLOAT) {
				mv.visitInsn(Opcodes.FNEG);
			}
		} else if (expressionUnary.op == Scanner.Kind.OP_EXCLAMATION) {
			if (expressionUnary.expression.type == Type.BOOLEAN) {
				mv.visitInsn(Opcodes.ICONST_1);
				mv.visitInsn(Opcodes.IXOR);
			} else if (expressionUnary.expression.type == Type.INTEGER) {
				mv.visitInsn(Opcodes.ICONST_M1);
				mv.visitInsn(Opcodes.IXOR);
			}
		}
		return null;
	}

	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg) throws Exception {
		if (lhsIdent.type == Types.getType(Scanner.Kind.KW_image)) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "deepCopy",
					RuntimeImageSupport.deepCopySig, false);
			mv.visitVarInsn(Opcodes.ASTORE, lhsIdent.dec.current_slot);
		} else if (lhsIdent.type == Types.getType(Scanner.Kind.KW_int)) { // READ THIS AGAIN
			mv.visitVarInsn(Opcodes.ISTORE, lhsIdent.dec.current_slot);
		} else if (lhsIdent.type == Types.getType(Scanner.Kind.KW_float)) {
			mv.visitVarInsn(Opcodes.FSTORE, lhsIdent.dec.current_slot);
		} else if (lhsIdent.type == Types.getType(Scanner.Kind.KW_boolean)) {
			mv.visitVarInsn(Opcodes.ISTORE, lhsIdent.dec.current_slot);
		} else if (lhsIdent.type == Types.getType(Scanner.Kind.KW_filename)) {
			mv.visitVarInsn(Opcodes.ASTORE, lhsIdent.dec.current_slot);
		}
		return null;
	}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg) throws Exception { // Next assignment
		mv.visitVarInsn(Opcodes.ALOAD, lhsPixel.dec.getSlot());
		lhsPixel.pixelSelector.ex.visit(this, arg);
		lhsPixel.pixelSelector.ey.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "setPixel",
				RuntimeImageSupport.setPixelSig, false);
		return null;
	}

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg) throws Exception { // Next assignment
		mv.visitVarInsn(Opcodes.ALOAD, lhsSample.dec.getSlot());
		lhsSample.pixelSelector.ex.visit(this, arg);
		lhsSample.pixelSelector.ey.visit(this, arg);
		switch (lhsSample.color) {
		case KW_alpha:
			mv.visitVarInsn(Opcodes.ILOAD, RuntimePixelOps.ALPHA);
			break;
		case KW_red:
			mv.visitVarInsn(Opcodes.ILOAD, RuntimePixelOps.RED);
			break;
		case KW_green:
			mv.visitVarInsn(Opcodes.ILOAD, RuntimePixelOps.GREEN);
			break;
		case KW_blue:
			mv.visitVarInsn(Opcodes.ILOAD, RuntimePixelOps.BLUE);
			break;
		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "updatePixelColor",
				RuntimeImageSupport.updatePixelColorSig, false);

		return null;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception { // Next assignment
		pixelSelector.ex.visit(this, arg);
		pixelSelector.ey.visit(this, arg);
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
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource(sourceFileName, null);

		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
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
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
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
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {
		statementAssign.e.visit(this, arg);
		statementAssign.lhs.visit(this, arg);
		// mv.visitVarInsn(Opcodes.ASTORE, statementAssign.lhs.dec.current_slot);
		// //Check again
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws Exception { // Next assignment
		Label l = new Label();
		statementIf.guard.visit(this, arg);
		mv.visitJumpInsn(IFEQ, l);
		statementIf.b.visit(this, arg);
		mv.visitLabel(l);
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws Exception {

		mv.visitVarInsn(ALOAD, 0);
		statementInput.e.visit(this, arg);
		mv.visitInsn(AALOAD);

		statementInput.dec.visit(this, arg);

		if (Types.getType(statementInput.dec.type) == Type.INTEGER) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitVarInsn(ISTORE, statementInput.dec.getSlot());
		} else if (Types.getType(statementInput.dec.type) == Type.FLOAT) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", false);
			mv.visitVarInsn(FSTORE, statementInput.dec.getSlot());
		} else if (Types.getType(statementInput.dec.type) == Type.BOOLEAN) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitVarInsn(ISTORE, statementInput.dec.getSlot());
		}

		else if (Types.getType(statementInput.dec.type) == Type.IMAGE) {

			if (statementInput.dec.width != null) {
				mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
				mv.visitInsn(Opcodes.DUP);
				statementInput.dec.width.visit(this, arg);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
			} else {
				mv.visitInsn(Opcodes.ACONST_NULL);
			}

			if (statementInput.dec.height != null) {
				mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
				mv.visitInsn(Opcodes.DUP);
				statementInput.dec.height.visit(this, arg);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
			} else {
				mv.visitInsn(Opcodes.ACONST_NULL);
			}

			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "readImage",
					RuntimeImageSupport.readImageSig, false);
			mv.visitVarInsn(Opcodes.ASTORE, statementInput.dec.getSlot());
		}
		 else {
				mv.visitVarInsn(Opcodes.ASTORE, statementInput.dec.getSlot());
			}

		return null;
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg) throws Exception {
		statementShow.e.visit(this, arg);
		Type type = statementShow.e.getType();
		switch (type) {
		case INTEGER: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
		}
			break;
		case BOOLEAN: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Z)V", false);
		}
			break;
		case FLOAT: {
			System.out.println("float");
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(F)V", false);
		}
			break;

		case IMAGE: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "makeFrame",
					RuntimeImageSupport.makeFrameSig, false);
		}
			break;

		case FILE: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
					"(Ljava/lang/String;)Ljava/lang/String;", false);
		}
			break;
		}
		return null;
	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg) throws Exception {
		Label label = new Label();
		mv.visitLabel(label);
		statementSleep.duration.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "toUnsignedLong", "(I)J", false);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws Exception { // Next assignment
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l2);
		statementWhile.b.visit(this, arg);
		mv.visitLabel(l1);
		statementWhile.guard.visit(this, arg);
		mv.visitJumpInsn(IFNE, l2);
		return null;
	}

	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg) throws Exception { // Next assignment
		mv.visitVarInsn(ALOAD, statementWrite.sourceDec.getSlot());
		mv.visitVarInsn(ALOAD, statementWrite.destDec.getSlot());
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "write", RuntimeImageSupport.writeSig,
				false);
		return null;
	}

}
