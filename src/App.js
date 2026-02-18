import React, {useEffect, useMemo, useState} from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  NativeEventEmitter,
  NativeModules,
  Modal,
  FlatList,
  Pressable,
} from "react-native";

import {getApiKey} from "./api/GeminiClient";
import {translateSmart, LANGUAGE_CODE_MAP} from "./api/GeminiClient";

const {BridgeModule} = NativeModules;

const LANGUAGES = [
  {label: "English", value: "English"},
  {label: "O‘zbek", value: "Uzbek"},
  {label: "Qoraqalpoq", value: "Karakalpak"},
  {label: "Русский", value: "Russian"},
  {label: "Deutsch", value: "German"},
  {label: "中文", value: "Chinese"},
];

function LanguagePicker({label, value, onChange}) {
  const [open, setOpen] = useState(false);

  const currentLabel = useMemo(() => {
    const found = LANGUAGES.find(l => l.value === value);
    return found ? found.label : value;
  }, [value]);

  return (
    <View style={styles.pickerRow}>
      <Text style={styles.label}>{label}</Text>
      <TouchableOpacity style={styles.pickerButton} onPress={() => setOpen(true)}>
        <Text style={styles.pickerButtonText}>{currentLabel}</Text>
      </TouchableOpacity>

      <Modal transparent visible={open} animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.modalBackdrop} onPress={() => setOpen(false)} />
        <View style={styles.modalSheet}>
          <Text style={styles.modalTitle}>{label}</Text>
          <FlatList
            data={LANGUAGES}
            keyExtractor={item => item.value}
            renderItem={({item}) => (
              <TouchableOpacity
                style={[styles.modalItem, item.value === value && styles.modalItemActive]}
                onPress={() => {
                  onChange(item.value);
                  setOpen(false);
                }}
              >
                <Text style={styles.modalItemText}>{item.label}</Text>
                <Text style={styles.modalItemSub}>{LANGUAGE_CODE_MAP[item.value] || ""}</Text>
              </TouchableOpacity>
            )}
          />
        </View>
      </Modal>
    </View>
  );
}

export default function App() {
  const [backendUrl, setBackendUrl] = useState(""); // optional
  const [accessToken, setAccessToken] = useState(""); // optional
  const [apiKey, setApiKey] = useState("");
  const [sourceLang, setSourceLang] = useState("English");
  const [targetLang, setTargetLang] = useState("Uzbek");
  const [running, setRunning] = useState(false);

  const [isBusy, setIsBusy] = useState(false);
  const [lastText, setLastText] = useState("");

  useEffect(() => {
    // Optional backend key fetcher (keeps your old flow)
    getApiKey(backendUrl, accessToken)
      .then(k => setApiKey(k || ""))
      .catch(() => setApiKey(""));
  }, [backendUrl, accessToken]);

  useEffect(() => {
    // Listen OCR blocks from native and translate -> overlay
    const emitter = new NativeEventEmitter(BridgeModule);
    const sub = emitter.addListener("OcrBlocks", async payload => {
      try {
        const blocks = payload?.blocks || [];
        const combined = blocks
          .map(b => (b?.text || "").trim())
          .filter(Boolean)
          .join("\n")
          .trim();

        if (!combined) return;

        // Don’t spam the translator for identical frames
        if (combined === lastText) return;
        setLastText(combined);

        setIsBusy(true);

        const translated = await translateSmart({
          text: combined,
          sourceLang,
          targetLang,
          backendUrl,
          accessToken,
        });

        await BridgeModule.updateOverlay(translated || "");
      } catch (e) {
        console.warn("OCR/translate/updateOverlay failed:", e?.message || e);
      } finally {
        setIsBusy(false);
      }
    });

    return () => sub.remove();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sourceLang, targetLang, backendUrl, accessToken, lastText]);

  const start = async () => {
    try {
      const ok = await BridgeModule.ensureOverlayPermission();
      if (!ok) {
        Alert.alert("Permission", "Overlay permission is required.");
        return;
      }

      // Configure native (apiKey can be empty; JS translator works anyway)
      await BridgeModule.configure(apiKey || "", sourceLang, targetLang);

      await BridgeModule.start();
      setRunning(true);
      Alert.alert("Started", "Screen Translation Started!");
    } catch (e) {
      Alert.alert("Error", e?.message || "Start failed");
    }
  };

  const stop = async () => {
    try {
      await BridgeModule.stop();
      setRunning(false);
      await BridgeModule.updateOverlay("");
    } catch (e) {
      Alert.alert("Error", e?.message || "Stop failed");
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>NukusTranslator</Text>

      <Text style={styles.hint}>
        Til tanlang. Backend sozlamasangiz ham ishlaydi (MyMemory orqali).
      </Text>

      <LanguagePicker label="Source language" value={sourceLang} onChange={setSourceLang} />
      <LanguagePicker label="Target language" value={targetLang} onChange={setTargetLang} />

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Optional backend (agar bo‘lsa)</Text>
        <TextInput
          style={styles.input}
          placeholder="Backend URL (optional)"
          value={backendUrl}
          onChangeText={setBackendUrl}
          autoCapitalize="none"
        />
        <TextInput
          style={styles.input}
          placeholder="Access Token (optional)"
          value={accessToken}
          onChangeText={setAccessToken}
          autoCapitalize="none"
          secureTextEntry
        />
      </View>

      <TouchableOpacity
        style={[styles.button, running ? styles.buttonStop : styles.buttonStart]}
        onPress={running ? stop : start}
        disabled={isBusy}
      >
        <Text style={styles.buttonText}>{running ? "STOP" : "START"}</Text>
      </TouchableOpacity>

      {isBusy ? (
        <View style={styles.busyRow}>
          <ActivityIndicator />
          <Text style={styles.busyText}>Translating…</Text>
        </View>
      ) : null}

      <Text style={styles.small}>
        Eslatma: Qoraqalpoq tili hamma tarjimonlarda yo‘q. Agar xato bo‘lsa, tizim avtomatik O‘zbekka fallback qiladi.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 20, backgroundColor: "#0b1020"},
  title: {fontSize: 28, fontWeight: "700", color: "white", marginBottom: 8},
  hint: {color: "#b8c0ff", marginBottom: 16, lineHeight: 18},

  pickerRow: {marginBottom: 12},
  label: {color: "#cbd5ff", marginBottom: 6, fontWeight: "600"},
  pickerButton: {
    borderWidth: 1,
    borderColor: "#2a3568",
    backgroundColor: "#121a35",
    paddingVertical: 12,
    paddingHorizontal: 12,
    borderRadius: 12,
  },
  pickerButtonText: {color: "white", fontSize: 16},

  section: {marginTop: 10, marginBottom: 14},
  sectionTitle: {color: "#cbd5ff", marginBottom: 8, fontWeight: "600"},
  input: {
    borderWidth: 1,
    borderColor: "#2a3568",
    backgroundColor: "#121a35",
    color: "white",
    padding: 12,
    borderRadius: 12,
    marginBottom: 8,
  },

  button: {padding: 14, borderRadius: 14, alignItems: "center"},
  buttonStart: {backgroundColor: "#2563eb"},
  buttonStop: {backgroundColor: "#ef4444"},
  buttonText: {color: "white", fontWeight: "700", fontSize: 16},

  busyRow: {flexDirection: "row", alignItems: "center", marginTop: 12, gap: 10},
  busyText: {color: "#cbd5ff"},

  small: {color: "#93a1ff", marginTop: 14, fontSize: 12, lineHeight: 16},

  modalBackdrop: {flex: 1, backgroundColor: "rgba(0,0,0,0.55)"},
  modalSheet: {
    position: "absolute",
    left: 12,
    right: 12,
    top: 90,
    bottom: 90,
    backgroundColor: "#0f1733",
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "#2a3568",
    overflow: "hidden",
  },
  modalTitle: {color: "white", fontWeight: "800", fontSize: 16, padding: 14},
  modalItem: {
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderTopWidth: 1,
    borderTopColor: "#1f2a5a",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  modalItemActive: {backgroundColor: "#121f4a"},
  modalItemText: {color: "white", fontSize: 16, fontWeight: "600"},
  modalItemSub: {color: "#93a1ff"},
});
