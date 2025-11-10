import { create } from 'zustand';
import { nanoid } from 'nanoid/non-secure';

export type Message = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
};

export type CompanyCard = {
  id: string;
  name: string;
  industry: string[];
  size?: string | null;
  region: string[];
  techKeywords: string[];
  summary: string;
  score: number;
  reason: string;
};

export type Filters = {
  industry: string[];
  size: string[];
  region: string[];
  tech: string[];
};

interface ChatState {
  sessionId: string;
  messages: Message[];
  filters: Filters;
  companies: CompanyCard[];
  streaming: boolean;
  setStreaming: (value: boolean) => void;
  pushMessage: (message: Omit<Message, 'id'>) => void;
  updateLastAssistantMessage: (content: string) => void;
  appendAssistantToken: (token: string) => void;
  setFilters: (filters: Partial<Filters>) => void;
  setCompanies: (companies: CompanyCard[]) => void;
  resetStream: () => void;
}

const defaultFilters: Filters = {
  industry: [],
  size: [],
  region: [],
  tech: []
};

export const useChatStore = create<ChatState>((set, get) => ({
  sessionId: nanoid(),
  messages: [],
  filters: defaultFilters,
  companies: [],
  streaming: false,
  setStreaming: (value) => set({ streaming: value }),
  pushMessage: (message) =>
    set((state) => ({
      messages: [...state.messages, { ...message, id: nanoid() }]
    })),
  updateLastAssistantMessage: (content) => {
    set((state) => {
      const messages = state.messages.map((msg, index) => {
        if (index === state.messages.length - 1 && msg.role === 'assistant') {
          return { ...msg, content };
        }
        return msg;
      });
      return { messages };
    });
  },
  appendAssistantToken: (token) => {
    set((state) => {
      const messages = [...state.messages];
      const last = messages[messages.length - 1];
      if (!last || last.role !== 'assistant') {
        messages.push({ id: nanoid(), role: 'assistant', content: token });
      } else {
        last.content += token;
      }
      return { messages };
    });
  },
  setFilters: (filters) =>
    set((state) => ({
      filters: { ...state.filters, ...filters }
    })),
  setCompanies: (companies) => set({ companies }),
  resetStream: () => set({ companies: [], streaming: false })
}));
