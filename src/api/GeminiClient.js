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



const LANGUAGE_CODE_MAP = {
  English: "en",
  Uzbek: "uz",
  "Karakalpak": "kaa", // may not be supported by all providers; we fallback to uz
  Russian: "ru",
  German: "de",
  Chinese: "zh-CN",
};

async function translateWithMyMemory(text, sourceLangName, targetLangName) {
  const src = LANGUAGE_CODE_MAP[sourceLangName] || "en";
  const tgt = LANGUAGE_CODE_MAP[targetLangName] || "ru";

  const tryPairs = [
    `${src}|${tgt}`,
    // Fallback: Karakalpak -> Uzbek if provider doesn't support 'kaa'
    `${src === "kaa" ? "uz" : src}|${tgt === "kaa" ? "uz" : tgt}`,
    `auto|${tgt === "kaa" ? "uz" : tgt}`,
  ];

  let lastErr = null;
  for (const pair of tryPairs) {
    try {
      const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${encodeURIComponent(pair)}`;
      const res = await fetch(url);
      const data = await res.json();
      const out = data?.responseData?.translatedText;
      if (out && typeof out === "string" && out.trim().length > 0) {
        return out;
      }
      lastErr = new Error(data?.responseDetails || "MyMemory translation failed");
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr || new Error("MyMemory translation failed");
}

export async function translateSmart({
  text,
  sourceLang,
  targetLang,
  backendUrl,
  accessToken,
}) {
  // Prefer your backend+Gemini if configured, fallback to free provider so app works out of the box.
  if (backendUrl && backendUrl.trim().length > 0 && accessToken && accessToken.trim().length > 0) {
    try {
      return await translateWithGemini({ text, sourceLang, targetLang, backendUrl, accessToken });
    } catch (e) {
      // fall through
      console.warn("Backend translation failed, falling back to MyMemory:", e?.message || e);
    }
  }
  return await translateWithMyMemory(text, sourceLang, targetLang);
}

export { LANGUAGE_CODE_MAP };
