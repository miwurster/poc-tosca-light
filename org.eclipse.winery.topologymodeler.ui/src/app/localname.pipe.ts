import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'localname'
})
export class LocalnamePipe implements PipeTransform {

    transform(value: any, args?: any): any {
        if (!value) {
            return value;
        }

        return value.split('}')[1];
    }

}
