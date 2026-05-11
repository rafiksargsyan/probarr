import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

function envScriptPlugin(): Plugin {
  return {
    name: 'env-script',
    apply: 'build',
    transformIndexHtml(html) {
      if (!process.env.DOCKER_BUILD) return html;
      return html.replace('<head>', `<head>\n    <script src="/env.js"></script>`);
    },
  };
}

export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
    envScriptPlugin(),
  ],
})
