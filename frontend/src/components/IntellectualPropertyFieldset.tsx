import { CompanyPayload } from '../api/client';

interface Props {
  value?: CompanyPayload['intellectualProperties'];
  onChange: (value: CompanyPayload['intellectualProperties']) => void;
}

const createItem = () => ({ type: '', registrationNumber: '', description: '' });

export const IntellectualPropertyFieldset = ({ value, onChange }: Props) => {
  const items = value ?? [];

  const updateValue = (index: number, key: keyof ReturnType<typeof createItem>, newValue: string) => {
    const copy = items.map((item, idx) => (idx === index ? { ...item, [key]: newValue } : item));
    onChange(copy);
  };

  const addItem = () => onChange([...(items.length ? items : []), createItem()]);

  const removeItem = (index: number) => {
    const next = items.filter((_, idx) => idx !== index);
    onChange(next.length ? next : undefined);
  };

  return (
    <div className="space-y-6">
      {items.map((item, index) => (
        <div key={index} className="rounded border border-slate-200 bg-white p-4 shadow-sm">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <label className="space-y-1 text-sm">
              <span className="font-medium">类型</span>
              <input
                value={item.type}
                onChange={(event) => updateValue(index, 'type', event.target.value)}
                className="w-full rounded border border-slate-300 p-2"
                placeholder="专利/软著/商标"
              />
            </label>
            <label className="space-y-1 text-sm">
              <span className="font-medium">编号</span>
              <input
                value={item.registrationNumber}
                onChange={(event) => updateValue(index, 'registrationNumber', event.target.value)}
                className="w-full rounded border border-slate-300 p-2"
              />
            </label>
            <label className="space-y-1 text-sm md:col-span-1 md:col-start-3">
              <span className="font-medium">备注</span>
              <input
                value={item.description}
                onChange={(event) => updateValue(index, 'description', event.target.value)}
                className="w-full rounded border border-slate-300 p-2"
              />
            </label>
          </div>
          <button type="button" className="mt-4 text-sm text-rose-600" onClick={() => removeItem(index)}>
            删除知识产权
          </button>
        </div>
      ))}
      <button type="button" className="rounded border border-dashed border-slate-300 px-4 py-2 text-sm" onClick={addItem}>
        + 添加知识产权信息
      </button>
    </div>
  );
};
