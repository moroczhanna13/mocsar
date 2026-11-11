import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Fejlesztéskor a /api és a /ws a Spring Boot szerverre megy (8080).
// GitHub Pages-en a VITE_BASE a repó neve (pl. /mocsar/), a VITE_API_URL és VITE_WS_URL a Render szerver.
export default defineConfig({
  plugins: [react()],
  base: process.env.VITE_BASE ?? '/',
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
});
