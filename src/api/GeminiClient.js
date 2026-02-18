/**
 * Backend proxy call (recommended): your app never holds the Gemini API key.
 * Provide:
 * - backendUrl (e.g. https://your-service.onrender.com)
 * - accessToken (Bearer token)
 */
export async function translateViaBackend({ backendUrl, accessToken, sourceLang, targetLang, text }) {
  if (!backendUrl) throw new Error("Missing backendUrl");
  if (!accessToken) throw new Error("Missing accessToken");
  const url = backendUrl.replace(/\/+$/, "") + "/translate";

  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + accessToken
    },
    body: JSON.stringify({ sourceLang, targetLang, text })
  });

  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(`Backend error ${res.status}: ${t}`);
  }

  const json = await res.json();
  const out = (json?.translation ?? "").trim();
  if (!out) throw new Error("Empty translation");
  return out;
}
