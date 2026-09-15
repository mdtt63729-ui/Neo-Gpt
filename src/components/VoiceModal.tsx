import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { AudioLines, Info } from 'lucide-react';
import { cn } from '../lib/utils';

interface VoiceModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTranscript: (text: string) => void;
  onSilenceSubmit: () => void;
}

export function VoiceModal({ isOpen, onClose, onTranscript, onSilenceSubmit }: VoiceModalProps) {
  const [step, setStep] = useState<1 | 2>(1);
  const recognitionRef = React.useRef<any>(null);
  const timeoutRef = React.useRef<NodeJS.Timeout | null>(null);

  React.useEffect(() => {
    if (isOpen) {
      setStep(2);
      
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (SpeechRecognition) {
        const recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        
        let finalTranscript = '';

        recognition.onresult = (event: any) => {
          let interimTranscript = '';
          for (let i = event.resultIndex; i < event.results.length; ++i) {
            if (event.results[i].isFinal) {
              finalTranscript += event.results[i][0].transcript + ' ';
            } else {
              interimTranscript += event.results[i][0].transcript;
            }
          }
          
          onTranscript(finalTranscript + interimTranscript);

          if (timeoutRef.current) clearTimeout(timeoutRef.current);
          timeoutRef.current = setTimeout(() => {
             recognition.stop();
             onSilenceSubmit();
             onClose();
          }, 2000);
        };

        recognition.onend = () => {
          if (timeoutRef.current) clearTimeout(timeoutRef.current);
        };

        try {
          recognition.start();
          recognitionRef.current = recognition;
        } catch (e) {
          console.error('Speech recognition error', e);
        }
      }
    } else {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    }
    
    return () => {
      if (recognitionRef.current) recognitionRef.current.stop();
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [isOpen]);

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ y: '100%' }}
          animate={{ y: 0 }}
          exit={{ y: '100%' }}
          transition={{ type: 'spring', bounce: 0, duration: 0.5 }}
          className="fixed inset-0 bg-white dark:bg-[#121212] z-50 flex flex-col"
        >
          <div className="flex-1 flex flex-col items-center px-6 pt-16 pb-6 overflow-y-auto custom-scrollbar">
            <AnimatePresence mode="wait">
              {step === 1 ? (
                <motion.div 
                  key="step1"
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 1.1 }}
                  className="flex flex-col items-center h-full w-full max-w-sm mx-auto"
                >
                  <div className="w-64 h-64 rounded-full bg-gradient-to-tr from-[#9bb1ff] to-[#e4eeff] dark:from-[#3a58c4] dark:to-[#1a2542] mb-12 shadow-inner" />
                  
                  <h2 className="text-4xl font-semibold mb-8 text-center text-gray-900 dark:text-white tracking-tight">Meet Voice</h2>
                  
                  <div className="space-y-6 w-full">
                    <div className="flex gap-4 items-start">
                      <AudioLines className="text-gray-700 dark:text-gray-300 mt-1 flex-shrink-0" size={24} />
                      <p className="text-gray-700 dark:text-gray-300 leading-relaxed text-lg">
                        Say what's on your mind. Neo Gpt listens, responds, and keeps the conversation flowing naturally.
                      </p>
                    </div>
                    
                    <div className="flex gap-4 items-start">
                      <Info className="text-gray-500 dark:text-gray-400 mt-1 flex-shrink-0" size={24} />
                      <p className="text-gray-600 dark:text-gray-400 leading-relaxed text-base">
                        Audio recordings are saved, and you can delete them at any time. <span className="underline cursor-pointer">Learn more</span>
                      </p>
                    </div>
                  </div>

                  <div className="mt-auto w-full pt-8">
                    <button 
                      onClick={() => setStep(2)}
                      className="w-full bg-black dark:bg-white text-white dark:text-black py-4 rounded-full text-lg font-medium hover:bg-gray-800 dark:hover:bg-gray-200 transition-colors"
                    >
                      Continue
                    </button>
                    <button 
                      onClick={onClose}
                      className="w-full text-gray-500 dark:text-gray-400 py-4 mt-2 text-sm font-medium hover:text-gray-800 dark:hover:text-gray-200 transition-colors"
                    >
                      Cancel
                    </button>
                  </div>
                </motion.div>
              ) : (
                <motion.div 
                  key="step2"
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 1.1 }}
                  className="flex flex-col items-center h-full w-full max-w-sm mx-auto"
                >
                  <div className="text-center mb-6 flex-shrink-0">
                    <h2 className="text-3xl font-medium text-gray-900 dark:text-white mb-2">Voices</h2>
                    <p className="text-gray-500 dark:text-gray-400 text-lg">Swipe to explore options</p>
                  </div>

                  <div className="w-full flex overflow-x-auto snap-x snap-mandatory custom-scrollbar pb-6 mb-auto" style={{ scrollSnapType: 'x mandatory' }}>
                    {/* Voice Options */}
                    {[
                      { name: 'Vale', desc: 'Bright and inquisitive', colors: 'from-[#84a3ff] via-[#d6e3ff] to-[#f4f7ff] dark:from-[#2c4bbf] dark:via-[#1e2e66] dark:to-[#121833]' },
                      { name: 'Echo', desc: 'Calm and steady', colors: 'from-[#ff9a9e] via-[#fecfef] to-[#f4f7ff] dark:from-[#c23b40] dark:via-[#6e1e36] dark:to-[#121833]' },
                      { name: 'Nova', desc: 'Energetic and sharp', colors: 'from-[#a18cd1] via-[#fbc2eb] to-[#f4f7ff] dark:from-[#5b409c] dark:via-[#361e6e] dark:to-[#121833]' },
                    ].map((voice, idx) => (
                      <div key={idx} className="min-w-full flex flex-col items-center snap-center px-4">
                        <div className={`w-64 h-64 sm:w-72 sm:h-72 rounded-full bg-gradient-to-b ${voice.colors} mb-8 shadow-sm flex-shrink-0`} />
                        <h2 className="text-2xl font-medium text-gray-900 dark:text-white mb-1">{voice.name}</h2>
                        <p className="text-gray-500 dark:text-gray-400 text-base">{voice.desc}</p>
                      </div>
                    ))}
                  </div>

                  <div className="flex gap-2 mb-6 flex-shrink-0">
                    {[1, 2, 3].map((dot, i) => (
                      <div 
                        key={dot} 
                        className={cn(
                          "w-2 h-2 rounded-full",
                          i === 0 ? "bg-gray-500 dark:bg-gray-400" : "bg-gray-200 dark:bg-zinc-800"
                        )}
                      />
                    ))}
                  </div>
                  
                  <div className="w-full mt-auto pt-4 flex-shrink-0">
                    <button 
                      onClick={() => {
                        setStep(1);
                        onClose();
                      }}
                      className="w-full bg-black dark:bg-white text-white dark:text-black py-4 rounded-full text-lg font-medium hover:bg-gray-800 dark:hover:bg-gray-200 transition-colors"
                    >
                      Start Voice
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
