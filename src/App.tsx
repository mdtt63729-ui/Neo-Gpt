import React, { useState, useRef, useEffect } from 'react';
import { Sparkles, RefreshCw, Plus, Mic, AudioLines, Camera, Image as ImageIcon, Paperclip, Puzzle, BrainCircuit, ArrowUp, Copy, ThumbsUp, ThumbsDown, Speaker, Share2, MoreVertical, X, Download, ChevronDown, Check, Square, EyeOff } from 'lucide-react';
import { cn } from './lib/utils';
import { callApi, testProviderConnection } from './api';
import { Message, MessageAttachment, ApiKeys, ProviderConfig } from './types';
import { motion, AnimatePresence } from 'motion/react';
import { App as CapacitorApp } from '@capacitor/app';
import { Sidebar } from './components/Sidebar';
import { haptic } from './lib/haptics';
import { Settings } from './components/Settings';
import { VoiceModal } from './components/VoiceModal';
import { DictationModal } from './components/DictationModal';
import { AuthScreen, AuthSession, AuthUser } from './components/AuthScreen';
import { firebaseAuth, firebaseReady } from './lib/firebase';
import { getNativeCurrentUser, nativeSignOut } from './lib/nativeGoogleAuth';

const SYSTEM_MODELS = [
  { id: 'venus-3.1', name: 'Venus 3.1', provider: 'system', providerId: 'system', input: 'text' as const },
];

const REMOVED_MODEL_IDS = new Set([
  'openrouter/deepseek/deepseek-chat',
  'openrouter/minimax/minimax-01',
  'openrouter/nvidia/nemotron-4-340b-instruct',
  'openrouter/zhipuai/glm-4-plus',
  'nvidia/nemotron-4-340b-instruct',
  'nvidia/deepseek-ai/deepseek-coder-33b-instruct',
  'nvidia/minimax-01',
  'nvidia/nemotron-mini-4b-instruct',
  'nvidia/google/gemma-7b-it',
  'gemini/gemini-1.0-pro',
  'gemini/gemini-1.5-pro-latest',
  'gemini/gemini-1.5-flash-latest',
  'gemini/gemini-1.5-flash-8b-latest',
  'gemini/gemini-2.5-pro',
  'gemini/gemini-2.5-flash',
  'gemini/gemini-2.5-flash-lite',
]);

const DEFAULT_PROVIDERS = [
  {
    id: 'gemini', name: 'Gemini', baseUrl: '', apiKey: '', enabled: true, builtIn: true,
    models: [
      { id: 'gemini/gemini-3.8-flash', name: 'Gemini 3.8 Flash', providerId: 'gemini', input: 'vision' as const, deletable: true },
      { id: 'gemini/gemini-3.7-flash', name: 'Gemini 3.7 Flash', providerId: 'gemini', input: 'vision' as const, deletable: true },
      { id: 'gemini/gemini-3.5-flash-lite', name: 'Gemini 3.5 Flash Lite', providerId: 'gemini', input: 'vision' as const, deletable: true },
      { id: 'gemini/gemini-3.6-flash', name: 'Gemini 3.6 Flash', providerId: 'gemini', input: 'vision' as const, deletable: true },
    ],
  },
  {
    id: 'openRouter', name: 'OpenRouter', baseUrl: 'https://openrouter.ai/api/v1', apiKey: '', enabled: true, builtIn: true,
    models: [
      { id: 'openrouter/qwen/qwen-2.5-72b-instruct', name: 'Qwen 3.8 Flash', providerId: 'openRouter', input: 'text' as const, deletable: true },
      { id: 'openrouter/openrouter/free', name: 'Venus 3.1 pro', providerId: 'openRouter', input: 'vision' as const, deletable: true },
      { id: 'openrouter/poolside/laguna-s-2.1:free', name: 'Laguna S 2.1 (free)', providerId: 'openRouter', input: 'text' as const, deletable: true },
      { id: 'openrouter/nvidia/nemotron-3-super-120b-a12b:free', name: 'Nemotron 3 Super (free)', providerId: 'openRouter', input: 'text' as const, deletable: true },
      { id: 'openrouter/thinkingmachines/inkling:free', name: 'Inkling (free)', providerId: 'openRouter', input: 'vision' as const, deletable: true },
      { id: 'openrouter/inclusionai/ling-3.0-flash-vl:free', name: 'Ling 3.0 Flash VL (free)', providerId: 'openRouter', input: 'vision' as const, deletable: true },
    ],
  },
  {
    id: 'nvidia', name: 'NVIDIA', baseUrl: 'https://integrate.api.nvidia.com/v1', apiKey: '', enabled: true, builtIn: true,
    models: [
      { id: 'nvidia/deepseek-ai/deepseek-v4-flash-0731', name: 'DeepSeek V4 Flash 0731', providerId: 'nvidia', input: 'text' as const, deletable: true },
      { id: 'nvidia/google/gemma-4-31b-it', name: 'Gemma 4 31B IT', providerId: 'nvidia', input: 'vision' as const, deletable: true },
      { id: 'nvidia/meta/llama-3.2-11b-vision-instruct', name: 'Llama 3.2 11B Vision Instruct', providerId: 'nvidia', input: 'vision' as const, deletable: true },
      { id: 'nvidia/nvidia/nemotron-3.5-lightning-30b-a3b', name: 'Nemotron 3.5 Lightning 30B A3B', providerId: 'nvidia', input: 'text' as const, deletable: true },
      { id: 'nvidia/mistralai/mistral-nemotron', name: 'Mistral-Nemotron', providerId: 'nvidia', input: 'text' as const, deletable: true },
    ],
  },
  {
    id: 'groq', name: 'Groq', baseUrl: 'https://api.groq.com/openai/v1', apiKey: '', enabled: true, builtIn: true,
    models: [
      { id: 'groq/llama-3.3-70b-versatile', name: 'Llama 3.3 70B Versatile', providerId: 'groq', input: 'text' as const, deletable: true },
      { id: 'groq/llama-3.1-8b-instant', name: 'Llama 3.1 8B Instant', providerId: 'groq', input: 'text' as const, deletable: true },
      { id: 'groq/deepseek-r1-distill-llama-70b', name: 'DeepSeek R1 Distill Llama 70B', providerId: 'groq', input: 'text' as const, deletable: true },
      { id: 'groq/mixtral-8x7b-32768', name: 'Mixtral 8x7B Instruct', providerId: 'groq', input: 'text' as const, deletable: true },
      { id: 'groq/gemma-2-9b-it', name: 'Gemma 2 9B IT', providerId: 'groq', input: 'text' as const, deletable: true },
      { id: 'groq/whisper-large-v3', name: 'Whisper Large v3', providerId: 'groq', input: 'text' as const, deletable: true },
    ],
  },
];


const RESPONSE_FORMAT_INSTRUCTION = `Format your answer for a mobile chat UI using standard Markdown. Use **bold** for important facts, key terms, conclusions, and point titles when it improves scanning. Use ##/### headings for meaningful sections, bullets or numbered lists for steps, and short paragraphs. Do not put Markdown syntax around ordinary words unnecessarily. Never output raw HTML for formatting. Keep the answer natural, readable, and well-spaced.`;

function renderInlineMarkdown(text: string, keyPrefix = 'i'): React.ReactNode[] {
  const nodes: React.ReactNode[] = [];
  const source = text.replace(/\\([*_~`])/g, '$1');
  let buffer = '';
  let key = 0;
  const pushText = () => { if (buffer) { nodes.push(buffer.replace(/\*\*/g, '').replace(/__/g, '').replace(/~~/g, '').replace(/(?<!\w)\*(?!\w)/g, '').replace(/(?<!\w)_(?!\w)/g, '')); buffer = ''; } };

  for (let i = 0; i < source.length;) {
    if (source.startsWith('**', i) || source.startsWith('__', i)) {
      const marker = source.slice(i, i + 2);
      const close = source.indexOf(marker, i + 2);
      if (close > i + 2) {
        pushText();
        nodes.push(<strong key={`${keyPrefix}-b-${key++}`}>{renderInlineMarkdown(source.slice(i + 2, close), `${keyPrefix}-b`)}</strong>);
        i = close + 2;
        continue;
      }
    }
    if (source.startsWith('~~', i)) {
      const close = source.indexOf('~~', i + 2);
      if (close > i + 2) {
        pushText();
        nodes.push(<del key={`${keyPrefix}-s-${key++}`}>{renderInlineMarkdown(source.slice(i + 2, close), `${keyPrefix}-s`)}</del>);
        i = close + 2;
        continue;
      }
    }
    if (source[i] === '`') {
      const close = source.indexOf('`', i + 1);
      if (close > i + 1) {
        pushText();
        nodes.push(<code key={`${keyPrefix}-c-${key++}`} className="neo-inline-code">{source.slice(i + 1, close)}</code>);
        i = close + 1;
        continue;
      }
    }
    if (source[i] === '[') {
      const labelEnd = source.indexOf('](', i + 1);
      if (labelEnd > i) {
        const urlEnd = source.indexOf(')', labelEnd + 2);
        const url = source.slice(labelEnd + 2, urlEnd);
        if (urlEnd > labelEnd && /^https?:\/\//i.test(url)) {
          pushText();
          nodes.push(<a key={`${keyPrefix}-a-${key++}`} href={url} target="_blank" rel="noreferrer" className="neo-markdown-link">{renderInlineMarkdown(source.slice(i + 1, labelEnd), `${keyPrefix}-a`)}</a>);
          i = urlEnd + 1;
          continue;
        }
      }
    }
    if (source[i] === '*' || source[i] === '_') {
      const marker = source[i];
      const close = source.indexOf(marker, i + 1);
      if (close > i + 1 && !/\\s/.test(source[i + 1])) {
        pushText();
        nodes.push(<em key={`${keyPrefix}-e-${key++}`}>{renderInlineMarkdown(source.slice(i + 1, close), `${keyPrefix}-e`)}</em>);
        i = close + 1;
        continue;
      }
    }
    buffer += source[i];
    i += 1;
  }
  pushText();
  return nodes;
}

function renderMarkdown(text: string): React.ReactNode {
  const normalized = text.replace(/\r\n?/g, '\n').replace(/\\([*_~`])/g, '$1');
  const lines = normalized.split('\n');
  const blocks: React.ReactNode[] = [];
  let codeLines: string[] | null = null;
  let codeLanguage = '';
  let listItems: { ordered: boolean; text: string }[] = [];

  const flushList = () => {
    if (!listItems.length) return;
    const ordered = listItems[0].ordered;
    const Tag = ordered ? 'ol' : 'ul';
    blocks.push(
      <Tag key={`list-${blocks.length}`} className="neo-markdown-list">
        {listItems.map((item, index) => {
          const colon = item.text.indexOf(':');
          const title = colon > 1 && colon < 70 ? item.text.slice(0, colon).trim() : '';
          const shouldHighlightTitle = Boolean(title && !title.includes('http'));
          return (
            <li key={`li-${index}`}>
              {shouldHighlightTitle ? <><strong>{renderInlineMarkdown(title)}</strong>{renderInlineMarkdown(item.text.slice(colon))}</> : renderInlineMarkdown(item.text)}
            </li>
          );
        })}
      </Tag>
    );
    listItems = [];
  };

  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (trimmed.startsWith('```')) {
      flushList();
      if (codeLines === null) { codeLines = []; codeLanguage = trimmed.slice(3).trim(); }
      else { blocks.push(<pre key={`code-${index}`} className="neo-code-block"><code data-language={codeLanguage || undefined}>{codeLines.join('\n')}</code></pre>); codeLines = null; codeLanguage = ''; }
      return;
    }
    if (codeLines !== null) { codeLines.push(line); return; }
    if (!trimmed) { flushList(); return; }
    const unordered = /^(?:[-*•])\s+(.+)$/.exec(trimmed);
    const ordered = /^(\d+)[.)]\s+(.+)$/.exec(trimmed);
    if (unordered || ordered) {
      const itemText = (unordered || ordered)![unordered ? 1 : 2];
      const isOrdered = Boolean(ordered);
      if (listItems.length && listItems[0].ordered !== isOrdered) flushList();
      listItems.push({ ordered: isOrdered, text: itemText });
      return;
    }
    flushList();
    const heading = /^(#{1,6})\s+(.+)$/.exec(trimmed);
    if (heading) {
      const level = Math.min(heading[1].length, 6);
      const Tag = (`h${level}`) as React.ElementType;
      blocks.push(<Tag key={`h-${index}`} className={`neo-markdown-h neo-markdown-h${level}`}>{renderInlineMarkdown(heading[2], `h-${index}`)}</Tag>);
      return;
    }
    const quote = /^>\s?(.*)$/.exec(trimmed);
    if (quote) { blocks.push(<blockquote key={`q-${index}`} className="neo-markdown-quote">{renderInlineMarkdown(quote[1], `q-${index}`)}</blockquote>); return; }
    blocks.push(<p key={`p-${index}`} className="neo-markdown-p">{renderInlineMarkdown(trimmed, `p-${index}`)}</p>);
  });
  if (codeLines !== null) blocks.push(<pre key="code-final" className="neo-code-block"><code data-language={codeLanguage || undefined}>{codeLines.join('\n')}</code></pre>);
  flushList();
  return <div className="neo-markdown">{blocks}</div>;
}

function getInitialTheme(): 'light' | 'dark' {
  try {
    const saved = localStorage.getItem('neo-gpt-settings');
    if (saved) {
      const parsed = JSON.parse(saved);
      if (parsed?.theme === 'dark' || parsed?.theme === 'light') return parsed.theme;
    }
  } catch {
    // Ignore malformed persisted settings and use the light theme.
  }
  return 'light';
}

function getWelcomeMessage(): string {
  const hour = new Date().getHours();
  if (hour >= 5 && hour < 12) {
    return ['Good morning', 'Good morning, how can I help?', 'Ready when you are'].at(Math.floor(Math.random() * 3)) || 'Good morning';
  }
  if (hour >= 12 && hour < 17) {
    return ['Good afternoon', 'How can I help you today?', 'What would you like to explore?'].at(Math.floor(Math.random() * 3)) || 'Good afternoon';
  }
  if (hour >= 17 && hour < 22) {
    return ['Good evening', 'What can I help you with?', 'Ready for your next idea?'].at(Math.floor(Math.random() * 3)) || 'Good evening';
  }
  return ['Good night', 'Need a hand before you go?', 'What can I help you with?'].at(Math.floor(Math.random() * 3)) || 'Good night';
}

export default function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isVoiceOpen, setIsVoiceOpen] = useState(false);
  const [isDictationOpen, setIsDictationOpen] = useState(false);
  const [isAttachmentOpen, setIsAttachmentOpen] = useState(false);
  const [pendingAttachments, setPendingAttachments] = useState<MessageAttachment[]>([]);
  const [isModelSelectOpen, setIsModelSelectOpen] = useState(false);
  const [isIncognito, setIsIncognito] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [chatHistory, setChatHistory] = useState<{ id: string; title: string; messages: Message[] }[]>([]);
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const lastBackPressRef = useRef(0);
  const historyHydratedRef = useRef(false);
  const [settingsHydrated, setSettingsHydrated] = useState(false);
  
  const [theme, setTheme] = useState<'light'|'dark'>(() => getInitialTheme());
  const [fontFamily, setFontFamily] = useState<'inter' | 'josefin'>('inter');
  const [apiKeys, setApiKeys] = useState<ApiKeys>({ openRouter: '', nvidia: '', gemini: '' });
  const [providers, setProviders] = useState<ProviderConfig[]>(DEFAULT_PROVIDERS);
  const [selectedModel, setSelectedModel] = useState('venus-3.1');
  
  const [expandedImage, setExpandedImage] = useState<string | null>(null);
  const [welcomeMessage, setWelcomeMessage] = useState<string>(() => getWelcomeMessage());
  const [launchReady, setLaunchReady] = useState(false);
  const [moreMenuMessageId, setMoreMenuMessageId] = useState<string | null>(null);
  const [longPressMessageId, setLongPressMessageId] = useState<string | null>(null);
  const longPressTimerRef = useRef<number | null>(null);
  const longPressTriggeredRef = useRef(false);
  const [feedbackByMessage, setFeedbackByMessage] = useState<Record<string, 'up' | 'down' | null>>({});
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [isHeaderMoreOpen, setIsHeaderMoreOpen] = useState(false);
  const [textareaHeight, setTextareaHeight] = useState(52);
  const [authState, setAuthState] = useState<'loading' | 'authenticated' | 'guest' | 'unauthenticated'>(() => localStorage.getItem('neo-gpt-guest-mode') === '1' ? 'guest' : 'loading');
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [authPromptFromGuest, setAuthPromptFromGuest] = useState(false);
  const [isNearChatBottom, setIsNearChatBottom] = useState(true);
  const lastMessageCountRef = useRef(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const photoInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const authSessionRef = useRef<AuthSession | null>(null);
  const handleGuest = () => {
    localStorage.setItem('neo-gpt-guest-mode', '1');
    authSessionRef.current = null;
    setAuthUser(null);
    setAuthPromptFromGuest(false);
    setAuthState('guest');
  };

  const handleAuthenticated = (user: AuthUser, session: AuthSession) => {
    authSessionRef.current = session;
    localStorage.removeItem('neo-gpt-guest-mode');
    setAuthUser(user);
    setAuthPromptFromGuest(false);
    setAuthState('authenticated');
  };

  const chatContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let unsubscribe: (() => void) | undefined;
    let cancelled = false;
    const restore = async () => {
      const guestMode = localStorage.getItem('neo-gpt-guest-mode') === '1';
      if (!firebaseReady()) {
        if (!guestMode && !cancelled) setAuthState('unauthenticated');
        else if (!cancelled) setAuthState('guest');
        return;
      }
      try {
        // Native Google authentication is restored first on Android/iOS.
        const nativeUser = await getNativeCurrentUser();
        if (nativeUser) {
          if (cancelled) return;
          try {
            const plugin = await import('@capacitor-firebase/authentication');
            const tokenResult = await plugin.FirebaseAuthentication.getIdToken({ forceRefresh: false });
            const token = tokenResult?.token || '';
            const session: AuthSession = { access_token: token, refresh_token: '', expires_at: Date.now() / 1000 + 3600 };
            localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
            authSessionRef.current = session;
            setAuthUser({ id: String(nativeUser.uid || ''), email: String(nativeUser.email || ''), name: nativeUser.displayName || nativeUser.email?.split('@')[0], avatarUrl: nativeUser.photoUrl || nativeUser.photoURL || undefined });
            localStorage.removeItem('neo-gpt-guest-mode');
            setAuthState('authenticated');
            return;
          } catch {
            // Fall through to the web Firebase session if native token retrieval fails.
          }
        }

        const auth = firebaseAuth();
        unsubscribe = auth.onAuthStateChanged(async (user: any) => {
          if (cancelled) return;
          if (!user) {
            const skipped = localStorage.getItem('neo-gpt-guest-mode') === '1';
            setAuthState(skipped ? 'guest' : 'unauthenticated');
            return;
          }
          try {
            const token = await user.getIdToken();
            const session: AuthSession = { access_token: token, refresh_token: '', expires_at: Date.now() / 1000 + 3600 };
            localStorage.setItem('neo-gpt-auth-session', JSON.stringify(session));
            authSessionRef.current = session;
            setAuthUser({ id: String(user.uid), email: String(user.email || ''), name: user.displayName || user.email?.split('@')[0], avatarUrl: user.photoURL || undefined });
            localStorage.removeItem('neo-gpt-guest-mode');
            setAuthState('authenticated');
          } catch {
            setAuthState('unauthenticated');
          }
        });
      } catch {
        if (!cancelled) setAuthState(guestMode ? 'guest' : 'unauthenticated');
      }
    };
    restore();
    return () => { cancelled = true; unsubscribe?.(); };
  }, []);

  useEffect(() => {
    const saved = localStorage.getItem('neo-gpt-settings');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
      if (parsed.theme) setTheme(parsed.theme);
      if (parsed.fontFamily) setFontFamily(parsed.fontFamily);
      if (parsed.apiKeys) setApiKeys(parsed.apiKeys);
      if (parsed.providers && Array.isArray(parsed.providers)) {
        const merged = DEFAULT_PROVIDERS.map(defaultProvider => {
          const savedProvider = parsed.providers.find((p: ProviderConfig) => p.id === defaultProvider.id);
          if (!savedProvider) return defaultProvider;
          const savedModels = Array.isArray(savedProvider.models) ? savedProvider.models : [];
          const mergedModels = [...defaultProvider.models, ...savedModels.filter((savedModel: any) =>
            !defaultProvider.models.some(defaultModel => defaultModel.id === String(savedModel.id)) &&
            !REMOVED_MODEL_IDS.has(String(savedModel.id))
          )];
          return { ...defaultProvider, ...savedProvider, models: mergedModels };
        });
        const custom = parsed.providers.filter((p: ProviderConfig) => !DEFAULT_PROVIDERS.some(d => d.id === p.id));
        setProviders([...merged, ...custom.map((p: ProviderConfig) => ({ ...p, models: (Array.isArray(p.models) ? p.models : []).filter(m => !REMOVED_MODEL_IDS.has(m.id)) }))]);
        const loadedProviders = [...merged, ...custom];
        setApiKeys(prev => ({ ...prev, openRouter: loadedProviders.find(p => p.id === 'openRouter')?.apiKey || prev.openRouter, nvidia: loadedProviders.find(p => p.id === 'nvidia')?.apiKey || prev.nvidia, gemini: loadedProviders.find(p => p.id === 'gemini')?.apiKey || prev.gemini }));
      } else if (parsed.apiKeys) {
        setProviders(prev => prev.map(p => ({ ...p, apiKey: parsed.apiKeys[p.id] || p.apiKey })));
      }
      if (parsed.selectedModel) setSelectedModel(parsed.selectedModel);
      } catch (error) {
        console.warn('Ignoring invalid Neo Gpt settings:', error);
      }
    }
    setSettingsHydrated(true);
  }, []);

  useEffect(() => {
    if (!settingsHydrated) return;
    localStorage.setItem('neo-gpt-settings', JSON.stringify({ theme, fontFamily, apiKeys, providers, selectedModel }));
  }, [theme, fontFamily, apiKeys, providers, selectedModel, settingsHydrated]);

  useEffect(() => {
    let raf1 = 0;
    let raf2 = 0;
    let timer = 0;
    raf1 = window.requestAnimationFrame(() => {
      raf2 = window.requestAnimationFrame(() => {
        timer = window.setTimeout(() => setLaunchReady(true), 110);
      });
    });
    return () => {
      window.cancelAnimationFrame(raf1);
      window.cancelAnimationFrame(raf2);
      window.clearTimeout(timer);
    };
  }, []);

  useEffect(() => {
    if (!inputRef.current) return;
    const textarea = inputRef.current;
    textarea.style.height = 'auto';
    const lineHeight = 25;
    const maxHeight = lineHeight * 8 + 28;
    const nextHeight = Math.min(Math.max(textarea.scrollHeight, 52), maxHeight);
    textarea.style.height = `${nextHeight}px`;
    setTextareaHeight(nextHeight);
  }, [input]);

  useEffect(() => {
    const handlePointer = () => { setMoreMenuMessageId(null); setLongPressMessageId(null); clearLongPressTimer(); };
    window.addEventListener('neo-gpt-close-more', handlePointer);
    window.addEventListener('pointerdown', handlePointer);
    return () => {
      window.removeEventListener('neo-gpt-close-more', handlePointer);
      window.removeEventListener('pointerdown', handlePointer);
    };
  }, []);

  useEffect(() => {
    try {
      const savedHistory = localStorage.getItem('neo-gpt-chat-history');
      if (savedHistory) {
        const parsed = JSON.parse(savedHistory);
        if (Array.isArray(parsed)) {
          setChatHistory(parsed.map((item: any) => ({
            id: String(item.id),
            title: String(item.title || 'New chat'),
            messages: Array.isArray(item.messages) ? item.messages : [],
          })));
        }
      }
    } catch {
      setChatHistory([]);
    } finally {
      historyHydratedRef.current = true;
    }
  }, []);

  useEffect(() => {
    if (!historyHydratedRef.current) return;
    localStorage.setItem('neo-gpt-chat-history', JSON.stringify(chatHistory.slice(0, 30)));
  }, [chatHistory]);

  useEffect(() => {
    if (!activeChatId || messages.length === 0) return;
    setChatHistory(prev => prev.map(chat => chat.id === activeChatId ? { ...chat, messages } : chat));
  }, [messages, activeChatId]);

  useEffect(() => {
    let removeListener: (() => void) | undefined;
    CapacitorApp.addListener('backButton', () => {
      if (isSidebarOpen) { setIsSidebarOpen(false); return; }
      if (isSettingsOpen) { setIsSettingsOpen(false); return; }
      if (isAttachmentOpen) { setIsAttachmentOpen(false); return; }
      if (isModelSelectOpen) { setIsModelSelectOpen(false); return; }
      if (isVoiceOpen) { setIsVoiceOpen(false); return; }
      if (isDictationOpen) { setIsDictationOpen(false); return; }
      if (expandedImage) { setExpandedImage(null); return; }

      const now = Date.now();
      if (now - lastBackPressRef.current < 2200) {
        CapacitorApp.exitApp();
      } else {
        lastBackPressRef.current = now;
        showToast('Press back again to exit Neo Gpt');
      }
    }).then(handle => { removeListener = () => handle.remove(); });
    return () => removeListener?.();
  }, [isSidebarOpen, isSettingsOpen, isAttachmentOpen, isModelSelectOpen, isVoiceOpen, isDictationOpen, expandedImage]);

  const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    const el = chatContainerRef.current;
    if (!el) return;
    el.scrollTo({ top: Math.max(0, el.scrollHeight - el.clientHeight - 8), behavior });
  };

  useEffect(() => {
    const el = chatContainerRef.current;
    if (!el) return;
    const onScroll = () => {
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
      setIsNearChatBottom(distance < 160);
    };
    onScroll();
    el.addEventListener('scroll', onScroll, { passive: true });
    return () => el.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const previousCount = lastMessageCountRef.current;
    lastMessageCountRef.current = messages.length;
    if (messages.length === 0) return;
    // Only follow newly-added content when the user is already near the bottom.
    // Never scroll the window/document or move the fixed header/input shell.
    if (messages.length > previousCount && (isNearChatBottom || previousCount === 0)) {
      requestAnimationFrame(() => scrollToBottom('smooth'));
    }
  }, [messages.length, isNearChatBottom]);

  useEffect(() => {
    const el = chatContainerRef.current;
    if (!el || !isLoading || !isNearChatBottom) return;
    const observer = new ResizeObserver(() => scrollToBottom('auto'));
    observer.observe(el.firstElementChild || el);
    return () => observer.disconnect();
  }, [isLoading, isNearChatBottom]);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 2500);
  };

  useEffect(() => {
    const handleExternalToast = (event: Event) => {
      const message = (event as CustomEvent<string>).detail;
      if (message) showToast(message);
    };
    window.addEventListener('neo-gpt-toast', handleExternalToast);
    return () => window.removeEventListener('neo-gpt-toast', handleExternalToast);
  }, []);

  const speakWithGemini = async (text: string) => {
    const key = apiKeys.gemini?.trim();
    if (!key) { showToast('Add a Gemini API key in Settings to use Gemini voice.'); return; }
    try {
      showToast('Generating Gemini voice…');
      const res = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-goog-api-key': key,
          'Api-Revision': '2026-05-20',
        },
        body: JSON.stringify({
          model: 'gemini-3.1-flash-tts-preview',
          input: `Read naturally and clearly. Keep the original wording.\n\n${text}`,
          response_format: { type: 'audio' },
          generation_config: { speech_config: [{ voice: 'Kore' }] },
        }),
      });
      const data = await res.json().catch(() => ({}));
      const encoded = data?.output_audio?.data || data?.outputAudio?.data;
      if (!res.ok || !encoded) throw new Error(data?.error?.message || 'Gemini did not return audio.');
      const binary = atob(encoded);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
      const AudioContextCtor = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContextCtor) throw new Error('Audio playback is not supported on this device.');
      const audioContext = new AudioContextCtor();
      if (audioContext.state === 'suspended') await audioContext.resume();
      // Gemini TTS returns raw 24 kHz, 16-bit PCM for the audio output.
      const pcm = new Int16Array(bytes.buffer);
      const buffer = audioContext.createBuffer(1, pcm.length, 24000);
      const channel = buffer.getChannelData(0);
      for (let i = 0; i < pcm.length; i++) channel[i] = pcm[i] / 32768;
      const source = audioContext.createBufferSource();
      source.buffer = buffer;
      source.connect(audioContext.destination);
      source.onended = () => { void audioContext.close(); };
      source.start();
    } catch (error) {
      showToast(error instanceof Error ? error.message : 'Gemini voice playback failed.');
    }
  };

  const clearLongPressTimer = () => {
    if (longPressTimerRef.current !== null) {
      window.clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
  };

  const startMessageLongPress = (messageId: string, sender: Message['sender']) => {
    if (sender !== 'user') return;
    clearLongPressTimer();
    longPressTriggeredRef.current = false;
    longPressTimerRef.current = window.setTimeout(() => {
      longPressTriggeredRef.current = true;
      haptic('selection');
      setLongPressMessageId(messageId);
      setMoreMenuMessageId(null);
    }, 650);
  };

  const finishMessageLongPress = () => {
    clearLongPressTimer();
  };

  const copyUserMessage = async (message: Message) => {
    try {
      if (message.text) await navigator.clipboard.writeText(message.text);
      else showToast('There is no text to copy.');
      if (message.text) showToast('Message copied');
    } catch {
      showToast('Could not copy message');
    }
    setLongPressMessageId(null);
  };

  const editUserMessage = (messageId: string) => {
    const index = messages.findIndex(message => message.id === messageId);
    if (index < 0) return;
    const message = messages[index];
    setInput(message.text || '');
    setPendingAttachments(message.attachments || []);
    // Editing starts a new response from this point; preserve everything before the edited message.
    setMessages(prev => prev.slice(0, index));
    setActiveChatId(prev => prev);
    setLongPressMessageId(null);
    setTimeout(() => inputRef.current?.focus(), 120);
    haptic('tap');
    showToast('Edit message');
  };

  const handleActionClick = (action: string, text?: string, messageId?: string) => {
    if (action === 'copy' && text) {
      navigator.clipboard?.writeText(text).then(() => showToast('Copied to clipboard')).catch(() => showToast('Could not copy text'));
    } else if (action === 'thumbsUp' && messageId) {
      setFeedbackByMessage(prev => ({ ...prev, [messageId]: prev[messageId] === 'up' ? null : 'up' }));
    } else if (action === 'thumbsDown' && messageId) {
      setFeedbackByMessage(prev => ({ ...prev, [messageId]: prev[messageId] === 'down' ? null : 'down' }));
    } else if (action === 'speaker' && text) {
      void speakWithGemini(text);
    } else if (action === 'share' && text) {
      if (navigator.share) {
        navigator.share({ title: 'Neo Gpt response', text }).catch(() => {});
      } else {
        navigator.clipboard?.writeText(text).then(() => showToast('Response copied for sharing')).catch(() => showToast('Sharing is not supported here'));
      }
    } else if (action === 'more' && messageId) {
      setMoreMenuMessageId(prev => prev === messageId ? null : messageId);
    } else if (action === 'plugin') {
      if (authState !== 'authenticated') { requireLogin(); return; }
      setIsAttachmentOpen(false); showToast('Plugins are ready for a future provider connection.');
    } else if (action === 'think') {
      setIsAttachmentOpen(false); showToast('Thinking mode is enabled for the next request.');
    }
  };

  const regenerateLastResponse = async (messageId: string) => {
    if (isLoading || isRegenerating) return;
    const index = messages.findIndex(message => message.id === messageId);
    if (index < 0) return;
    const userMessage = [...messages].slice(0, index).reverse().find(message => message.sender === 'user');
    if (!userMessage) { showToast('Nothing to regenerate yet.'); return; }
    setIsRegenerating(true);
    setMoreMenuMessageId(null);
    setMessages(prev => prev.filter(message => message.id !== messageId));
    try {
      const response = await callApi(
        userMessage.text || '',
        false,
        selectedModel,
        apiKeys,
        providers.map(p => ({ id: p.id, name: p.name, baseUrl: p.baseUrl, apiKey: p.apiKey, enabled: p.enabled })),
        userMessage.attachments || [],
      );
      setMessages(prev => [...prev, { id: `${Date.now()}-regen`, sender: 'ai', text: response.text || 'No response returned.' }]);
    } finally {
      setIsRegenerating(false);
    }
  };


  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if ((!input.trim() && pendingAttachments.length === 0) || isLoading || isRegenerating) return;

    haptic('tap');
    const userText = input.trim();
    const attachmentsToSend = [...pendingAttachments];
    setInput('');
    setPendingAttachments([]);
    setIsAttachmentOpen(false);
    
    const newMessage: Message = {
      id: Date.now().toString(),
      sender: 'user',
      text: userText || undefined,
      attachments: attachmentsToSend.length ? attachmentsToSend : undefined,
    };

    const isStartingNewChat = messages.length === 0;
    setMessages(prev => [...prev, newMessage]);
    if (isStartingNewChat) {
      setActiveChatId(newMessage.id);
      setChatHistory(prev => [{ id: newMessage.id, title: userText.slice(0, 42) || 'New chat', messages: [newMessage] }, ...prev].slice(0, 30));
    }
    setIsLoading(true);

    const isImageCommand = userText.toLowerCase().startsWith('/image');
    
    try {
      const prompt = isImageCommand ? userText.substring(6).trim() : userText;
      const response = await callApi(prompt, isImageCommand, selectedModel, apiKeys, providers.map(p => ({ id: p.id, name: p.name, baseUrl: p.baseUrl, apiKey: p.apiKey, enabled: p.enabled })), attachmentsToSend);
      
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
            const imgResponse = await callApi(imgDesc, true, selectedModel, apiKeys, providers.map(p => ({ id: p.id, name: p.name, baseUrl: p.baseUrl, apiKey: p.apiKey, enabled: p.enabled })));
            
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
      // Do not refocus the textarea here. On Android this reopens the soft keyboard
      // after every response, even when the user did not ask for it.
      inputRef.current?.blur();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    e.target.value = '';
    if (!files.length) return;

    let remaining = files.length;
    const next: MessageAttachment[] = [];
    files.forEach(file => {
      const base: MessageAttachment = { name: file.name, type: file.type || 'application/octet-stream', size: file.size };
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = () => {
          next.push({ ...base, dataUrl: typeof reader.result === 'string' ? reader.result : undefined });
          remaining -= 1;
          if (remaining === 0) {
            setPendingAttachments(prev => [...prev, ...next]);
            setIsAttachmentOpen(false);
            showToast(`${next.length} image${next.length > 1 ? 's' : ''} attached`);
          }
        };
        reader.onerror = () => {
          remaining -= 1;
          if (remaining === 0) setIsAttachmentOpen(false);
        };
        reader.readAsDataURL(file);
      } else {
        next.push(base);
        remaining -= 1;
        if (remaining === 0) {
          setPendingAttachments(prev => [...prev, ...next]);
          setIsAttachmentOpen(false);
          showToast(`${next.length} file${next.length > 1 ? 's' : ''} attached`);
        }
      }
    });
  };

  const requireLogin = () => {
    haptic('warning');
    setIsAttachmentOpen(false);
    setIsHeaderMoreOpen(false);
    showToast('Please log in to use protected features.');
    setAuthState('unauthenticated');
  };
  const triggerFileInput = () => { if (authState !== 'authenticated') return requireLogin(); setIsAttachmentOpen(false); fileInputRef.current?.click(); };
  const triggerPhotoInput = () => { if (authState !== 'authenticated') return requireLogin(); setIsAttachmentOpen(false); photoInputRef.current?.click(); };
  const triggerCameraInput = () => { if (authState !== 'authenticated') return requireLogin(); setIsAttachmentOpen(false); cameraInputRef.current?.click(); };

  const openLogin = () => { haptic('tap'); setIsHeaderMoreOpen(false); setAuthPromptFromGuest(true); setAuthState('unauthenticated'); };
  const cancelLogin = () => { setAuthPromptFromGuest(false); setAuthState('guest'); };


  const availableModels = [
    ...SYSTEM_MODELS,
    ...providers.filter(p => p.enabled).flatMap(p => p.models),
  ].filter((model, index, all) => all.findIndex(m => m.id === model.id) === index);

  const currentModelName = availableModels.find(m => m.id === selectedModel)?.name || 'Venus 3.1';

  const selectModel = (modelId: string) => {
    if (!availableModels.some(model => model.id === modelId)) return;
    setSelectedModel(modelId);
    setIsModelSelectOpen(false);
  };

  useEffect(() => {
    if (!availableModels.some(model => model.id === selectedModel)) {
      setSelectedModel('venus-3.1');
    }
  }, [providers, selectedModel]);

  const handleTestProvider = async (provider: ProviderConfig) => {
    const result = await testProviderConnection(provider.id, provider.apiKey, provider.baseUrl);
    if (result.ok) showToast(result.message);
    else showToast(result.message);
    return result;
  };

  if (authState === 'loading') {
    return <div className={`neo-auth-loading ${theme}`}><div className="neo-auth-loading-mark">N</div></div>;
  }
  if (authState === 'unauthenticated') {
    return <AuthScreen onAuthenticated={handleAuthenticated} onSkip={handleGuest} onCancel={authPromptFromGuest ? cancelLogin : undefined} />;
  }

  const logout = async () => {
    try { if (firebaseReady()) await firebaseAuth().signOut(); } catch { /* local logout still completes */ }
    await nativeSignOut();
    localStorage.removeItem('neo-gpt-auth-session');
    localStorage.removeItem('neo-gpt-guest-mode');
    authSessionRef.current = null; setAuthUser(null); setAuthPromptFromGuest(false); setIsSettingsOpen(false); setIsSidebarOpen(false); setAuthState('unauthenticated');
  };

  return (
    <div className={theme}>
      <motion.div
        initial={false}
        animate={{ opacity: launchReady ? 1 : 0 }}
        transition={{ duration: 0.58, ease: [0.22, 1, 0.36, 1] }}
        className={cn(
        "grid grid-rows-[auto_minmax(0,1fr)_auto] h-[100dvh] w-full bg-white dark:bg-[#121212] overflow-hidden relative shadow-2xl neo-launch-orchestrator",
        launchReady ? 'is-revealed' : 'is-preparing',
        fontFamily === 'inter' ? 'font-inter' : 'font-josefin',
        "max-w-[480px] mx-auto border-x border-gray-100 dark:border-zinc-800 neo-launch-shell"
      )}>
        {/* Global top chrome stays mounted on chat, settings and other app pages. */}
        <motion.header
          initial={false}
          className={cn("neo-topbar relative row-start-1 flex items-center justify-between px-4 pb-3 z-[130] neo-launch-header", isSidebarOpen && "neo-topbar-menu-open")}>
          <div className="flex items-center gap-3">
            <motion.button
              type="button"
              whileTap={{ scale: 0.92 }}
              animate={{ scale: isSidebarOpen ? 1 : 1 }}
              transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
              onClick={() => { haptic('tap'); setIsSidebarOpen(prev => !prev); }}
              aria-label={isSidebarOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={isSidebarOpen}
              className="neo-hamburger-button p-3 bg-gray-50 dark:bg-zinc-800 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-700 transition-colors"
            >
              <span className="neo-hamburger" aria-hidden="true">
                <motion.span
                  className="neo-hamburger-line"
                  animate={isSidebarOpen ? { rotate: 45, y: 6 } : { rotate: 0, y: 0 }}
                  transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                />
                <motion.span
                  className="neo-hamburger-line"
                  animate={isSidebarOpen ? { opacity: 0, scaleX: 0.25 } : { opacity: 1, scaleX: 1 }}
                  transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
                />
                <motion.span
                  className="neo-hamburger-line"
                  animate={isSidebarOpen ? { rotate: -45, y: -6 } : { rotate: 0, y: 0 }}
                  transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                />
              </span>
            </motion.button>
            
            {/* Model Selector Capsule */}
            <div className="relative">
              <motion.button 
                whileTap={{ scale: 0.94 }}
                onClick={() => { haptic('selection'); setIsModelSelectOpen(!isModelSelectOpen); }}
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
                            haptic('selection');
                            selectModel(model.id);
                          }}
                          className="w-full flex items-center justify-between px-4 py-3 hover:bg-gray-50 dark:hover:bg-zinc-800 text-left"
                        >
                          <div>
                            <div className="text-gray-900 dark:text-gray-100 font-medium text-sm">{model.name}</div>
                            <div className="text-xs text-gray-500 dark:text-gray-400 capitalize">{model.providerId === 'system' ? 'Neo Gpt' : (providers.find(p => p.id === model.providerId)?.name || model.providerId)}</div>
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

          <div className="relative flex items-center gap-2 min-w-0">
            {authState === 'guest' ? (
              <motion.button type="button" whileTap={{ scale: 0.96 }} onClick={openLogin} className="neo-login-button" aria-label="Log in">Log in</motion.button>
            ) : (
              <motion.button type="button" layout whileTap={{ scale: 0.9 }} onClick={() => { haptic('tap'); setMessages([]); setActiveChatId(null); setInput(''); setPendingAttachments([]); setIsDictationOpen(false); setIsAttachmentOpen(false); setIsModelSelectOpen(false); setWelcomeMessage(getWelcomeMessage()); setIsHeaderMoreOpen(false); showToast('New chat started'); }} className="neo-conversation-action" aria-label="New chat" title="New chat"><Plus size={20}/></motion.button>
            )}
            <div className="relative">
              <motion.button type="button" layout whileTap={{ scale: 0.9 }} onClick={() => { haptic('tap'); setIsHeaderMoreOpen(v => !v); }} className="neo-conversation-action" aria-label="More options" aria-expanded={isHeaderMoreOpen} title="More options"><MoreVertical size={20}/></motion.button>
              <AnimatePresence>
                {isHeaderMoreOpen && (
                  <>
                    <motion.button type="button" aria-label="Close more options" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setIsHeaderMoreOpen(false)} className="fixed inset-0 z-[140] cursor-default" />
                    <motion.div initial={{ opacity: 0, y: -6, scale: .97 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: -6, scale: .97 }} transition={{ duration: .18 }} className="absolute right-0 top-12 z-[150] w-48 rounded-2xl border border-gray-100 bg-white p-1.5 shadow-xl dark:border-zinc-700 dark:bg-zinc-900">
                      <button type="button" onClick={() => { setIsIncognito(v => !v); setIsHeaderMoreOpen(false); showToast(isIncognito ? 'Incognito off' : 'Incognito on'); }} className="w-full rounded-xl px-3 py-2.5 text-left text-sm font-medium hover:bg-gray-100 dark:hover:bg-zinc-800">{isIncognito ? 'Turn off incognito' : 'Turn on incognito'}</button>
                      {authState === 'authenticated' && <button type="button" onClick={() => { haptic('tap'); setIsSettingsOpen(true); setIsHeaderMoreOpen(false); }} className="w-full rounded-xl px-3 py-2.5 text-left text-sm font-medium hover:bg-gray-100 dark:hover:bg-zinc-800">Settings</button>}
                      {messages.length > 0 && <button type="button" onClick={() => { setMessages([]); setActiveChatId(null); setInput(''); setPendingAttachments([]); setIsHeaderMoreOpen(false); setWelcomeMessage(getWelcomeMessage()); showToast('Chat reset'); }} className="w-full rounded-xl px-3 py-2.5 text-left text-sm font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30">Clear current chat</button>}
                    </motion.div>
                  </>
                )}
              </AnimatePresence>
            </div>
          </div>
        </motion.header>

        {/* Main Chat Area */}
        <motion.main
          ref={chatContainerRef}
          initial={false}
          className="relative row-start-2 min-h-0 z-10 overflow-y-auto custom-scrollbar px-4 pb-8 dark:bg-[#121212] neo-content-fade neo-chat-scroll neo-launch-content">
          {messages.length === 0 && !isLoading && (
            <div className="h-full flex flex-col items-center justify-center text-center px-4 neo-welcome-stage">
              <motion.h1
                initial={false}
                className="text-2xl font-bold text-gray-800 dark:text-gray-200 tracking-tight neo-welcome-title"
              >{welcomeMessage}</motion.h1>
            </div>
          )}
          
          <div className="flex flex-col gap-6">
            <AnimatePresence initial={false}>
              {messages.map((msg) => (
                <motion.div 
                  key={msg.id}
                  initial={{ opacity: 0, y: 7 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: msg.sender === 'user' ? 0.26 : 0.32, ease: [0.22, 1, 0.36, 1] }}
                  data-message-entry="true"
                  className={cn(
                    "flex w-full",
                    msg.sender === 'user' ? 'justify-end' : 'justify-start'
                  )}
                >
                  {msg.sender === 'user' ? (
                    <div
                      className="relative bg-blue-100 dark:bg-blue-900/50 text-gray-900 dark:text-white px-3.5 py-3.5 rounded-[24px] rounded-tr-[8px] max-w-[88%] break-words shadow-sm text-[15px] leading-relaxed select-text"
                      onPointerDown={() => startMessageLongPress(msg.id, msg.sender)}
                      onPointerUp={finishMessageLongPress}
                      onPointerCancel={finishMessageLongPress}
                      onPointerLeave={finishMessageLongPress}
                      onContextMenu={(event) => {
                        event.preventDefault();
                        clearLongPressTimer();
                        longPressTriggeredRef.current = true;
                        haptic('selection');
                        setLongPressMessageId(msg.id);
                      }}
                    >
                      {msg.attachments?.length ? (
                        <div className="flex flex-wrap gap-2 mb-2">
                          {msg.attachments.map((file, index) => file.dataUrl ? (
                            <img key={`${file.name}-${index}`} src={file.dataUrl} alt={file.name} className="w-36 h-36 rounded-2xl object-cover border border-white/60 dark:border-white/10" />
                          ) : (
                            <div key={`${file.name}-${index}`} className="flex items-center gap-2 bg-white/60 dark:bg-black/20 rounded-xl px-3 py-2 text-xs"><Paperclip size={15}/><span className="max-w-[180px] truncate">{file.name}</span></div>
                          ))}
                        </div>
                      ) : null}
                      {msg.text ? <div>{msg.text}</div> : null}
                      <AnimatePresence>
                        {longPressMessageId === msg.id && (
                          <motion.div
                            initial={{ opacity: 0, y: 8, scale: .96 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, y: 8, scale: .96 }}
                            transition={{ duration: .16, ease: [0.22, 1, 0.36, 1] }}
                            className="neo-message-longpress-menu"
                            onPointerDown={(event) => event.stopPropagation()}
                          >
                            <button type="button" onClick={() => { haptic('tap'); editUserMessage(msg.id); }}>
                              <span>Edit</span>
                            </button>
                            <button type="button" onClick={() => { haptic('tap'); void copyUserMessage(msg); }}>
                              <Copy size={16} /><span>Copy</span>
                            </button>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </div>
                  ) : (
                    <div className="flex flex-col gap-3 w-full max-w-full">
                      {msg.imageUrl ? (
                        <div 
                          className="rounded-[24px] overflow-hidden border border-gray-100 dark:border-zinc-800 shadow-sm cursor-pointer hover:opacity-95 transition-opacity"
                          onClick={() => setExpandedImage(msg.imageUrl || null)}
                        >
                          <img src={msg.imageUrl} alt="Generated" className="w-full h-auto object-cover" loading="lazy" />
                        </div>
                      ) : (
                        <div className="neo-assistant-response">
                          {renderMarkdown(msg.text || '')}
                        </div>
                      )}
                      
                      {/* Action Bar for AI message */}
                      <div className="relative flex items-center gap-1.5 text-gray-500 dark:text-gray-400 mt-1">
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('copy', msg.text)} className="neo-message-action" aria-label="Copy"><Copy size={17} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} animate={{ scale: feedbackByMessage[msg.id] === 'up' ? [1, 1.18, 1] : 1 }} onClick={() => handleActionClick('thumbsUp', undefined, msg.id)} className={cn('neo-message-action', feedbackByMessage[msg.id] === 'up' && 'is-selected-up')} aria-label="Like"><ThumbsUp size={17} fill={feedbackByMessage[msg.id] === 'up' ? 'currentColor' : 'none'} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} animate={{ scale: feedbackByMessage[msg.id] === 'down' ? [1, 1.18, 1] : 1 }} onClick={() => handleActionClick('thumbsDown', undefined, msg.id)} className={cn('neo-message-action', feedbackByMessage[msg.id] === 'down' && 'is-selected-down')} aria-label="Dislike"><ThumbsDown size={17} fill={feedbackByMessage[msg.id] === 'down' ? 'currentColor' : 'none'} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('speaker', msg.text)} className="neo-message-action" aria-label="Read aloud"><Speaker size={17} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('share', msg.text)} className="neo-message-action" aria-label="Share"><Share2 size={17} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('more', undefined, msg.id)} className="neo-message-action" aria-label="More"><MoreVertical size={17} /></motion.button>
                        <AnimatePresence>
                          {moreMenuMessageId === msg.id && (
                            <motion.div initial={{ opacity: 0, y: 6, scale: 0.96 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 6, scale: 0.96 }} className="neo-message-more-menu">
                              <button type="button" onClick={() => regenerateLastResponse(msg.id)}><RefreshCw size={16}/> Regenerate</button>
                              <button type="button" onClick={() => { setMoreMenuMessageId(null); handleActionClick('share', msg.text); }}><Share2 size={16}/> Share</button>
                            </motion.div>
                          )}
                        </AnimatePresence>
                      </div>
                    </div>
                  )}
                </motion.div>
              ))}
            </AnimatePresence>
            
            {(isLoading || isRegenerating) && (
              <motion.div
                initial={{ opacity: 0, y: 5 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                className="flex justify-start w-full"
                aria-live="polite"
                aria-label="Thinking"
              >
                <div className="neo-thinking-clean">
                  <span className="neo-thinking-label">Thinking</span>
                  <span className="neo-thinking-dots" aria-hidden="true">
                    <i />
                    <i />
                    <i />
                  </span>
                </div>
              </motion.div>
            )}
            <div ref={messagesEndRef} className="h-4" />
          </div>
        </motion.main>

        {/* Bottom Input Area */}
        <motion.div
          initial={false}
          className="relative row-start-3 z-[90] px-4 pt-4 neo-bottom-shell neo-launch-bottom">
          <input type="file" ref={cameraInputRef} onChange={handleFileUpload} accept="image/*" capture="environment" className="hidden" />
          <input type="file" ref={photoInputRef} onChange={handleFileUpload} accept="image/*" multiple className="hidden" />
          <input type="file" ref={fileInputRef} onChange={handleFileUpload} accept="image/*,.pdf,.txt,.md,.json,.csv,.doc,.docx" multiple className="hidden" />

          <AnimatePresence>
            {pendingAttachments.length > 0 && (
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 8 }}
                className="mb-2 flex gap-2 overflow-x-auto px-1 pb-1"
              >
                {pendingAttachments.map((file, index) => (
                  <div key={`${file.name}-${index}`} className="relative flex-shrink-0 w-20 h-20 rounded-2xl overflow-hidden border border-gray-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 shadow-sm">
                    {file.dataUrl ? <img src={file.dataUrl} alt={file.name} className="w-full h-full object-cover" /> : <div className="w-full h-full flex flex-col items-center justify-center px-1 text-center"><Paperclip size={20} className="text-gray-500" /><span className="text-[10px] mt-1 truncate w-full px-1">{file.name}</span></div>}
                    <button type="button" aria-label={`Remove ${file.name}`} onClick={() => setPendingAttachments(prev => prev.filter((_, i) => i !== index))} className="absolute top-1 right-1 w-6 h-6 rounded-full bg-black/65 text-white flex items-center justify-center"><X size={13}/></button>
                  </div>
                ))}
              </motion.div>
            )}

            {isAttachmentOpen && (
              <>
                <motion.button type="button" aria-label="Close attachment menu" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setIsAttachmentOpen(false)} className="fixed inset-0 z-30 bg-transparent cursor-default" />
                <motion.div
                  initial={{ opacity: 0, y: 12, scale: 0.96 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 8, scale: 0.96 }}
                  className="fixed left-4 bottom-[calc(env(safe-area-inset-bottom,0px)+92px)] z-40 w-[min(300px,calc(100vw-32px))] bg-[#f8f9fa] dark:bg-[#1e1e1e] rounded-[28px] shadow-2xl border border-gray-100 dark:border-zinc-800 p-2 overflow-hidden"
                >
                <div className="flex flex-col gap-1">
                  <motion.button whileTap={{ scale: 0.98 }} onClick={triggerCameraInput} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors">
                    <div className="bg-white dark:bg-zinc-800 p-2.5 rounded-full shadow-sm">
                      <Camera size={20} className="text-gray-700 dark:text-gray-200" />
                    </div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">Camera</span>
                  </motion.button>
                  <motion.button whileTap={{ scale: 0.98 }} onClick={triggerPhotoInput} className="flex items-center gap-4 px-4 py-3 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-[24px] transition-colors">
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
              </>
            )}
          </AnimatePresence>

          <motion.form layout onSubmit={handleSubmit} className="relative flex items-center bg-[#f4f4f5] dark:bg-zinc-800/80 rounded-[32px] px-2 py-1.5 shadow-sm border border-gray-100 dark:border-zinc-700 transition-all focus-within:ring-2 focus-within:ring-blue-100 dark:focus-within:ring-blue-900/50 focus-within:border-blue-200 dark:focus-within:border-blue-800">
            <motion.button
              layout
              whileTap={{ scale: 0.9 }}
              type="button"
              onClick={() => { haptic('tap'); if (authState !== 'authenticated') { requireLogin(); return; } setIsAttachmentOpen(v => !v); }}
              className="p-3 text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 transition-colors rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700 flex-shrink-0"
            >
              <Plus size={24} />
            </motion.button>
            
            {isDictationOpen ? (
              <DictationModal
                isOpen={isDictationOpen}
                inline
                geminiApiKey={apiKeys.gemini}
                onClose={() => setIsDictationOpen(false)}
                onTranscript={(text) => setInput(prev => prev ? `${prev} ${text}`.trim() : text)}
                onComplete={() => {
                  setIsDictationOpen(false);
                  setTimeout(() => inputRef.current?.focus(), 120);
                }}
              />
            ) : <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask Neo Gpt"
              rows={1}
              className="flex-1 bg-transparent border-none outline-none resize-none max-h-32 py-3.5 px-2 text-gray-800 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 font-medium text-[16px] flex items-center"
              style={{ minHeight: '52px', height: `${textareaHeight}px`, maxHeight: `${25 * 8 + 28}px`, transition: 'height 220ms cubic-bezier(.22,1,.36,1)' }}
            />}

            <motion.div layout className="flex items-center gap-1 pr-1 flex-shrink-0">
              <motion.button
                layout
                whileTap={{ scale: 0.9 }}
                type="button"
                onClick={() => { haptic('tap'); setIsAttachmentOpen(false); setIsModelSelectOpen(false); setIsDictationOpen(true); }}
                className="p-3 text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 transition-colors rounded-full hover:bg-gray-200 dark:hover:bg-zinc-700"
              >
                <Mic size={22} />
              </motion.button>

              {isLoading ? (
                <motion.button
                  layout
                  whileTap={{ scale: 0.9 }}
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  type="button"
                  onClick={() => { haptic('warning'); setIsLoading(false); showToast('Stopped'); }}
                  className="w-10 h-10 bg-gray-900 dark:bg-white rounded-full flex items-center justify-center text-white dark:text-gray-900 shadow-sm transition-colors ml-1"
                >
                  <Square size={16} fill="currentColor" strokeWidth={0} />
                </motion.button>
              ) : input.trim() || pendingAttachments.length ? (
                <motion.button
                  layout
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
                  layout
                  whileTap={{ scale: 0.9 }}
                  type="button"
                  onClick={() => { haptic('tap'); setIsVoiceOpen(true); }}
                  className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white shadow-sm hover:bg-blue-600 transition-colors ml-1"
                >
                  <AudioLines size={20} />
                </motion.button>
              )}
            </motion.div>
          </motion.form>
        </motion.div>

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
          isAuthenticated={authState === 'authenticated'}
          onLogin={openLogin}
          chatHistory={chatHistory}
          onDeleteChat={(id) => {
            setChatHistory(prev => prev.filter(chat => chat.id !== id));
            if (activeChatId === id) { setActiveChatId(null); setMessages([]); setInput(''); setPendingAttachments([]); setWelcomeMessage(getWelcomeMessage()); }
            showToast('Chat deleted');
          }}
          onSelectChat={(id) => {
            const chat = chatHistory.find(item => item.id === id);
            if (!chat) return;
            setActiveChatId(chat.id);
            setMessages(chat.messages);
            setInput('');
            setIsSidebarOpen(false);
          }}
          onNewChat={() => { setMessages([]); setActiveChatId(null); setInput(''); setPendingAttachments([]); setIsDictationOpen(false); setIsSidebarOpen(false); setIsAttachmentOpen(false); setIsModelSelectOpen(false); setWelcomeMessage(getWelcomeMessage()); showToast('New chat started'); }}
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
          providers={providers}
          setProviders={setProviders}
          onTestProvider={handleTestProvider}
          onSelectProviderModel={(modelId) => setSelectedModel(modelId)}
          accountUser={authUser}
          onLogout={logout}
        />
        
        <VoiceModal 
          isOpen={isVoiceOpen} 
          onClose={() => setIsVoiceOpen(false)}
          onTranscript={(text) => setInput(prev => prev ? `${prev} ${text}`.trim() : text)}
          onSilenceSubmit={() => {
            setTimeout(() => {
              handleSubmit();
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

      </motion.div>
    </div>
  );
}
