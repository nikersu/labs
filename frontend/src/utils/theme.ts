export type Theme = 'dark' | 'light'

export const applyTheme = (theme: Theme) => {
  if (theme === 'light') {
    document.documentElement.classList.remove('dark')
  } else {
    document.documentElement.classList.add('dark')
  }
  localStorage.setItem('theme', theme)
}

export const getTheme = (): Theme => {
  const saved = localStorage.getItem('theme') as Theme | null
  return saved || 'dark'
}





