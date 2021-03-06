package org.gcreator.runero.gml.lex;

import static org.gcreator.runero.gml.lex.Token.*;

import java.util.ArrayList;

import org.gcreator.runero.gml.Constant;
import org.gcreator.runero.gml.exec.ExprArgument;
import org.gcreator.runero.gml.exec.Assignment;
import org.gcreator.runero.gml.exec.Break;
import org.gcreator.runero.gml.exec.Continue;
import org.gcreator.runero.gml.exec.Do;
import org.gcreator.runero.gml.exec.Expression;
import org.gcreator.runero.gml.exec.For;
import org.gcreator.runero.gml.exec.Function;
import org.gcreator.runero.gml.exec.If;
import org.gcreator.runero.gml.exec.Operator;
import org.gcreator.runero.gml.exec.Repeat;
import org.gcreator.runero.gml.exec.Return;
import org.gcreator.runero.gml.exec.Statement;
import org.gcreator.runero.gml.exec.Variable;
import org.gcreator.runero.gml.exec.VariableDecl;
import org.gcreator.runero.gml.exec.VariableRef;
import org.gcreator.runero.gml.exec.While;
import org.gcreator.runero.gml.exec.With;
import org.gcreator.runero.gml.lex.GmlTokenizer.ANSI;
import org.gcreator.runero.gml.lex.TokenVariable.TokenVariableSub;

/**
 * Turns GML tokens into something useful (maybe)
 * 
 * 
 * @author serge
 *
 */
public class GmlCompiler {

    public static ArrayList<Statement> interpretGml(TokenWord[] tokens) {
        GmlCompiler c = new GmlCompiler(tokens);
        c.execute();
        return c.main;
    }

    public GmlCompiler(TokenWord[] tokens) {
        this.tokens = tokens;
        main = new ArrayList<Statement>();
    }

    ArrayList<Statement> main;
    TokenWord[] tokens;
    TokenGroup tricks;
    int trickI;

    int i;

    /**
     * Where the magic happens!
     * 
     */
    public void execute() {
        for (i = 0; i < tokens.length; i++) {
            TokenWord w = tokens[i];
            Statement s = getStatement(w);
            if (s != null)
                main.add(s);
        }
    }

    private Statement getStatement(TokenWord w) {
        if (w.token == SEMICOLON) { // useless
            return null;
        } else if (w.token == FUNCTION) {
            return getFunction((TokenWordPair) w);
        } else if (w.token == ARRAY) {
            System.err.println("what why is array here noooo");
            return null;
        } else if (w.token == VARIABLE) {
            TokenWord next = next();
            if (next.token == ASSIGN_EQUAL || next.token == EQUAL) { // := =
                return getAssignment(w, Assignment.OPERATION.SET);
            } else if (next.token == PLUS_EQUAL) { // +=
                return getAssignment(w, Assignment.OPERATION.ADD);
            } else if (next.token == MINUS_EQUAL) { // -=
                return getAssignment(w, Assignment.OPERATION.SUB);
            } else if (next.token == MULTIPLY_EQUAL) { // *=
                return getAssignment(w, Assignment.OPERATION.MULTIPLY);
            } else if (next.token == DIVIDE_EQUAL) { // /=
                return getAssignment(w, Assignment.OPERATION.DIVIDE);
            } else if (next.token == MOD_EQUAL) { // %=
                return getAssignment(w, Assignment.OPERATION.MOD);
            } else if (next.token == AND_EQUAL) { // &=
                return getAssignment(w, Assignment.OPERATION.AND);
            } else if (next.token == OR_EQUALS) { // |=
                return getAssignment(w, Assignment.OPERATION.OR);
            } else if (next.token == XOR_EQUAL) { // ^=
                return getAssignment(w, Assignment.OPERATION.XOR);
            } else {
                error("Unexpected " + next);
                return null;
            }
        } else if (w.token == BEGIN) {
            // useless...
            return null;
        } else if (w.token == END) {
            // also useless..
            return null;
        } else if (w.token == IF) { // if also handles THEN and ELSE
            return getIf();
        } else if (w.token == WHILE) {
            return getWhile();
        } else if (w.token == DO) { // do also handles UNTIL (expr)
            return getDo();
        } else if (w.token == FOR) {
            return getFor();
        } else if (w.token == WITH) {
            return getWith();
        } else if (w.token == REPEAT) {
            return getRepeat();
        } else if (w.token == EXIT) {
            return getExit();
        } else if (w.token == RETURN) {
            return getReturn();
        } else if (w.token == BREAK) {
            return getBreak();
        } else if (w.token == CONTINUE) {
            return getContinue();
        } else if (w.token == SWITCH) { // switch takes care of CASE and DEFAULT
            return getSwitch();
        } else if (w.token == VAR) {
            return getVar(false);
        } else if (w.token == GLOBALVAR) {
            return getVar(true);
        } else {
            error("unexpected " + w);
            return null;
        }
    }

    private Statement getVar(boolean global) {
        VariableDecl d = new VariableDecl(global);
        while (i < tokens.length) {
            TokenWord w = next();
            if (w.token == SEMICOLON)
                break;
            if (w.token == COMMA)
                continue;
            if (w.token != VARIABLE) {
                error("Unexpected variable name " + w);
                break;
            }
            TokenVariable v = (TokenVariable) w;
            if (v.variables.size() == 0) {
                error("Unexpected variable name " + v);
                break;
            }
            d.vars.add(v.data.getData());
        }

        return d;
    }

    @SuppressWarnings("unused")
    private Statement getSwitch() {
        TokenWord expr = next();
        Expression ex = getExpression(expr);
        ArrayList<Statement> code = getBlock();

        // TODO Switch statement (fun fun fun!)
        // look for default: case: .... break/return/exit
        return null;
    }

    private Statement getContinue() {
        return new Continue();
    }

    private Statement getBreak() {
        return new Break();
    }

    private Statement getReturn() {
        Return r = new Return();
        if (hasNext()) {
            TokenWord next = next();
            if (next.token != SEMICOLON) {
                i--;
                r.value = getArgument(true);
            }
        }

        return r;
    }

    private Statement getExit() {
        // Exit is basically the same thing
        return new Return();
    }

    private Statement getRepeat() {
        Repeat r = new Repeat();
        r.condition = getArgument(false);
        r.exec = getBlock();
        return r;
    }

    private Statement getWith() {
        With w = new With();
        w.who = getArgument(false);
        w.code = getBlock();
        return w;
    }

    private Statement getFor() {
        TokenWord next = next();
        if (next.token != PAR_OPEN) {
            error("Expected symbol '('");
            return null;
        }
        TokenGroup g = (TokenGroup) next;
        For f = new For();
        tricks = g;
        trickI = 0;
        f.initial = getStatement(next());
        f.condition = getArgument(true);
        f.increcement = getStatement(next());
        tricks = null;
        f.code = getBlock();
        return f;
    }

    private Statement getDo() {
        Do d = new Do();
        d.code = getBlock();
        TokenWord next = next();
        if (next.token != UNTIL) {
            error("Expected 'until'");
            return null;
        }
        d.condition = getArgument(true);
        return d;
    }

    private Statement getWhile() {
        While w = new While();
        w.condition = getArgument(false);
        w.exec = getBlock();
        return w;
    }

    private Statement getIf() {
        If s = new If();
        s.condition = getArgument(false);
        s.exec = getBlock();
        if (!hasNext())
            return s;
        TokenWord next = next();
        if (next.token == ELSE) {
            s.hasElse = true;
            s.elseExec = getBlock();
        } else
            regress();
        return s;
    }

    private ArrayList<Statement> getBlock() {
        ArrayList<Statement> list = new ArrayList<Statement>();
        TokenWord next = next();
        if (next.token == BEGIN) {
            while (i < tokens.length) {
                next = next();
                if (next.token == END)
                    break;
                Statement s = getStatement(next);
                if (s != null)
                    list.add(s);
            }
        } else {
            list.add(getStatement(next));
        }
        return list;
    }

    private Assignment getAssignment(TokenWord var, Assignment.OPERATION op) {
        Assignment a = new Assignment(op);
        a.variable = getVariable(var);
        a.value = getArgument(true);
        return a;
    }

    public ExprArgument getArgument() {
        i--;
        return getArgument(false);
    }

    public VariableRef getVariable() {
        i--;
        return getVariable(tokens[0]);
    }

    /**
     * gets an argument 
     * 
     * @return
     */
    private ExprArgument getArgument(boolean allowSemicolon) {
        // UNARY? VAL (OP UNARY? VAL)*
        // VAL = FUNCTION, VARIABLE, NUMBER, STRING, parenthesis
        // OP = Operator

        ExprArgument a = new ExprArgument();
        TokenWord first = next();
        if (isUnary(first.token)) {
            // darn.
            if (first.token == PLUS) {
                // useless
            } else {
                Expression e = getUnaryExpression(first.token);
                if (e != null) {
                    a.add(e);
                    a.debugVal += e.op + " ";
                }
            }
            first = next();
        }
        if (!isValue(first.token)) {
            error("Unexpected " + first);
            return null;
        }

        a.add(getExpression(first));
        a.debugVal += first + ", ";
        boolean reqOp = true;
        while (hasNext()) {
            TokenWord next = next();
            boolean unary = false;
            boolean op = isOperator(next.token);
            if (op && isUnary(next.token)) {
                unary = true;
                if (next.token == PLUS || next.token == MINUS) {
                    // might not actually be unary
                    TokenWord n2 = next();
                    if (isOperator(n2.token)) { // x = y - ~y
                        unary = true;
                        reqOp = true;
                    } else {
                        unary = false;
                    }
                    regress();// n2 is not used, we are just peeking
                } else {
                    // NOT and BITW_INVERT
                    unary = true;
                    reqOp = true;
                    TokenWord n2 = next();
                    if (isOperator(n2.token)) { // x = y - ~y
                        reqOp = true;
                    } else {
                        reqOp = false;
                    }
                    regress();// n2 is not used, we are just peeking
                }
            }

            if (allowSemicolon && next.token == SEMICOLON) {
                break;
            }
            if (unary) {
                Expression e = getUnaryExpression(next.token);
                if (e != null) {
                    a.add(e);
                    a.debugVal += next + ", ";
                }
            } else if (op && reqOp) {
                a.add(getExpression(next));
                a.debugVal += next + ", ";
                reqOp = false;
            } else if (!op && !reqOp && isValue(next.token)) {
                a.add(getExpression(next));
                reqOp = true;
                a.debugVal += next + ", ";
                // check to see if there is more things to add
                if (hasNext()) {
                    next = next();
                    if (next.token != SEMICOLON || !allowSemicolon)
                        regress();

                    if (allowSemicolon && (next.token == SEMICOLON || !isOperator(next.token)))
                        break; // end of statement
                }
            } else {
                // end of statement
                regress();
                break;
            }
        }
        return a;
    }

    private boolean isOperator(Token t) {
        return t == EQUAL || t == AND || t == OR || t == XOR || t == NOT || t == PLUS || t == MINUS || t == NOT_EQUALS
                || t == INT_DIVIDE || t == DIV || t == MULTIPLY || t == MODULO || t == GREATER || t == LESS
                || t == GREATER_EQUAL || t == LESS_EQUAL || t == COMPARATOR_EQUALS || t == BITW_AND || t == BITW_OR
                || t == BITW_XOR || t == BITW_INVERT || t == BITW_RIGHT || t == BITW_LEFT;
    }

    private boolean isValue(Token t) {
        return t == FUNCTION || t == VARIABLE || t == NUMBER || t == STRING || t == PAR_OPEN;
    }

    private boolean isUnary(Token t) {
        return t == NOT || t == PLUS || t == MINUS || t == BITW_INVERT;
    }

    private Expression getUnaryExpression(Token token) {
        if (token == MINUS)
            return new Expression(Operator.NEGATE);
        else if (token == PLUS)
            ; // useless
        else if (token == NOT)
            return new Expression(Operator.NOT);
        else if (token == BITW_INVERT)
            return new Expression(Operator.BITW_INVERT);
        return null;
    }

    private boolean hasNext() {
        if (tricks != null)
            return trickI + 1 < tricks.tokens.size();
        return i + 1 < tokens.length;
    }

    private TokenWord next() {
        if (tricks != null) {
            if (trickI >= tricks.tokens.size())
                error("Unexpected end of data");
            else {
                return tricks.tokens.get(trickI++);
            }
            return null;
        }

        i++;
        if (i >= tokens.length)
            error("Unexpected end of data");
        else {
            return tokens[i];
        }
        return null;
    }

    private void regress() {
        if (tricks != null)
            trickI--;
        else
            i--;
    }

    private Function getFunction(TokenWordPair func) {
        Function f = new Function(func.data.getData());
        TokenGroup args = func.child;
        if (args.tokens.size() > 0) {
            ExprArgument a = new ExprArgument();
            for (TokenWord w : args.tokens) {
                if (w.token == COMMA) {
                    // TODO: check syntax
                    f.args.add(a);
                    a = new ExprArgument();
                    continue;
                }
                a.expressions.add(getExpression(w));
                a.debugVal += w + ", ";
            }
            f.args.add(a);
        }
        return f;
    }

    private ExprArgument getExpression(TokenGroup g) {
        ExprArgument a = new ExprArgument();
        for (TokenWord t : g.tokens) {
            a.add(getExpression(t));
            a.debugVal += t + ", ";
        }
        return a;
    }

    private Expression getExpression(TokenWord t) {
        if (t.token == NUMBER) {
            return new Expression(new Constant(t.number));
        } else if (t.token == STRING) {
            return new Expression(new Constant(t.data.getData()));
        } else if (t.token == FUNCTION) {
            TokenWordPair func = (TokenWordPair) t;
            return new Expression(getFunction(func));
        } else if (t.token == VARIABLE) {
            return new Expression(getVariable(t));
        } else if (t.token == PAR_OPEN) {
            return new Expression(getExpression((TokenGroup) t));
        } else if (t.token == AND)
            return new Expression(Operator.AND);
        else if (t.token == OR)
            return new Expression(Operator.OR);
        else if (t.token == XOR)
            return new Expression(Operator.XOR);
        else if (t.token == NOT)
            return new Expression(Operator.NOT);
        else if (t.token == LESS)
            return new Expression(Operator.LESS);
        else if (t.token == GREATER)
            return new Expression(Operator.GREATER);
        else if (t.token == LESS_EQUAL)
            return new Expression(Operator.LESS_EQUAL);
        else if (t.token == GREATER_EQUAL)
            return new Expression(Operator.GREATER_EQUAL);
        else if (t.token == EQUAL)// =
            return new Expression(Operator.EQUALS);
        else if (t.token == COMPARATOR_EQUALS)
            return new Expression(Operator.EQUALS);
        else if (t.token == NOT_EQUALS)
            return new Expression(Operator.NOT_EQUAL);
        else if (t.token == PLUS)
            return new Expression(Operator.PLUS);
        else if (t.token == MINUS)
            return new Expression(Operator.MINUS);
        else if (t.token == MULTIPLY)
            return new Expression(Operator.MULTIPLY);
        else if (t.token == DIV)
            return new Expression(Operator.DIVIDE);
        else if (t.token == INT_DIVIDE)
            return new Expression(Operator.INT_DIVIDE);
        else if (t.token == MODULO)
            return new Expression(Operator.MODULO);
        else if (t.token == BITW_AND)
            return new Expression(Operator.BITW_AND);
        else if (t.token == BITW_OR)
            return new Expression(Operator.BITW_OR);
        else if (t.token == BITW_XOR)
            return new Expression(Operator.BITW_XOR);
        else if (t.token == BITW_LEFT)
            return new Expression(Operator.BITW_LEFT);
        else if (t.token == BITW_RIGHT)
            return new Expression(Operator.BITW_RIGHT);
        else if (t.token == BITW_INVERT)
            return new Expression(Operator.BITW_INVERT);
        regress();
        error("Unexpected in expression: " + t);
        return null;
    }

    private VariableRef getVariable(TokenWord t) {
        if (t.token != VARIABLE) {
            System.out.println("Not a variable! " + t);
            return null;
        }
        TokenVariable v = (TokenVariable) t;
        VariableRef var = new VariableRef();
        for (TokenVariableSub s : v.variables) {
            Variable vv = new Variable();
            if (s.isExpression) {
                vv.isExpression = true;
                vv.expression = getExpression(s.expression);
            } else if (s.isArray) {
                vv.name = s.name;
                vv.isArray = true;
                vv.arrayIndex = getExpression(s.arrayIndex);
            } else {
                vv.name = s.name;
            }
            var.ref.add(vv);
        }
        return var;

    }

    private void error(String msg) {
        System.out.println(ANSI.RED + "Error in parser: " + msg + ANSI.BLACK);
        throw new ParseError(msg);
    }

}
