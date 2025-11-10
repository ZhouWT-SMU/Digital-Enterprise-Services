import type { CompanyCard as CompanyCardType } from '../store/useChatStore';

interface Props {
  company: CompanyCardType;
}

export function CompanyCard({ company }: Props) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md">
      <div className="flex items-start justify-between">
        <h3 className="text-lg font-semibold text-slate-900">{company.name}</h3>
        {typeof company.score === 'number' && (
          <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600">
            {company.score.toFixed(2)}
          </span>
        )}
      </div>
      <p className="mt-2 text-sm text-slate-600">{company.summary || '暂无简介'}</p>
      <div className="mt-3 space-y-2 text-xs text-slate-500">
        <div className="flex flex-wrap gap-2">
          {company.industry?.map((item) => (
            <span key={item} className="rounded-full bg-slate-100 px-2 py-0.5">
              {item}
            </span>
          ))}
        </div>
        {company.size && <div>规模：{company.size}</div>}
        <div className="flex flex-wrap gap-2">
          {company.region?.map((item) => (
            <span key={item} className="rounded-full bg-emerald-100 px-2 py-0.5 text-emerald-700">
              {item}
            </span>
          ))}
        </div>
        <div className="flex flex-wrap gap-2">
          {company.techKeywords?.map((item) => (
            <span key={item} className="rounded-full bg-indigo-100 px-2 py-0.5 text-indigo-700">
              {item}
            </span>
          ))}
        </div>
      </div>
      <p className="mt-3 text-xs text-slate-500">命中理由：{company.reason}</p>
    </div>
  );
}
