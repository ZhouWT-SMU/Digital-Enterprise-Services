import { CompanyPayload } from '../api/client';

interface Props {
  value: CompanyPayload['coreOfferings'];
  onChange: (offerings: CompanyPayload['coreOfferings']) => void;
}

const createOffering = () => ({ name: '', type: '产品' as const, description: '' });

export const CoreOfferingsFieldset = ({ value, onChange }: Props) => {
  const items = value.length ? value : [createOffering()];

  const updateValue = (index: number, key: 'name' | 'type' | 'description', newValue: string) => {
    const copy = items.map((item, idx) => (idx === index ? { ...item, [key]: newValue } : item));
    onChange(copy);
  };

  const addOffering = () => onChange([...items, createOffering()]);

  const removeOffering = (index: number) => onChange(items.filter((_, idx) => idx !== index));

  return (
    <div className="space-y-6">
      {items.map((item, index) => (
        <div key={index} className="rounded border border-slate-200 bg-white p-4 shadow-sm">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label className="space-y-1 text-sm">
              <span className="font-medium">核心产品/服务名称</span>
              <input
                value={item.name}
                onChange={(event) => updateValue(index, 'name', event.target.value)}
                className="w-full rounded border border-slate-300 p-2"
                required
              />
            </label>
            <label className="space-y-1 text-sm">
              <span className="font-medium">类型</span>
              <select
                value={item.type}
                onChange={(event) => updateValue(index, 'type', event.target.value)}
                className="w-full rounded border border-slate-300 p-2"
              >
                <option value="产品">产品</option>
                <option value="服务">服务</option>
                <option value="方案">方案</option>
              </select>
            </label>
          </div>
          <label className="mt-4 block space-y-1 text-sm">
            <span className="font-medium">产品简述</span>
            <textarea
              value={item.description}
              onChange={(event) => updateValue(index, 'description', event.target.value)}
              className="w-full rounded border border-slate-300 p-2"
              rows={4}
            />
          </label>
          {items.length > 1 && (
            <button
              type="button"
              className="mt-4 text-sm text-rose-600"
              onClick={() => removeOffering(index)}
            >
              删除该项
            </button>
          )}
        </div>
      ))}
      <button
        type="button"
        className="rounded border border-dashed border-slate-300 px-4 py-2 text-sm text-slate-700"
        onClick={addOffering}
      >
        + 添加核心产品/服务
      </button>
    </div>
  );
};
