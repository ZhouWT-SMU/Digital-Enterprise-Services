import { useState } from 'react';
import { DocumentMetadata, uploadDocument } from '../api/client';

type UploadState = 'idle' | 'uploading' | 'success' | 'error';

export const useFileUploader = () => {
  const [state, setState] = useState<UploadState>('idle');
  const [error, setError] = useState<string | null>(null);
  const [document, setDocument] = useState<DocumentMetadata | null>(null);

  const onUpload = async (file: File) => {
    setState('uploading');
    setError(null);
    try {
      const metadata = await uploadDocument(file);
      setDocument(metadata);
      setState('success');
      return metadata;
    } catch (err) {
      setError((err as Error).message);
      setState('error');
      throw err;
    }
  };

  return {
    state,
    error,
    document,
    onUpload
  };
};
