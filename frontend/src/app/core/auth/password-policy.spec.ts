import { FormControl } from '@angular/forms';

import { PASSWORD_RULES, failedPasswordRules, passwordPolicyValidator } from './password-policy';

describe('password policy', () => {
  it('accepts a password that meets every rule', () => {
    expect(failedPasswordRules('S3cure!pass')).toEqual([]);
  });

  it('fails every rule except the maximum for an empty string', () => {
    expect(failedPasswordRules('')).toEqual(['length', 'upper', 'lower', 'digit', 'symbol']);
  });

  it.each([
    ['S3cur!p', 'length'],
    ['s3cure!pass', 'upper'],
    ['S3CURE!PASS', 'lower'],
    ['Secure!pass', 'digit'],
    ['S3curepass', 'symbol'],
    ['S3cure!' + 'a'.repeat(66), 'maxBytes'],
  ])('"%s" breaks only the %s rule', (password, ruleId) => {
    expect(failedPasswordRules(password)).toEqual([ruleId]);
  });

  it('accepts exactly 72 bytes', () => {
    expect(failedPasswordRules('S3cure!' + 'a'.repeat(65))).toEqual([]);
  });

  it('counts bytes, not characters (é is 2 bytes in UTF-8)', () => {
    const password = 'S3cure!' + 'é'.repeat(33); // 40 characters, 73 bytes
    expect(password.length).toBe(40);
    expect(failedPasswordRules(password)).toEqual(['maxBytes']);
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
      passwordPolicy: PASSWORD_RULES.map((r) => r.id).filter((id) => id !== 'maxBytes'),
    });
  });
});