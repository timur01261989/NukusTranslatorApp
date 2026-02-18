/**
 * Very simple cache keyed by raw text.
 * Real product: also include screen density, app package, and language pair.
 */
export class CacheManager {
  constructor(max = 500) {
    this.max = max;
    this.map = new Map(); // key -> value
  }

  get(key) {
    return this.map.get(key);
  }

  set(key, val) {
    if (this.map.size >= this.max) {
      // delete oldest
      const first = this.map.keys().next().value;
      this.map.delete(first);
    }
    this.map.set(key, val);
  }
}
