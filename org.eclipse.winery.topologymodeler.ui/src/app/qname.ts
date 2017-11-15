/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

/**
 * Retrieves the local name and the namespace from the qname
 */
export class QName {
  private _localName: string;
  private _nameSpace: string;

  constructor(private _qName: string) {
  }

  /**
   * Getter for localName
   */
  get localName(): string {
    this._localName = this._qName.split('}')[1];
    return this._localName;
  }

  /**
   * Setter for localName
   */
  set localName(value: string) {
    this._localName = value;
  }

  /**
   * Getter for namespace
   */
  get nameSpace(): string {
    this._nameSpace = this._qName.split('}')[0];
    return this._nameSpace + '}';
  }

  /**
   * Setter for namespace
   */
  set nameSpace(value: string) {
    this._nameSpace = value;
  }
}
