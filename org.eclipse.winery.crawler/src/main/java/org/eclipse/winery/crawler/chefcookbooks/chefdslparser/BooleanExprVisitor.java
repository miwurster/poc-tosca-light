/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.crawler.chefcookbooks.chefdslparser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.winery.crawler.chefcookbooks.chefcookbook.CookbookParseResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanExprVisitor extends ChefDSLBaseVisitor<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryBaseVisitor.class.getName());

    private CookbookParseResult parseResult;

    public BooleanExprVisitor(CookbookParseResult existingParseResult) {
        this.parseResult = existingParseResult;
    }

    @Override
    public Boolean visitExprAnd(ChefDSLParser.ExprAndContext ctx) {
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);

        return ctx.expr(0).accept(booleanExprVisitor)
            && ctx.expr(1).accept(booleanExprVisitor);
    }

    @Override
    public Boolean visitExprOr(ChefDSLParser.ExprOrContext ctx) {
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);

        return ctx.expr(0).accept(booleanExprVisitor)
            || ctx.expr(1).accept(booleanExprVisitor);
    }

    @Override
    public Boolean visitArgAnd(ChefDSLParser.ArgAndContext ctx) {
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);

        return ctx.arg(0).accept(booleanExprVisitor)
            && ctx.arg(1).accept(booleanExprVisitor);
    }

    @Override
    public Boolean visitArgOr(ChefDSLParser.ArgOrContext ctx) {
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);

        return ctx.arg(0).accept(booleanExprVisitor)
            || ctx.arg(1).accept(booleanExprVisitor);
    }

    @Override
    public Boolean visitExprArg(ChefDSLParser.ExprArgContext ctx) {
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);
        return ctx.arg().accept(booleanExprVisitor);
    }

    @Override
    public Boolean visitArgGreater(ChefDSLParser.ArgGreaterContext ctx) {
        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        List<String> firstArgList = ctx.arg(0).accept(primaryVisitor);
        List<String> secondArgList = ctx.arg(1).accept(primaryVisitor);

        if (firstArgList != null && secondArgList != null && firstArgList.size() == 1 && secondArgList.size() == 1) {
            String firstArgument = firstArgList.get(0);
            String secondArgument = secondArgList.get(0);
            return Double.parseDouble(firstArgument) > Double.parseDouble(secondArgument);
        } else {
            LOGGER.info("One of the compared Arguments is an array or null. This is not implemented yet. \n" +
                firstArgList + " is compared to: " + secondArgList);
            return false;
        }
    }

    @Override
    public Boolean visitArgGreaterEqual(ChefDSLParser.ArgGreaterEqualContext ctx) {
        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        List<String> firstArgList = ctx.arg(0).accept(primaryVisitor);
        List<String> secondArgList = ctx.arg(1).accept(primaryVisitor);

        if (firstArgList != null && secondArgList != null && firstArgList.size() == 1 && secondArgList.size() == 1) {
            String firstArgument = firstArgList.get(0);
            String secondArgument = secondArgList.get(0);
            return firstArgument != null && secondArgument != null
                && (Double.parseDouble(firstArgument) >= Double.parseDouble(secondArgument));
        } else {
            LOGGER.info("One of the compared Arguments is an array or null. This is not implemented yet. \n" +
                firstArgList + " is compared to: " + secondArgList);
            return false;
        }
    }

    @Override
    public Boolean visitArgLess(ChefDSLParser.ArgLessContext ctx) {
        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        List<String> firstArgList = ctx.arg(0).accept(primaryVisitor);
        List<String> secondArgList = ctx.arg(1).accept(primaryVisitor);

        if (firstArgList != null && secondArgList != null && firstArgList.size() == 1 && secondArgList.size() == 1) {
            String firstArgument = firstArgList.get(0);
            String secondArgument = secondArgList.get(0);
            return Double.parseDouble(firstArgument) < Double.parseDouble(secondArgument);
        } else {
            LOGGER.info("One of the compared Arguments is an array or null. This is not implemented yet. \n" +
                firstArgList + " is compared to: " + secondArgList);
            return false;
        }
    }

    @Override
    public Boolean visitArgLessEqual(ChefDSLParser.ArgLessEqualContext ctx) {
        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        List<String> firstArgList = ctx.arg(0).accept(primaryVisitor);
        List<String> secondArgList = ctx.arg(1).accept(primaryVisitor);

        if (firstArgList != null && secondArgList != null && firstArgList.size() == 1 && secondArgList.size() == 1) {
            String firstArgument = firstArgList.get(0);
            String secondArgument = secondArgList.get(0);
            return Double.parseDouble(firstArgument) <= Double.parseDouble(secondArgument);
        } else {
            LOGGER.info("One of the compared Arguments is an array or null. This is not implemented yet. \n" +
                firstArgList + " is compared to: " + secondArgList);
            return false;
        }
    }

    @Override
    public Boolean visitArgEqual(ChefDSLParser.ArgEqualContext ctx) {
        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        List<String> firstArgList = ctx.arg(0).accept(primaryVisitor);
        List<String> secondArgList = ctx.arg(1).accept(primaryVisitor);

        if (firstArgList != null && secondArgList != null && firstArgList.size() == 1 && secondArgList.size() == 1) {
            String firstArgument = firstArgList.get(0);
            String secondArgument = secondArgList.get(0);
            return (firstArgument == null && secondArgument == null) || (firstArgument != null && firstArgument.equals(secondArgument));
        } else {
            LOGGER.info("One of the compared Arguments is an array or null. This is not implemented yet. \n" +
                firstArgList + " is compared to: " + secondArgList);
            return false;
        }
    }

    @Override
    public Boolean visitExprCommand(ChefDSLParser.ExprCommandContext ctx) {
        Boolean exprResult;
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);
        exprResult = ctx.command().accept(booleanExprVisitor);
        return exprResult;
    }

    @Override
    public Boolean visitOperationPrimary(ChefDSLParser.OperationPrimaryContext ctx) {
        List<String> argumentList;
        List<String> callArguments = null;

        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        CallArgsVisitor callArgsVisitor = new CallArgsVisitor(parseResult);
        argumentList = ctx.primary().accept(primaryVisitor);

        if (argumentList != null && argumentList.size() == 1) {
            String argument = argumentList.get(0);
            if (ctx.operation() != null && ctx.call_args() != null) {
                String operation = ctx.operation().getText();
                if ("start_with?".equals(operation)) {
                    callArguments = ctx.call_args().accept(callArgsVisitor);
                    for (String callArgument : callArguments) {
                        if (argument.startsWith(callArgument)) {
                            return true;
                        }
                    }
                } else {
                    LOGGER.info("Operation \"" + argumentList + "is not implemented in" + this.getClass());
                }
            }
        } else {
            LOGGER.info("Argument is an array. This is not implemented. \n" +
                "Argument" + argumentList);
        }

        return false;
    }

    @Override
    public Boolean visitPrimFuncCall(ChefDSLParser.PrimFuncCallContext ctx) {
        PrimaryBaseVisitor primaryVisitor = new PrimaryBaseVisitor(parseResult);
        CallArgsVisitor callArgsVisitor = new CallArgsVisitor(parseResult);
        List<String> argumentList = ctx.primary().accept(primaryVisitor);

        argumentList = new ArrayList<>();
        argumentList.add("das müsste false sein");
        if (argumentList.size() == 1) {
            String argument = argumentList.get(0);
            if (ctx.function() != null && ctx.function().call_args() != null) {
                String operation = ctx.function().operation().getText();
                if ("start_with?".equals(operation)) {
                    List<String> callArguments = ctx.function().call_args().accept(callArgsVisitor);
                    for (String callArgument : callArguments) {
                        if (argument.startsWith(callArgument)) {
                            return true;
                        }
                    }
                } else {
                    LOGGER.info("Operation \"" + argumentList + "is not implemented in" + this.getClass());
                }
            }
        } else {
            LOGGER.info("Argument is an array. This is not implemented. \n" +
                "Argument" + argumentList);
        }

        return false;
    }

    @Override
    public Boolean visitPrimCompstmtInBrackets(ChefDSLParser.PrimCompstmtInBracketsContext ctx) {
        BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);
        return ctx.inner_comptstmt().accept(booleanExprVisitor);
    }

    @Override
    public Boolean visitArgIndexOf(ChefDSLParser.ArgIndexOfContext ctx) {
        PrimaryBaseVisitor argPrimaryVisitor = new PrimaryBaseVisitor(parseResult);
        List<String> argList = ctx.arg(0).accept(argPrimaryVisitor);
        if (argList != null && argList.size() == 1) {
            String leftArgument = argList.get(0);
            argList = ctx.arg(0).accept(argPrimaryVisitor);
            if (argList != null && argList.size() == 1) {
                String rightArgument = regexToString(argList.get(0));
                return leftArgument.contains(rightArgument);
            }
        }

        return false;
    }

    public String regexToString(String rightArgument) {
        if (rightArgument.startsWith("/") && rightArgument.endsWith("/")) {
            rightArgument = rightArgument.substring(1, rightArgument.length() - 1);
        }
        return rightArgument;
    }

    @Override
    public Boolean visitArgPrimary(ChefDSLParser.ArgPrimaryContext ctx) {
        Boolean exprResult = false;
        String arg = null;
        if (ctx.primary().getClass() == ChefDSLParser.PrimCompstmtInBracketsContext.class) {
            BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);
            exprResult = ctx.primary().accept(booleanExprVisitor);
        } else if (ctx.primary().getClass() == ChefDSLParser.PrimFuncCallContext.class) {
            BooleanExprVisitor booleanExprVisitor = new BooleanExprVisitor(parseResult);
            exprResult = ctx.primary().accept(booleanExprVisitor);
        } else {

            PrimaryBaseVisitor argPrimaryVisitor = new PrimaryBaseVisitor(parseResult);
            List<String> argList = ctx.primary().accept(argPrimaryVisitor);

            if (argList != null && argList.size() == 1) {
                arg = argList.get(0);
            }
            if ("false".equals(arg)) {
                return false;
            } else if ("true".equals(arg)) {
                return true;
            } else {
                LOGGER.error("Argument is not an expected boolean. If argument is null, " +
                    "Primary Visitor is not implemented. \n" +
                    "Argument is:" + ctx.primary().getText());
            }
        }
        return resolveNullArgument(exprResult);
    }

    /**
     * Helper method to resolve null arguments. Arguments are when the corresponding visitor is not implemented.
     *
     * @param arg Boolean argument which is resolved and nullchecked.
     * @return Returns false when argument is null.
     */
    private Boolean resolveNullArgument(Boolean arg) {
        if (arg == null) {
            arg = false;
        }
        return arg;
    }
}
