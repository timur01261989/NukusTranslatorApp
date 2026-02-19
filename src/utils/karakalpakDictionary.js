/**
 * Very small starter dictionary for Karakalpak-specific words.
 * Extend this list over time (can be moved to Supabase later).
 */
export const KARAKALPAK_TERMS = [
  // [pattern, replacement]
  ["Nókis", "Nukus"],
  ["Qum Awil", "Qum Awil"],
  ["awıl", "awil"],
];

export function applyKarakalpakHints(text) {
  if (!text) return text;
  let out = String(text);
  for (const [a, b] of KARAKALPAK_TERMS) {
    try {
      out = out.replaceAll(a, b);
    } catch (_) {
      // older JS engines
      out = out.split(a).join(b);
    }
  }
  return out;
}
