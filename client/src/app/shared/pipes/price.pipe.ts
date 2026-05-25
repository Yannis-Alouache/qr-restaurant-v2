import { Pipe } from '@angular/core';

@Pipe({
  name: 'price',
  standalone: true,
})
export class PricePipe {
  transform(value: number): string {
    return `${value.toFixed(2)} €`;
  }
}
