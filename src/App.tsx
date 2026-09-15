import React, { useState, useRef, useEffect } from 'react';
import { Menu, Sparkles, RefreshCw, Plus, Mic, AudioLines, Camera, Image as ImageIcon, Paperclip, Puzzle, BrainCircuit, ArrowUp, Copy, ThumbsUp, ThumbsDown, Speaker, Share2, MoreVertical, X, Download, ChevronDown, Check, Eye, EyeOff, Square } from 'lucide-react';
import { cn } from './lib/utils';
import { callApi, testProviderConnection } from './api';
import { Message, MessageAttachment, ApiKeys, ProviderConfig } from './types';
import { motion, AnimatePresence } from 'motion/react';
import { App as CapacitorApp } from '@capacitor/app';
import { Sidebar } from './components/Sidebar';
import { Settings } from './components/Settings';
import { VoiceModal } from './components/VoiceModal';
import { DictationModal } from './components/DictationModal';

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
  const pattern = /(\*\*|__)(.+?)\1|~~(.+?)~~|`([^`]+)`|\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)|(\*|_)([^*_]+?)\7/g;
  let last = 0;
  let match: RegExpExecArray | null;
  let key = 0;
  while ((match = pattern.exec(text))) {
    if (match.index > last) nodes.push(text.slice(last, match.index));
    if (match[1]) nodes.push(<strong key={`${keyPrefix}-b-${key++}`}>{renderInlineMarkdown(match[2], `${keyPrefix}-b`)}</strong>);
    else if (match[3]) nodes.push(<del key={`${keyPrefix}-s-${key++}`}>{renderInlineMarkdown(match[3], `${keyPrefix}-s`)}</del>);
    else if (match[4]) nodes.push(<code key={`${keyPrefix}-c-${key++}`} className="neo-inline-code">{match[4]}</code>);
    else if (match[5]) nodes.push(<a key={`${keyPrefix}-a-${key++}`} href={match[6]} target="_blank" rel="noreferrer" className="neo-markdown-link">{renderInlineMarkdown(match[5], `${keyPrefix}-a`)}</a>);
    else if (match[8]) nodes.push(<em key={`${keyPrefix}-e-${key++}`}>{renderInlineMarkdown(match[8], `${keyPrefix}-e`)}</em>);
    last = match.index + match[0].length;
  }
  if (last < text.length) nodes.push(text.slice(last));
  return nodes;
}

function renderMarkdown(text: string): React.ReactNode {
  const normalized = text.replace(/\r\n?/g, '\n');
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
          const shouldHighlightTitle = colon > 1 && colon < 70 && !item.text.slice(0, colon).includes('http');
          return (
            <li key={`li-${index}`}>
              {shouldHighlightTitle ? <><strong>{renderInlineMarkdown(item.text.slice(0, colon))}</strong>{renderInlineMarkdown(item.text.slice(colon))}</> : renderInlineMarkdown(item.text)}
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
      if (codeLines === null) {
        codeLines = [];
        codeLanguage = trimmed.slice(3).trim();
      } else {
        blocks.push(<pre key={`code-${index}`} className="neo-code-block"><code data-language={codeLanguage || undefined}>{codeLines.join('\n')}</code></pre>);
        codeLines = null;
        codeLanguage = '';
      }
      return;
    }
    if (codeLines !== null) {
      codeLines.push(line);
      return;
    }
    if (!trimmed) {
      flushList();
      return;
    }

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
    if (quote) {
      blocks.push(<blockquote key={`q-${index}`} className="neo-markdown-quote">{renderInlineMarkdown(quote[1], `q-${index}`)}</blockquote>);
      return;
    }

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

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const photoInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const chatContainerRef = useRef<HTMLDivElement>(null);

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

  useEffect(() => {
    const handleExternalToast = (event: Event) => {
      const message = (event as CustomEvent<string>).detail;
      if (message) showToast(message);
    };
    window.addEventListener('neo-gpt-toast', handleExternalToast);
    return () => window.removeEventListener('neo-gpt-toast', handleExternalToast);
  }, []);

  const handleActionClick = (action: string, text?: string) => {
    if (action === 'copy' && text) {
      navigator.clipboard?.writeText(text).then(() => showToast('Copied to clipboard')).catch(() => showToast('Could not copy text'));
    } else if (action === 'thumbsUp') {
      showToast('Thanks for the feedback!');
    } else if (action === 'thumbsDown') {
      showToast('Feedback submitted');
    } else if (action === 'speaker' && text) {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(new SpeechSynthesisUtterance(text));
        showToast('Reading response aloud');
      } else {
        showToast('Voice playback is not supported here');
      }
    } else if (action === 'share' && text) {
      if (navigator.share) {
        navigator.share({ title: 'Neo Gpt response', text }).catch(() => {});
      } else {
        navigator.clipboard?.writeText(text).then(() => showToast('Response copied for sharing')).catch(() => showToast('Sharing is not supported here'));
      }
    } else if (action === 'more') {
      showToast('More actions coming soon');
    } else {
      showToast('Action selected');
    }
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if ((!input.trim() && pendingAttachments.length === 0) || isLoading) return;

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

  const triggerFileInput = () => { setIsAttachmentOpen(false); fileInputRef.current?.click(); };
  const triggerPhotoInput = () => { setIsAttachmentOpen(false); photoInputRef.current?.click(); };
  const triggerCameraInput = () => { setIsAttachmentOpen(false); cameraInputRef.current?.click(); };

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

  return (
    <div className={theme}>
      <div className={cn(
        "flex flex-col h-[100dvh] w-full bg-white dark:bg-[#121212] overflow-hidden relative shadow-2xl",
        fontFamily === 'inter' ? 'font-inter' : 'font-josefin',
        // Mobile constraint wrapper
        "max-w-[480px] mx-auto border-x border-gray-100 dark:border-zinc-800"
      )}>
        {/* Top Bar */}
        <header className="neo-topbar flex items-center justify-between px-4 pb-3 z-10 dark:bg-[#121212]">
          <div className="flex items-center gap-3">
            <motion.button 
              type="button"
              whileTap={{ scale: 0.94 }}
              onClick={() => setIsSidebarOpen(true)}
              className="p-3 bg-gray-50 dark:bg-zinc-800 rounded-full hover:bg-gray-100 dark:hover:bg-zinc-700 transition-colors"
            >
              <Menu size={20} className="text-gray-700 dark:text-gray-200" />
            </motion.button>
            
            {/* Model Selector Capsule */}
            <div className="relative">
              <motion.button 
                whileTap={{ scale: 0.94 }}
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
        <main ref={chatContainerRef} className="flex-1 overflow-y-auto custom-scrollbar px-4 pb-28 pt-2 dark:bg-[#121212] neo-content-fade">
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
                    <div className="bg-blue-100 dark:bg-blue-900/50 text-gray-900 dark:text-white px-3.5 py-3.5 rounded-[24px] rounded-tr-[8px] max-w-[88%] break-words shadow-sm text-[15px] leading-relaxed">
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
                      <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400 mt-1">
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('copy', msg.text)} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><Copy size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('thumbsUp')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><ThumbsUp size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('thumbsDown')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><ThumbsDown size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('speaker', msg.text)} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><Speaker size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('share', msg.text)} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><Share2 size={16} /></motion.button>
                        <motion.button whileTap={{ scale: 0.8 }} onClick={() => handleActionClick('more')} className="p-2 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-full transition-colors"><MoreVertical size={16} /></motion.button>
                      </div>
                    </div>
                  )}
                </motion.div>
              ))}
            </AnimatePresence>
            
            {isLoading && (
              <motion.div
                initial={{ opacity: 0, y: 8, scale: 0.98 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.22, ease: 'easeOut' }}
                className="flex justify-start w-full"
                aria-live="polite"
                aria-label="Thinking"
              >
                <div className="neo-thinking-clean">
                  <span className="neo-thinking-label">Thinking</span>
                  <span className="neo-thinking-dots" aria-hidden="true">
                    <motion.i animate={{ opacity: [0.25, 1, 0.25] }} transition={{ repeat: Infinity, duration: 1.2, delay: 0 }} />
                    <motion.i animate={{ opacity: [0.25, 1, 0.25] }} transition={{ repeat: Infinity, duration: 1.2, delay: 0.18 }} />
                    <motion.i animate={{ opacity: [0.25, 1, 0.25] }} transition={{ repeat: Infinity, duration: 1.2, delay: 0.36 }} />
                  </span>
                </div>
              </motion.div>
            )}
            <div ref={messagesEndRef} className="h-4" />
          </div>
        </main>

        {/* Bottom Input Area */}
        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-white via-white/95 to-transparent dark:from-[#121212] dark:via-[#121212]/95 pt-6 px-4 z-20 neo-bottom-shell">
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
                  onClick={() => { setIsLoading(false); showToast('Stopped'); }}
                  className="w-10 h-10 bg-gray-900 dark:bg-white rounded-full flex items-center justify-center text-white dark:text-gray-900 shadow-sm transition-colors ml-1"
                >
                  <Square size={16} fill="currentColor" strokeWidth={0} />
                </motion.button>
              ) : input.trim() || pendingAttachments.length ? (
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
          chatHistory={chatHistory}
          onSelectChat={(id) => {
            const chat = chatHistory.find(item => item.id === id);
            if (!chat) return;
            setActiveChatId(chat.id);
            setMessages(chat.messages);
            setInput('');
            setIsSidebarOpen(false);
          }}
          onNewChat={() => { setMessages([]); setActiveChatId(null); setInput(''); setIsSidebarOpen(false); setIsAttachmentOpen(false); setIsModelSelectOpen(false); showToast('New chat started'); }}
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
