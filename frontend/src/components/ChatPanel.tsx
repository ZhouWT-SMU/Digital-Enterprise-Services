import { useEffect, useRef } from 'react';
import type { KeyboardEvent } from 'react';
import type { Message } from '../store/useChatStore';

interface ChatPanelProps {
  messages: Message[];
  onSend: (message: string) => void;
  streaming: boolean;
}

export function ChatPanel({ messages, onSend, streaming }: ChatPanelProps) {
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      const value = event.currentTarget.value.trim();
      if (value) {
        onSend(value);
        event.currentTarget.value = '';
      }
    }
  };

  const handleSendClick = () => {
    const value = inputRef.current?.value.trim();
    if (value) {
      onSend(value);
      if (inputRef.current) {
        inputRef.current.value = '';
      }
    }
  };

  return (
    <div className="flex h-full flex-1 flex-col">
      <div className="flex-1 space-y-4 overflow-y-auto rounded-xl bg-white/80 p-6 shadow-inner">
        {messages.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center text-center text-slate-500">
            <h3 className="text-lg font-semibold text-slate-700">欢迎使用企业筛选助手</h3>
            <p className="mt-2 max-w-md text-sm">
              输入您的条件，例如“帮我找在华东、有AI能力、支持K8s的中型制造企业”，我会结合知识库为您筛选。
            </p>
          </div>
        )}
        {messages.map((message) => (
          <div
            key={message.id}
            className={message.role === 'user' ? 'ml-auto max-w-xl rounded-2xl bg-brand px-4 py-2 text-white' : 'max-w-xl rounded-2xl bg-slate-100 px-4 py-2 text-slate-800'}
          >
            {message.content}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      <div className="mt-4 flex items-end gap-3 rounded-2xl bg-white p-4 shadow-lg">
        <textarea
          ref={inputRef}
          rows={2}
          className="flex-1 resize-none rounded-xl border border-slate-200 px-3 py-2 text-sm shadow-sm focus:border-brand focus:outline-none"
          placeholder="描述您的筛选需求，Enter 发送，Shift+Enter 换行"
          onKeyDown={handleKeyDown}
        />
        <button
          type="button"
          onClick={handleSendClick}
          disabled={streaming}
          className="rounded-xl bg-brand px-4 py-2 text-sm font-medium text-white shadow disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {streaming ? '筛选中...' : '发送'}
        </button>
      </div>
    </div>
  );
}
