import { PricePipe } from './price.pipe';

describe('PricePipe', () => {
  const pipe = new PricePipe();

  it('formats prices in French style with a comma', () => {
    expect(pipe.transform(12.5)).toBe('12,50 €');
    expect(pipe.transform(6.9)).toBe('6,90 €');
    expect(pipe.transform(0)).toBe('0,00 €');
  });
});
