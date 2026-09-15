import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Palette, KeyRound, Brain, MessageSquareText, LogOut, User, Edit2, Check, ChevronDown, Plus, Trash2, Wifi, ShieldCheck, X, Sun, Moon, ChevronRight } from 'lucide-react';
import { cn } from '../lib/utils';
import { ApiKeys, ProviderConfig, ModelConfig } from '../types';
import type { ConnectionTestResult } from '../api';
import type { AuthUser } from './AuthScreen';

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
  providers: ProviderConfig[];
  setProviders: React.Dispatch<React.SetStateAction<ProviderConfig[]>>;
  onTestProvider: (provider: ProviderConfig) => Promise<ConnectionTestResult>;
  onSelectProviderModel: (modelId: string) => void;
  accountUser: AuthUser | null;
  onLogout: () => Promise<void>;
}

const emptyDraft = { name: '', baseUrl: '', apiKey: '' };

type SettingsView = 'main' | 'account' | 'appearance' | 'providers' | 'provider' | 'memory' | 'systemPrompt';

function Toggle({ checked, onChange, label }: { checked: boolean; onChange: () => void; label: string }) {
  return (
    <motion.button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      whileTap={{ scale: 0.96 }}
      onClick={onChange}
      className={cn('neo-settings-switch', checked && 'is-on')}
    >
      <motion.span
        className="neo-settings-switch-thumb"
        animate={{ x: checked ? 22 : 2 }}
        transition={{ type: 'spring', stiffness: 500, damping: 32, mass: 0.55 }}
      />
    </motion.button>
  );
}

export function Settings({ isOpen, onClose, fontFamily, setFontFamily, theme, setTheme, apiKeys, setApiKeys, selectedModel, setSelectedModel, providers, setProviders, onTestProvider, onSelectProviderModel, accountUser, onLogout }: SettingsProps) {
  const [currentView, setCurrentView] = useState<SettingsView>('main');
  const [providerId, setProviderId] = useState<string | null>(null);
  const [isFontDropdownOpen, setIsFontDropdownOpen] = useState(false);
  const [showAddProvider, setShowAddProvider] = useState(false);
  const [providerDraft, setProviderDraft] = useState(emptyDraft);
  const [showAddModel, setShowAddModel] = useState(false);
  const [modelDraft, setModelDraft] = useState({ name: '', id: '', input: 'text' as 'text'|'vision' });
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<ConnectionTestResult | null>(null);
  const [memoryEnabled, setMemoryEnabled] = useState(() => localStorage.getItem('neo-gpt-memory-enabled') !== 'false');
  const [systemPrompt, setSystemPrompt] = useState(() => localStorage.getItem('neo-gpt-system-prompt') || '');

  const notify = (message: string) => window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: message }));
  const activeProvider = providers.find(p => p.id === providerId) || null;

  useEffect(() => {
    if (!isOpen) return;
    setCurrentView('main');
    setProviderId(null);
    setShowAddProvider(false);
    setShowAddModel(false);
    setIsFontDropdownOpen(false);
    setTestResult(null);
  }, [isOpen]);

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (!isOpen || event.key !== 'Escape') return;
      if (showAddModel) { setShowAddModel(false); return; }
      if (showAddProvider) { setShowAddProvider(false); return; }
      if (currentView === 'provider') { setCurrentView('providers'); setProviderId(null); return; }
      if (currentView !== 'main') { setCurrentView('main'); return; }
      onClose();
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, currentView, showAddProvider, showAddModel, onClose]);

  const goBack = () => {
    setIsFontDropdownOpen(false); setShowAddProvider(false); setShowAddModel(false); setTestResult(null);
    if (currentView === 'provider') { setCurrentView('providers'); setProviderId(null); }
    else if (currentView === 'main') onClose();
    else setCurrentView('main');
  };

  const updateProvider = (id: string, patch: Partial<ProviderConfig>) => setProviders(prev => prev.map(p => p.id === id ? { ...p, ...patch } : p));
  const openProvider = (id: string) => { setProviderId(id); setCurrentView('provider'); setTestResult(null); setShowAddModel(false); };

  const addCustomProvider = () => {
    const name = providerDraft.name.trim(); const baseUrl = providerDraft.baseUrl.trim();
    if (!name || !baseUrl) { notify('Provider name and API endpoint are required.'); return; }
    const id = `custom-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    setProviders(prev => [...prev, { id, name, baseUrl, apiKey: providerDraft.apiKey.trim(), enabled: true, builtIn: false, models: [] }]);
    setProviderDraft(emptyDraft); setShowAddProvider(false); setProviderId(id); setCurrentView('provider'); notify(`${name} added`);
  };

  const deleteProvider = () => {
    if (!activeProvider || activeProvider.builtIn) return;
    if (!window.confirm(`Delete ${activeProvider.name}?`)) return;
    if (selectedModel.startsWith(`custom/${activeProvider.id}/`)) setSelectedModel('venus-3.1');
    setProviders(prev => prev.filter(p => p.id !== activeProvider.id)); setProviderId(null); setCurrentView('providers'); notify('Provider deleted');
  };

  const addModel = () => {
    if (!activeProvider) return;
    const id = modelDraft.id.trim(); const name = modelDraft.name.trim();
    if (!id || !name) { notify('Model name and model ID are required.'); return; }
    const fullId = activeProvider.builtIn ? `${activeProvider.id === 'openRouter' ? 'openrouter' : activeProvider.id}/${id}` : `custom/${activeProvider.id}/${id}`;
    const model: ModelConfig = { id: fullId, name, providerId: activeProvider.id, input: modelDraft.input, deletable: true };
    updateProvider(activeProvider.id, { models: [...activeProvider.models.filter(m => m.id !== fullId), model] });
    setModelDraft({ name: '', id: '', input: 'text' }); setShowAddModel(false); notify(`${name} added`);
  };

  const deleteModel = (model: ModelConfig) => {
    if (!model.deletable) { notify('Built-in model cannot be removed.'); return; }
    updateProvider(model.providerId, { models: (activeProvider?.models || []).filter(m => m.id !== model.id) });
    if (selectedModel === model.id) setSelectedModel('venus-3.1'); notify('Model removed');
  };

  const testActiveProvider = async () => {
    if (!activeProvider) return;
    setTesting(true); setTestResult(null);
    try { const result = await onTestProvider(activeProvider); setTestResult(result); if (result.ok && result.models.length === 0) notify('Connected successfully. No models were returned by this provider.'); }
    finally { setTesting(false); }
  };

  const saveMemory = () => { const next = !memoryEnabled; setMemoryEnabled(next); localStorage.setItem('neo-gpt-memory-enabled', String(next)); notify(next ? 'Memory enabled' : 'Memory disabled'); };
  const saveSystemPrompt = (value: string) => { setSystemPrompt(value); localStorage.setItem('neo-gpt-system-prompt', value); };

  const settingRows = [
    { key: 'appearance', label: 'Appearance', description: 'Theme and app font', icon: Palette },
    { key: 'providers', label: 'API & Models', description: 'Providers, keys and model selection', icon: KeyRound },
    { key: 'memory', label: 'Memory', description: 'Control saved preferences for Neo Gpt', icon: Brain },
    { key: 'systemPrompt', label: 'System Prompt', description: 'Customize how Neo Gpt responds', icon: MessageSquareText },
  ] as const;

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 18 }}
          transition={{ duration: 0.42, ease: [0.16, 1, 0.3, 1] }}
          className="absolute inset-0 z-[80] flex flex-col overflow-hidden neo-settings bg-[#f8f9fa] dark:bg-[#121212]"
        >
          <div className="neo-settings-content flex-1 overflow-y-auto px-4">
            <div className="neo-settings-spacer" />
            <div className="flex items-center gap-3 py-2">
              <motion.button type="button" whileTap={{ scale: 0.9 }} onClick={goBack} aria-label="Back" className="neo-settings-back">
                <ArrowLeft size={22} />
              </motion.button>
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">Settings</h1>
            </div>

            <AnimatePresence mode="wait">
              {currentView === 'main' && (
                <motion.div key="main" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -12 }} transition={{ duration: 0.38 }} className="space-y-5 pb-8">
                  <motion.div role="button" tabIndex={0} onClick={() => setCurrentView('account')} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setCurrentView('account'); }} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08, duration: 0.5 }} className="neo-account-card w-full text-left cursor-pointer">
                    <div className="relative">
                      <div className="neo-account-avatar overflow-hidden">{accountUser?.avatarUrl ? <img src={accountUser.avatarUrl} alt="Account avatar" className="w-full h-full object-cover" /> : (accountUser?.name || accountUser?.email || 'N').slice(0, 2).toUpperCase()}</div>
                      <motion.button type="button" whileTap={{ scale: 0.88 }} onClick={() => notify('Profile photo changes are managed by your authentication provider.')} aria-label="Account profile" className="neo-account-edit"><User size={13}/></motion.button>
                    </div>
                    <div className="min-w-0">
                      <div className="text-lg font-bold text-gray-900 dark:text-white truncate">{accountUser?.name || accountUser?.email || 'Account'}</div>
                      <div className="text-sm text-gray-500 dark:text-gray-400 truncate">{accountUser?.email || 'Signed in'}</div>
                    </div>
                    <ChevronRight size={19} className="ml-auto text-gray-400" />
                  </motion.div>

                  <div className="neo-settings-section">
                    <div className="neo-settings-section-title">Preferences</div>
                    <div className="neo-settings-card">
                      {settingRows.map(({ key, label, description, icon: Icon }, index) => (
                        <motion.button key={key} type="button" whileTap={{ scale: 0.985 }} onClick={() => setCurrentView(key)} className={cn('neo-settings-row', index < settingRows.length - 1 && 'has-divider')}>
                          <span className="neo-settings-row-icon"><Icon size={20}/></span>
                          <span className="min-w-0 text-left flex-1"><span className="block text-[16px] font-semibold text-gray-900 dark:text-gray-100">{label}</span><span className="block text-xs mt-0.5 text-gray-500 dark:text-gray-400 truncate">{description}</span></span>
                          <ChevronRight size={18} className="text-gray-400 flex-shrink-0" />
                        </motion.button>
                      ))}
                    </div>
                  </div>

                  <motion.button type="button" whileTap={{ scale: 0.985 }} onClick={async () => { if (window.confirm('Log out of Neo Gpt?')) await onLogout(); }} className="neo-settings-logout">
                    <LogOut size={20}/><span>Log out</span>
                  </motion.button>
                </motion.div>
              )}

              {currentView === 'account' && (
                <motion.div key="account" initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -18 }} className="space-y-4 pb-8 pt-4">
                  <div className="neo-settings-card p-5">
                    <div className="neo-account-avatar mx-auto mb-4 overflow-hidden">{accountUser?.avatarUrl ? <img src={accountUser.avatarUrl} alt="Account avatar" className="w-full h-full object-cover" /> : (accountUser?.name || accountUser?.email || 'N').slice(0, 2).toUpperCase()}</div>
                    <h2 className="text-center text-xl font-bold text-gray-900 dark:text-white">{accountUser?.name || 'Account'}</h2>
                    <p className="mt-1 text-center text-sm text-gray-500 dark:text-gray-400 break-all">{accountUser?.email || 'Signed in'}</p>
                    <div className="mt-5 rounded-2xl bg-gray-50 p-4 text-sm dark:bg-zinc-800/70"><div className="font-semibold text-gray-900 dark:text-white">Account status</div><div className="mt-1 text-gray-500 dark:text-gray-400">Authenticated and session persistence is enabled.</div></div>
                  </div>
                  <motion.button type="button" whileTap={{ scale: .985 }} onClick={async () => { if (window.confirm('Log out of Neo Gpt?')) await onLogout(); }} className="neo-settings-logout"><LogOut size={20}/><span>Sign out</span></motion.button>
                </motion.div>
              )}

              {currentView === 'appearance' && (
                <motion.div key="appearance" initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -18 }} className="space-y-4 pb-8 pt-4">
                  <div className="neo-settings-card">
                    <div className="neo-settings-row has-divider">
                      <span className="neo-settings-row-icon">{theme === 'dark' ? <Moon size={20}/> : <Sun size={20}/>}</span>
                      <span className="min-w-0 flex-1"><span className="block text-[16px] font-semibold">Dark mode</span><span className="block text-xs mt-0.5 text-gray-500 dark:text-gray-400">Use a darker Neo Gpt interface</span></span>
                      <Toggle checked={theme === 'dark'} onChange={() => setTheme(theme === 'dark' ? 'light' : 'dark')} label="Dark mode" />
                    </div>
                    <div className="neo-settings-row relative">
                      <span className="neo-settings-row-icon"><Palette size={20}/></span>
                      <span className="min-w-0 flex-1"><span className="block text-[16px] font-semibold">App font</span><span className="block text-xs mt-0.5 text-gray-500 dark:text-gray-400">{fontFamily === 'inter' ? 'Inter' : 'Josefin Sans'}</span></span>
                      <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={() => setIsFontDropdownOpen(v => !v)} className="neo-font-select"><span>{fontFamily === 'inter' ? 'Inter' : 'Josefin Sans'}</span><ChevronDown size={16}/></motion.button>
                      <AnimatePresence>{isFontDropdownOpen && <motion.div initial={{ opacity: 0, y: -5, scale: .97 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -5, scale: .97 }} className="neo-font-menu">
                        <button type="button" onClick={() => { setFontFamily('inter'); setIsFontDropdownOpen(false); }}><span>Inter</span>{fontFamily === 'inter' && <Check size={17}/>}</button>
                        <button type="button" onClick={() => { setFontFamily('josefin'); setIsFontDropdownOpen(false); }}><span>Josefin Sans</span>{fontFamily === 'josefin' && <Check size={17}/>}</button>
                      </motion.div>}</AnimatePresence>
                    </div>
                  </div>
                </motion.div>
              )}

              {currentView === 'memory' && (
                <motion.div key="memory" initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -18 }} className="space-y-4 pb-8 pt-4">
                  <div className="neo-settings-card">
                    <div className="neo-settings-row">
                      <span className="neo-settings-row-icon"><Brain size={20}/></span>
                      <span className="min-w-0 flex-1"><span className="block text-[16px] font-semibold">Use memory</span><span className="block text-xs mt-0.5 text-gray-500 dark:text-gray-400">Allow Neo Gpt to keep useful preferences on this device.</span></span>
                      <Toggle checked={memoryEnabled} onChange={saveMemory} label="Use memory" />
                    </div>
                  </div>
                </motion.div>
              )}

              {currentView === 'systemPrompt' && (
                <motion.div key="systemPrompt" initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -18 }} className="space-y-4 pb-8 pt-4">
                  <div className="neo-settings-card p-4">
                    <div className="flex items-start gap-3 mb-3"><span className="neo-settings-row-icon"><MessageSquareText size={20}/></span><div><div className="font-semibold text-gray-900 dark:text-white">System prompt</div><div className="text-xs text-gray-500 dark:text-gray-400 mt-1">Optional instructions for how Neo Gpt should behave.</div></div></div>
                    <textarea value={systemPrompt} onChange={e => saveSystemPrompt(e.target.value)} placeholder="Example: Answer clearly, be concise, and use Bengali when I write in Bengali." rows={8} className="neo-settings-textarea" />
                  </div>
                </motion.div>
              )}

              {currentView === 'providers' && (
                <motion.div key="providers" initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -18 }} className="space-y-4 pb-8 pt-4">
                  <div className="flex items-center justify-between gap-3"><p className="text-xs text-gray-500 dark:text-gray-400">Enable providers and manage their models.</p><motion.button type="button" whileTap={{scale:.95}} onClick={() => setShowAddProvider(true)} className="neo-primary-small"><Plus size={16}/> Custom</motion.button></div>
                  {providers.map(provider => <motion.div key={provider.id} whileTap={{scale:.995}} className="neo-settings-card overflow-hidden"><div className="neo-settings-row">
                    <button type="button" onClick={() => openProvider(provider.id)} className="flex-1 min-w-0 text-left"><div className="font-semibold text-gray-900 dark:text-white truncate">{provider.name}</div><div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{provider.models.length} model{provider.models.length === 1 ? '' : 's'} {provider.builtIn ? '• built-in' : '• custom'}</div></button>
                    <Toggle checked={provider.enabled} onChange={() => updateProvider(provider.id,{enabled:!provider.enabled})} label={`Enable ${provider.name}`} />
                    <motion.button type="button" whileTap={{scale:.9}} onClick={() => openProvider(provider.id)} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800"><ChevronRight size={18}/></motion.button>
                  </div></motion.div>)}
                  {showAddProvider && <div className="fixed inset-0 z-[200] bg-black/45 flex items-end sm:items-center justify-center p-4" onPointerDown={() => setShowAddProvider(false)}><motion.div initial={{y:30,opacity:0}} animate={{y:0,opacity:1}} onPointerDown={e=>e.stopPropagation()} className="neo-modal"><div className="flex justify-between items-center"><h3 className="font-semibold text-lg">Add Custom Provider</h3><button type="button" onClick={()=>setShowAddProvider(false)}><X/></button></div><input value={providerDraft.name} onChange={e=>setProviderDraft({...providerDraft,name:e.target.value})} placeholder="Provider name" className="neo-settings-input"/><input value={providerDraft.baseUrl} onChange={e=>setProviderDraft({...providerDraft,baseUrl:e.target.value})} placeholder="API base URL" className="neo-settings-input"/><input type="password" value={providerDraft.apiKey} onChange={e=>setProviderDraft({...providerDraft,apiKey:e.target.value})} placeholder="API key" className="neo-settings-input"/><button type="button" onClick={addCustomProvider} className="neo-primary-button">Add Provider</button></motion.div></div>}
                </motion.div>
              )}

              {currentView === 'provider' && activeProvider && (
                <motion.div key="provider" initial={{ opacity: 0, x: 22 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -18 }} className="space-y-4 pb-8 pt-4">
                  <div className="neo-settings-card p-4 space-y-4"><div className="flex items-center justify-between gap-3"><div className="min-w-0"><div className="font-semibold text-lg truncate">{activeProvider.name}</div><div className="text-xs text-gray-500 dark:text-gray-400">{activeProvider.builtIn ? 'Built-in provider' : 'Custom OpenAI-compatible provider'}</div></div><Toggle checked={activeProvider.enabled} onChange={()=>updateProvider(activeProvider.id,{enabled:!activeProvider.enabled})} label={`Enable ${activeProvider.name}`} /></div>
                    <div><label className="block text-sm font-semibold mb-1.5">API Key</label><input type="password" value={activeProvider.apiKey} onChange={e=>{const value=e.target.value;updateProvider(activeProvider.id,{apiKey:value});if(activeProvider.builtIn){if(activeProvider.id==='openRouter')setApiKeys({...apiKeys,openRouter:value});if(activeProvider.id==='nvidia')setApiKeys({...apiKeys,nvidia:value});if(activeProvider.id==='gemini')setApiKeys({...apiKeys,gemini:value});}}} placeholder="API key" className="neo-settings-input"/></div>
                    {!activeProvider.builtIn && <div><label className="block text-sm font-semibold mb-1.5">API Base URL</label><input value={activeProvider.baseUrl} onChange={e=>updateProvider(activeProvider.id,{baseUrl:e.target.value})} placeholder="https://api.example.com/v1" className="neo-settings-input"/></div>}
                    <button type="button" disabled={testing} onClick={testActiveProvider} className="neo-primary-button bg-emerald-500 disabled:opacity-60"><Wifi size={17}/>{testing?'Testing connection…':'Test Connection'}</button>
                    {testResult && <div className={cn('rounded-2xl p-3 text-sm',testResult.ok?'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-300':'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-300')}><div className="flex gap-2 items-start"><ShieldCheck size={18}/><span>{testResult.message}</span></div>{testResult.ok && testResult.models.length>0 && <div className="mt-3 space-y-2"><div className="text-xs font-semibold">Select a model to add and use</div>{testResult.models.slice(0,40).map(model=>{const fullId=activeProvider.builtIn?`${activeProvider.id==='openRouter'?'openrouter':activeProvider.id}/${model.id}`:`custom/${activeProvider.id}/${model.id}`;return <button key={fullId} type="button" onClick={()=>{const item:ModelConfig={id:fullId,name:model.name||model.id,providerId:activeProvider.id,input:model.input||'text',deletable:true};updateProvider(activeProvider.id,{models:activeProvider.models.some(x=>x.id===fullId)?activeProvider.models:[...activeProvider.models,item]});onSelectProviderModel(fullId);setTestResult(null);setCurrentView('providers');setProviderId(null);notify(`${item.name} selected`);}} className="w-full text-left rounded-xl bg-white/70 dark:bg-zinc-900/50 px-3 py-2 hover:bg-white dark:hover:bg-zinc-800"><div className="font-medium text-xs">{model.name||model.id}</div><div className="text-[10px] opacity-70 break-all">{model.id}</div></button>})}</div>}</div>}
                    {!activeProvider.builtIn && <button type="button" onClick={deleteProvider} className="w-full flex items-center justify-center gap-2 rounded-2xl border border-red-200 dark:border-red-900/40 text-red-600 py-3 font-semibold"><Trash2 size={17}/> Delete Provider</button>}
                  </div>
                  <div className="neo-settings-card p-4"><div className="flex items-center justify-between mb-3"><div><div className="font-semibold">Models</div><div className="text-xs text-gray-500 dark:text-gray-400">Add, remove or select models.</div></div><button type="button" onClick={()=>setShowAddModel(true)} className="neo-primary-small"><Plus size={16}/> Add</button></div>
                    <div className="space-y-2 max-h-[42vh] overflow-y-auto">{activeProvider.models.map(model=><div key={model.id} className={cn('flex items-center gap-2 rounded-2xl border p-3',selectedModel===model.id?'border-blue-500 bg-blue-50 dark:bg-blue-900/20':'border-gray-100 dark:border-zinc-800')}><button type="button" onClick={()=>{onSelectProviderModel(model.id);setCurrentView('providers');setProviderId(null);setTestResult(null)}} className="flex-1 text-left min-w-0"><div className="font-medium text-sm truncate">{model.name}</div><div className="text-[11px] text-gray-500 dark:text-gray-400 break-all">{model.id}</div></button>{selectedModel===model.id&&<Check size={17} className="text-blue-500"/>}<button type="button" onClick={()=>deleteModel(model)} className="p-2 text-red-500 rounded-full hover:bg-red-50 dark:hover:bg-red-900/20"><Trash2 size={16}/></button></div>)}</div>
                  </div>
                  {showAddModel && <div className="fixed inset-0 z-[200] bg-black/45 flex items-end sm:items-center justify-center p-4" onPointerDown={()=>setShowAddModel(false)}><motion.div initial={{y:30,opacity:0}} animate={{y:0,opacity:1}} onPointerDown={e=>e.stopPropagation()} className="neo-modal"><div className="flex justify-between items-center"><h3 className="font-semibold text-lg">Add Model</h3><button type="button" onClick={()=>setShowAddModel(false)}><X/></button></div><input value={modelDraft.name} onChange={e=>setModelDraft({...modelDraft,name:e.target.value})} placeholder="Model display name" className="neo-settings-input"/><input value={modelDraft.id} onChange={e=>setModelDraft({...modelDraft,id:e.target.value})} placeholder="Model ID / slug" className="neo-settings-input"/><select value={modelDraft.input} onChange={e=>setModelDraft({...modelDraft,input:e.target.value as 'text'|'vision'})} className="neo-settings-input"><option value="text">Text</option><option value="vision">Text + Vision</option></select><button type="button" onClick={addModel} className="neo-primary-button">Add Model</button></motion.div></div>}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
