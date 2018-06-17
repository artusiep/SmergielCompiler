import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FileUtil;
import utils.IMessage;

import java.util.HashMap;
import java.util.Map;

public class SmergielJavaListener extends SmergielBaseListener implements IMessage {

    private final StringBuilder main = new StringBuilder();
    private final StringBuilder methods = new StringBuilder();
    private final Map<String, TerminalNode> declaredId = new HashMap<>();
    private final Map<String, TerminalNode> calledId = new HashMap<>();
    private String idType = "main:";

    private final String className;

    SmergielJavaListener(String className) {
        this.className = className;
    }

    @Override
    public void enterProgram(SmergielParser.ProgramContext ctx) {
        main.append("import java.util.Scanner;\n");
        main.append(String.format("public class %s {\n", className));
        main.append("private static Scanner read = new Scanner(System.in);\n");
        main.append("public static void main(String[] args) {\n");

        if (ctx.listOfStatement() != null) {
            ctx.listOfStatement().statement().forEach(s -> main.append(statement(s)));
        }
    }

    @Override
    public void exitProgram(SmergielParser.ProgramContext ctx) {
        main.append("}");
        main.append(methods.toString());
        main.append("}");
        checkIdDeclared();
        FileUtil.write("Test" + ".java", main.toString());
    }

    @Override
    public void exitMethod(SmergielParser.MethodContext ctx) {
        final int paramSize = ctx.listOfArguments() != null ? ctx.listOfArguments().Identifier().size() : 0;
        declaredId.put(ctx.Identifier().getText() + "/" + paramSize, ctx.Identifier());
        idType = ctx.Identifier().getText() + ":";

        methods.append("public static double ");
        methods.append(ctx.Identifier().getText());
        methods.append("(");
        if (ctx.listOfArguments() != null) {
            boolean notFirst = false;
            for (final TerminalNode id : ctx.listOfArguments().Identifier()) {
                declaredId.put(idType + id.getText(), id);
                if (notFirst) methods.append(",");
                methods.append("double ");
                methods.append(id.getText());
                notFirst = true;
            }
        }
        methods.append("){");
        String returnStatement = "return 0;";

        if (ctx.listOfStatement() != null) {
            ctx.listOfStatement().statement().forEach(s -> methods.append(statement(s)));
            returnStatement = returnToJava(ctx.listOfStatement().returnStatement());
        }

        methods.append(returnStatement);
        methods.append("}");
    }

    private String returnToJava(SmergielParser.ReturnStatementContext returnStatement) {
        if (returnStatement != null)
            return "return " + expressionToJava(returnStatement.expression()) + ";";
        return "";
    }

    private String elseSentencesToJava(SmergielParser.ListOfStatementContext listOfStatementContext) {
        final StringBuilder builder = new StringBuilder();
        builder.append("else {");
        builder.append(listOfStatementToJava(listOfStatementContext));
        builder.append("}");
        return builder.toString();
    }

    private String ifStatement(SmergielParser.ComparisonContext comparisonContext, Iterable<SmergielParser.RightComparisonContext> rightComparisons, SmergielParser.ListOfStatementContext listOfStatementContext, SmergielParser.ElseSentencesContext elseSentences) {
        final String comparison = comparisonToJava(comparisonContext, rightComparisons);
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("if(%s){", comparison));
        builder.append(listOfStatementToJava(listOfStatementContext));
        builder.append("}");
        if (elseSentences != null) {
            builder.append(elseSentencesToJava(elseSentences.listOfStatement()));
        }
        return builder.toString();
    }

    private String listOfStatementToJava(SmergielParser.ListOfStatementContext listOfStatementContext) {
        final StringBuilder builder = new StringBuilder();
        listOfStatementContext.statement().forEach(s -> builder.append(statement(s)));
        builder.append(returnToJava(listOfStatementContext.returnStatement()));
        return builder.toString();
    }

    private String statement(SmergielParser.StatementContext statement) {
        final StringBuilder builder = new StringBuilder();
        if (statement.assignStatement() != null)
            builder.append(assignStatement(statement.assignStatement().Identifier(), statement.assignStatement().expression()));
        else if (statement.ifStatement() != null)
            builder.append(ifStatement(statement.ifStatement().comparison(), statement.ifStatement().rightComparison(), statement.ifStatement().listOfStatement(), statement.ifStatement().elseSentences()));
        else if (statement.readStatement() != null)
            builder.append(readStatement(statement.readStatement().listOfIdentifier()));
        else if (statement.writeStatement() != null)
            builder.append(writeStatement(statement.writeStatement().listOfExpression()));
        return builder.toString();
    }


    private String assignStatement(@Nullable TerminalNode identifier, SmergielParser.ExpressionContext expression) {
        String line = "\t\t";
        if (identifier != null) {

            if (!declaredId.containsKey(idType + identifier.getText())) {
                line += "double ";
                declaredId.put(idType + identifier.getText(), identifier);
            }

            line += identifier.getText() + " = ";
        }

        line += expressionToJava(expression) + ";";
        return line;
    }

    private String readStatement(SmergielParser.ListOfIdentifierContext listOfIdentifier) {
        final StringBuilder builder = new StringBuilder();
        for (final TerminalNode id : listOfIdentifier.Identifier()) {
            final String declare = declaredId.containsKey(idType + id.getText()) ? "" : "double ";
            builder.append(String.format("\t\tSystem.out.println(\"Gimme %s: \");", id.getText()));
            builder.append(String.format("\t\t" + declare + "%s = read.nextDouble();", id.getText()));
            declaredId.put(idType + id.getText(), id);
        }

        return builder.toString();
    }

    private String writeStatement(SmergielParser.ListOfExpressionContext listOfExpression) {
        final String expressions = listOfExpressionToJava(listOfExpression);
        return String.format("System.out.println(%s);", expressions);
    }


    @NotNull
    private String comparisonToJava(SmergielParser.ComparisonContext comparison, Iterable<SmergielParser.RightComparisonContext> rightComparisons) {
        final StringBuilder buffer = new StringBuilder(leftComparisonToJava(comparison));

        for (final SmergielParser.RightComparisonContext rightComparison : rightComparisons) {
            buffer.append(" ");
            buffer.append(logicalToJava(rightComparison.LogicalOperator()));
            buffer.append(" ");
            buffer.append(leftComparisonToJava(rightComparison.comparison()));
        }
        return buffer.toString();
    }

    private String leftComparisonToJava(SmergielParser.ComparisonContext comparison) {
        final String compare = comparison.Compare().getText();
        final String left = expressionToJava(comparison.expression(0));
        final String right = expressionToJava(comparison.expression(1));

        return left + " " + compare + " " + right;
    }

    private String logicalToJava(TerminalNode logicalOperator) {
        final char logicalChar = logicalOperator.getText().charAt(0);
        switch (logicalChar) {
            case '&':
                return "&&";
            case '|':
                return "||";
        }

        return "";
    }

    @NotNull
    private String listOfExpressionToJava(SmergielParser.ListOfExpressionContext listOfExpression) {
        final StringBuilder buffer = new StringBuilder();

        for (final SmergielParser.ExpressionContext expression : listOfExpression.expression()) {
            final String text = expressionToJava(expression);
            if (buffer.length() != 0 && !text.isEmpty()) buffer.append(" +\" \"+ ");
            buffer.append(expressionToJava(expression));
        }

        return buffer.toString();
    }

    @NotNull
    private String primaryToJava(SmergielParser.PrimaryContext primary) {
        if (primary.identifierCall() != null) {
            final TerminalNode id = primary.identifierCall().Identifier();
            identifierCall(primary.identifierCall().Identifier());
            return id.toString();
        }

        if (primary.methodCall() != null) {
            final int size = primary.methodCall().listOfExpression() != null ? primary.methodCall().listOfExpression().expression().size() : 0;
            calledId.put(primary.methodCall().Identifier() + "/" + size, primary.methodCall().Identifier());
            return primary.methodCall().getText();
        }

        if (primary.Constant() != null)
            return primary.Constant().getText();

        if (primary.expression() != null)
            return "(" + expressionToJava(primary.expression()) + ")";

        return "";
    }


    private void identifierCall(TerminalNode id) {
        calledId.put(idType + id.getText(), id);
    }

    @NotNull
    private String expressionToJava(SmergielParser.ExpressionContext expression) {
        final StringBuilder buffer = new StringBuilder(primaryToJava(expression.primary()));

        for (final SmergielParser.RightPrimaryContext rightPrimary : expression.rightPrimary()) {
            buffer.append(rightPrimary.Operator().getText());
            buffer.append(primaryToJava(rightPrimary.primary()));
        }

        return buffer.toString();
    }

    private void checkIdDeclared() {
        for (final String id : calledId.keySet()) {
            if (id != null && !declaredId.containsKey(id)) {
                final TerminalNode node = calledId.get(id);
                final String[] split = node.getText().split(":");
                final String idText = split.length > 1 ? split[1] : id;
                System.err.printf(ID_NOT_DECLARED_ERROR, node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine(), idText);
                System.exit(1);
            }
        }
    }


    public String getMain() {
        return main.toString();
    }

}
