/** Lightweight haptic feedback for supported Android/WebView devices. */
export type HapticPattern = 'tap' | 'success' | 'warning' | 'selection';

const patterns: Record<HapticPattern, number | number[]> = {
  tap: 8,
  selection: 5,
  success: [8, 28, 12],
  warning: [14, 24, 14],
};

export function haptic(type: HapticPattern = 'tap'): void {
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(patterns[type]);
    }
  } catch {
    // Haptics are optional and must never affect app functionality.
  }
}
