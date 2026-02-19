import React, {useEffect, useMemo, useRef, useState} from "react";
import {
  Animated,
  PanResponder,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";

/**
 * Draggable + auto-hide Floating Button (in-app).
 * - Drag & drop: user can move it
 * - Auto-hide: after idle, opacity reduces; tap to restore
 *
 * NOTE: System-level overlay button (over other apps) must be implemented natively.
 * This component is for inside the RN app UI.
 */

const IDLE_MS = 6000;
const MIN_OPACITY = 0.2;

export default function FloatingButton({
  initialX = 20,
  initialY = 120,
  label = "●",
  onPress,
}) {
  const pos = useRef(new Animated.ValueXY({x: initialX, y: initialY})).current;
  const opacity = useRef(new Animated.Value(1)).current;

  const idleTimer = useRef(null);
  const [dragging, setDragging] = useState(false);

  const scheduleHide = () => {
    if (idleTimer.current) clearTimeout(idleTimer.current);
    idleTimer.current = setTimeout(() => {
      Animated.timing(opacity, {
        toValue: MIN_OPACITY,
        duration: 250,
        useNativeDriver: true,
      }).start();
    }, IDLE_MS);
  };

  const wake = () => {
    if (idleTimer.current) clearTimeout(idleTimer.current);
    Animated.timing(opacity, {
      toValue: 1,
      duration: 120,
      useNativeDriver: true,
    }).start();
    scheduleHide();
  };

  useEffect(() => {
    scheduleHide();
    return () => {
      if (idleTimer.current) clearTimeout(idleTimer.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: (_, g) =>
          Math.abs(g.dx) > 2 || Math.abs(g.dy) > 2,
        onPanResponderGrant: () => {
          setDragging(true);
          wake();
          pos.setOffset({x: pos.x.__getValue(), y: pos.y.__getValue()});
          pos.setValue({x: 0, y: 0});
        },
        onPanResponderMove: Animated.event([null, {dx: pos.x, dy: pos.y}], {
          useNativeDriver: false,
        }),
        onPanResponderRelease: (_, g) => {
          pos.flattenOffset();
          setDragging(false);
          // snap a bit inside screen (optional)
          wake();
        },
        onPanResponderTerminate: () => {
          pos.flattenOffset();
          setDragging(false);
          wake();
        },
      }),
    [pos]
  );

  return (
    <Animated.View
      style={[
        styles.wrap,
        {
          transform: pos.getTranslateTransform(),
          opacity,
        },
      ]}
      {...panResponder.panHandlers}
    >
      <Pressable
        onPress={() => {
          wake();
          if (!dragging && onPress) onPress();
        }}
        onPressIn={wake}
        style={({pressed}) => [
          styles.btn,
          pressed ? styles.pressed : null,
        ]}
      >
        <Text style={styles.label}>{label}</Text>
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    position: "absolute",
    left: 0,
    top: 0,
    zIndex: 9999,
  },
  btn: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: "#111",
    alignItems: "center",
    justifyContent: "center",
    elevation: 6,
  },
  pressed: {
    transform: [{scale: 0.98}],
  },
  label: {
    color: "white",
    fontSize: 18,
    fontWeight: "700",
  },
});
