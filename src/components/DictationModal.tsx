import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Mic, X, Square, ArrowUp } from 'lucide-react';

interface DictationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTranscript: (text: string) => void;
  onComplete: () => void;
}

const WAVEFORM = [4, 7, 10, 14, 19, 26, 34, 22, 13, 28, 36, 24, 14, 8, 5, 10, 18, 27, 35, 23, 12, 7, 14, 21, 30, 17, 9, 5];

export function DictationModal({ isOpen, onClose, onTranscript, onComplete }: DictationModalProps) {
  const [transcript, setTranscript] = useState('');
  const [isTranscribing, setIsTranscribing] = useState(false);
  const recognitionRef = useRef<any>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    setTranscript('');
    setIsTranscribing(false);
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setTranscript('Speech recognition is not supported on this device.');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = navigator.language || 'en-US';
    let finalTranscript = '';

    recognition.onresult = (event: any) => {
      let interimTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) finalTranscript += event.results[i][0].transcript + ' ';
        else interimTranscript += event.results[i][0].transcript;
      }
      const currentText = (finalTranscript + interimTranscript).trim();
      setTranscript(currentText);
      onTranscript(currentText);
      setIsTranscribing(false);

      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => {
        setIsTranscribing(true);
        recognition.stop();
        onComplete();
        onClose();
      }, 2000);
    };

    recognition.onend = () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };

    recognition.onerror = (event: any) => {
      if (event?.error !== 'aborted') setTranscript('Could not access the microphone.');
    };

    try {
      recognition.start();
      recognitionRef.current = recognition;
    } catch {
      setTranscript('Could not start the microphone.');
    }

    return () => {
      try { recognition.stop(); } catch {}
      recognitionRef.current = null;
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [isOpen]);

  const stopAndClose = () => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    try { recognitionRef.current?.stop(); } catch {}
    recognitionRef.current = null;
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[60] pointer-events-none"
        >
          <motion.div
            initial={{ y: 90, opacity: 0, scale: 0.94 }}
            animate={{ y: 0, opacity: 1, scale: 1 }}
            exit={{ y: 70, opacity: 0, scale: 0.96 }}
            transition={{ type: 'spring', stiffness: 420, damping: 28, mass: 0.7 }}
            className="pointer-events-auto absolute left-1/2 -translate-x-1/2 bottom-[calc(env(safe-area-inset-bottom,0px)+18px)] w-[calc(100%-48px)] max-w-[420px] h-[58px] rounded-full bg-[#202124] dark:bg-[#202124] shadow-2xl border border-white/10 flex items-center gap-2 px-2"
          >
            <motion.button
              type="button"
              whileTap={{ scale: 0.82 }}
              transition={{ type: 'spring', stiffness: 500, damping: 18 }}
              onClick={stopAndClose}
              className="w-11 h-11 rounded-full flex items-center justify-center text-white/90 hover:bg-white/10 flex-shrink-0"
              aria-label="Cancel voice input"
            >
              <X size={22} strokeWidth={2.2} />
            </motion.button>

            <div className="flex-1 min-w-0 h-full flex items-center justify-center overflow-hidden">
              {transcript && isTranscribing ? (
                <motion.span
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="text-sm font-medium text-white/80 truncate px-2"
                >Transcribing...</motion.span>
              ) : transcript ? (
                <div className="flex items-center gap-[2px] w-full justify-center overflow-hidden" aria-hidden="true">
                  {WAVEFORM.map((height, i) => (
                    <motion.span
                      key={i}
                      className="w-[3px] rounded-full bg-white/65"
                      animate={{ height: [Math.max(3, height * 0.35), height, Math.max(3, height * 0.55), Math.max(3, height * 0.35)] }}
                      transition={{ duration: 0.75 + (i % 5) * 0.08, repeat: Infinity, ease: 'easeInOut', delay: i * 0.018 }}
                    />
                  ))}
                </div>
              ) : (
                <div className="flex items-center gap-[2px] w-full justify-center overflow-hidden" aria-hidden="true">
                  {WAVEFORM.map((height, i) => (
                    <motion.span
                      key={i}
                      className="w-[3px] rounded-full bg-white/50"
                      animate={{ height: [Math.max(3, height * 0.18), height * 0.55, Math.max(3, height * 0.28)] }}
                      transition={{ duration: 0.7 + (i % 6) * 0.07, repeat: Infinity, ease: 'easeInOut', delay: i * 0.025 }}
                    />
                  ))}
                </div>
              )}
            </div>

            <motion.button
              type="button"
              whileTap={{ scale: 0.84 }}
              transition={{ type: 'spring', stiffness: 500, damping: 18 }}
              onClick={() => { try { recognitionRef.current?.stop(); } catch {} onComplete(); onClose(); }}
              className="w-10 h-10 rounded-full flex items-center justify-center bg-white/10 text-white/80 flex-shrink-0"
              aria-label="Stop voice input"
            >
              <Square size={15} fill="currentColor" strokeWidth={0} />
            </motion.button>

            <motion.button
              type="button"
              whileTap={{ scale: 0.84 }}
              animate={{ scale: transcript ? [1, 1.06, 1] : 1, opacity: transcript ? 1 : 0.45 }}
              transition={{ type: 'spring', stiffness: 450, damping: 16, repeat: transcript ? Infinity : 0, repeatDelay: 1.8 }}
              onClick={() => { try { recognitionRef.current?.stop(); } catch {} onComplete(); onClose(); }}
              className="w-11 h-11 rounded-full bg-[#3b82f6] text-white flex items-center justify-center shadow-lg flex-shrink-0"
              aria-label="Send transcription"
            >
              <ArrowUp size={21} strokeWidth={2.7} />
            </motion.button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
