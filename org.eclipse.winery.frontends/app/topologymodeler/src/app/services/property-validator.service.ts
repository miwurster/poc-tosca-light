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

import { Injectable } from '@angular/core';
import { PropertyKVType } from '../models/enums';

@Injectable()
export class PropertyValidatorService {

    constructor() {
    }

    private getRegexPatternForXSDType(type: string): RegExp {
        switch (type) {
            case PropertyKVType.XSD_STRING:
                // return RegExp(`^[a-zA-Z\\n\\r\\t" "]+$`);
                return RegExp(`^.*$`); // todo
            case PropertyKVType.XSD_FLOAT:
                return RegExp(`^(([+-]?(\\d+([.]\\d*)?([eE][+-]?\\d+)?|[.]\\d+([eE][+-]?\\d+)?))|INF|-INF|NaN)$`);
            case PropertyKVType.XSD_DECIMAL:
                return RegExp(`^[+-]?(\\d*\\.)?\\d+$`);
            case PropertyKVType.XSD_ANYURI:
                return RegExp(`^[a-zA-Z\\n\\r\\t" "]+$`); // todo
            case PropertyKVType.XSD_QNAME:
                return RegExp(`^[a-zA-Z\\n\\r\\t" "]+$`); // todo
            default:
                return undefined;
        }
    }

    validateAgainstXSDType(property: string, xsdType: string): boolean {
        const regex = this.getRegexPatternForXSDType(xsdType);
        if (regex) {
            return regex.test(property);
        } else {
            return false;
        }
    }
}
