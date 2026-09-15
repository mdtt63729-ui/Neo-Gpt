import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Bell, AudioLines, Shield, User, MonitorSmartphone, Database, LayoutDashboard, Bug, Info, LogOut, Check, ChevronDown, Edit2, Sparkles, AlertCircle, Settings as SettingsIcon, Moon, Sun, Key } from 'lucide-react';
import { cn } from '../lib/utils';
import { ApiKeys } from '../types';

interface ModelType { id: string; name: string; provider: string; }

interface SettingsProps {
  isOpen: boolean;
  onClose: () => void;
  fontFamily: string;
  setFontFamily: (font: 'inter'|'josefin') => void;
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;
  apiKeys: ApiKeys;
  setApiKeys: (keys: ApiKeys) => void;
  selectedModel: string;
  setSelectedModel: (id: string) => void;
  models: ModelType[];
}

export function Settings({ isOpen, onClose, fontFamily, setFontFamily, theme, setTheme, apiKeys, setApiKeys, selectedModel, setSelectedModel, models }: SettingsProps) {
  const [currentView, setCurrentView] = useState<'main' | 'account' | 'appearance' | 'providers'>('main');
  const [isFontDropdownOpen, setIsFontDropdownOpen] = useState(false);

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ y: '100%' }}
          animate={{ y: 0 }}
          exit={{ y: '100%' }}
          transition={{ type: 'spring', bounce: 0, duration: 0.4 }}
          className="absolute inset-0 bg-[#f8f9fa] dark:bg-[#121212] z-50 flex flex-col overflow-hidden"
        >
          <div className="flex items-center px-4 py-4 pt-12 dark:bg-[#121212]">
            <motion.button whileTap={{ scale: 0.9 }} onClick={() => currentView === 'main' ? onClose() : setCurrentView('main')} className="p-2 hover:bg-gray-200 dark:hover:bg-zinc-800 rounded-full bg-white dark:bg-zinc-800 shadow-sm transition-colors">
              <ArrowLeft size={24} className="text-gray-700 dark:text-gray-200" />
            </motion.button>
            {currentView === 'main' && (
              <div className="flex-1 flex flex-col items-center -ml-8">
                <div className="relative">
                  <div className="w-20 h-20 bg-orange-400 rounded-full flex items-center justify-center text-white text-3xl font-medium shadow-md">
                    DM
                  </div>
                  <div className="absolute bottom-0 right-0 w-6 h-6 bg-white dark:bg-zinc-800 rounded-full flex items-center justify-center shadow-sm">
                    <Edit2 size={12} className="text-gray-700 dark:text-gray-200" />
                  </div>
                </div>
                <h2 className="text-xl font-semibold mt-3 text-gray-900 dark:text-white">DHUN Music</h2>
              </div>
            )}
          </div>

          <div className="flex-1 overflow-y-auto px-4 pb-8">
            <AnimatePresence mode="wait">
              {currentView === 'main' && (
                <motion.div
                  key="main"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="space-y-6"
                >
                  <div>
                    <h3 className="text-gray-500 dark:text-gray-400 text-sm font-semibold mb-2 px-2 uppercase tracking-wide">My Neo Gpt</h3>
                    <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <User size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Personalization</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Database size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Memory</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 transition-colors">
                        <LayoutDashboard size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Plugins</span>
                      </motion.button>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-gray-500 dark:text-gray-400 text-sm font-semibold mb-2 px-2 uppercase tracking-wide">Account</h3>
                    <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                      <motion.button whileTap={{ scale: 0.98 }} onClick={() => setCurrentView('account')} className="w-full flex items-center justify-between p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <div className="flex items-center gap-4">
                          <User size={24} className="text-gray-700 dark:text-gray-300" />
                          <div className="text-left">
                            <div className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Workspace</div>
                            <div className="text-sm text-gray-500 dark:text-gray-400">Personal</div>
                          </div>
                        </div>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} onClick={() => setCurrentView('providers')} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Key size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">API Providers & Models</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Sparkles size={24} className="text-blue-500" />
                        <span className="text-[17px] font-medium text-blue-500">Upgrade plan</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Database size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Usage and limits</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 transition-colors">
                        <User size={24} className="text-gray-700 dark:text-gray-300" />
                        <div className="text-left">
                          <div className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Email</div>
                          <div className="text-sm text-gray-500 dark:text-gray-400">dhunmusic521@gmail.com</div>
                        </div>
                      </motion.button>
                    </div>
                  </div>
                  
                  <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                      <motion.button whileTap={{ scale: 0.98 }} onClick={() => setCurrentView('appearance')} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <SettingsIcon size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Appearance</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Bell size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Notifications</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <AudioLines size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Voice</span>
                      </motion.button>
                      <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Shield size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Safety</span>
                      </motion.button>
                       <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 border-b border-gray-100 dark:border-zinc-800 transition-colors">
                        <Bug size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Report bug</span>
                      </motion.button>
                       <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 transition-colors">
                        <Info size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">About</span>
                      </motion.button>
                  </div>

                  <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm mt-6">
                    <motion.button whileTap={{ scale: 0.98 }} className="w-full flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-zinc-800 text-red-500 transition-colors">
                        <LogOut size={24} />
                        <span className="text-[17px] font-medium">Log out</span>
                    </motion.button>
                  </div>
                </motion.div>
              )}

              {currentView === 'appearance' && (
                <motion.div
                  key="appearance"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  className="space-y-6 pt-4"
                >
                  <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                    {/* Theme Toggle */}
                    <div className="p-4 border-b border-gray-100 dark:border-zinc-800 flex justify-between items-center relative">
                      <div className="flex items-center gap-4">
                        {theme === 'light' ? <Sun size={24} className="text-gray-700 dark:text-gray-300" /> : <Moon size={24} className="text-gray-700 dark:text-gray-300" />}
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Dark Mode</span>
                      </div>
                      <button 
                        onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
                        className={cn(
                          "w-12 h-6 rounded-full relative transition-colors duration-300",
                          theme === 'dark' ? "bg-blue-500" : "bg-gray-300"
                        )}
                      >
                        <div className={cn(
                          "w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform duration-300 shadow-sm",
                          theme === 'dark' ? "translate-x-6" : "translate-x-0.5"
                        )} />
                      </button>
                    </div>

                    {/* Font Family */}
                    <div className="p-4 border-b border-gray-100 dark:border-zinc-800 flex justify-between items-center relative">
                      <div className="flex items-center gap-4">
                        <SettingsIcon size={24} className="text-gray-700 dark:text-gray-300" />
                        <span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">App Font</span>
                      </div>
                      <motion.button 
                        whileTap={{ scale: 0.95 }}
                        onClick={() => setIsFontDropdownOpen(!isFontDropdownOpen)}
                        className="flex items-center gap-2 text-gray-600 dark:text-gray-300 bg-gray-50 dark:bg-zinc-800 px-3 py-1.5 rounded-[12px] font-medium transition-colors"
                      >
                        {fontFamily === 'inter' ? 'Inter' : 'Josefin Sans'}
                        <ChevronDown size={16} />
                      </motion.button>

                      {isFontDropdownOpen && (
                        <div className="absolute top-14 right-4 bg-white dark:bg-zinc-800 rounded-xl shadow-lg border border-gray-100 dark:border-zinc-700 w-48 z-10 overflow-hidden">
                          <button 
                            onClick={() => { setFontFamily('inter'); setIsFontDropdownOpen(false); }}
                            className="w-full text-left px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-700 flex items-center justify-between font-inter text-gray-800 dark:text-gray-100 transition-colors"
                          >
                            Inter (Default)
                            {fontFamily === 'inter' && <Check size={16} className="text-blue-500" />}
                          </button>
                          <button 
                            onClick={() => { setFontFamily('josefin'); setIsFontDropdownOpen(false); }}
                            className="w-full text-left px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-700 flex items-center justify-between font-josefin text-gray-800 dark:text-gray-100 transition-colors"
                          >
                            Josefin Sans
                            {fontFamily === 'josefin' && <Check size={16} className="text-blue-500" />}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </motion.div>
              )}

              {currentView === 'providers' && (
                <motion.div
                  key="providers"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  className="space-y-6 pt-4"
                >
                  <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm p-4 space-y-4">
                    <p className="text-sm text-gray-500 dark:text-gray-400 font-medium">Add your API keys to unlock advanced models in the chat.</p>
                    
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">OpenRouter Key</label>
                        <input 
                          type="password" 
                          value={apiKeys.openRouter} 
                          onChange={(e) => setApiKeys({...apiKeys, openRouter: e.target.value})}
                          className="w-full bg-gray-50 dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 rounded-2xl px-4 py-3 text-gray-800 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                          placeholder="sk-or-..."
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">NVIDIA NIM Key</label>
                        <input 
                          type="password" 
                          value={apiKeys.nvidia} 
                          onChange={(e) => setApiKeys({...apiKeys, nvidia: e.target.value})}
                          className="w-full bg-gray-50 dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 rounded-2xl px-4 py-3 text-gray-800 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                          placeholder="nvapi-..."
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Gemini API Key</label>
                        <input 
                          type="password" 
                          value={apiKeys.gemini} 
                          onChange={(e) => setApiKeys({...apiKeys, gemini: e.target.value})}
                          className="w-full bg-gray-50 dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 rounded-2xl px-4 py-3 text-gray-800 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                          placeholder="AIza..."
                        />
                      </div>
                    </div>
                  </div>

                  <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm p-5 space-y-4">
                    <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300">Select Model</label>
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-3">Choose the AI model to use for chat responses. Only models with valid API keys will work.</p>
                    <div className="space-y-2 max-h-64 overflow-y-auto pr-2 custom-scrollbar">
                      {models.map((model) => (
                        <motion.button
                          key={model.id}
                          whileTap={{ scale: 0.98 }}
                          onClick={() => setSelectedModel(model.id)}
                          className={cn(
                            "w-full flex items-center justify-between px-4 py-3 rounded-2xl border transition-colors",
                            selectedModel === model.id 
                              ? "border-blue-500 bg-blue-50 dark:bg-blue-900/20" 
                              : "border-gray-100 dark:border-zinc-800 bg-gray-50 dark:bg-zinc-800/50 hover:bg-gray-100 dark:hover:bg-zinc-800"
                          )}
                        >
                          <div className="text-left flex flex-col">
                            <span className={cn("text-[15px] font-medium", selectedModel === model.id ? "text-blue-700 dark:text-blue-400" : "text-gray-800 dark:text-gray-200")}>{model.name}</span>
                            <span className="text-[12px] text-gray-500 dark:text-gray-400 capitalize">{model.provider}</span>
                          </div>
                          {selectedModel === model.id && <Check size={18} className="text-blue-500" />}
                        </motion.button>
                      ))}
                    </div>
                  </div>
                </motion.div>
              )}

              {currentView === 'account' && (
                <motion.div
                  key="account"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  className="space-y-6 pt-4 text-center text-gray-500 dark:text-gray-400 font-medium"
                >
                  Account details here...
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
