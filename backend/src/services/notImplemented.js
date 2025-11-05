import express from 'express';

export function notImplementedRouter(message) {
  const router = express.Router();
  router.all('*', (_req, res) => {
    res.status(501).json({
      success: false,
      message,
    });
  });
  return router;
}
