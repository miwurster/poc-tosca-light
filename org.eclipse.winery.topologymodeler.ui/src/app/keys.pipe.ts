import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'keysPipe'
})
export class KeysPipe implements PipeTransform {
    keys = [];

    transform(value, args: string[]): any {
        this.keys = [];
        for (const key in value) {
            if (value.hasOwnProperty(key)) {
                this.keys.push({key: key, value: value[key]});
            }
        }
        return this.keys;
    }
}
