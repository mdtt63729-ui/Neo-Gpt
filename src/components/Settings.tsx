import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, Database, LayoutDashboard, User, Key, Sparkles, Settings as SettingsIcon, Moon, Sun, Check, ChevronDown, Edit2, LogOut, Plus, Trash2, Wifi, ShieldCheck, X } from 'lucide-react';
import { cn } from '../lib/utils';
import { ApiKeys, ProviderConfig, ModelConfig } from '../types';
import type { ConnectionTestResult } from '../api';

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
}

const emptyDraft = { name: '', baseUrl: '', apiKey: '' };

export function Settings({ isOpen, onClose, fontFamily, setFontFamily, theme, setTheme, apiKeys, setApiKeys, selectedModel, setSelectedModel, providers, setProviders, onTestProvider, onSelectProviderModel }: SettingsProps) {
  const [currentView, setCurrentView] = useState<'main' | 'account' | 'appearance' | 'providers' | 'provider'>('main');
  const [providerId, setProviderId] = useState<string | null>(null);
  const [isFontDropdownOpen, setIsFontDropdownOpen] = useState(false);
  const [showAddProvider, setShowAddProvider] = useState(false);
  const [providerDraft, setProviderDraft] = useState(emptyDraft);
  const [showAddModel, setShowAddModel] = useState(false);
  const [modelDraft, setModelDraft] = useState({ name: '', id: '', input: 'text' as 'text'|'vision' });
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<ConnectionTestResult | null>(null);

  const notify = (message: string) => window.dispatchEvent(new CustomEvent('neo-gpt-toast', { detail: message }));
  const activeProvider = providers.find(p => p.id === providerId) || null;

  useEffect(() => {
    if (isOpen) {
      setCurrentView('main');
      setProviderId(null);
      setShowAddProvider(false);
      setShowAddModel(false);
      setIsFontDropdownOpen(false);
      setTestResult(null);
    }
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
    setIsFontDropdownOpen(false);
    setShowAddProvider(false);
    setShowAddModel(false);
    setTestResult(null);
    if (currentView === 'provider') { setCurrentView('providers'); setProviderId(null); }
    else if (currentView === 'main') onClose();
    else setCurrentView('main');
  };

  const updateProvider = (id: string, patch: Partial<ProviderConfig>) => {
    setProviders(prev => prev.map(p => p.id === id ? { ...p, ...patch } : p));
  };

  const openProvider = (id: string) => {
    setProviderId(id); setCurrentView('provider'); setTestResult(null); setShowAddModel(false);
  };

  const addCustomProvider = () => {
    const name = providerDraft.name.trim();
    const baseUrl = providerDraft.baseUrl.trim();
    if (!name || !baseUrl) { notify('Provider name and API endpoint are required.'); return; }
    const id = `custom-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    setProviders(prev => [...prev, { id, name, baseUrl, apiKey: providerDraft.apiKey.trim(), enabled: true, builtIn: false, models: [] }]);
    setProviderDraft(emptyDraft); setShowAddProvider(false); setProviderId(id); setCurrentView('provider');
    notify(`${name} added`);
  };

  const deleteProvider = () => {
    if (!activeProvider || activeProvider.builtIn) return;
    if (!window.confirm(`Delete ${activeProvider.name}?`)) return;
    if (selectedModel.startsWith(`custom/${activeProvider.id}/`)) setSelectedModel('venus-3.1');
    setProviders(prev => prev.filter(p => p.id !== activeProvider.id));
    setProviderId(null); setCurrentView('providers'); notify('Provider deleted');
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
    if (selectedModel === model.id) setSelectedModel('venus-3.1');
    notify('Model removed');
  };

  const testActiveProvider = async () => {
    if (!activeProvider) return;
    setTesting(true); setTestResult(null);
    const result = await onTestProvider(activeProvider);
    setTesting(false); setTestResult(result);
    if (result.ok && result.models.length === 0) notify('Connected successfully. No models were returned by this provider.');
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div initial={{ y: '100%' }} animate={{ y: 0 }} exit={{ y: '100%' }} transition={{ type: 'spring', bounce: 0, duration: 0.4 }} className="absolute inset-0 bg-[#f8f9fa] dark:bg-[#121212] z-50 flex flex-col overflow-hidden neo-settings">
          <div className="flex items-center px-4 py-4 neo-settings-header dark:bg-[#121212]">
            <motion.button type="button" whileTap={{ scale: 0.92 }} onClick={(event) => { event.preventDefault(); event.stopPropagation(); goBack(); }} onPointerDown={(event) => event.stopPropagation()} aria-label="Back" className="relative z-20 w-11 h-11 p-2 flex items-center justify-center hover:bg-gray-200 dark:hover:bg-zinc-800 rounded-full bg-white dark:bg-zinc-800 shadow-sm transition-colors touch-manipulation"><ArrowLeft size={24} className="text-gray-700 dark:text-gray-200" /></motion.button>
            {currentView === 'main' && <div className="flex-1 flex flex-col items-center -ml-8"><div className="relative"><div className="w-20 h-20 bg-orange-400 rounded-full flex items-center justify-center text-white text-3xl font-medium shadow-md">DM</div><div className="absolute bottom-0 right-0 w-6 h-6 bg-white dark:bg-zinc-800 rounded-full flex items-center justify-center shadow-sm"><Edit2 size={12} /></div></div><h2 className="text-xl font-semibold mt-3 text-gray-900 dark:text-white">DHUN Music</h2></div>}
            {currentView === 'providers' && <h2 className="flex-1 text-center text-lg font-semibold text-gray-900 dark:text-white pr-10">API Providers</h2>}
            {currentView === 'provider' && <h2 className="flex-1 text-center text-lg font-semibold text-gray-900 dark:text-white pr-10 truncate">{activeProvider?.name || 'Provider'}</h2>}
          </div>

          <div className="flex-1 overflow-y-auto px-4 pb-8">
            <AnimatePresence mode="wait">
              {currentView === 'main' && (
                <motion.div key="main" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
                  <div><h3 className="text-gray-500 dark:text-gray-400 text-sm font-semibold mb-2 px-2 uppercase tracking-wide">My Neo Gpt</h3><div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                    {['Personalization','Memory','Plugins'].map((label, i) => <motion.button key={label} type="button" whileTap={{ scale: 0.97 }} onClick={() => notify(`${label} is not configured yet.`)} className="w-full flex items-center gap-4 p-4 border-b border-gray-100 dark:border-zinc-800"><span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">{label}</span></motion.button>)}
                  </div></div>
                  <div><h3 className="text-gray-500 dark:text-gray-400 text-sm font-semibold mb-2 px-2 uppercase tracking-wide">Account</h3><div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                    <motion.button type="button" whileTap={{ scale: 0.97 }} onClick={() => setCurrentView('account')} className="w-full flex items-center gap-4 p-4 border-b border-gray-100 dark:border-zinc-800"><User size={24}/><div className="text-left"><div className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Workspace</div><div className="text-sm text-gray-500">Personal</div></div></motion.button>
                    <motion.button type="button" whileTap={{ scale: 0.97 }} onClick={() => setCurrentView('providers')} className="w-full flex items-center gap-4 p-4 border-b border-gray-100 dark:border-zinc-800"><Key size={24}/><span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">API Providers & Models</span></motion.button>
                    <motion.button type="button" whileTap={{ scale: 0.97 }} onClick={() => notify('Upgrade plan is not configured yet.')} className="w-full flex items-center gap-4 p-4 border-b border-gray-100 dark:border-zinc-800"><Sparkles size={24} className="text-blue-500"/><span className="text-[17px] font-medium text-blue-500">Upgrade plan</span></motion.button>
                    <motion.button type="button" whileTap={{ scale: 0.97 }} onClick={() => notify('Usage and limits is not configured yet.')} className="w-full flex items-center gap-4 p-4"><Database size={24}/><span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Usage and limits</span></motion.button>
                  </div></div>
                  <div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm"><motion.button type="button" whileTap={{ scale: 0.97 }} onClick={() => setCurrentView('appearance')} className="w-full flex items-center gap-4 p-4"><SettingsIcon size={24}/><span className="text-[17px] font-medium text-gray-800 dark:text-gray-100">Appearance</span></motion.button></div>
                </motion.div>
              )}

              {currentView === 'appearance' && <motion.div key="appearance" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="space-y-6 pt-4"><div className="bg-white dark:bg-zinc-900 rounded-[28px] overflow-hidden shadow-sm">
                <div className="p-4 border-b border-gray-100 dark:border-zinc-800 flex justify-between items-center"><div className="flex items-center gap-4">{theme === 'light' ? <Sun size={24}/> : <Moon size={24}/>}<span className="text-[17px] font-medium">Dark Mode</span></div><button type="button" onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')} className={cn('w-12 h-6 rounded-full relative', theme === 'dark' ? 'bg-blue-500' : 'bg-gray-300')}><div className={cn('w-5 h-5 rounded-full bg-white absolute top-0.5 shadow-sm', theme === 'dark' ? 'translate-x-6' : 'translate-x-0.5')} /></button></div>
                <div className="p-4 flex justify-between items-center relative"><div className="flex items-center gap-4"><SettingsIcon size={24}/><span className="text-[17px] font-medium">App Font</span></div><motion.button type="button" whileTap={{ scale: 0.94 }} onClick={() => setIsFontDropdownOpen(!isFontDropdownOpen)} className="flex items-center gap-2 bg-gray-50 dark:bg-zinc-800 px-3 py-1.5 rounded-[12px] font-medium">{fontFamily === 'inter' ? 'Inter' : 'Josefin Sans'}<ChevronDown size={16}/></motion.button>{isFontDropdownOpen && <div className="fixed right-6 top-[calc(env(safe-area-inset-top,0px)+118px)] bg-white dark:bg-zinc-900 rounded-2xl shadow-2xl w-52 z-[120] overflow-hidden border border-gray-100 dark:border-zinc-700"><button type="button" onClick={() => {setFontFamily('inter');setIsFontDropdownOpen(false)}} className="w-full text-left px-4 py-3 flex justify-between text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-zinc-800">Inter {fontFamily==='inter'&&<Check size={16}/>}</button><button type="button" onClick={() => {setFontFamily('josefin');setIsFontDropdownOpen(false)}} className="w-full text-left px-4 py-3 flex justify-between text-gray-800 dark:text-gray-100 hover:bg-gray-50 dark:hover:bg-zinc-800">Josefin Sans {fontFamily==='josefin'&&<Check size={16}/>}</button></div>}</div>
              </div></motion.div>}

              {currentView === 'providers' && <motion.div key="providers" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="space-y-4 pt-4">
                <div className="flex items-center justify-between px-1"><div><p className="text-sm text-gray-500 dark:text-gray-400">Keep only the providers you want visible in the model selector.</p></div><motion.button type="button" whileTap={{scale:0.95}} onClick={() => setShowAddProvider(true)} className="flex items-center gap-1 px-3 py-2 rounded-xl bg-blue-500 text-white text-sm font-semibold"><Plus size={16}/> Custom</motion.button></div>
                {providers.map(provider => <motion.div key={provider.id} whileTap={{scale:0.995}} className="bg-white dark:bg-zinc-900 rounded-[24px] shadow-sm overflow-hidden">
                  <div className="p-4 flex items-center gap-3"><div className="flex-1 min-w-0"><button type="button" onClick={() => openProvider(provider.id)} className="text-left w-full"><div className="font-semibold text-gray-900 dark:text-white truncate">{provider.name}</div><div className="text-xs text-gray-500">{provider.models.length} model{provider.models.length === 1 ? '' : 's'} {provider.builtIn ? '• built-in' : '• custom'}</div></button></div><button type="button" aria-label={`Toggle ${provider.name}`} onClick={() => updateProvider(provider.id,{enabled:!provider.enabled})} className={cn('w-12 h-7 rounded-full relative transition-colors',provider.enabled?'bg-blue-500':'bg-gray-300 dark:bg-zinc-700')}><span className={cn('absolute top-1 w-5 h-5 rounded-full bg-white shadow transition-transform',provider.enabled?'translate-x-6':'translate-x-1')} /></button><button type="button" onClick={() => openProvider(provider.id)} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-800"><ChevronDown size={18}/></button></div>
                </motion.div>)}
                {showAddProvider && <div className="fixed inset-0 z-[80] bg-black/40 flex items-end sm:items-center justify-center p-4"><motion.div initial={{y:30,opacity:0}} animate={{y:0,opacity:1}} className="w-full max-w-md bg-white dark:bg-zinc-900 rounded-[28px] p-5 space-y-4"><div className="flex justify-between items-center"><h3 className="font-semibold text-lg">Add Custom Provider</h3><button type="button" onClick={()=>setShowAddProvider(false)}><X/></button></div><input value={providerDraft.name} onChange={e=>setProviderDraft({...providerDraft,name:e.target.value})} placeholder="Provider name" className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 px-4 py-3 outline-none"/><input value={providerDraft.baseUrl} onChange={e=>setProviderDraft({...providerDraft,baseUrl:e.target.value})} placeholder="API base URL, e.g. https://api.example.com/v1" className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 px-4 py-3 outline-none"/><input type="password" value={providerDraft.apiKey} onChange={e=>setProviderDraft({...providerDraft,apiKey:e.target.value})} placeholder="API key (optional for now)" className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 px-4 py-3 outline-none"/><button type="button" onClick={addCustomProvider} className="w-full rounded-2xl bg-blue-500 text-white py-3 font-semibold">Add Provider</button></motion.div></div>}
              </motion.div>}

              {currentView === 'provider' && activeProvider && <motion.div key="provider" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="space-y-4 pt-4">
                <div className="bg-white dark:bg-zinc-900 rounded-[26px] p-4 space-y-4 shadow-sm"><div className="flex items-center justify-between"><div><div className="font-semibold text-lg">{activeProvider.name}</div><div className="text-xs text-gray-500">{activeProvider.builtIn ? 'Built-in provider' : 'Custom OpenAI-compatible provider'}</div></div><button type="button" onClick={()=>updateProvider(activeProvider.id,{enabled:!activeProvider.enabled})} className={cn('w-12 h-7 rounded-full relative',activeProvider.enabled?'bg-blue-500':'bg-gray-300 dark:bg-zinc-700')}><span className={cn('absolute top-1 w-5 h-5 rounded-full bg-white',activeProvider.enabled?'translate-x-6':'translate-x-1')}/></button></div>
                  <div><label className="block text-sm font-semibold mb-1.5">API Key</label><input type="password" value={activeProvider.apiKey} onChange={e=>{const value=e.target.value;updateProvider(activeProvider.id,{apiKey:value});if(activeProvider.builtIn){if(activeProvider.id==='openRouter')setApiKeys({...apiKeys,openRouter:value});if(activeProvider.id==='nvidia')setApiKeys({...apiKeys,nvidia:value});if(activeProvider.id==='gemini')setApiKeys({...apiKeys,gemini:value});}}} placeholder={activeProvider.id==='gemini'?'AIza...':activeProvider.id==='nvidia'?'nvapi-...':activeProvider.id==='groq'?'gsk_...':'sk-or-...'} className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 px-4 py-3 outline-none"/></div>
                  {!activeProvider.builtIn && <div><label className="block text-sm font-semibold mb-1.5">API Base URL</label><input value={activeProvider.baseUrl} onChange={e=>updateProvider(activeProvider.id,{baseUrl:e.target.value})} placeholder="https://api.example.com/v1" className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 border border-gray-200 dark:border-zinc-700 px-4 py-3 outline-none"/></div>}
                  <button type="button" disabled={testing} onClick={testActiveProvider} className="w-full flex items-center justify-center gap-2 rounded-2xl bg-emerald-500 disabled:opacity-60 text-white py-3 font-semibold"><Wifi size={17}/>{testing?'Testing connection…':'Test Connection'}</button>
                  {testResult && <div className={cn('rounded-2xl p-3 text-sm',testResult.ok?'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-300':'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-300')}><div className="flex gap-2 items-start"><ShieldCheck size={18}/><span>{testResult.message}</span></div>{testResult.ok && testResult.models.length>0 && <div className="mt-3 space-y-2"><div className="text-xs font-semibold">Select a model to add and use</div>{testResult.models.slice(0,40).map(model => { const fullId = activeProvider.builtIn ? `${activeProvider.id === 'openRouter' ? 'openrouter' : activeProvider.id}/${model.id}` : `custom/${activeProvider.id}/${model.id}`; return <button key={fullId} type="button" onClick={() => { const item: ModelConfig = { id: fullId, name: model.name || model.id, providerId: activeProvider.id, input: model.input || 'text', deletable: true }; updateProvider(activeProvider.id,{models: activeProvider.models.some(x=>x.id===fullId)?activeProvider.models:[...activeProvider.models,item]}); onSelectProviderModel(fullId); setTestResult(null); setCurrentView('providers'); setProviderId(null); notify(`${item.name} selected`); }} className="w-full text-left rounded-xl bg-white/70 dark:bg-zinc-900/50 px-3 py-2 hover:bg-white dark:hover:bg-zinc-800"><div className="font-medium text-xs">{model.name || model.id}</div><div className="text-[10px] opacity-70 break-all">{model.id}</div></button>; })}</div>}</div>}
                  {!activeProvider.builtIn && <button type="button" onClick={deleteProvider} className="w-full flex items-center justify-center gap-2 rounded-2xl border border-red-200 text-red-600 py-3 font-semibold"><Trash2 size={17}/> Delete Provider</button>}
                </div>
                <div className="bg-white dark:bg-zinc-900 rounded-[26px] p-4 shadow-sm"><div className="flex items-center justify-between mb-3"><div><div className="font-semibold">Models</div><div className="text-xs text-gray-500">Add, remove or select models for this provider.</div></div><button type="button" onClick={()=>setShowAddModel(true)} className="flex items-center gap-1 px-3 py-2 rounded-xl bg-blue-500 text-white text-sm font-semibold"><Plus size={16}/> Add</button></div>
                  <div className="space-y-2 max-h-[42vh] overflow-y-auto">{activeProvider.models.map(model=><div key={model.id} className={cn('flex items-center gap-2 rounded-2xl border p-3',selectedModel===model.id?'border-blue-500 bg-blue-50 dark:bg-blue-900/20':'border-gray-100 dark:border-zinc-800')}><button type="button" onClick={()=>{onSelectProviderModel(model.id);setCurrentView('providers');setProviderId(null);setTestResult(null)}} className="flex-1 text-left"><div className="font-medium text-sm">{model.name}</div><div className="text-[11px] text-gray-500 break-all">{model.id}</div></button>{selectedModel===model.id&&<Check size={17} className="text-blue-500"/>}<button type="button" onClick={()=>deleteModel(model)} className="p-2 text-red-500 rounded-full hover:bg-red-50 dark:hover:bg-red-900/20"><Trash2 size={16}/></button></div>)}</div>
                </div>
                {showAddModel && <div className="fixed inset-0 z-[80] bg-black/40 flex items-end sm:items-center justify-center p-4"><motion.div initial={{y:30,opacity:0}} animate={{y:0,opacity:1}} className="w-full max-w-md bg-white dark:bg-zinc-900 rounded-[28px] p-5 space-y-4"><div className="flex justify-between items-center"><h3 className="font-semibold text-lg">Add Model</h3><button type="button" onClick={()=>setShowAddModel(false)}><X/></button></div><input value={modelDraft.name} onChange={e=>setModelDraft({...modelDraft,name:e.target.value})} placeholder="Model display name" className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 px-4 py-3 outline-none"/><input value={modelDraft.id} onChange={e=>setModelDraft({...modelDraft,id:e.target.value})} placeholder="Model ID / slug" className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 px-4 py-3 outline-none"/><select value={modelDraft.input} onChange={e=>setModelDraft({...modelDraft,input:e.target.value as 'text'|'vision'})} className="w-full rounded-2xl bg-gray-50 dark:bg-zinc-800 px-4 py-3 outline-none"><option value="text">Text</option><option value="vision">Text + Vision</option></select><button type="button" onClick={addModel} className="w-full rounded-2xl bg-blue-500 text-white py-3 font-semibold">Add Model</button></motion.div></div>}
              </motion.div>}

              {currentView === 'account' && <motion.div key="account" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="pt-4 text-center text-gray-500">Account details here...</motion.div>}
            </AnimatePresence>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
