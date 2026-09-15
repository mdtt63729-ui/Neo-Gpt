export const LLM_API_URL = 'https://backend.buildpicoapps.com/aero/run/llm-api?pk=v1-Z0FBQUFBQnFxQklNbzV1Ty12RUkxaklPdUpnZ2hJdWtDZm9hcmVlQjZkdU55eDd4SlBna2xxZHlXOGNRc0diMDVjOEtUNGVPV1hPYm0xWnlSRTBWWEdFNndUV0xMNjB4b2c9PQ==';
export const IMAGE_API_URL = 'https://backend.buildpicoapps.com/aero/run/image-generation-api?pk=v1-Z0FBQUFBQnFxQklNbzV1Ty12RUkxaklPdUpnZ2hJdWtDZm9hcmVlQjZkdU55eDd4SlBna2xxZHlXOGNRc0diMDVjOEtUNGVPV1hPYm0xWnlSRTBWWEdFNndUV0xMNjB4b2c9PQ==';

const VENUS_WEBSOCKET_URL = 'wss://backend.buildpicoapps.com/api/chatbot/chat';
const VENUS_APP_ID = 'party-develop';
const VENUS_SYSTEM_PROMPT = 'You are Neo Gpt, a helpful, intelligent and friendly AI assistant. Answer the user clearly and accurately. If the user asks to generate, create or make an image, photo, or picture by describing it, reply with "/image " followed by a concise image description. Otherwise, respond normally.';

type UnknownRecord = Record<string, unknown>;

function extractText(value: unknown): string | null {
  if (typeof value === 'string') return value.trim() || null;
  if (!value || typeof value !== 'object') return null;
  const data = value as UnknownRecord;
  const choices = Array.isArray(data.choices) ? data.choices : undefined;
  const firstChoice = choices?.[0] as UnknownRecord | undefined;
  const message = firstChoice?.message as UnknownRecord | undefined;
  const error = data.error as UnknownRecord | undefined;
  const geminiCandidates = Array.isArray(data.candidates) ? data.candidates : [];
  const geminiParts = geminiCandidates.flatMap(candidate => {
    const content = (candidate as UnknownRecord)?.content as UnknownRecord | undefined;
    return Array.isArray(content?.parts) ? content.parts : [];
  });
  const candidates: unknown[] = [
    data.text, data.response, data.answer, data.output, data.content, data.message,
    data.result, data.data, data.output_text, error?.message, message?.content, firstChoice?.text,
    ...geminiParts.map(part => (part as UnknownRecord)?.text),
  ];
  for (const candidate of candidates) {
    const text = extractText(candidate);
    if (text) return text;
  }
  return null;
}

async function readResponse(res: Response): Promise<{ raw: string; data: unknown }> {
  const raw = await res.text();
  if (!raw) return { raw: '', data: null };
  try { return { raw, data: JSON.parse(raw) }; } catch { return { raw, data: raw }; }
}

function callVenus(prompt: string): Promise<{ status: 'success' | 'error'; text: string }> {
  return new Promise((resolve) => {
    if (typeof WebSocket === 'undefined') {
      resolve({ status: 'error', text: 'Venus requires WebSocket support on this device.' });
      return;
    }
    const chatId = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const websocket = new WebSocket(VENUS_WEBSOCKET_URL);
    let responseText = '';
    let settled = false;
    let opened = false;
    let timeoutId = 0;
    const finish = (result: { status: 'success' | 'error'; text: string }) => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timeoutId);
      try { websocket.close(); } catch { /* noop */ }
      resolve(result);
    };
    timeoutId = window.setTimeout(() => finish({ status: 'error', text: responseText.trim() || 'Venus timed out while waiting for a response. Please try again.' }), 90000);
    websocket.addEventListener('open', () => {
      opened = true;
      try {
        websocket.send(JSON.stringify({ chatId, appId: VENUS_APP_ID, systemPrompt: VENUS_SYSTEM_PROMPT, message: prompt }));
      } catch { finish({ status: 'error', text: 'Venus could not send the message. Please try again.' }); }
    });
    websocket.addEventListener('message', async (event) => {
      try {
        if (typeof event.data === 'string') responseText += event.data;
        else if (event.data instanceof ArrayBuffer) responseText += new TextDecoder().decode(event.data);
        else if (typeof Blob !== 'undefined' && event.data instanceof Blob) responseText += await event.data.text();
      } catch { finish({ status: 'error', text: 'Venus returned unreadable response data.' }); }
    });
    websocket.addEventListener('error', () => finish({ status: 'error', text: opened ? (responseText.trim() || 'Venus connection failed while receiving the response.') : 'Could not connect to the Venus endpoint. Check your internet connection and try again.' }));
    websocket.addEventListener('close', (event) => {
      if (settled) return;
      const text = responseText.trim();
      if (text) finish({ status: 'success', text });
      else finish({ status: 'error', text: `Venus returned no response (WebSocket ${event.code || 'closed'}).` });
    });
  });
}

function normalizeBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, '');
}

function chatCompletionsUrl(baseUrl: string): string {
  const base = normalizeBaseUrl(baseUrl);
  return /\/chat\/completions$/i.test(base) ? base : `${base}/chat/completions`;
}

export interface ConnectionTestResult {
  ok: boolean;
  message: string;
  models: { id: string; name?: string; input?: 'text' | 'vision' }[];
}

export async function testProviderConnection(providerId: string, apiKey: string, baseUrl?: string): Promise<ConnectionTestResult> {
  try {
    if (!apiKey.trim()) return { ok: false, message: 'Enter an API key first.', models: [] };
    if (providerId === 'gemini') {
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${encodeURIComponent(apiKey.trim())}`);
      const { raw, data } = await readResponse(res);
      if (!res.ok) return { ok: false, message: `Gemini connection failed (${res.status}). ${extractText(data) || raw || ''}`.trim(), models: [] };
      const list = Array.isArray((data as UnknownRecord)?.models) ? (data as UnknownRecord).models as UnknownRecord[] : [];
      const models = list.filter(m => {
        const methods = Array.isArray(m.supportedGenerationMethods) ? m.supportedGenerationMethods.map(String) : [];
        return String(m.name || '').includes('models/') && (methods.length === 0 || methods.includes('generateContent'));
      }).map(m => {
        const description = `${m.description || ''}`.toLowerCase();
        return {
          id: String(m.name).replace(/^models\//, ''),
          name: String(m.displayName || m.name),
          input: (description.includes('image') || description.includes('multimodal') ? 'vision' : 'text') as 'text' | 'vision',
        };
      });
      return { ok: true, message: `Connected. Found ${models.length} Gemini models.`, models };
    }
    const url = providerId === 'openRouter'
      ? 'https://openrouter.ai/api/v1/models'
      : providerId === 'nvidia'
        ? 'https://integrate.api.nvidia.com/v1/models'
        : providerId === 'groq'
          ? 'https://api.groq.com/openai/v1/models'
          : `${normalizeBaseUrl(baseUrl || '')}/models`;
    const res = await fetch(url, { headers: { Authorization: `Bearer ${apiKey.trim()}`, Accept: 'application/json' } });
    const { raw, data } = await readResponse(res);
    if (!res.ok) return { ok: false, message: `Connection failed (${res.status}). ${extractText(data) || raw || ''}`.trim(), models: [] };
    const list = Array.isArray((data as UnknownRecord)?.data) ? (data as UnknownRecord).data as UnknownRecord[] : [];
    const models = list.map(m => ({
      id: String(m.id || ''), name: String(m.name || m.id || ''), input: 'text' as const,
    })).filter(m => m.id);
    return { ok: true, message: `Connected. Found ${models.length} models.`, models };
  } catch (error) {
    return { ok: false, message: 'Connection failed. Check the API key, endpoint and network connection.', models: [] };
  }
}

export async function callApi(
  prompt: string,
  isImageGen: boolean = false,
  modelId: string = 'venus-3.1',
  apiKeys: Record<string, string> = {},
  customProviders: Array<{ id: string; name: string; baseUrl: string; apiKey: string; enabled: boolean }> = []
): Promise<{ status: 'success' | 'error'; text?: string; imageUrl?: string }> {
  if (isImageGen) {
    try {
      const seed = Math.floor(Math.random() * 100000);
      return { status: 'success', imageUrl: `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?width=1024&height=1024&nologo=true&seed=${seed}` };
    } catch { return { status: 'error', text: 'An error occurred. Please try again.' }; }
  }
  try {
    if (modelId === 'venus-3.1') return await callVenus(prompt);

    let url = '';
    let apiKey = '';
    let actualModel = modelId;
    let label = '';
    if (modelId.startsWith('openrouter/')) {
      actualModel = modelId.replace(/^openrouter\//, '');
      apiKey = apiKeys.openRouter || '';
      url = 'https://openrouter.ai/api/v1/chat/completions'; label = 'OpenRouter';
    } else if (modelId.startsWith('nvidia/')) {
      actualModel = modelId.replace(/^nvidia\//, '');
      apiKey = apiKeys.nvidia || '';
      url = 'https://integrate.api.nvidia.com/v1/chat/completions'; label = 'NVIDIA';
    } else if (modelId.startsWith('groq/')) {
      actualModel = modelId.replace(/^groq\//, '');
      const groqProvider = customProviders.find(p => p.id === 'groq' && p.enabled);
      apiKey = groqProvider?.apiKey || '';
      url = 'https://api.groq.com/openai/v1/chat/completions'; label = 'Groq';
      if (actualModel === 'whisper-large-v3') {
        return { status: 'error', text: 'Whisper Large v3 is an audio transcription model and cannot be used for normal chat messages.' };
      }
    } else if (modelId.startsWith('gemini/')) {
      actualModel = modelId.replace(/^gemini\//, '');
      apiKey = apiKeys.gemini || '';
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(actualModel)}:generateContent`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', 'x-goog-api-key': apiKey.trim() },
        body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
      });
      const { raw, data } = await readResponse(res);
      if (!res.ok) return { status: 'error', text: `Gemini error (${res.status}): ${extractText(data) || raw || 'Request failed'}` };
      return { status: 'success', text: extractText(data) || 'No response from Gemini.' };
    } else if (modelId.startsWith('custom/')) {
      const parts = modelId.split('/');
      const providerId = parts[1];
      actualModel = parts.slice(2).join('/');
      const provider = customProviders.find(p => p.id === providerId && p.enabled);
      if (!provider) return { status: 'error', text: 'This custom provider is disabled or no longer exists.' };
      apiKey = provider.apiKey;
      url = chatCompletionsUrl(provider.baseUrl); label = provider.name;
    } else {
      return { status: 'error', text: 'Unknown model. Please choose another model.' };
    }

    if (!apiKey.trim()) return { status: 'error', text: `${label} requires a valid API key. Open Settings → API Providers & Models.` };
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey.trim()}`,
        ...(label === 'OpenRouter' ? {
          'HTTP-Referer': window.location.origin,
          'X-Title': 'Neo Gpt',
        } : {}),
      },
      body: JSON.stringify({ model: actualModel, messages: [{ role: 'user', content: prompt }] }),
    });
    const { raw, data } = await readResponse(res);
    if (!res.ok) return { status: 'error', text: `${label} error (${res.status}): ${extractText(data) || raw || 'Request failed'}` };
    return { status: 'success', text: extractText(data) || `No response from ${label}.` };
  } catch (error) {
    console.error('Error calling API:', error);
    return { status: 'error', text: 'An error occurred. Check your network, endpoint or API key.' };
  }
}
