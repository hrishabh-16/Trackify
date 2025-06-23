import { CanActivateFn } from '@angular/router';

export const teamGuard: CanActivateFn = (route, state) => {
  return true;
};
