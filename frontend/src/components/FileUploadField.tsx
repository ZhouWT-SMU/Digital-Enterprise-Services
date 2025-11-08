import { ChangeEvent } from 'react';
import { useFileUploader } from '../hooks/useFileUploader';

interface Props {
  label: string;
  description?: string;
  onUploaded: (fileId: string) => void;
}

const ACCEPTED_TYPES = '.pdf,.docx,.png,.jpg,.jpeg';

export const FileUploadField = ({ label, description, onUploaded }: Props) => {
  const { state, error, document, onUpload } = useFileUploader();

  const handleChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    await onUpload(file).then((metadata) => onUploaded(metadata.id));
  };

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-slate-700">{label}</label>
      {description && <p className="text-sm text-slate-500">{description}</p>}
      <input
        type="file"
        accept={ACCEPTED_TYPES}
        onChange={handleChange}
        className="block w-full rounded border border-dashed border-slate-300 bg-white p-4 text-sm"
      />
      {state === 'uploading' && <p className="text-xs text-amber-600">上传中...</p>}
      {state === 'success' && document && <p className="text-xs text-emerald-600">已上传：{document.filename}</p>}
      {state === 'error' && <p className="text-xs text-rose-600">{error}</p>}
    </div>
  );
};
