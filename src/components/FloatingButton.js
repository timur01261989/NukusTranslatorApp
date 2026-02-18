import React from "react";
import { View, TouchableOpacity, Text, StyleSheet } from "react-native";

export default function FloatingButton({ running, onToggle }) {
  return (
    <View pointerEvents="box-none" style={styles.wrap}>
      <TouchableOpacity onPress={onToggle} style={[styles.btn, running ? styles.on : styles.off]}>
        <Text style={styles.txt}>{running ? "STOP" : "START"}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { position: "absolute", right: 16, bottom: 32 },
  btn: { paddingVertical: 12, paddingHorizontal: 16, borderRadius: 24, elevation: 4 },
  on: { backgroundColor: "#d32f2f" },
  off: { backgroundColor: "#2e7d32" },
  txt: { color: "white", fontWeight: "700" }
});
