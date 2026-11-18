import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from './auth/auth.models';

export function problemMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the server. Try again.';
    }
    const detail = (err.error as Partial<ProblemDetail> | null)?.detail;
    if (typeof detail === 'string' && detail.length > 0) {
      return detail;
    }
  }
  return fallback;
}