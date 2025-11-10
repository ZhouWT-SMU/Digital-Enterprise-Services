import clsx from 'clsx';
import type { Filters } from '../store/useChatStore';

const industryOptions = ['制造业', '金融', '能源', '零售', '互联网'];
const sizeOptions = ['小型', '中型', '大型'];
const regionOptions = ['华东', '华北', '华南', '西南', '东北'];
const techOptions = ['AI', 'Kubernetes', '云计算', '物联网', '大数据'];

interface FilterBarProps {
  filters: Filters;
  onChange: (filters: Partial<Filters>) => void;
}

function ToggleChip({
  label,
  active,
  onClick
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        'rounded-full border px-3 py-1 text-sm transition',
        active
          ? 'border-brand bg-brand text-white'
          : 'border-slate-200 bg-white text-slate-600 hover:border-brand'
      )}
    >
      {label}
    </button>
  );
}

export function FilterBar({ filters, onChange }: FilterBarProps) {
  const toggleFilter = (key: keyof Filters, value: string) => {
    const current = filters[key] || [];
    const exists = current.includes(value);
    const next = exists ? current.filter((item) => item !== value) : [...current, value];
    onChange({ [key]: next });
  };

  return (
    <aside className="flex w-full flex-col gap-6 border-r border-slate-200 bg-white/70 p-6 backdrop-blur lg:w-80">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">行业</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {industryOptions.map((item) => (
            <ToggleChip
              key={item}
              label={item}
              active={filters.industry.includes(item)}
              onClick={() => toggleFilter('industry', item)}
            />
          ))}
        </div>
      </div>
      <div>
        <h2 className="text-lg font-semibold text-slate-900">规模</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {sizeOptions.map((item) => (
            <ToggleChip
              key={item}
              label={item}
              active={filters.size.includes(item)}
              onClick={() => toggleFilter('size', item)}
            />
          ))}
        </div>
      </div>
      <div>
        <h2 className="text-lg font-semibold text-slate-900">地区</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {regionOptions.map((item) => (
            <ToggleChip
              key={item}
              label={item}
              active={filters.region.includes(item)}
              onClick={() => toggleFilter('region', item)}
            />
          ))}
        </div>
      </div>
      <div>
        <h2 className="text-lg font-semibold text-slate-900">技术关键词</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          {techOptions.map((item) => (
            <ToggleChip
              key={item}
              label={item}
              active={filters.tech.includes(item)}
              onClick={() => toggleFilter('tech', item)}
            />
          ))}
        </div>
      </div>
    </aside>
  );
}
