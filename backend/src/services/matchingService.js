import { difyClient } from './workflowClient.js';

const fallbackResponse = (payload) => ({
  matchedEnterprises: [],
  summary: 'Dify workflow integration pending. Please configure workflowClient.js to call your workflow.',
  echoRequest: payload,
});

export async function matchEnterprise(payload) {
  if (!difyClient) {
    return fallbackResponse(payload);
  }

  try {
    const response = await difyClient.runWorkflow(payload);
    return response;
  } catch (error) {
    console.warn('Dify workflow invocation failed, using fallback response.', error);
    return fallbackResponse(payload);
  }
}
