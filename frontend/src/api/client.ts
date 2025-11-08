import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

export interface DocumentMetadata {
  id: string;
  filename: string;
  contentType: string;
  size: number;
  uploadedAt: string;
}

export interface CompanyPayload {
  name: string;
  unifiedSocialCreditCode: string;
  establishmentDate: string;
  address: {
    country: string;
    province: string;
    city: string;
    district: string;
    streetAddress: string;
  };
  scale: string;
  industries: string[];
  companyType: string;
  businessOverview: string;
  coreOfferings: Array<{
    name: string;
    type: '产品' | '服务' | '方案';
    description: string;
  }>;
  technologyStack?: string[];
  intellectualProperties?: Array<{
    type: string;
    registrationNumber: string;
    description?: string;
  }>;
  contact: {
    name: string;
    title: string;
    phone: string;
    workEmail: string;
  };
  businessLicenseFileId?: string;
  attachmentFileIds?: string[];
}

export const uploadDocument = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post<DocumentMetadata>('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data;
};

export const createCompany = async (payload: CompanyPayload) => {
  const response = await apiClient.post('/companies', payload);
  return response.data;
};

export const matchCompany = async (query: string) => {
  const response = await apiClient.post('/companies/match', null, {
    params: { query }
  });
  return response.data;
};
