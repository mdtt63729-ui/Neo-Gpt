import React, { useState, useRef, useEffect } from 'react';
import { Menu, Sparkles, RefreshCw, Plus, Mic, AudioLines, Camera, Image as ImageIcon, Paperclip, Puzzle, BrainCircuit, ArrowUp, Copy, ThumbsUp, ThumbsDown, Speaker, Share2, MoreVertical, X, Download, ChevronDown, Check, Eye, EyeOff, Square } from 'lucide-react';
import { cn } from './lib/utils';
import { callApi } from './api';
import { Message, ApiKeys } from './types';
import { motion, AnimatePresence } from 'motion/react';
import { Sidebar } from './components/Sidebar';
import { Settings } from './components/Settings';
import { VoiceModal } from './components/VoiceModal';
import { DictationModal } from './components/DictationModal';

const MODELS = [
  { id: 'venus-3.1', name: 'Venus 3.1', provider: 'default' },
  // OpenRouter
  { id: 'openrouter/deepseek/deepseek-chat', name: 'DeepSeek V4 Flash', provider: 'openrouter' },
  { id: 'openrouter/qwen/qwen-2.5-72b-instruct', name: 'Qwen 3.8 Flash', provider: 'openrouter' },
  { id: 'openrouter/minimax/minimax-01', name: 'MiniMax M3', provider: 'openrouter' },
  { id: 'openrouter/nvidia/nemotron-4-340b-instruct', name: 'Nemotron 3 Super 120B A12B', provider: 'openrouter' },
  { id: 'openrouter/zhipuai/glm-4-plus', name: 'GLM-5.3 Flash', provider: 'openrouter' },
  // Nvidia
  { id: 'nvidia/nemotron-4-340b-instruct', name: 'Nemotron 3 Super 120B A12B', provider: 'nvidia' },
  { id: 'nvidia/deepseek-ai/deepseek-coder-33b-instruct', name: 'DeepSeek V4 Pro', provider: 'nvidia' },
  { id: 'nvidia/minimax-01', name: 'MiniMax M3', provider: 'nvidia' },
  { id: 'nvidia/nemotron-mini-4b-instruct', name: 'Nemotron 3.5 Lightning 30B A3B', provider: 'nvidia' },
  { id: 'nvidia/google/gemma-7b-it', name: 'Gemma 4 31B IT', provider: 'nvidia' },
  // Gemini
  { id: 'gemini/gemini-1.5-pro-latest', name: 'Gemini 3.8 Flash', provider: 'gemini' },
  { id: 'gemini/gemini-1.5-flash-latest', name: 'Gemini 3.7 Flash', provider: 'gemini' },
  { id: 'gemini/gemini-1.5-flash-8b-latest', name: 'Gemini 3.7 Flash Lite', provider: 'gemini' },
  { id: 'gemini/gemini-1.0-pro-latest', name: 'Gemini 3.6 Flash', provider: 'gemini' },
];

export default function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isVoiceOpen, setIsVoiceOpen] = useState(false);
  const [isDictationOpen, setIsDictationOpen] = useState(false);
  const [isAttachmentOpen, setIsAttachmentOpen] = useState(false);
  const [isModelSelectOpen, setIsModelSelectOpen] = useState(false);
  const [isIncognito, setIsIncognito] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  
  const [theme, setTheme] = useState<'light'|'dark'>('light');
  const [fontFamily, setFontFamily] = useState<'inter' | 'josefin'>('inter');
  const [apiKeys, setApiKeys] = useState<ApiKeys>({ openRouter: '', nvidia: '', gemini: '' });
  const [selectedModel, setSelectedModel] = useState('venus-3.1');
  
  const [expandedImage, setExpandedImage] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const chatContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const saved = localStorage.getItem('neo-gpt-settings');
    if (saved) {
      const parsed = JSON.parse(saved);
      if (parsed.theme) setTheme(parsed.theme);
      if (parsed.fontFamily) setFontFamily(parsed.fontFamily);
      if (parsed.apiKeys) setApiKeys(parsed.apiKeys);
      if (parsed.selectedModel) setSelectedModel(parsed.selectedModel);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('neo-gpt-settings', JSON.stringify({ theme, fontFamily, apiKeys, selectedModel }));
  }, [theme, fontFamily, apiKeys, selectedModel]);

  const scrollToBottom = () => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTo({
        top: chatContainerRef.current.scrollHeight,
        behavior: 'smooth'
      });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 2500);
  };

  const handleActionClick = (action: string, text?: string) => {
    if (action === 'copy' && text) {
      navigator.clipboard.writeText(text);
      showToast('Copied to clipboard');
    } else if (action === 'thumbsUp') {
      showToast('Thanks for the feedback!');
    } else if (action === 'thumbsDown') {
      showToast('Feedback submitted');
    } else {
      showToast('Coming soon');
    }
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!input.trim() || isLoading) return;

    const userText = input.trim();
    setInput('');
    setIsAttachmentOpen(false);
    
    const newMessage: Message = {
      id: Date.now().toString(),
      sender: 'user',
      text: userText,
    };

    setMessages(prev => [...prev, newMessage]);
    setIsLoading(true);

    const isImageCommand = userText.toLowerCase().startsWith('/image');
    
    try {
      const prompt = isImageCommand ? userText.substring(6).trim() : userText;
      const response = await callApi(prompt, isImageCommand, selectedModel, apiKeys);
      
      if (response.status === 'success') {
        if (isImageCommand) {
          setMessages(prev => [...prev, {
            id: Date.now().toString(),
            sender: 'ai',
            imageUrl: response.imageUrl
          }]);
        } else {
          if (response.text?.trim().toLowerCase().startsWith('/image')) {
            const imgDesc = response.text.substring(response.text.toLowerCase().indexOf('/image') + 6).trim();
            const imgResponse = await callApi(imgDesc, true, selectedModel, apiKeys);
            
            if (imgResponse.status === 'success') {
               setMessages(prev => [...prev, {
                id: Date.now().toString(),
                sender: 'ai',
                imageUrl: imgResponse.imageUrl
              }]);
            } else {
               setMessages(prev => [...prev, {
                id: Date.now().toString(),
                sender: 'ai',
                text: 'Error generating image.'
              }]);
            }
          } else {
            setMessages(prev => [...prev, {
              id: Date.now().toString(),
              sender: 'ai',
              text: response.text
            }]);
          }
        }
      } else {
        setMessages(prev => [...prev, {
          id: Date.now().toString(),
          sender: 'ai',
          text: response.text || 'An error occurred. Please try again.'
        }]);
      }
    } catch (error) {
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        sender: 'ai',
        text: 'An error occurred. Please try again.'
      }]);
    } finally {
      setIsLoading(false);
      setTimeout(() => inputRef.current?.focus(), 10);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        sender: 'user',
        text: `Attached file: ${file.name}`
      }]);
      setIsAttachmentOpen(false);
    }
  };
  
  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  const availableModels = MODELS.filter(m => 
    m.provider === 'default' ||
    (m.provider === 'openrouter' && apiKeys.openRouter) ||
    (m.provider === 'nvidia' && apiKeys.nvidia) ||
    (m.provider === 'gemini' && apiKeys.gemini)
  );

  const currentModelName = MODELS.find(m => m.id === selectedModel)?.name || 'Venus 3.1';

  return (
    <div className={theme}>
      <div className={cn(
        "flex flex-col h-[100dvh] w-full bg-white dark:bg-[#121212] overflow-hidden relative shadow-2xl transition-colors duration-300",
        fontFamily === 'inter' ? 'font-inter' : 'font-josefin',
        // Mobile constraint wrapper
        "max-w-[480px] mx-auto border-x border-gray-100 dark:border-zinc-800"
      )}>
        {/* Top Bar */}
        <header className="flex items-center justify-between px-4 py-3 z-10 dark:bg-[#121212]">
          <div className="flex items-center gap-3">
            <motion.button 
              whileTap={{ scale: 0.9 }}
              onClick={() => setIsSidebarOpen(true)}
              className="p-3 bg-gray-50 dark:bg-zinc-800 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-700 transition-colors"
            >
              <Menu size={20} className="text-gray-700 dark:text-gray-200" />
            </motion.button>
            
            {/* Model Selector Capsule */}
            <div className="relative">
              <motion.button 
                whileTap={{ scale: 0.95 }}
                onClick={() => setIsModelSelectOpen(!isModelSelectOpen)}
                className="flex items-center gap-2 px-4 py-2.5 bg-gray-100 dark:bg-zinc-800 text-gray-800 dark:text-gray-200 rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700 transition-colors"
              >
                <span className="font-medium text-sm truncate max-w-[120px]">{currentModelName}</span>
                <ChevronDown size={16} />
              </motion.button>

              <AnimatePresence>
                {isModelSelectOpen && (
                  <>
                    <motion.div 
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      onClick={() => setIsModelSelectOpen(false)}
                      className="fixed inset-0 z-40"
                    />
                    <motion.div
                      initial={{ opacity: 0, y: -10, scale: 0.95 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -10, scale: 0.95 }}
                      className="absolute top-12 left-0 w-64 bg-white dark:bg-zinc-900 rounded-3xl shadow-xl border border-gray-100 dark:border-zinc-700 z-50 overflow-hidden py-2 max-h-[60vh] overflow-y-auto"
                    >
                      {availableModels.map(model => (
                        <motion.button
                          whileTap={{ scale: 0.98 }}
                          key={model.id}
                          onClick={() => {
                            setSelectedModel(model.id);
                            setIsModelSelectOpen(false);
                          }}
                          className="w-full flex items-center justify-between px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 text-left"
                        >
                          <div>
                            <div className="text-gray-900 dark:text-gray-100 font-medium text-sm">{model.name}</div>
                            <div className="text-xs text-gray-500 dark:text-gray-400 capitalize">{model.provider}</div>
                          </div>
                          {selectedModel === model.id && <Check size={16} className="text-blue-500" />}
                        </motion.button>
                      ))}
                    </motion.div>
                  </>
                )}
              </AnimatePresence>
            </div>
          </div>

          <motion.button 
            whileTap={{ scale: 0.9 }}
            onClick={() => { 
              setIsIncognito(!isIncognito); 
              showToast(isIncognito ? 'Incognito disabled' : 'Incognito enabled');
            }}
            className={cn(
              "p-3 rounded-full transition-colors", 
              isIncognito 
                ? "bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400" 
                : "bg-gray-50 dark:bg-zinc-800 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-700"
            )}
          >
            {isIncognito ? <EyeOff size={20} /> : <Eye size={20} />}
          </motion.button>
        </header>

        {/* Main Chat Area */}
        <main ref={chatContainerRef} className="flex-1 overflow-y-auto custom-scrollbar px-4 pb-24 pt-4 dark:bg-[#121212]">
          {messages.length === 0 && !isLoading && (
            <div className="h-full flex flex-col items-center justify-center text-center px-4">
              <h1 className="text-2xl font-bold text-gray-800 dark:text-gray-200 tracking-tight">How can I help you today?</h1>
            </div>
          )}
          
          <div className="flex flex-col gap-6">
            <AnimatePresence initial={false}>
              {messages.map((msg) => (
                <motion.div 
                  key={msg.id}
                  initial={{ opacity: 0, y: 10, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  className={cn(
                    "flex w-full",
                    msg.sender === 'user' ? 'justify-end' : 'justify-start'
                  )}
                >
                  {msg.sender === 'user' ? (
                    <div className="bg-blue-100 dark:bg-blue-900/50 text-gray-900 dark:text-white px-5 py-3.5 rounded-[24px] rounded-tr-[8px] max-w-[85%] break-words shadow-sm text-[15px] leading-relaxed">
                      {msg.text}
                    </div>
                  ) : (
                    <div className="flex flex-col gap-3 w-full max-w-[90%]">
                      {msg.imageUrl ? (
                        <div 
                          className="rounded-[24px] overflow-hidden border border-gray-100 dark:border-zinc-800 shadow-sm cursor-pointer hover:opacity-95 transition-opacity"
                          onClick={() => setExpandedImage(msg.imageUrl || null)}
                        >
                          <img src={msg.imageUrl} alt="Generated" className="w-full h-auto object-cover" loading="lazy" />
                        </div>
                      ) : (
                        <div className="text-gray-800 dark:text-gray-200 font-medium text-[16px] leading-relaxed break-words whitespace-pre-wrap">
                          {msg.text}
                        </div>
                      )}
                      
                      {/* Action Bar for AI message */}
                      <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400 mt-1">
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('copy', msg.text)} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><Copy size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('thumbsUp')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><ThumbsUp size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('thumbsDown')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><ThumbsDown size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('speaker')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><Speaker size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('share')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><Share2 size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('more')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><MoreVertical size={16} /></motion.button>
                      </div>
                    </div>
                  )}
                </motion.div>
              ))}
            </AnimatePresence>
            
            {isLoading && (
              <motion.div 
                initial={{ opacity: 0 }} 
                animate={{ opacity: 1 }}
                className="flex justify-start w-full"
              >
                <div className="bg-gray-50 dark:bg-zinc-800/50 px-4 py-4 rounded-[24px] rounded-tl-[8px] flex items-center gap-2 shadow-sm">
                  <motion.div animate={{ scale: [1, 1.3, 1], opacity: [0.4, 1, 0.4] }} transition={{ repeat: Infinity, duration: 1.4 }} className="w-2.5 h-2.5 bg-blue-500 rounded-full" />
                  <motion.div animate={{ scale: [1, 1.3, 1], opacity: [0.4, 1, 0.4] }} transition={{ repeat: Infinity, duration: 1.4, delay: 0.2 }} className="w-2.5 h-2.5 bg-blue-500 rounded-full" />
                  <motion.div animate={{ scale: [1, 1.3, 1], opacity: [0.4, 1, 0.4] }} transition={{ repeat: Infinity, duration: 1.4, delay: 0.4 }} className="w-2.5 h-2.5 bg-blue-500 rounded-full" />
                </div>
              </motion.div>
            )}
            <div ref={messagesEndRef} className="h-4" />
          </div>
        </main>

        {/* Bottom Input Area */}
        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-white via-white to-transparent dark:from-[#121212] dark:via-[#121212] pt-6 pb-4 px-4 z-20">
          <input 
            type="file" 
            ref={fileInputRef} 
            onChange={handleFileUpload}
            className="hidden" 
            multiple 
          />
          <AnimatePresence>
            {isAttachmentOpen && (
              <motion.div
                initial={{ opacity: 0, y: 20, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                className="absolute bottom-20 left-4 bg-[#f8f9fa] dark:bg-[#1e1e1e] rounded-[32px] shadow-xl border border-gray-100 dark:border-zinc-800 p-2 min-w-[220px] overflow-hidden"
              >
                <div className="flex flex-col gap-1">
                  <motion.button whileTap={{ scale: 0.98 }} onClick={triggerFileInput} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors">
                    <div className="bg-white dark:bg-zinc-800 p-2.5 rounded-full shadow-sm">
                      <Camera size={20} className="text-gray-700 dark:text-gray-200" />
                    </div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">Camera</span>
                  </motion.button>
                  <motion.button whileTap={{ scale: 0.98 }} onClick={triggerFileInput} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors">
                    <div className="bg-white dark:bg-zinc-800 p-2.5 rounded-full shadow-sm">
                      <ImageIcon size={20} className="text-gray-700 dark:text-gray-200" />
                    </div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">Photos</span>
                  </motion.button>
                  <motion.button whileTap={{ scale: 0.98 }} onClick={triggerFileInput} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors">
                    <div className="bg-white dark:bg-zinc-800 p-2.5 rounded-full shadow-sm">
                      <Paperclip size={20} className="text-gray-700 dark:text-gray-200" />
                    </div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">Files</span>
                  </motion.button>
                  <motion.button whileTap={{ scale: 0.98 }} onClick={() => handleActionClick('plugin')} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors">
                    <div className="bg-white dark:bg-zinc-800 p-2.5 rounded-full shadow-sm">
                      <Puzzle size={20} className="text-gray-700 dark:text-gray-200" />
                    </div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">Plugins</span>
                  </motion.button>
                  <motion.button whileTap={{ scale: 0.98 }} onClick={() => handleActionClick('think')} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors mt-2 border-t border-gray-200 dark:border-zinc-700 pt-3">
                    <div className="bg-white dark:bg-zinc-800 p-2.5 rounded-full shadow-sm">
                      <BrainCircuit size={20} className="text-gray-700 dark:text-gray-200" />
                    </div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">Think harder</span>
                  </motion.button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <form onSubmit={handleSubmit} className="relative flex items-center bg-[#f4f4f5] dark:bg-zinc-800/80 rounded-[32px] px-2 py-1.5 shadow-sm border border-gray-100 dark:border-zinc-700 transition-all focus-within:ring-2 focus-within:ring-blue-100 dark:focus-within:ring-blue-900/50 focus-within:border-blue-200 dark:focus-within:border-blue-800">
            <motion.button
              whileTap={{ scale: 0.9 }}
              type="button"
              onClick={() => setIsAttachmentOpen(!isAttachmentOpen)}
              className="p-3 text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 transition-colors rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700 flex-shrink-0"
            >
              <Plus size={24} />
            </motion.button>
            
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask Neo Gpt"
              rows={1}
              className="flex-1 bg-transparent border-none outline-none resize-none max-h-32 py-3.5 px-2 text-gray-800 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 font-medium text-[16px] flex items-center"
              style={{ minHeight: '52px' }}
            />

            <div className="flex items-center gap-1 pr-1 flex-shrink-0">
              <motion.button 
                whileTap={{ scale: 0.9 }} 
                type="button" 
                onClick={() => setIsDictationOpen(true)}
                className="p-3 text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 transition-colors rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700"
              >
                <Mic size={22} />
              </motion.button>

              {isLoading ? (
                <motion.button
                  whileTap={{ scale: 0.9 }}
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  type="button"
                  onClick={() => setIsLoading(false)}
                  className="w-10 h-10 bg-gray-900 dark:bg-white rounded-full flex items-center justify-center text-white dark:text-gray-900 shadow-sm transition-colors ml-1"
                >
                  <Square size={16} fill="currentColor" strokeWidth={0} />
                </motion.button>
              ) : input.trim() ? (
                <motion.button
                  whileTap={{ scale: 0.9 }}
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  type="submit"
                  className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white shadow-sm hover:bg-blue-600 transition-colors ml-1"
                >
                  <ArrowUp size={20} strokeWidth={2.5} />
                </motion.button>
              ) : (
                <motion.button 
                  whileTap={{ scale: 0.9 }} 
                  type="button" 
                  onClick={() => setIsVoiceOpen(true)}
                  className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white shadow-sm hover:bg-blue-600 transition-colors ml-1"
                >
                  <AudioLines size={20} />
                </motion.button>
              )}
            </div>
          </form>
        </div>

        {/* Global Toast */}
        <AnimatePresence>
          {toastMsg && (
            <motion.div
              initial={{ opacity: 0, y: 50, x: '-50%' }}
              animate={{ opacity: 1, y: 0, x: '-50%' }}
              exit={{ opacity: 0, y: 50, x: '-50%' }}
              className="fixed bottom-24 left-1/2 bg-black dark:bg-white text-white dark:text-black px-6 py-3 rounded-full shadow-lg z-[100] text-sm font-medium whitespace-nowrap"
            >
              {toastMsg}
            </motion.div>
          )}
        </AnimatePresence>

        {/* Overlays */}
        <Sidebar 
          isOpen={isSidebarOpen} 
          onClose={() => setIsSidebarOpen(false)} 
          onOpenSettings={() => setIsSettingsOpen(true)}
        />
        
        <Settings 
          isOpen={isSettingsOpen} 
          onClose={() => setIsSettingsOpen(false)}
          fontFamily={fontFamily}
          setFontFamily={setFontFamily}
          theme={theme}
          setTheme={setTheme}
          apiKeys={apiKeys}
          setApiKeys={setApiKeys}
          selectedModel={selectedModel}
          setSelectedModel={setSelectedModel}
          models={availableModels}
        />
        
        <VoiceModal 
          isOpen={isVoiceOpen} 
          onClose={() => setIsVoiceOpen(false)}
          onTranscript={(text) => setInput(text)}
          onSilenceSubmit={() => {
            setTimeout(() => {
              handleSubmit();
            }, 100);
          }}
        />

        <DictationModal
          isOpen={isDictationOpen}
          onClose={() => setIsDictationOpen(false)}
          onTranscript={(text) => setInput(text)}
          onComplete={() => {
            setTimeout(() => {
              inputRef.current?.focus();
            }, 100);
          }}
        />

        {/* Expanded Image Viewer */}
        <AnimatePresence>
          {expandedImage && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setExpandedImage(null)}
              className="fixed inset-0 bg-black/90 z-[60] flex items-center justify-center p-4 backdrop-blur-sm"
            >
              <button 
                className="absolute top-6 right-6 p-3 bg-white/10 rounded-full text-white hover:bg-white/20 transition-colors"
                onClick={() => setExpandedImage(null)}
              >
                <X size={24} />
              </button>
              <motion.img 
                initial={{ scale: 0.9 }}
                animate={{ scale: 1 }}
                exit={{ scale: 0.9 }}
                src={expandedImage} 
                alt="Expanded" 
                className="max-w-full max-h-[80vh] rounded-2xl shadow-2xl object-contain"
                onClick={(e) => e.stopPropagation()}
              />
              <button 
                className="absolute bottom-10 left-1/2 -translate-x-1/2 flex items-center gap-2 bg-white text-black px-6 py-3 rounded-full font-medium hover:bg-gray-100 transition-colors shadow-lg"
                onClick={(e) => {
                  e.stopPropagation();
                  const a = document.createElement('a');
                  a.href = expandedImage;
                  a.download = 'neo-gpt-image.png';
                  document.body.appendChild(a);
                  a.click();
                  document.body.removeChild(a);
                }}
              >
                <Download size={20} />
                Download
              </button>
            </motion.div>
          )}
        </AnimatePresence>

      </div>
    </div>
  );
}
