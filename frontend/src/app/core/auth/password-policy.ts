import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export interface PasswordRule {
  id: string;
  label: string;
  test: (value: string) => boolean;
}

export const PASSWORD_RULES: readonly PasswordRule[] = [
  { id: 'length', label: 'At least 8 characters', test: (v) => v.length >= 8 },
  { id: 'upper', label: 'An uppercase letter', test: (v) => /[A-Z]/.test(v) },
  { id: 'lower', label: 'A lowercase letter', test: (v) => /[a-z]/.test(v) },
  { id: 'digit', label: 'A digit', test: (v) => /\d/.test(v) },
  { id: 'symbol', label: 'A symbol (not a letter or digit)', test: (v) => /[^A-Za-z0-9]/.test(v) },
];

export function failedPasswordRules(value: string): string[] {
  return PASSWORD_RULES.filter((rule) => !rule.test(value)).map((rule) => rule.id);
}

export function passwordPolicyValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const failed = failedPasswordRules(String(control.value ?? ''));
    return failed.length === 0 ? null : { passwordPolicy: failed };
  };
}