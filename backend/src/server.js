import express from 'express';
import cors from 'cors';
import { matchEnterprise } from './services/matchingService.js';
import { notImplementedRouter } from './services/notImplemented.js';
import path from 'path';
import { fileURLToPath } from 'url';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendPath = path.resolve(__dirname, '../../frontend');
app.use(express.static(frontendPath));

app.post('/api/match-enterprise', async (req, res) => {
  try {
    const result = await matchEnterprise(req.body);
    res.json({ success: true, data: result });
  } catch (error) {
    console.error('Error during enterprise matching', error);
    res.status(500).json({
      success: false,
      message: 'Failed to process enterprise matching request.',
      details: error.message,
    });
  }
});

app.use('/api/patent-search', notImplementedRouter('Patent search workflow not implemented yet.'));
app.use('/api/company-search', notImplementedRouter('Company search workflow not implemented yet.'));

app.get('*', (req, res) => {
  res.sendFile(path.join(frontendPath, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});
