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

// Import the core angular services.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

// Import the rxJs modules for their side-effects.
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/delay';
import 'rxjs/add/operator/do';

export interface ILoaded {
  isLoaded: boolean;
}

@Injectable()
export class LoadedService {

  constructor() { }

  /**
   * Getter for loading state
   * @returns stream
   */
  public getLoadingState(): Observable<ILoaded> {
    const stream = Observable
      .of({isLoaded: true})
      .do(() => console.log('loading started...'))
      .delay(1500)
      .do(() => console.log('loading finished after 1.5s'));
    return stream;
  }

}
