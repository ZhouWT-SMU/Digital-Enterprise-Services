import { useCallback } from 'react';
import { ChatPanel } from '../components/ChatPanel';
import { CompanyCard } from '../components/CompanyCard';
import { FilterBar } from '../components/FilterBar';
import { useChatStore } from '../store/useChatStore';
import type { CompanyCard as CompanyCardType } from '../store/useChatStore';

const API_BASE = import.meta.env.VITE_API_BASE || '';

async function streamChat(
  payload: Record<string, unknown>,
  onToken: (token: string) => void,
  onCompanies: (companies: unknown) => void,
  onDone: () => void,
  onError: (error: string) => void
) {
  try {
    const response = await fetch(`${API_BASE}/api/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok || !response.body) {
      throw new Error('无法连接聊天流');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let boundary = buffer.indexOf('\n\n');
      while (boundary !== -1) {
        const chunk = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        const event = parseSseChunk(chunk);
        if (event) {
          if (event.event === 'token') {
            onToken(event.data);
          } else if (event.event === 'companies') {
            onCompanies(JSON.parse(event.data));
          } else if (event.event === 'done') {
            onDone();
          }
        }
        boundary = buffer.indexOf('\n\n');
      }
    }
    onDone();
  } catch (error) {
    onError(error instanceof Error ? error.message : '未知错误');
  }
}

function parseSseChunk(chunk: string): { event: string; data: string } | null {
  const lines = chunk.split('\n');
  let event = 'message';
  let data = '';
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.replace('event:', '').trim();
    } else if (line.startsWith('data:')) {
      data += line.replace('data:', '').trim();
    }
  }
  if (!event || !data) {
    return null;
  }
  return { event, data };
}

export default function Chat() {
  const messages = useChatStore((state) => state.messages);
  const filters = useChatStore((state) => state.filters);
  const setFilters = useChatStore((state) => state.setFilters);
  const pushMessage = useChatStore((state) => state.pushMessage);
  const appendAssistantToken = useChatStore((state) => state.appendAssistantToken);
  const setCompanies = useChatStore((state) => state.setCompanies);
  const streaming = useChatStore((state) => state.streaming);
  const setStreaming = useChatStore((state) => state.setStreaming);
  const companies = useChatStore((state) => state.companies);
  const sessionId = useChatStore((state) => state.sessionId);

  const handleSend = useCallback(
    async (content: string) => {
      if (streaming) return;
      pushMessage({ role: 'user', content });
      pushMessage({ role: 'assistant', content: '' });
      setCompanies([]);
      setStreaming(true);

      const payload = {
        sessionId,
        message: content,
        uiFilters: filters
      };

      await streamChat(
        payload,
        (token) => {
          appendAssistantToken(token);
        },
        (companyEvent) => {
          if (companyEvent && typeof companyEvent === 'object' && 'companies' in companyEvent) {
            setCompanies((companyEvent as { companies: CompanyCardType[] }).companies);
          }
        },
        () => {
          setStreaming(false);
        },
        (error) => {
          appendAssistantToken(`\n[服务异常] ${error}`);
          setStreaming(false);
        }
      );
    },
    [appendAssistantToken, filters, pushMessage, setCompanies, setStreaming, streaming]
  );

  return (
    <div className="flex min-h-screen flex-col bg-gradient-to-br from-slate-100 via-white to-slate-200">
      <header className="border-b border-slate-200 bg-white/90 px-6 py-4 shadow-sm backdrop-blur">
        <h1 className="text-2xl font-semibold text-slate-900">企业智能筛选助手</h1>
        <p className="mt-1 text-sm text-slate-500">结合 Dify 知识库，为您精准推荐符合条件的企业</p>
      </header>
      <main className="flex flex-1 flex-col lg:flex-row">
        <FilterBar filters={filters} onChange={setFilters} />
        <section className="flex flex-1 flex-col gap-6 p-6">
          <div className="flex flex-1 flex-col gap-6 lg:flex-row">
            <div className="flex-1">
              <ChatPanel messages={messages} onSend={handleSend} streaming={streaming} />
            </div>
            <aside className="lg:w-80">
              <div className="rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm">
                <h2 className="text-lg font-semibold text-slate-900">推荐企业</h2>
                <p className="mt-1 text-xs text-slate-500">实时更新匹配的公司列表</p>
                <div className="mt-4 space-y-4">
                  {companies.length === 0 ? (
                    <p className="text-sm text-slate-400">暂无推荐，请输入需求或等待模型返回。</p>
                  ) : (
                    companies.map((company) => <CompanyCard key={company.id} company={company} />)
                  )}
                </div>
              </div>
            </aside>
          </div>
        </section>
      </main>
    </div>
  );
}
