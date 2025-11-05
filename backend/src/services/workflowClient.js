import axios from 'axios';

/**
 * Placeholder Dify workflow client. Replace the implementation below once
 * your Dify workflow is ready and the dify-java-client integration details
 * are finalized.
 */
class DifyWorkflowClient {
  constructor({ baseURL, apiKey }) {
    this.http = axios.create({
      baseURL,
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
    });
  }

  async runWorkflow(payload) {
    const { data } = await this.http.post('/workflows/enterprise-matching:run', payload);
    return data;
  }
}

const baseURL = process.env.DIFY_BASE_URL;
const apiKey = process.env.DIFY_API_KEY;

export const difyClient = baseURL && apiKey ? new DifyWorkflowClient({ baseURL, apiKey }) : null;
export default DifyWorkflowClient;
