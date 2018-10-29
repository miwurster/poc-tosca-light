import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SmartServiceJSONComponent } from './smartServiceJSON.component';
import {
    JsonSchemaFormModule, Bootstrap4FrameworkModule
} from 'angular2-json-schema-form';


@NgModule({
  imports: [
      CommonModule,
      FormsModule,
      Bootstrap4FrameworkModule,
      JsonSchemaFormModule.forRoot(Bootstrap4FrameworkModule)
  ],
  declarations: [SmartServiceJSONComponent]
})
export class SmartServiceJSONModule {
}
