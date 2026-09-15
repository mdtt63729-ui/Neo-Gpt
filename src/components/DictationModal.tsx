import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'motion/react';
import { X, Square } from 'lucide-react';

interface DictationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTranscript: (text: string) => void;
  onComplete: () => void;
  inline?: boolean;
}

const BAR_COUNT = 28;

export function DictationModal({ isOpen, onClose, onTranscript, onComplete, inline = false }: DictationModalProps) {
  const [isListening, setIsListening] = useState(false);
  const [level, setLevel] = useState(0.12);
  const recognitionRef = useRef<any>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const rafRef = useRef<number | null>(null);
  const silenceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const finalTextRef = useRef('');
  const interimTextRef = useRef('');
  const hasSpokenRef = useRef(false);

  const clearSilenceTimer = () => {
    if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);
    silenceTimerRef.current = null;
  };

  const stopAudioMeter = () => {
    if (rafRef.current) cancelAnimationFrame(rafRef.current);
    rafRef.current = null;
    streamRef.current?.getTracks().forEach(track => track.stop());
    streamRef.current = null;
    analyserRef.current = null;
    if (audioContextRef.current) void audioContextRef.current.close();
    audioContextRef.current = null;
    setLevel(0.08);
  };

  const commitTranscript = (close = true) => {
    clearSilenceTimer();
    const text = `${finalTextRef.current} ${interimTextRef.current}`.replace(/\s+/g, ' ').trim();
    if (text) onTranscript(text);
    try { recognitionRef.current?.stop(); } catch {}
    recognitionRef.current = null;
    stopAudioMeter();
    setIsListening(false);
    if (close) {
      onComplete();
      onClose();
    }
  };

  const armSilenceTimer = () => {
    clearSilenceTimer();
    if (!hasSpokenRef.current) return;
    silenceTimerRef.current = setTimeout(() => commitTranscript(true), 2000);
  };

  useEffect(() => {
    if (!isOpen) return;
    finalTextRef.current = '';
    interimTextRef.current = '';
    hasSpokenRef.current = false;
    setIsListening(false);

    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      onTranscript('Speech recognition is not supported on this device.');
      onComplete();
      onClose();
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = navigator.language || 'en-IN';
    recognition.onstart = () => setIsListening(true);
    recognition.onresult = (event: any) => {
      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const piece = String(event.results[i]?.[0]?.transcript || '').trim();
        if (!piece) continue;
        if (event.results[i].isFinal) finalTextRef.current = `${finalTextRef.current} ${piece}`.trim();
        else interim = `${interim} ${piece}`.trim();
      }
      interimTextRef.current = interim;
      if (finalTextRef.current || interimTextRef.current) {
        hasSpokenRef.current = true;
        armSilenceTimer();
      }
    };
    recognition.onerror = (event: any) => {
      if (event?.error !== 'aborted') {
        onTranscript('Could not access the microphone.');
        onComplete();
        onClose();
      }
    };
    recognition.onend = () => {
      if (isListening && hasSpokenRef.current) armSilenceTimer();
    };
    recognitionRef.current = recognition;

    const startMeter = async () => {
      try {
        const stream = await navigator.mediaDevices?.getUserMedia?.({ audio: true });
        if (!stream) return;
        streamRef.current = stream;
        const context = new AudioContext();
        const analyser = context.createAnalyser();
        analyser.fftSize = 256;
        analyser.smoothingTimeConstant = 0.78;
        const source = context.createMediaStreamSource(stream);
        source.connect(analyser);
        audioContextRef.current = context;
        analyserRef.current = analyser;
        const data = new Uint8Array(analyser.fftSize);
        const tick = () => {
          if (!analyserRef.current) return;
          analyserRef.current.getByteTimeDomainData(data);
          let sum = 0;
          for (let i = 0; i < data.length; i += 1) {
            const sample = (data[i] - 128) / 128;
            sum += sample * sample;
          }
          const rms = Math.sqrt(sum / data.length);
          setLevel(Math.min(1, Math.max(0.06, rms * 3.4)));
          rafRef.current = requestAnimationFrame(tick);
        };
        tick();
      } catch {
        // Speech recognition can still work when the browser does not expose a meter.
      }
    };

    void startMeter();
    try { recognition.start(); } catch { onTranscript('Could not start the microphone.'); onComplete(); onClose(); }

    return () => {
      clearSilenceTimer();
      try { recognition.stop(); } catch {}
      recognitionRef.current = null;
      stopAudioMeter();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const waveform = Array.from({ length: BAR_COUNT }, (_, index) => {
    const center = 1 - Math.abs(index - (BAR_COUNT - 1) / 2) / ((BAR_COUNT - 1) / 2);
    const pulse = 0.18 + level * (0.45 + center * 0.9);
    return Math.max(5, Math.round(7 + 30 * pulse));
  });

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.98 }}
      transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
      className={inline ? 'neo-dictation-inline absolute inset-0 z-10 flex items-center px-1' : 'fixed inset-x-0 bottom-4 z-[60] flex justify-center px-4'}
      aria-live="polite"
    >
      <div className="neo-dictation-inner">
        <motion.button type="button" whileTap={{ scale: 0.82 }} onClick={() => commitTranscript(true)} className="neo-dictation-cancel" aria-label="Cancel voice input"><X size={19}/></motion.button>
        <div className="neo-dictation-wave" aria-hidden="true">
          {waveform.map((height, index) => (
            <motion.span key={index} animate={{ height: isListening ? height : 5, opacity: isListening ? 0.45 + level * 0.55 : 0.35 }} transition={{ duration: 0.12, ease: 'easeOut' }} />
          ))}
        </div>
        <motion.button type="button" whileTap={{ scale: 0.84 }} animate={{ scale: isListening ? [1, 1.05, 1] : 1 }} transition={{ repeat: isListening ? Infinity : 0, duration: 1.5 }} onClick={() => commitTranscript(true)} className="neo-dictation-stop" aria-label="Stop voice input"><Square size={13} fill="currentColor" strokeWidth={0}/></motion.button>
      </div>
    </motion.div>
  );
}
