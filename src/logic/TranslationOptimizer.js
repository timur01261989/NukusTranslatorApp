/**
 * Decide whether OCR result is "new enough" to call Gemini again.
 * Strategy:
 * - Hash all recognized texts (sorted) + coarse bboxes -> signature
 * - If signature unchanged for N frames -> do nothing
 */
export function signatureFromBlocks(blocks) {
  const parts = blocks
    .map(b => `${b.text}|${Math.round(b.left/10)}:${Math.round(b.top/10)}:${Math.round(b.right/10)}:${Math.round(b.bottom/10)}`)
    .sort();
  return parts.join("||");
}
