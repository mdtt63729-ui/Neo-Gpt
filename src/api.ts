export const LLM_API_URL = 'https://backend.buildpicoapps.com/aero/run/llm-api?pk=v1-Z0FBQUFBQnFxQklNbzV1Ty12RUkxaklPdUpnZ2hJdWtDZm9hcmVlQjZkdU55eDd4SlBna2xxZHlXOGNRc0diMDVjOEtUNGVPV1hPYm0xWnlSRTBWWEdFNndUV0xMNjB4b2c9PQ==';
export const IMAGE_API_URL = 'https://backend.buildpicoapps.com/aero/run/image-generation-api?pk=v1-Z0FBQUFBQnFxQklNbzV1Ty12RUkxaklPdUpnZ2hJdWtDZm9hcmVlQjZkdU55eDd4SlBna2xxZHlXOGNRc0diMDVjOEtUNGVPV1hPYm0xWnlSRTBWWEdFNndUV0xMNjB4b2c9PQ==';

type UnknownRecord = Record<string, unknown>;

function extractText(value: unknown): string | null {
  if (typeof value === 'string') return value.trim() || null;
  if (!value || typeof value !== 'object') return null;

  const data = value as UnknownRecord;
  const candidates: unknown[] = [
    data.text,
    data.response,
    data.answer,
    data.output,
    data.content,
    data.message,
    data.result,
    data.data,
    data.choices && Array.isArray(data.choices)
      ? (data.choices[0] as UnknownRecord | undefined)?.message
      : undefined,
  ];

  for (const candidate of candidates) {
    const text = extractText(candidate);
    if (text) return text;
  }

  return null;
}

function stripHtml(value: string): string {
  const doc = new DOMParser().parseFromString(value, 'text/html');
  return (doc.body.textContent || '').replace(/\s+/g, ' ').trim();
}

async function readResponse(res: Response): Promise<{ raw: string; data: unknown }> {
  const raw = await res.text();
  if (!raw) return { raw: '', data: null };
  try {
    return { raw, data: JSON.parse(raw) };
  } catch {
    return { raw, data: raw };
  }
}

export async function callApi(
  prompt: string,
  isImageGen: boolean = false,
  modelId: string = 'venus-3.1',
  apiKeys: Record<string, string> = {}
) {
  if (isImageGen) {
    try {
      const seed = Math.floor(Math.random() * 100000);
      const url = `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?width=1024&height=1024&nologo=true&seed=${seed}`;
      return { status: 'success', imageUrl: url };
    } catch (error) {
      console.error('Error calling Image API:', error);
      return { status: 'error', text: 'An error occurred. Please try again.' };
    }
  }

  try {
    if (modelId.startsWith('openrouter/') && apiKeys.openRouter) {
      const actualModel = modelId.replace('openrouter/', '');
      const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKeys.openRouter}`
        },
        body: JSON.stringify({ model: actualModel, messages: [{ role: 'user', content: prompt }] })
      });
      const { raw, data } = await readResponse(res);
      if (!res.ok) return { status: 'error', text: `OpenRouter error (${res.status}): ${extractText(data) || raw || 'Request failed'}` };
      return { status: 'success', text: extractText(data) || 'No response from OpenRouter.' };
    }

    if (modelId.startsWith('nvidia/') && apiKeys.nvidia) {
      const actualModel = modelId.replace('nvidia/', '');
      const res = await fetch('https://integrate.api.nvidia.com/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKeys.nvidia}`
        },
        body: JSON.stringify({ model: actualModel, messages: [{ role: 'user', content: prompt }] })
      });
      const { raw, data } = await readResponse(res);
      if (!res.ok) return { status: 'error', text: `NVIDIA error (${res.status}): ${extractText(data) || raw || 'Request failed'}` };
      return { status: 'success', text: extractText(data) || 'No response from NVIDIA.' };
    }

    if (modelId.startsWith('gemini/') && apiKeys.gemini) {
      const actualModel = modelId.replace('gemini/', '');
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${actualModel}:generateContent?key=${apiKeys.gemini}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] })
      });
      const { raw, data } = await readResponse(res);
      if (!res.ok) return { status: 'error', text: `Gemini error (${res.status}): ${extractText(data) || raw || 'Request failed'}` };
      return { status: 'success', text: extractText(data) || 'No response from Gemini.' };
    }

    const finalPrompt = "Follow instructions precisely! If the user asks to generate, create or make an image, photo, or picture by describing it, You will reply with '/image ' + description. Otherwise, you will respond normally. Avoid additional explanations.\nUser: " + prompt;

    const res = await fetch(LLM_API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt: finalPrompt })
    });

    const { raw, data } = await readResponse(res);
    if (!res.ok) {
      const message = extractText(data) || (typeof data === 'string' ? stripHtml(data) : '') || raw;
      return { status: 'error', text: `Venus endpoint error (${res.status}): ${message || 'Request failed'}` };
    }

    const text = extractText(data) || (typeof data === 'string' ? stripHtml(data) : '');
    if (!text) {
      console.error('Unexpected Venus endpoint response:', data);
      return { status: 'error', text: 'Venus endpoint returned an empty or unsupported response.' };
    }

    return { status: 'success', text };
  } catch (error) {
    console.error('Error calling API:', error);
    return { status: 'error', text: 'An error occurred. Check your network or API keys.' };
  }
}
