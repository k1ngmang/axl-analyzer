package com.kingmang.axl.rule;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public final class CyclomaticComplexityCalculator {

    public int calculate(Node body) {
        Counter counter = new Counter();
        body.accept(counter, null);
        return counter.cc + 1;
    }

    private static final class Counter extends VoidVisitorAdapter<Void> {
        private int cc;

        @Override
        public void visit(IfStmt node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(ForStmt node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(ForEachStmt node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(WhileStmt node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(DoStmt node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(CatchClause node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(ConditionalExpr node, Void arg) {
            cc++;
            super.visit(node, arg);
        }

        @Override
        public void visit(SwitchEntry node, Void arg) {
            if (!node.getLabels().isEmpty()) {
                cc++;
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(BinaryExpr node, Void arg) {
            if (node.getOperator() == BinaryExpr.Operator.AND
                    || node.getOperator() == BinaryExpr.Operator.OR) {
                cc++;
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(LambdaExpr node, Void arg) {
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration node, Void arg) {
        }

        @Override
        public void visit(EnumDeclaration node, Void arg) {
        }

        @Override
        public void visit(RecordDeclaration node, Void arg) {
        }

        @Override
        public void visit(AnnotationDeclaration node, Void arg) {
        }

        @Override
        public void visit(ObjectCreationExpr node, Void arg) {
            node.getScope().ifPresent(scope -> scope.accept(this, arg));
            node.getArguments().forEach(argument -> argument.accept(this, arg));
        }
    }
}
