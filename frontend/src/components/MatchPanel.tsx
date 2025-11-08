import { FormEvent, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { matchCompany } from '../api/client';

export const MatchPanel = () => {
  const [query, setQuery] = useState('');
  const mutation = useMutation({ mutationFn: matchCompany });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!query.trim()) return;
    mutation.mutate(query);
  };

  return (
    <aside className="sticky top-10 h-fit rounded bg-white p-6 shadow">
      <h2 className="text-lg font-semibold text-slate-800">企业问答/匹配</h2>
      <p className="mt-2 text-sm text-slate-600">输入业务需求或问题，系统将调用 Dify ChatFlow 返回最匹配的企业。</p>
      <form onSubmit={handleSubmit} className="mt-4 space-y-4">
        <textarea
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          className="w-full rounded border border-slate-300 p-3 text-sm"
          rows={5}
          placeholder="例如：需要具有工业互联网平台建设经验的 SaaS 服务商"
        />
        <button
          type="submit"
          className="w-full rounded bg-emerald-600 py-2 text-sm font-semibold text-white"
          disabled={mutation.isPending}
        >
          {mutation.isPending ? '匹配中...' : '开始匹配'}
        </button>
      </form>
      {mutation.data && (
        <div className="mt-6 space-y-2">
          <h3 className="text-sm font-semibold text-slate-700">匹配结果</h3>
          <pre className="whitespace-pre-wrap rounded bg-slate-900 p-3 text-xs text-emerald-200">
            {JSON.stringify(mutation.data, null, 2)}
          </pre>
        </div>
      )}
      {mutation.error && (
        <p className="mt-4 text-sm text-rose-600">{(mutation.error as Error).message}</p>
      )}
    </aside>
  );
};
