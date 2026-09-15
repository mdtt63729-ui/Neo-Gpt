export interface MessageAttachment {
  name: string;
  type: string;
  size: number;
  dataUrl?: string;
}

export interface Message {
  id: string;
  sender: 'user' | 'ai';
  text?: string;
  imageUrl?: string;
  attachments?: MessageAttachment[];
  isLoading?: boolean;
}

export interface ApiKeys {
  openRouter: string;
  nvidia: string;
  gemini: string;
}

export interface ModelConfig {
  id: string;
  name: string;
  providerId: string;
  input?: 'text' | 'vision';
  deletable?: boolean;
}

export interface ProviderConfig {
  id: string;
  name: string;
  baseUrl: string;
  apiKey: string;
  enabled: boolean;
  builtIn: boolean;
  models: ModelConfig[];
}
