import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Mic, X, Square, ArrowUp } from 'lucide-react';

interface DictationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTranscript: (text: string) => void;
  onComplete: () => void;
  inline?: boolean;
}

const WAVEFORM = [4, 7, 10, 14, 19, 26, 34, 22, 13, 28, 36, 24, 14, 8, 5, 10, 18, 27, 35, 23, 12, 7, 14, 21, 30, 17, 9, 5];

export function DictationModal({ isOpen, onClose, onTranscript, onComplete, inline = false }: DictationModalProps) {
  const [transcript, setTranscript] = useState('');
  const [isTranscribing, setIsTranscribing] = useState(false);
  const recognitionRef = useRef<any>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!isOpen) return;
    setTranscript(''); setIsTranscribing(false);
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) { setTranscript('Speech recognition is not supported on this device.'); return; }
    const recognition = new SpeechRecognition();
    recognition.continuous = true; recognition.interimResults = true; recognition.lang = navigator.language || 'en-US';
    let finalTranscript = '';
    recognition.onresult = (event: any) => {
      let interimTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) finalTranscript += event.results[i][0].transcript + ' ';
        else interimTranscript += event.results[i][0].transcript;
      }
      const currentText = (finalTranscript + interimTranscript).trim();
      setTranscript(currentText); onTranscript(currentText); setIsTranscribing(false);
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => { setIsTranscribing(true); try { recognition.stop(); } catch {} onComplete(); onClose(); }, 2000);
    };
    recognition.onend = () => { if (timeoutRef.current) clearTimeout(timeoutRef.current); };
    recognition.onerror = (event: any) => { if (event?.error !== 'aborted') setTranscript('Could not access the microphone.'); };
    try { recognition.start(); recognitionRef.current = recognition; } catch { setTranscript('Could not start the microphone.'); }
    return () => { try { recognition.stop(); } catch {}; recognitionRef.current = null; if (timeoutRef.current) clearTimeout(timeoutRef.current); };
  }, [isOpen]);

  const stopAndClose = () => { if (timeoutRef.current) clearTimeout(timeoutRef.current); try { recognitionRef.current?.stop(); } catch {}; recognitionRef.current = null; onClose(); };
  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: 6, scale: 0.985 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 6, scale: 0.985 }}
        transition={{ type: 'spring', stiffness: 420, damping: 30, mass: 0.7 }}
        className={inline ? 'neo-dictation-inline absolute inset-0 z-10 flex items-center px-2' : 'fixed inset-x-0 bottom-[calc(env(safe-area-inset-bottom,0px)+18px)] z-[60] flex justify-center px-4'}
        aria-live="polite"
      >
        <div className={inline ? 'w-full h-[52px] flex items-center gap-1.5 px-1' : 'w-full max-w-[420px] h-[58px] rounded-full bg-[#202124] shadow-2xl border border-white/10 flex items-center gap-2 px-2'}>
          <motion.button type="button" whileTap={{ scale: 0.82 }} onClick={stopAndClose} className={inline ? 'w-10 h-10 rounded-full flex items-center justify-center text-gray-500 dark:text-gray-300 flex-shrink-0' : 'w-11 h-11 rounded-full flex items-center justify-center text-white/90 flex-shrink-0'} aria-label="Cancel voice input"><X size={21} /></motion.button>
          <div className={inline ? 'flex-1 min-w-0 h-full flex items-center justify-center overflow-hidden' : 'flex-1 min-w-0 h-full flex items-center justify-center overflow-hidden'}>
            {transcript && isTranscribing ? <span className={inline ? 'text-sm font-medium text-gray-500 dark:text-gray-300 truncate' : 'text-sm font-medium text-white/80 truncate'}>Transcribing...</span> : (
              <div className="flex items-center gap-[2px] w-full justify-center overflow-hidden" aria-hidden="true">
                {WAVEFORM.map((height, i) => <motion.span key={i} className={inline ? 'w-[3px] rounded-full bg-blue-500/80 dark:bg-blue-400/80' : 'w-[3px] rounded-full bg-white/65'} animate={{ height: [Math.max(3, height * 0.18), height * 0.55, Math.max(3, height * 0.3)] }} transition={{ duration: 0.7 + (i % 6) * 0.07, repeat: Infinity, ease: 'easeInOut', delay: i * 0.025 }} />)}
              </div>
            )}
          </div>
          <motion.button type="button" whileTap={{ scale: 0.84 }} onClick={() => { try { recognitionRef.current?.stop(); } catch {} onComplete(); onClose(); }} className={inline ? 'w-9 h-9 rounded-full flex items-center justify-center text-gray-500 dark:text-gray-300 flex-shrink-0' : 'w-10 h-10 rounded-full flex items-center justify-center bg-white/10 text-white/80 flex-shrink-0'} aria-label="Stop voice input"><Square size={14} fill="currentColor" strokeWidth={0} /></motion.button>
          <motion.button type="button" whileTap={{ scale: 0.84 }} animate={{ scale: transcript ? [1, 1.06, 1] : 1, opacity: transcript ? 1 : 0.5 }} transition={{ type: 'spring', stiffness: 450, damping: 16, repeat: transcript ? Infinity : 0, repeatDelay: 1.8 }} onClick={() => { try { recognitionRef.current?.stop(); } catch {} onComplete(); onClose(); }} className="w-10 h-10 rounded-full bg-blue-500 text-white flex items-center justify-center shadow-md flex-shrink-0" aria-label="Send transcription"><ArrowUp size={20} strokeWidth={2.7} /></motion.button>
        </div>
      </motion.div>
    </AnimatePresence>
  );
}
