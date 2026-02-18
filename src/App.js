import React, { useEffect, useMemo, useState } from "react";
import { SafeAreaView, View, Text, TextInput, StyleSheet, NativeModules, NativeEventEmitter, Platform } from "react-native";
import FloatingButton from "./components/FloatingButton";
import { translateViaBackend } from "./api/GeminiClient";
import { CacheManager } from "./logic/CacheManager";
import { signatureFromBlocks } from "./logic/TranslationOptimizer";

const { BridgeModule } = NativeModules;

const emitter = BridgeModule ? new NativeEventEmitter(BridgeModule) : null;

export default function App() {
  const cache = useMemo(() => new CacheManager(800), []);
  const [apiKey, setApiKey] = useState(""); // deprecated (local mode)
  const [backendUrl, setBackendUrl] = useState("http://10.0.2.2:8080");
  const [accessToken, setAccessToken] = useState("nst_101a64d507fb287ac5eb39d0e7305c29");

  const [sourceLang, setSourceLang] = useState("English");
  const [targetLang, setTargetLang] = useState("Uzbek");
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState("Idle");

  useEffect(() => {
    async function loadKey() {
      try {
        if (BridgeModule?.getApiKey) {
          const k = await BridgeModule.getApiKey();
          if (k) setApiKey(k);
        }
      } catch {}
    }
    loadKey();
  }, []);

  useEffect(() => {
    if (!apiKey) return;
    BridgeModule?.saveApiKey?.(apiKey).catch(()=>{});
  }, [apiKey]);

  const [lastSig, setLastSig] = useState("");

  useEffect(() => {
    if (!emitter) return;
    const sub = emitter.addListener("OCR_BLOCKS", async (payload) => {
      try {
        const blocks = payload?.blocks ?? [];
        const sig = signatureFromBlocks(blocks);
        if (sig && sig === lastSig) return;
        setLastSig(sig);

        
        // Translate each unique text
        const uniqueTexts = Array.from(new Set(blocks.map(b => b.text).filter(Boolean)));
        const translations = {};

        for (const t of uniqueTexts) {
          const cached = cache.get(`${sourceLang}->${targetLang}:${t}`);
          if (cached) {
            translations[t] = cached;
            continue;
          }
          setStatus(`Translating: ${t.slice(0, 24)}...`);
          const tr = await translateViaBackend({ backendUrl, accessToken, sourceLang, targetLang, text: t });
          cache.set(`${sourceLang}->${targetLang}:${t}`, tr);
          translations[t] = tr;
        }

        // Send back blocks with translated text
        const outBlocks = blocks.map(b => ({
          ...b,
          translated: translations[b.text] ?? ""
        }));

        BridgeModule.updateOverlay(outBlocks);
        setStatus(`Overlay updated (${outBlocks.length})`);
      } catch (e) {
        setStatus(`Error: ${String(e.message || e)}`);
      }
    });

    return () => sub.remove();
  }, [apiKey, sourceLang, targetLang, cache, lastSig]);

  const toggle = async () => {
    if (!BridgeModule) {
      setStatus("BridgeModule not available (Android only).");
      return;
    }
    try {
      if (!running) {
        if (Platform.OS === "android") {
          const ok = await BridgeModule.ensureOverlayPermission();
          if (!ok) {
            setStatus("Overlay permission required");
            return;
          }
        }
        await BridgeModule.configure("", sourceLang, targetLang);
        await BridgeModule.start();
        setRunning(true);
        setStatus("Running");
      } else {
        await BridgeModule.stop();
        setRunning(false);
        setStatus("Stopped");
      }
    } catch (e) {
      setStatus(`Error: ${String(e.message || e)}`);
    }
  };

  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.card}>
        <Text style={styles.h1}>Live Screen Translator</Text>
        <Text style={styles.label}>Backend URL</Text>
        <TextInput value={backendUrl} onChangeText={setBackendUrl} style={styles.input} placeholder="https://your-backend" />

        <Text style={styles.label}>Access Token</Text>
        <TextInput value={accessToken} onChangeText={setAccessToken} secureTextEntry style={styles.input} placeholder="nst_..." />

        <Text style={styles.label}>Source language</Text>
        <TextInput value={sourceLang} onChangeText={setSourceLang} style={styles.input} />
        <Text style={styles.label}>Target language</Text>
        <TextInput value={targetLang} onChangeText={setTargetLang} style={styles.input} />
        <Text style={styles.status}>{status}</Text>
      </View>

      <FloatingButton running={running} onToggle={toggle} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, padding: 16, backgroundColor: "#0b0f14" },
  card: { backgroundColor: "#121826", borderRadius: 16, padding: 16 },
  h1: { fontSize: 22, fontWeight: "800", color: "white", marginBottom: 12 },
  label: { color: "#b0bec5", marginTop: 10, marginBottom: 6 },
  input: { backgroundColor: "#1f2a3a", borderRadius: 10, padding: 12, color: "white" },
  status: { color: "#cfd8dc", marginTop: 14 }
});
