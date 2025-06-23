import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'expenseStatus'
})
export class ExpenseStatusPipe implements PipeTransform {

  transform(value: unknown, ...args: unknown[]): unknown {
    return null;
  }

}
