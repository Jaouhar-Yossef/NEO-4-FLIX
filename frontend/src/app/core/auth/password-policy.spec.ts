import { FormControl } from '@angular/forms';

import { PASSWORD_RULES, failedPasswordRules, passwordPolicyValidator } from './password-policy';

describe('password policy', () => {
  it('accepts a password that meets every rule', () => {
    expect(failedPasswordRules('S3cure!pass')).toEqual([]);
  });

  it('fails every rule for an empty string', () => {
    expect(failedPasswordRules('')).toEqual(PASSWORD_RULES.map((r) => r.id));
  });

  it.each([
    ['S3cur!p', 'length'],
    ['s3cure!pass', 'upper'],
    ['S3CURE!PASS', 'lower'],
    ['Secure!pass', 'digit'],
    ['S3curepass', 'symbol'],
  ])('"%s" breaks only the %s rule', (password, ruleId) => {
    expect(failedPasswordRules(password)).toEqual([ruleId]);
  });
});

describe('passwordPolicyValidator', () => {
  const validate = passwordPolicyValidator();

  it('returns null for a valid password', () => {
    expect(validate(new FormControl('S3cure!pass'))).toBeNull();
  });

  it('lists the failed rules for an invalid password', () => {
    expect(validate(new FormControl('abc'))).toEqual({
      passwordPolicy: ['length', 'upper', 'digit', 'symbol'],
    });
  });

  it('treats a null value as empty', () => {
    expect(validate(new FormControl(null))).toEqual({
      passwordPolicy: PASSWORD_RULES.map((r) => r.id),
    });
  });
});