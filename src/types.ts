export interface Message {
  id: string;
  sender: 'user' | 'ai';
  text?: string;
  imageUrl?: string;
  isLoading?: boolean;
}

export interface ApiKeys {
  openRouter: string;
  nvidia: string;
  gemini: string;
}
