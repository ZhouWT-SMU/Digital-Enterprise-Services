import { FormEvent, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { CompanyPayload, createCompany } from '../api/client';
import { CoreOfferingsFieldset } from './CoreOfferingsFieldset';
import { IntellectualPropertyFieldset } from './IntellectualPropertyFieldset';
import { FileUploadField } from './FileUploadField';

const createInitialForm = (): CompanyPayload => ({
  name: '',
  unifiedSocialCreditCode: '',
  establishmentDate: '',
  address: {
    country: '中国',
    province: '',
    city: '',
    district: '',
    streetAddress: ''
  },
  scale: '1-49',
  industries: [],
  companyType: '民营',
  businessOverview: '',
  coreOfferings: [
    {
      name: '',
      type: '产品',
      description: ''
    }
  ],
  technologyStack: [],
  contact: {
    name: '',
    title: '',
    phone: '',
    workEmail: ''
  }
});

const INDUSTRY_OPTIONS = ['软件和信息服务业', '制造业', '金融服务', '生命科学', '能源', '交通物流'];
const SCALE_OPTIONS = ['1-49', '50-149', '150-499', '500+'];
const COMPANY_TYPES = ['民营', '国企', '外资', '合资', '事业单位', '其他'];

export const CompanyForm = () => {
  const [form, setForm] = useState<CompanyPayload>(createInitialForm());
  const [attachments, setAttachments] = useState<string[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createCompany,
    onSuccess: () => {
      setMessage('企业信息提交成功，Dify 工作流已开始处理。');
      setForm(createInitialForm());
      setAttachments([]);
    },
    onError: (error: Error) => {
      setMessage(`提交失败：${error.message}`);
    }
  });

  const updateField = (path: string, value: unknown) => {
    setForm((prev) => {
      const clone = JSON.parse(JSON.stringify(prev)) as CompanyPayload;
      const keys = path.split('.');
      let cursor: any = clone;
      for (let i = 0; i < keys.length - 1; i += 1) {
        cursor = cursor[keys[i]];
      }
      cursor[keys[keys.length - 1]] = value;
      return clone;
    });
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    mutation.mutate({
      ...form,
      attachmentFileIds: attachments.length ? attachments : undefined
    });
  };

  return (
    <form className="space-y-10" onSubmit={handleSubmit}>
      <section className="rounded bg-white p-6 shadow">
        <h2 className="text-lg font-semibold text-slate-800">一、企业基础信息</h2>
        <div className="mt-4 grid grid-cols-1 gap-6 md:grid-cols-2">
          <label className="text-sm font-medium text-slate-700">
            企业名称
            <input
              value={form.name}
              onChange={(event) => updateField('name', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            统一社会信用代码
            <input
              value={form.unifiedSocialCreditCode}
              onChange={(event) => updateField('unifiedSocialCreditCode', event.target.value.toUpperCase())}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              placeholder="18位大写字母或数字"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            成立日期
            <input
              type="date"
              value={form.establishmentDate}
              onChange={(event) => updateField('establishmentDate', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            企业规模
            <select
              value={form.scale}
              onChange={(event) => updateField('scale', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
            >
              {SCALE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-4">
          <label className="text-sm font-medium text-slate-700">
            国家
            <input
              value={form.address.country}
              onChange={(event) => updateField('address.country', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            省份
            <input
              value={form.address.province}
              onChange={(event) => updateField('address.province', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            城市
            <input
              value={form.address.city}
              onChange={(event) => updateField('address.city', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            区/县
            <input
              value={form.address.district}
              onChange={(event) => updateField('address.district', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
        </div>
        <label className="mt-4 block text-sm font-medium text-slate-700">
          详细地址
          <input
            value={form.address.streetAddress}
            onChange={(event) => updateField('address.streetAddress', event.target.value)}
            className="mt-1 w-full rounded border border-slate-300 p-2"
            required
          />
        </label>
        <label className="mt-4 block text-sm font-medium text-slate-700">
          所属行业（可多选）
          <select
            multiple
            value={form.industries}
            onChange={(event) =>
              updateField(
                'industries',
                Array.from(event.target.selectedOptions, (option) => option.value)
              )
            }
            className="mt-1 h-32 w-full rounded border border-slate-300 p-2"
          >
            {INDUSTRY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <label className="mt-4 block text-sm font-medium text-slate-700">
          企业类型
          <select
            value={form.companyType}
            onChange={(event) => updateField('companyType', event.target.value)}
            className="mt-1 w-full rounded border border-slate-300 p-2"
          >
            {COMPANY_TYPES.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <div className="mt-6">
          <FileUploadField
            label="营业执照文件（PDF/JPG）"
            description="上传后将获得文件标识，可回填至工作流"
            onUploaded={(fileId) => updateField('businessLicenseFileId', fileId)}
          />
        </div>
      </section>

      <section className="rounded bg-white p-6 shadow">
        <h2 className="text-lg font-semibold text-slate-800">二、能力与业务画像</h2>
        <label className="mt-4 block text-sm font-medium text-slate-700">
          业务简介（500-1500字）
          <textarea
            value={form.businessOverview}
            onChange={(event) => updateField('businessOverview', event.target.value)}
            className="mt-1 w-full rounded border border-slate-300 p-2"
            rows={8}
            minLength={500}
            maxLength={1500}
            required
          />
        </label>
        <div className="mt-6">
          <CoreOfferingsFieldset
            value={form.coreOfferings}
            onChange={(value) => updateField('coreOfferings', value)}
          />
        </div>
        <label className="mt-6 block text-sm font-medium text-slate-700">
          技术栈（以逗号分隔）
          <input
            value={form.technologyStack?.join(', ') ?? ''}
            onChange={(event) =>
              updateField(
                'technologyStack',
                event.target.value
                  .split(',')
                  .map((item) => item.trim())
                  .filter(Boolean)
              )
            }
            className="mt-1 w-full rounded border border-slate-300 p-2"
            placeholder="Java, Spring Boot, MySQL, 阿里云"
          />
        </label>
        <div className="mt-6">
          <IntellectualPropertyFieldset
            value={form.intellectualProperties}
            onChange={(value) => updateField('intellectualProperties', value)}
          />
        </div>
        <div className="mt-6 space-y-4">
          <h3 className="text-sm font-semibold text-slate-700">其他附件</h3>
          <FileUploadField
            label="上传证照/报告等附件（PDF/DOCX/PNG/JPG）"
            onUploaded={(fileId) => setAttachments((prev) => [...prev, fileId])}
          />
          {attachments.length > 0 && (
            <ul className="text-sm text-slate-600">
              {attachments.map((id) => (
                <li key={id}>{id}</li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="rounded bg-white p-6 shadow">
        <h2 className="text-lg font-semibold text-slate-800">三、联系与法律</h2>
        <div className="mt-4 grid grid-cols-1 gap-6 md:grid-cols-2">
          <label className="text-sm font-medium text-slate-700">
            联系人姓名
            <input
              value={form.contact.name}
              onChange={(event) => updateField('contact.name', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            职务
            <input
              value={form.contact.title}
              onChange={(event) => updateField('contact.title', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            电话
            <input
              value={form.contact.phone}
              onChange={(event) => updateField('contact.phone', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700">
            工作邮箱
            <input
              type="email"
              value={form.contact.workEmail}
              onChange={(event) => updateField('contact.workEmail', event.target.value)}
              className="mt-1 w-full rounded border border-slate-300 p-2"
              required
            />
          </label>
        </div>
      </section>

      <div className="flex items-center justify-between">
        {message && <p className="text-sm text-slate-600">{message}</p>}
        <button
          type="submit"
          className="rounded bg-indigo-600 px-6 py-2 text-sm font-semibold text-white shadow"
          disabled={mutation.isPending}
        >
          {mutation.isPending ? '提交中...' : '提交企业信息'}
        </button>
      </div>
    </form>
  );
};
