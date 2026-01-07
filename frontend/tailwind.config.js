/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Используем CSS переменные для поддержки темной/светлой темы
        dark: {
          bg: 'var(--color-bg)',
          surface: 'var(--color-surface)',
          surface2: 'var(--color-surface2)',
          text: 'var(--color-text)',
          text2: 'var(--color-text2)',
          primary: 'var(--color-primary)',
          primaryHover: 'var(--color-primary-hover)',
          error: 'var(--color-error)',
          success: 'var(--color-success)',
        },
      },
    },
  },
  plugins: [],
}

