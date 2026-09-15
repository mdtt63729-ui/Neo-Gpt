import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'motion/react';
import { X, Square } from 'lucide-react';

interface DictationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTranscript: (text: string) => void;
  onComplete: () => void;
  inline?: boolean;
  geminiApiKey?: string;
}

const BAR_COUNT = 28;

/**
 * Mobile-safe dictation. Prefer Web Speech when the WebView exposes it, but
 * never assume it exists. Android WebViews vary by OS/device, so a MediaRecorder
 * + Gemini Transcribe fallback prevents the microphone button from crashing the app.
 */
export function DictationModal({ isOpen, onClose, onTranscript, onComplete, inline = false, geminiApiKey = '' }: DictationModalProps) {
  const [isListening, setIsListening] = useState(false);
  const [level, setLevel] = useState(0.08);
  const [status, setStatus] = useState('Listening');
  const recognitionRef = useRef<any>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const rafRef = useRef<number | null>(null);
  const silenceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const finalTextRef = useRef('');
  const interimTextRef = useRef('');
  const hasSpokenRef = useRef(false);
  const usingRecorderRef = useRef(false);
  const finishingRef = useRef(false);

  const clearSilenceTimer = () => {
    if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);
    silenceTimerRef.current = null;
  };

  const stopMeterOnly = () => {
    if (rafRef.current) cancelAnimationFrame(rafRef.current);
    rafRef.current = null;
    analyserRef.current = null;
    if (audioContextRef.current) {
      try { void audioContextRef.current.close(); } catch {}
    }
    audioContextRef.current = null;
    setLevel(0.08);
  };

  const stopTracks = () => {
    streamRef.current?.getTracks().forEach(track => {
      try { track.stop(); } catch {}
    });
    streamRef.current = null;
  };

  const cleanup = () => {
    clearSilenceTimer();
    try { recognitionRef.current?.stop(); } catch {}
    recognitionRef.current = null;
    try { recorderRef.current?.stop(); } catch {}
    recorderRef.current = null;
    stopTracks();
    stopMeterOnly();
    setIsListening(false);
  };

  const emitTranscript = (text: string) => {
    const clean = text.replace(/\s+/g, ' ').trim();
    if (clean) onTranscript(clean);
  };

  const finish = (text = '') => {
    if (finishingRef.current) return;
    finishingRef.current = true;
    clearSilenceTimer();
    emitTranscript(text || `${finalTextRef.current} ${interimTextRef.current}`);
    cleanup();
    onComplete();
    onClose();
    window.setTimeout(() => { finishingRef.current = false; }, 120);
  };

  const blobToBase64 = (blob: Blob): Promise<string> => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const value = String(reader.result || '');
      const comma = value.indexOf(',');
      resolve(comma >= 0 ? value.slice(comma + 1) : value);
    };
    reader.onerror = () => reject(reader.error || new Error('Could not read recording.'));
    reader.readAsDataURL(blob);
  });

  const transcribeRecording = async (blob: Blob) => {
    if (!geminiApiKey.trim()) {
      onComplete();
      onClose();
      window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: 'Add a Gemini API key in Settings for voice dictation on this device.' }));
      return;
    }
    try {
      setStatus('Transcribing');
      const data = await blobToBase64(blob);
      const mimeType = blob.type || 'audio/webm';
      const response = await fetch('https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-transcribe:generateContent', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': geminiApiKey.trim() },
        body: JSON.stringify({
          contents: [{ parts: [
            { text: 'Transcribe only the spoken words. Return the clean transcript and nothing else.' },
            { inlineData: { mimeType, data } },
          ] }],
          generationConfig: { audioTranscriptionConfig: { languageCodes: [], mode: 'SMART' } },
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload?.error?.message || `Gemini transcription failed (${response.status}).`);
      const text = payload?.candidates?.[0]?.content?.parts?.map((part: any) => part?.text || '').join(' ').trim() || '';
      finish(text);
    } catch (error) {
      cleanup();
      onComplete();
      onClose();
      window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: error instanceof Error ? error.message : 'Voice transcription failed.' }));
    }
  };

  const stopRecorderAndTranscribe = () => {
    clearSilenceTimer();
    const recorder = recorderRef.current;
    if (!recorder || recorder.state === 'inactive') {
      const blob = chunksRef.current.length ? new Blob(chunksRef.current, { type: 'audio/webm' }) : null;
      if (blob && blob.size > 0) void transcribeRecording(blob);
      else finish();
      return;
    }
    setStatus('Transcribing');
    recorder.onstop = () => {
      const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
      recorderRef.current = null;
      if (blob.size > 0) void transcribeRecording(blob);
      else finish();
    };
    try { recorder.stop(); } catch { finish(); }
  };

  const armSilenceTimer = () => {
    clearSilenceTimer();
    if (!hasSpokenRef.current) return;
    silenceTimerRef.current = window.setTimeout(() => {
      if (usingRecorderRef.current) stopRecorderAndTranscribe();
      else finish();
    }, 2000);
  };

  const startAudioMeter = (stream: MediaStream) => {
    try {
      const AudioContextCtor = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContextCtor) return;
      const context = new AudioContextCtor();
      const analyser = context.createAnalyser();
      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.82;
      context.createMediaStreamSource(stream).connect(analyser);
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
        const next = Math.min(1, Math.max(0.04, rms * 3.6));
        setLevel(next);
        if (next > 0.085) {
          hasSpokenRef.current = true;
          armSilenceTimer();
        }
        rafRef.current = requestAnimationFrame(tick);
      };
      tick();
    } catch {
      // The waveform is cosmetic; dictation continues without the meter.
    }
  };

  const startRecorderFallback = async () => {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: 'Microphone recording is not available on this device.' }));
      onComplete();
      onClose();
      return;
    }
    try {
      stopTracks();
      stopMeterOnly();
      const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true } });
      streamRef.current = stream;
      startAudioMeter(stream);
      chunksRef.current = [];
      const preferred = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus'].find(type => MediaRecorder.isTypeSupported?.(type));
      const recorder = preferred ? new MediaRecorder(stream, { mimeType: preferred }) : new MediaRecorder(stream);
      recorder.ondataavailable = event => { if (event.data?.size) chunksRef.current.push(event.data); };
      recorder.onstart = () => { setIsListening(true); setStatus('Listening'); };
      recorder.onerror = () => { window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: 'Microphone recording failed. Please try again.' })); cleanup(); onComplete(); onClose(); };
      recorderRef.current = recorder;
      usingRecorderRef.current = true;
      recorder.start(250);
      setIsListening(true);
      setStatus('Listening');
    } catch (error) {
      cleanup();
      window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: error instanceof Error ? error.message : 'Microphone permission was denied.' }));
      onComplete();
      onClose();
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    finishingRef.current = false;
    finalTextRef.current = '';
    interimTextRef.current = '';
    hasSpokenRef.current = false;
    usingRecorderRef.current = false;
    chunksRef.current = [];
    setIsListening(false);
    setStatus('Listening');

    let disposed = false;
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      void startRecorderFallback();
      return () => { disposed = true; if (!disposed) return; cleanup(); };
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = navigator.language || 'en-IN';
      recognition.onstart = () => { if (!disposed) { setIsListening(true); setStatus('Listening'); } };
      recognition.onresult = (event: any) => {
        if (disposed) return;
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
        // Android WebViews can expose SpeechRecognition but still reject start.
        // Fall back to recorder instead of propagating an exception into React.
        if (disposed || event?.error === 'aborted') return;
        usingRecorderRef.current = true;
        try { recognition.stop(); } catch {}
        recognitionRef.current = null;
        void startRecorderFallback();
      };
      recognition.onend = () => {
        if (disposed || finishingRef.current) return;
        if (hasSpokenRef.current) armSilenceTimer();
      };
      recognitionRef.current = recognition;
      try {
        recognition.start();
      } catch {
        recognitionRef.current = null;
        void startRecorderFallback();
      }
    } catch {
      void startRecorderFallback();
    }

    // The audio meter is best-effort and separately guarded from recognition.
    const meterPromise = navigator.mediaDevices?.getUserMedia
      ? navigator.mediaDevices.getUserMedia({ audio: true })
      : Promise.reject(new Error('Microphone API unavailable'));
    void meterPromise.then(stream => {
      if (disposed) { stream.getTracks().forEach(track => track.stop()); return; }
      streamRef.current = stream;
      startAudioMeter(stream);
    }).catch(() => {});

    return () => {
      disposed = true;
      cleanup();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const waveform = Array.from({ length: BAR_COUNT }, (_, index) => {
    const center = 1 - Math.abs(index - (BAR_COUNT - 1) / 2) / ((BAR_COUNT - 1) / 2);
    const pulse = 0.16 + level * (0.42 + center * 0.9);
    return Math.max(5, Math.round(7 + 30 * pulse));
  });

  const stop = () => {
    if (usingRecorderRef.current) stopRecorderAndTranscribe();
    else finish();
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.985, y: 4 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.985, y: 4 }}
      transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
      className={inline ? 'neo-dictation-inline absolute inset-0 z-10 flex items-center px-1' : 'fixed inset-x-0 bottom-4 z-[60] flex justify-center px-4'}
      aria-live="polite"
    >
      <div className="neo-dictation-inner">
        <motion.button type="button" whileTap={{ scale: 0.82 }} onClick={() => finish()} className="neo-dictation-cancel" aria-label="Cancel voice input"><X size={19}/></motion.button>
        <div className="neo-dictation-wave" aria-hidden="true">
          {waveform.map((height, index) => (
            <motion.span key={index} animate={{ height: isListening ? height : 5, opacity: isListening ? 0.45 + level * 0.55 : 0.35 }} transition={{ duration: 0.14, ease: 'easeOut' }} />
          ))}
        </div>
        <motion.span className="neo-dictation-status">{status}</motion.span>
        <motion.button type="button" whileTap={{ scale: 0.84 }} animate={{ scale: isListening ? [1, 1.04, 1] : 1 }} transition={{ repeat: isListening ? Infinity : 0, duration: 1.5 }} onClick={stop} className="neo-dictation-stop" aria-label="Stop voice input"><Square size={13} fill="currentColor" strokeWidth={0}/></motion.button>
      </div>
    </motion.div>
  );
}
