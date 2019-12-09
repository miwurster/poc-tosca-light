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

public class CollectionVisitor extends ChefDSLBaseVisitor<List<String>> {

    private CookbookParseResult extractedCookbookConfigs;

    public CollectionVisitor(CookbookParseResult cookbookConfigurations) {
        this.extractedCookbookConfigs = cookbookConfigurations;
    }

    @Override
    public List<String> visitPrimCollection(ChefDSLParser.PrimCollectionContext ctx) {
        CollectionVisitor collectionVisitor = new CollectionVisitor(extractedCookbookConfigs);
        return ctx.collection().accept(collectionVisitor);
    }

    @Override
    public List<String> visitWArray(ChefDSLParser.WArrayContext ctx) {
        List<String> stringArray = new ArrayList<>();
        for (int count = 0; count < ctx.getChildCount() - 3; count++) {
            stringArray.add(ctx.getChild(2 + count).getText());
        }
        return stringArray;
    }

    @Override
    public List<String> visitPrim12(ChefDSLParser.Prim12Context ctx) {
        List<String> args = new ArrayList<>();
        if (ctx.args() != null) {
            ArgsVisitor argsVisitor = new ArgsVisitor(extractedCookbookConfigs);
            args = ctx.args().accept(argsVisitor);
        }

        return args;
    }
}
